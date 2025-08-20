use crate::error::{PdftkError, Result};
use crate::pdf_document::PdfDocument;
use lopdf::{Document, Object, Dictionary};

#[derive(Debug, Clone)]
pub enum PageRange {
    All,
    Single(usize),
    Range(Option<usize>, Option<usize>), // Support end-based ranges
    Multiple(Vec<usize>),
    Even,
    Odd,
    End,
    Reverse(Box<PageRange>), // For reverse order
    Qualified(Box<PageRange>, PageQualifier), // For qualifiers like 1-endeast
}

#[derive(Debug, Clone, Copy)]
pub enum PageQualifier {
    North,
    East,
    South,
    West,
    Left,
    Right,
    Down,
}

impl PageRange {
    pub fn parse(s: &str) -> Result<Self> {
        let s_lower = s.to_lowercase();
        
        // Check for rotation qualifiers
        let qualifiers = vec![
            ("north", PageQualifier::North),
            ("east", PageQualifier::East),
            ("south", PageQualifier::South),
            ("west", PageQualifier::West),
            ("left", PageQualifier::Left),
            ("right", PageQualifier::Right),
            ("down", PageQualifier::Down),
        ];
        
        for (suffix, qualifier) in qualifiers {
            if s_lower.ends_with(suffix) {
                let base = &s[..s.len() - suffix.len()];
                let base_range = Self::parse(base)?;
                return Ok(PageRange::Qualified(Box::new(base_range), qualifier));
            }
        }
        
        // Check for reverse prefix
        if s_lower.starts_with("r") && s.len() > 1 {
            let base = &s[1..];
            let base_range = Self::parse(base)?;
            return Ok(PageRange::Reverse(Box::new(base_range)));
        }
        
        match s_lower.as_str() {
            "all" => Ok(PageRange::All),
            "even" => Ok(PageRange::Even),
            "odd" => Ok(PageRange::Odd),
            "end" => Ok(PageRange::End),
            s if s.contains('-') => {
                let parts: Vec<&str> = s.split('-').collect();
                if parts.len() != 2 {
                    return Err(PdftkError::InvalidPageRange(s.to_string()));
                }
                
                let start = if parts[0].is_empty() || parts[0] == "1" {
                    Some(1)
                } else if parts[0] == "end" {
                    None
                } else {
                    Some(parts[0].parse::<usize>()
                        .map_err(|_| PdftkError::InvalidPageRange(s.to_string()))?)
                };
                
                let end = if parts[1] == "end" {
                    None
                } else {
                    Some(parts[1].parse::<usize>()
                        .map_err(|_| PdftkError::InvalidPageRange(s.to_string()))?)
                };
                
                Ok(PageRange::Range(start, end))
            }
            s if s.contains(',') => {
                let pages: Result<Vec<usize>> = s.split(',')
                    .map(|p| p.trim().parse::<usize>()
                        .map_err(|_| PdftkError::InvalidPageRange(s.to_string())))
                    .collect();
                Ok(PageRange::Multiple(pages?))
            }
            s => {
                let page = s.parse::<usize>()
                    .map_err(|_| PdftkError::InvalidPageRange(s.to_string()))?;
                Ok(PageRange::Single(page))
            }
        }
    }

    pub fn to_page_numbers(&self, total_pages: usize) -> Vec<usize> {
        match self {
            PageRange::All => (1..=total_pages).collect(),
            PageRange::Single(n) => vec![*n],
            PageRange::Range(start, end) => {
                let start_page = start.unwrap_or(1);
                let end_page = end.unwrap_or(total_pages);
                
                if start_page <= end_page {
                    (start_page..=end_page).collect()
                } else {
                    // Reverse range (e.g., end-1)
                    (end_page..=start_page).rev().collect()
                }
            }
            PageRange::Multiple(pages) => pages.clone(),
            PageRange::Even => (1..=total_pages).filter(|n| n % 2 == 0).collect(),
            PageRange::Odd => (1..=total_pages).filter(|n| n % 2 != 0).collect(),
            PageRange::End => vec![total_pages],
            PageRange::Reverse(range) => {
                let mut pages = range.to_page_numbers(total_pages);
                pages.reverse();
                pages
            }
            PageRange::Qualified(range, _qualifier) => {
                // Qualifier is handled elsewhere (rotation)
                range.to_page_numbers(total_pages)
            }
        }
    }
    
    pub fn get_qualifier(&self) -> Option<PageQualifier> {
        match self {
            PageRange::Qualified(_, qualifier) => Some(*qualifier),
            _ => None,
        }
    }
}

