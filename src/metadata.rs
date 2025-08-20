use crate::error::{PdftkError, Result};
use crate::pdf_document::PdfDocument;
use lopdf::{Object, Dictionary, Stream};
use chrono::{DateTime, Utc};
use std::collections::HashMap;

#[derive(Debug, Clone)]
pub struct Metadata {
    pub title: Option<String>,
    pub author: Option<String>,
    pub subject: Option<String>,
    pub keywords: Option<String>,
    pub creator: Option<String>,
    pub producer: Option<String>,
    pub creation_date: Option<DateTime<Utc>>,
    pub modification_date: Option<DateTime<Utc>>,
    pub custom: HashMap<String, String>,
}

impl Default for Metadata {
    fn default() -> Self {
        Metadata {
            title: None,
            author: None,
            subject: None,
            keywords: None,
            creator: None,
            producer: Some("pdftk-rs".to_string()),
            creation_date: None,
            modification_date: None,
            custom: HashMap::new(),
        }
    }
}

impl Metadata {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn from_pdf(doc: &PdfDocument) -> Result<Self> {
        let mut metadata = Metadata::new();
        
        if let Some(info_id) = doc.document.trailer.get(b"Info")
            .ok()
            .and_then(|obj| obj.as_reference().ok()) {
            
            if let Ok(Object::Dictionary(info)) = doc.document.get_object(info_id) {
                metadata.title = Self::extract_string(info, b"Title");
                metadata.author = Self::extract_string(info, b"Author");
                metadata.subject = Self::extract_string(info, b"Subject");
                metadata.keywords = Self::extract_string(info, b"Keywords");
                metadata.creator = Self::extract_string(info, b"Creator");
                metadata.producer = Self::extract_string(info, b"Producer");
                metadata.creation_date = Self::extract_date(info, b"CreationDate");
                metadata.modification_date = Self::extract_date(info, b"ModDate");
            }
        }
        
        Ok(metadata)
    }

    fn extract_string(dict: &Dictionary, key: &[u8]) -> Option<String> {
        dict.get(key)
            .ok()
            .and_then(|obj| {
                if let Object::String(s, _) = obj {
                    Some(String::from_utf8_lossy(s).to_string())
                } else {
                    None
                }
            })
    }

    fn extract_date(dict: &Dictionary, key: &[u8]) -> Option<DateTime<Utc>> {
        Self::extract_string(dict, key)
            .and_then(|s| Self::parse_pdf_date(&s))
    }

    fn parse_pdf_date(date_str: &str) -> Option<DateTime<Utc>> {
        if date_str.starts_with("D:") {
            let date_str = &date_str[2..];
            
            if date_str.len() >= 14 {
                let year = date_str[0..4].parse::<i32>().ok()?;
                let month = date_str[4..6].parse::<u32>().ok()?;
                let day = date_str[6..8].parse::<u32>().ok()?;
                let hour = date_str[8..10].parse::<u32>().ok()?;
                let minute = date_str[10..12].parse::<u32>().ok()?;
                let second = date_str[12..14].parse::<u32>().ok()?;
                
                use chrono::NaiveDate;
                let naive_date = NaiveDate::from_ymd_opt(year, month, day)?;
                let naive_datetime = naive_date.and_hms_opt(hour, minute, second)?;
                
                return Some(DateTime::from_naive_utc_and_offset(naive_datetime, Utc));
            }
        }
        None
    }

    fn format_pdf_date(date: &DateTime<Utc>) -> String {
        format!("D:{}", date.format("%Y%m%d%H%M%S"))
    }

    pub fn apply_to_pdf(&self, doc: &mut PdfDocument) -> Result<()> {
        let mut info = Dictionary::new();
        
        if let Some(ref title) = self.title {
            info.set("Title", Object::String(title.clone().into_bytes(), lopdf::StringFormat::Literal));
        }
        if let Some(ref author) = self.author {
            info.set("Author", Object::String(author.clone().into_bytes(), lopdf::StringFormat::Literal));
        }
        if let Some(ref subject) = self.subject {
            info.set("Subject", Object::String(subject.clone().into_bytes(), lopdf::StringFormat::Literal));
        }
        if let Some(ref keywords) = self.keywords {
            info.set("Keywords", Object::String(keywords.clone().into_bytes(), lopdf::StringFormat::Literal));
        }
        if let Some(ref creator) = self.creator {
            info.set("Creator", Object::String(creator.clone().into_bytes(), lopdf::StringFormat::Literal));
        }
        if let Some(ref producer) = self.producer {
            info.set("Producer", Object::String(producer.clone().into_bytes(), lopdf::StringFormat::Literal));
        }
        if let Some(ref creation_date) = self.creation_date {
            let date_str = Self::format_pdf_date(creation_date);
            info.set("CreationDate", Object::String(date_str.into_bytes(), lopdf::StringFormat::Literal));
        }
        if let Some(ref mod_date) = self.modification_date {
            let date_str = Self::format_pdf_date(mod_date);
            info.set("ModDate", Object::String(date_str.into_bytes(), lopdf::StringFormat::Literal));
        }
        
        for (key, value) in &self.custom {
            info.set(key.as_bytes(), Object::String(value.clone().into_bytes(), lopdf::StringFormat::Literal));
        }
        
        let info_id = doc.document.add_object(Object::Dictionary(info));
        doc.document.trailer.set("Info", Object::Reference(info_id));
        
        Ok(())
    }

