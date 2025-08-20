use crate::error::{PdftkError, Result};
use crate::pdf_document::PdfDocument;
use lopdf::{Object, Dictionary, Stream};
use std::path::Path;
use std::fs;
use chrono::{DateTime, Utc};

#[derive(Debug, Clone)]
pub struct Attachment {
    pub filename: String,
    pub description: Option<String>,
    pub data: Vec<u8>,
    pub mime_type: String,
    pub creation_date: Option<DateTime<Utc>>,
    pub modification_date: Option<DateTime<Utc>>,
}

impl Attachment {
    pub fn from_file<P: AsRef<Path>>(path: P) -> Result<Self> {
        let path = path.as_ref();
        
        if !path.exists() {
            return Err(PdftkError::FileNotFound(path.display().to_string()));
        }
        
        let filename = path.file_name()
            .and_then(|n| n.to_str())
            .ok_or_else(|| PdftkError::InvalidAttachment)?
            .to_string();
        
        let data = fs::read(path)?;
        
        let mime_type = Self::guess_mime_type(&filename);
        
        let metadata = fs::metadata(path)?;
        let modification_date = metadata.modified()
            .ok()
            .map(|t| DateTime::from(t));
        
        Ok(Attachment {
            filename,
            description: None,
            data,
            mime_type,
            creation_date: None,
            modification_date,
        })
    }
    
    fn guess_mime_type(filename: &str) -> String {
        let ext = filename.rsplit('.').next().unwrap_or("").to_lowercase();
        
        match ext.as_str() {
            "pdf" => "application/pdf",
            "txt" => "text/plain",
            "html" | "htm" => "text/html",
            "xml" => "text/xml",
            "jpg" | "jpeg" => "image/jpeg",
            "png" => "image/png",
            "gif" => "image/gif",
            "doc" => "application/msword",
            "docx" => "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls" => "application/vnd.ms-excel",
            "xlsx" => "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "zip" => "application/zip",
            _ => "application/octet-stream",
        }.to_string()
    }
    
    pub fn to_pdf_object(&self, doc: &mut PdfDocument) -> Result<lopdf::ObjectId> {
        let mut file_spec = Dictionary::new();
        file_spec.set("Type", Object::Name(b"Filespec".to_vec()));
        file_spec.set("F", Object::String(self.filename.clone().into_bytes(), lopdf::StringFormat::Literal));
        
        if let Some(ref desc) = self.description {
            file_spec.set("Desc", Object::String(desc.clone().into_bytes(), lopdf::StringFormat::Literal));
        }
        
        let mut ef_dict = Dictionary::new();
        
        let mut stream = Stream::new(Dictionary::new(), self.data.clone());
        stream.dict.set("Type", Object::Name(b"EmbeddedFile".to_vec()));
        stream.dict.set("Subtype", Object::Name(self.mime_type.clone().into_bytes()));
        stream.dict.set("Length", Object::Integer(self.data.len() as i64));
        
        let stream_id = doc.document.add_object(Object::Stream(stream));
        ef_dict.set("F", Object::Reference(stream_id));
        
        file_spec.set("EF", Object::Dictionary(ef_dict));
        
        let file_spec_id = doc.document.add_object(Object::Dictionary(file_spec));
        
        Ok(file_spec_id)
    }
}

pub fn attach_files(doc: &mut PdfDocument, attachments: Vec<Attachment>) -> Result<()> {
    let mut names_array = Vec::new();
    
    for attachment in attachments {
        let file_spec_id = attachment.to_pdf_object(doc)?;
        
        names_array.push(Object::String(attachment.filename.clone().into_bytes(), lopdf::StringFormat::Literal));
        names_array.push(Object::Reference(file_spec_id));
    }
    
    if !names_array.is_empty() {
        let mut names_dict = Dictionary::new();
        names_dict.set("Names", Object::Array(names_array));
        let names_id = doc.document.add_object(Object::Dictionary(names_dict));
        
        let mut ef_dict = Dictionary::new();
        ef_dict.set("EmbeddedFiles", Object::Reference(names_id));
        let ef_id = doc.document.add_object(Object::Dictionary(ef_dict));
        
        let catalog_id = doc.document.trailer.get(b"Root")
            .ok()
            .and_then(|r| r.as_reference().ok())
            .ok_or_else(|| PdftkError::InvalidOperation("No catalog found".to_string()))?;
        if let Ok(Object::Dictionary(cat_dict)) = doc.document.get_object_mut(catalog_id) {
            cat_dict.set("Names", Object::Reference(ef_id));
        }
    }
    
    Ok(())
}