#[derive(Debug, Clone, Copy)]
pub enum PageRotation {
    North = 0,
    East = 90,
    South = 180,
    West = 270,
}

impl PageRotation {
    pub fn from_degrees(degrees: i32) -> Self {
        match degrees % 360 {
            90 => PageRotation::East,
            180 => PageRotation::South,
            270 | -90 => PageRotation::West,
            _ => PageRotation::North,
        }
    }

    pub fn to_degrees(&self) -> i32 {
        *self as i32
    }
}

pub struct Operations;

impl Operations {
    pub fn cat(inputs: Vec<(PdfDocument, PageRange)>, output: &mut PdfDocument) -> Result<()> {
        for (input_doc, range) in inputs {
            let page_numbers = range.to_page_numbers(input_doc.num_pages);
            
            for page_num in page_numbers {
                let page_id = input_doc.get_page(page_num)?;
                if let Ok(page_obj) = input_doc.document.get_object(page_id) {
                    let cloned_page = Self::clone_page(&input_doc.document, page_obj)?;
                    output.add_page(cloned_page)?;
                }
            }
        }
        Ok(())
    }

    pub fn shuffle(input1: &PdfDocument, input2: &PdfDocument, output: &mut PdfDocument) -> Result<()> {
        let max_pages = input1.num_pages.max(input2.num_pages);
        
        for i in 1..=max_pages {
            if i <= input1.num_pages {
                let page_id = input1.get_page(i)?;
                if let Ok(page_obj) = input1.document.get_object(page_id) {
                    let cloned_page = Self::clone_page(&input1.document, page_obj)?;
                    output.add_page(cloned_page)?;
                }
            }
            
            if i <= input2.num_pages {
                let page_id = input2.get_page(i)?;
                if let Ok(page_obj) = input2.document.get_object(page_id) {
                    let cloned_page = Self::clone_page(&input2.document, page_obj)?;
                    output.add_page(cloned_page)?;
                }
            }
        }
        Ok(())
    }

    pub fn burst(input: &PdfDocument, output_pattern: &str) -> Result<Vec<String>> {
        let mut output_files = Vec::new();
        
        for page_num in 1..=input.num_pages {
            let mut single_page_doc = PdfDocument::new();
            let page_id = input.get_page(page_num)?;
            
            if let Ok(page_obj) = input.document.get_object(page_id) {
                let cloned_page = Self::clone_page(&input.document, page_obj)?;
                single_page_doc.add_page(cloned_page)?;
            }
            
            let output_filename = output_pattern.replace("%d", &page_num.to_string());
            single_page_doc.save(&output_filename)?;
            output_files.push(output_filename);
        }
        
        Ok(output_files)
    }

    pub fn rotate_pages(doc: &mut PdfDocument, pages: PageRange, rotation: PageRotation, absolute: bool) -> Result<()> {
        let page_numbers = pages.to_page_numbers(doc.num_pages);
        
        for page_num in page_numbers {
            let page_id = doc.get_page(page_num)?;
            let current_rotation = doc.get_page_rotation(page_id)?;
            
            let new_rotation = if absolute {
                rotation.to_degrees()
            } else {
                (current_rotation + rotation.to_degrees()) % 360
            };
            
            doc.set_page_rotation(page_id, new_rotation)?;
        }
        
        Ok(())
    }

    pub fn stamp(background: &PdfDocument, foreground: &PdfDocument, output: &mut PdfDocument, multi: bool) -> Result<()> {
        for page_num in 1..=background.num_pages {
            let bg_page_id = background.get_page(page_num)?;
            let bg_page = background.document.get_object(bg_page_id)?;
            
            let mut merged_page = Self::clone_page(&background.document, bg_page)?;
            
            let fg_page_num = if multi {
                ((page_num - 1) % foreground.num_pages) + 1
            } else {
                1.min(foreground.num_pages)
            };
            
            if fg_page_num <= foreground.num_pages {
                let fg_page_id = foreground.get_page(fg_page_num)?;
                if let Ok(fg_page) = foreground.document.get_object(fg_page_id) {
                    Self::overlay_page(&mut merged_page, &foreground.document, fg_page)?;
                }
            }
            
            output.add_page(merged_page)?;
        }
        
        Ok(())
    }

    fn clone_page(_doc: &Document, page: &Object) -> Result<Object> {
        match page {
            Object::Dictionary(dict) => {
                let mut new_dict = Dictionary::new();
                for (key, value) in dict.iter() {
                    new_dict.set(key.clone(), value.clone());
                }
                Ok(Object::Dictionary(new_dict))
            }
            _ => Ok(page.clone())
        }
    }