    pub fn to_text(&self) -> String {
        let mut output = String::new();
        
        if let Some(ref title) = self.title {
            output.push_str(&format!("Title: {}\n", title));
        }
        if let Some(ref author) = self.author {
            output.push_str(&format!("Author: {}\n", author));
        }
        if let Some(ref subject) = self.subject {
            output.push_str(&format!("Subject: {}\n", subject));
        }
        if let Some(ref keywords) = self.keywords {
            output.push_str(&format!("Keywords: {}\n", keywords));
        }
        if let Some(ref creator) = self.creator {
            output.push_str(&format!("Creator: {}\n", creator));
        }
        if let Some(ref producer) = self.producer {
            output.push_str(&format!("Producer: {}\n", producer));
        }
        if let Some(ref creation_date) = self.creation_date {
            output.push_str(&format!("CreationDate: {}\n", creation_date));
        }
        if let Some(ref mod_date) = self.modification_date {
            output.push_str(&format!("ModDate: {}\n", mod_date));
        }
        
        for (key, value) in &self.custom {
            output.push_str(&format!("{}: {}\n", key, value));
        }
        
        output
    }

    pub fn from_text(text: &str) -> Result<Self> {
        let mut metadata = Metadata::new();
        
        for line in text.lines() {
            if let Some(colon_pos) = line.find(':') {
                let key = line[..colon_pos].trim();
                let value = line[colon_pos + 1..].trim();
                
                match key {
                    "Title" => metadata.title = Some(value.to_string()),
                    "Author" => metadata.author = Some(value.to_string()),
                    "Subject" => metadata.subject = Some(value.to_string()),
                    "Keywords" => metadata.keywords = Some(value.to_string()),
                    "Creator" => metadata.creator = Some(value.to_string()),
                    "Producer" => metadata.producer = Some(value.to_string()),
                    "CreationDate" => {
                        if let Ok(date) = DateTime::parse_from_rfc3339(value) {
                            metadata.creation_date = Some(date.with_timezone(&Utc));
                        }
                    }
                    "ModDate" | "ModificationDate" => {
                        if let Ok(date) = DateTime::parse_from_rfc3339(value) {
                            metadata.modification_date = Some(date.with_timezone(&Utc));
                        }
                    }
                    _ => {
                        metadata.custom.insert(key.to_string(), value.to_string());
                    }
                }
            }
        }
        
        Ok(metadata)
    }
}

pub fn update_xmp_metadata(doc: &mut PdfDocument, xmp_data: &str) -> Result<()> {
    let xmp_bytes = xmp_data.as_bytes().to_vec();
    
    let mut xmp_stream = Stream::new(Dictionary::new(), xmp_bytes.clone());
    xmp_stream.dict.set("Type", Object::Name(b"Metadata".to_vec()));
    xmp_stream.dict.set("Subtype", Object::Name(b"XML".to_vec()));
    xmp_stream.dict.set("Length", Object::Integer(xmp_bytes.len() as i64));
    
    let xmp_id = doc.document.add_object(Object::Stream(xmp_stream));
    
    let catalog_id = doc.document.trailer.get(b"Root")
        .ok()
        .and_then(|r| r.as_reference().ok())
        .ok_or_else(|| PdftkError::PdfParse("No catalog found".to_string()))?;
    if let Ok(Object::Dictionary(cat_dict)) = doc.document.get_object_mut(catalog_id) {
        cat_dict.set("Metadata", Object::Reference(xmp_id));
    }
    
    Ok(())
}

pub fn extract_xmp_metadata(doc: &PdfDocument) -> Result<Option<String>> {
    if let Ok(cat_dict) = doc.document.catalog() {
        if let Ok(Object::Reference(xmp_ref)) = cat_dict.get(b"Metadata") {
            if let Ok(Object::Stream(stream)) = doc.document.get_object(*xmp_ref) {
                return Ok(Some(String::from_utf8_lossy(&stream.content).to_string()));
            }
        }
    }
    
    Ok(None)
}