pub fn attach_file_to_page(doc: &mut PdfDocument, page_num: usize, attachment: Attachment) -> Result<()> {
    let page_id = doc.get_page(page_num)?;
    let file_spec_id = attachment.to_pdf_object(doc)?;
    
    let mut annot_dict = Dictionary::new();
    annot_dict.set("Type", Object::Name(b"Annot".to_vec()));
    annot_dict.set("Subtype", Object::Name(b"FileAttachment".to_vec()));
    annot_dict.set("Rect", Object::Array(vec![
        Object::Integer(100),
        Object::Integer(100),
        Object::Integer(120),
        Object::Integer(120),
    ]));
    annot_dict.set("FS", Object::Reference(file_spec_id));
    annot_dict.set("Name", Object::Name(b"PaperclipTag".to_vec()));
    annot_dict.set("Contents", Object::String(attachment.filename.clone().into_bytes(), lopdf::StringFormat::Literal));
    
    let annot_id = doc.document.add_object(Object::Dictionary(annot_dict));
    
    if let Ok(Object::Dictionary(page_dict)) = doc.document.get_object_mut(page_id) {
        if let Ok(Object::Array(annots)) = page_dict.get_mut(b"Annots") {
            annots.push(Object::Reference(annot_id));
        } else {
            page_dict.set("Annots", Object::Array(vec![Object::Reference(annot_id)]));
        }
    }
    
    Ok(())
}

pub fn extract_attachments(doc: &PdfDocument) -> Result<Vec<Attachment>> {
    let mut attachments = Vec::new();
    
    if let Ok(cat_dict) = doc.document.catalog() {
        if let Ok(Object::Reference(names_ref)) = cat_dict.get(b"Names") {
            if let Ok(Object::Dictionary(names_dict)) = doc.document.get_object(*names_ref) {
                if let Ok(Object::Reference(ef_ref)) = names_dict.get(b"EmbeddedFiles") {
                    if let Ok(Object::Dictionary(ef_dict)) = doc.document.get_object(*ef_ref) {
                        if let Ok(Object::Array(names_array)) = ef_dict.get(b"Names") {
                            let mut i = 0;
                            while i < names_array.len() {
                                if i + 1 < names_array.len() {
                                    if let (Object::String(name, _), Object::Reference(file_spec_ref)) = 
                                        (&names_array[i], &names_array[i + 1]) {
                                        if let Ok(attachment) = extract_attachment(doc, *file_spec_ref, String::from_utf8_lossy(name).to_string()) {
                                            attachments.push(attachment);
                                        }
                                    }
                                }
                                i += 2;
                            }
                        }
                    }
                }
            }
        }
    }
    
    Ok(attachments)
}

fn extract_attachment(doc: &PdfDocument, file_spec_id: lopdf::ObjectId, filename: String) -> Result<Attachment> {
    if let Ok(Object::Dictionary(file_spec)) = doc.document.get_object(file_spec_id) {
        let description = file_spec.get(b"Desc")
            .ok()
            .and_then(|obj| {
                if let Object::String(s, _) = obj {
                    Some(String::from_utf8_lossy(s).to_string())
                } else {
                    None
                }
            });
        
        if let Ok(Object::Dictionary(ef_dict)) = file_spec.get(b"EF") {
            if let Ok(Object::Reference(stream_ref)) = ef_dict.get(b"F") {
                if let Ok(Object::Stream(stream)) = doc.document.get_object(*stream_ref) {
                    let mime_type = stream.dict.get(b"Subtype")
                        .ok()
                        .and_then(|obj| {
                            if let Object::Name(name) = obj {
                                Some(String::from_utf8_lossy(name).to_string())
                            } else {
                                None
                            }
                        })
                        .unwrap_or_else(|| "application/octet-stream".to_string());
                    
                    return Ok(Attachment {
                        filename,
                        description,
                        data: stream.content.clone(),
                        mime_type,
                        creation_date: None,
                        modification_date: None,
                    });
                }
            }
        }
    }
    
    Err(PdftkError::InvalidAttachment)
}

pub fn unpack_files(doc: &PdfDocument, output_dir: &Path) -> Result<Vec<String>> {
    let attachments = extract_attachments(doc)?;
    let mut output_files = Vec::new();
    
    if !output_dir.exists() {
        fs::create_dir_all(output_dir)?;
    }
    
    for attachment in attachments {
        let output_path = output_dir.join(&attachment.filename);
        fs::write(&output_path, &attachment.data)?;
        output_files.push(output_path.display().to_string());
    }
    
    Ok(output_files)
}