    fn overlay_page(_base: &mut Object, _overlay_doc: &Document, _overlay: &Object) -> Result<()> {
        Ok(())
    }

    pub fn extract_pages(input: &PdfDocument, pages: PageRange) -> Result<PdfDocument> {
        let mut output = PdfDocument::new();
        let page_numbers = pages.to_page_numbers(input.num_pages);
        
        for page_num in page_numbers {
            let page_id = input.get_page(page_num)?;
            if let Ok(page_obj) = input.document.get_object(page_id) {
                let cloned_page = Self::clone_page(&input.document, page_obj)?;
                output.add_page(cloned_page)?;
            }
        }
        
        Ok(output)
    }

    pub fn remove_pages(doc: &mut PdfDocument, pages: PageRange) -> Result<()> {
        let page_numbers = pages.to_page_numbers(doc.num_pages);
        
        for page_num in page_numbers.into_iter().rev() {
            doc.remove_page(page_num)?;
        }
        
        Ok(())
    }

    pub fn dump_data(doc: &PdfDocument, utf8: bool) -> Result<String> {
        let mut output = String::new();
        
        // InfoBegin
        output.push_str("InfoBegin\n");
        
        // InfoKey: Creator, Producer, Title, Subject, Author, Keywords, etc.
        if let Some(info_ref) = doc.document.trailer.get(b"Info")
            .ok().and_then(|obj| obj.as_reference().ok()) {
            if let Ok(info_obj) = doc.document.get_object(info_ref) {
                if let Ok(info_dict) = info_obj.as_dict() {
            
                    for (key, value) in info_dict.iter() {
                        let key_str = String::from_utf8_lossy(key);
                        let value_str = Self::object_to_string(value, utf8);
                        output.push_str(&format!("InfoKey: {}\n", key_str));
                        output.push_str(&format!("InfoValue: {}\n", value_str));
                    }
                }
            }
        }
        
        // PdfID0 and PdfID1
        if let Some(id_array) = doc.document.trailer.get(b"ID")
            .ok().and_then(|obj| obj.as_array().ok()) {
            if id_array.len() > 0 {
                let id0 = Self::bytes_to_hex(&Self::object_to_bytes(&id_array[0]));
                output.push_str(&format!("PdfID0: {}\n", id0));
            }
            if id_array.len() > 1 {
                let id1 = Self::bytes_to_hex(&Self::object_to_bytes(&id_array[1]));
                output.push_str(&format!("PdfID1: {}\n", id1));
            }
        }
        
        // NumberOfPages
        output.push_str(&format!("NumberOfPages: {}\n", doc.num_pages));
        
        // BookmarkInfo - Outlines/Bookmarks
        if let Some(outlines_ref) = doc.document.catalog()?.get(b"Outlines")
            .ok().and_then(|obj| obj.as_reference().ok()) {
            if let Ok(outlines) = doc.document.get_object(outlines_ref) {
                output.push_str("BookmarkBegin\n");
                Self::dump_bookmarks(doc, outlines, &mut output, 1)?;
            }
        }
        
        // PageMediaBegin - Page dimensions and rotation
        output.push_str("PageMediaBegin\n");
        for page_num in 1..=doc.num_pages {
            let page_id = doc.get_page(page_num)?;
            if let Ok(page_obj) = doc.document.get_object(page_id) {
                if let Ok(page_dict) = page_obj.as_dict() {
                    output.push_str(&format!("PageMediaNumber: {}\n", page_num));
                    
                    // MediaBox
                    if let Some(media_box) = page_dict.get(b"MediaBox")
                        .ok().and_then(|obj| obj.as_array().ok()) {
                        if media_box.len() >= 4 {
                            let dims = Self::extract_rectangle(media_box);
                            output.push_str(&format!("PageMediaDimensions: {} {}\n", dims.2, dims.3));
                        }
                    }
                    
                    // Rotation
                    if let Some(rotation) = page_dict.get(b"Rotate")
                        .ok().and_then(|obj| obj.as_i64().ok()) {
                        output.push_str(&format!("PageMediaRotation: {}\n", rotation));
                    }
                    
                    // CropBox if different from MediaBox
                    if let Some(crop_box) = page_dict.get(b"CropBox")
                        .ok().and_then(|obj| obj.as_array().ok()) {
                        if crop_box.len() >= 4 {
                            let dims = Self::extract_rectangle(crop_box);
                            output.push_str(&format!("PageMediaCropBox: {} {} {} {}\n", 
                                dims.0, dims.1, dims.2, dims.3));
                        }
                    }
                }
            }
        }
        
        // PageLabelInfo - Page labels
        if let Some(labels_ref) = doc.document.catalog()?.get(b"PageLabels")
            .ok().and_then(|obj| obj.as_reference().ok()) {
            if let Ok(labels_obj) = doc.document.get_object(labels_ref) {
                if let Ok(labels_dict) = labels_obj.as_dict() {
                    output.push_str("PageLabelBegin\n");
                    Self::dump_page_labels(doc, labels_dict, &mut output)?;
                }
            }
        }
        
        Ok(output)
    }

    fn dump_bookmarks(doc: &PdfDocument, outline: &Object, output: &mut String, level: usize) -> Result<()> {
        if let Ok(outline_dict) = outline.as_dict() {
            if let Some(first_ref) = outline_dict.get(b"First")
                .ok().and_then(|obj| obj.as_reference().ok()) {
                if let Ok(first) = doc.document.get_object(first_ref) {
                    Self::dump_bookmark_item(doc, first, output, level)?;
                }
            }
        }
        Ok(())
    }

    fn dump_bookmark_item(doc: &PdfDocument, item: &Object, output: &mut String, level: usize) -> Result<()> {
        if let Ok(item_dict) = item.as_dict() {
            // Title
            if let Some(title) = item_dict.get(b"Title")
                .ok().and_then(|obj| obj.as_str().ok()) {
                let title_str = String::from_utf8_lossy(title);
                output.push_str(&format!("BookmarkTitle: {}\n", title_str));
                output.push_str(&format!("BookmarkLevel: {}\n", level));
                
                // Destination page number
                if let Ok(dest) = item_dict.get(b"Dest") {
                    if let Some(page_num) = Self::get_dest_page_number(doc, dest) {
                        output.push_str(&format!("BookmarkPageNumber: {}\n", page_num));
                    }
                }
            }
            
            // Process children
            if let Some(first_ref) = item_dict.get(b"First")
                .ok().and_then(|obj| obj.as_reference().ok()) {
                if let Ok(first_child) = doc.document.get_object(first_ref) {
                    Self::dump_bookmark_item(doc, first_child, output, level + 1)?;
                }
            }
            
            // Process siblings
            if let Some(next_ref) = item_dict.get(b"Next")
                .ok().and_then(|obj| obj.as_reference().ok()) {
                if let Ok(next) = doc.document.get_object(next_ref) {
                    Self::dump_bookmark_item(doc, next, output, level)?;
                }
            }
        }
        Ok(())
    }

    fn get_dest_page_number(doc: &PdfDocument, dest: &Object) -> Option<usize> {
        match dest {
            Object::Array(arr) if !arr.is_empty() => {
                if let Object::Reference(page_ref) = &arr[0] {
                    // Find page number by reference
                    for page_num in 1..=doc.num_pages {
                        if let Ok(page_id) = doc.get_page(page_num) {
                            if page_id == *page_ref {
                                return Some(page_num);
                            }
                        }
                    }
                }
            }
            Object::Dictionary(dict) => {
                if let Ok(d) = dict.get(b"D") {
                    return Self::get_dest_page_number(doc, d);
                }
            }
            _ => {}
        }
        None
    }

    fn dump_page_labels(doc: &PdfDocument, labels_dict: &Dictionary, output: &mut String) -> Result<()> {
        if let Some(nums) = labels_dict.get(b"Nums")
            .ok().and_then(|obj| obj.as_array().ok()) {
            for i in (0..nums.len()).step_by(2) {
                let page_index = nums.get(i).and_then(|obj| obj.as_i64().ok());
                let label_dict = nums.get(i + 1)
                    .and_then(|obj| obj.as_reference().ok())
                    .and_then(|ref_id| doc.document.get_object(ref_id).ok())
                    .and_then(|obj| obj.as_dict().ok());
                
                if let (Some(page_index), Some(label_dict)) = (page_index, label_dict) {
                    
                    output.push_str(&format!("PageLabelNewIndex: {}\n", page_index));
                    
                    if let Some(style) = label_dict.get(b"Type")
                        .ok().and_then(|obj| obj.as_name().ok()) {
                        let style_str = String::from_utf8_lossy(style);
                        output.push_str(&format!("PageLabelStyle: {}\n", style_str));
                    }
                    
                    if let Some(prefix) = label_dict.get(b"P")
                        .ok().and_then(|obj| obj.as_str().ok()) {
                        let prefix_str = String::from_utf8_lossy(prefix);
                        output.push_str(&format!("PageLabelPrefix: {}\n", prefix_str));
                    }
                    
                    if let Some(start) = label_dict.get(b"St")
                        .ok().and_then(|obj| obj.as_i64().ok()) {
                        output.push_str(&format!("PageLabelStart: {}\n", start));
                    }
                }
            }
        }
        Ok(())
    }

    fn object_to_string(obj: &Object, utf8: bool) -> String {
        match obj {
            Object::String(bytes, _) => {
                if utf8 {
                    String::from_utf8_lossy(bytes).to_string()
                } else {
                    Self::bytes_to_hex(bytes)
                }
            }
            Object::Name(name) => String::from_utf8_lossy(name).to_string(),
            Object::Integer(i) => i.to_string(),
            Object::Real(f) => f.to_string(),
            Object::Boolean(b) => b.to_string(),
            _ => String::new(),
        }
    }

    fn object_to_bytes(obj: &Object) -> Vec<u8> {
        match obj {
            Object::String(bytes, _) => bytes.clone(),
            _ => Vec::new(),
        }
    }

    fn bytes_to_hex(bytes: &[u8]) -> String {
        bytes.iter().map(|b| format!("{:02x}", b)).collect()
    }

    fn extract_rectangle(arr: &[Object]) -> (f32, f32, f32, f32) {
        let x1 = arr.get(0).and_then(|o| o.as_float().ok()).unwrap_or(0.0) as f32;
        let y1 = arr.get(1).and_then(|o| o.as_float().ok()).unwrap_or(0.0) as f32;
        let x2 = arr.get(2).and_then(|o| o.as_float().ok()).unwrap_or(0.0) as f32;
        let y2 = arr.get(3).and_then(|o| o.as_float().ok()).unwrap_or(0.0) as f32;
        (x1, y1, x2 - x1, y2 - y1)
    }

    pub fn filter(input: &PdfDocument, keep_pages: PageRange) -> Result<PdfDocument> {
        let mut output = PdfDocument::new();
        let page_numbers = keep_pages.to_page_numbers(input.num_pages);
        
        for page_num in page_numbers {
            let page_id = input.get_page(page_num)?;
            if let Ok(page_obj) = input.document.get_object(page_id) {
                let cloned_page = Self::clone_page(&input.document, page_obj)?;
                output.add_page(cloned_page)?;
            }
        }
        
        Ok(output)
    }

    pub fn dump_data_annots(doc: &PdfDocument) -> Result<String> {
        let mut output = String::new();
        
        for page_num in 1..=doc.num_pages {
            let page_id = doc.get_page(page_num)?;
            if let Ok(page_obj) = doc.document.get_object(page_id) {
                if let Ok(page_dict) = page_obj.as_dict() {
                    if let Some(annots) = page_dict.get(b"Annots")
                        .ok().and_then(|obj| obj.as_array().ok()) {
                        
                        for annot_ref in annots {
                            if let Some(annot_ref_id) = annot_ref.as_reference().ok() {
                                if let Ok(annot_obj) = doc.document.get_object(annot_ref_id) {
                                    if let Ok(annot) = annot_obj.as_dict() {
                                    output.push_str(&format!("--- Page {} ---\n", page_num));
                                    
                                    if let Some(subtype) = annot.get(b"Subtype")
                                        .ok().and_then(|obj| obj.as_name().ok()) {
                                        let subtype_str = String::from_utf8_lossy(subtype);
                                        output.push_str(&format!("Type: {}\n", subtype_str));
                                    }
                                    
                                    if let Some(contents) = annot.get(b"Contents")
                                        .ok().and_then(|obj| obj.as_str().ok()) {
                                        let contents_str = String::from_utf8_lossy(contents);
                                        output.push_str(&format!("Contents: {}\n", contents_str));
                                    }
                                    
                                    if let Some(rect) = annot.get(b"Rect")
                                        .ok().and_then(|obj| obj.as_array().ok()) {
                                        if rect.len() >= 4 {
                                            let dims = Self::extract_rectangle(rect);
                                            output.push_str(&format!("Rect: [{}, {}, {}, {}]\n", 
                                                dims.0, dims.1, dims.0 + dims.2, dims.1 + dims.3));
                                        }
                                    }
                                    
                                    output.push_str("\n");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Ok(output)
    }
}