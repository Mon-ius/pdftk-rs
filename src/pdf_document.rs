use crate::error::{PdftkError, Result};
use lopdf::{Document, Object, ObjectId};
use std::path::{Path, PathBuf};

#[derive(Debug, Clone)]
pub struct PdfDocument {
    pub document: Document,
    pub filename: PathBuf,
    pub password: Option<String>,
    pub is_authorized: bool,
    pub num_pages: usize,
}

impl PdfDocument {
    pub fn open<P: AsRef<Path>>(path: P, password: Option<String>) -> Result<Self> {
        let path = path.as_ref();
        if !path.exists() {
            return Err(PdftkError::FileNotFound(path.display().to_string()));
        }

        let mut document = Document::load(path)
            .map_err(|e| PdftkError::PdfParse(e.to_string()))?;

        let is_authorized = if document.is_encrypted() {
            if let Some(ref pwd) = password {
                match document.decrypt(pwd) {
                    Ok(_) => true,
                    Err(_) => return Err(PdftkError::InvalidPassword),
                }
            } else {
                return Err(PdftkError::PasswordRequired);
            }
        } else {
            true
        };

        let num_pages = document.get_pages().len();

        Ok(PdfDocument {
            document,
            filename: path.to_path_buf(),
            password,
            is_authorized,
            num_pages,
        })
    }

    pub fn new() -> Self {
        PdfDocument {
            document: Document::with_version("1.7"),
            filename: PathBuf::new(),
            password: None,
            is_authorized: true,
            num_pages: 0,
        }
    }

    pub fn get_page(&self, page_num: usize) -> Result<ObjectId> {
        if page_num == 0 || page_num > self.num_pages {
            return Err(PdftkError::InvalidPageNumber(page_num));
        }
        
        let pages = self.document.get_pages();
        pages.get(&(page_num as u32))
            .copied()
            .ok_or_else(|| PdftkError::InvalidPageNumber(page_num))
    }

    pub fn get_page_rotation(&self, page_id: ObjectId) -> Result<i32> {
        if let Ok(Object::Dictionary(dict)) = self.document.get_object(page_id) {
            if let Ok(Object::Integer(rotation)) = dict.get(b"Rotate") {
                return Ok(*rotation as i32);
            }
        }
        Ok(0)
    }

    pub fn set_page_rotation(&mut self, page_id: ObjectId, rotation: i32) -> Result<()> {
        if let Ok(Object::Dictionary(dict)) = self.document.get_object_mut(page_id) {
            dict.set("Rotate", rotation);
            Ok(())
        } else {
            Err(PdftkError::InvalidOperation("Cannot set page rotation".to_string()))
        }
    }

    pub fn add_page(&mut self, page: Object) -> Result<ObjectId> {
        let page_id = self.document.add_object(page);
        
        let cat_dict = self.document.catalog()?;
        let pages_id = cat_dict.get(b"Pages")
            .ok()
            .and_then(|p| p.as_reference().ok())
            .ok_or_else(|| PdftkError::PdfParse("Cannot find pages root".to_string()))?;

        if let Ok(Object::Dictionary(pages_dict)) = self.document.get_object_mut(pages_id) {
            if let Ok(Object::Array(kids)) = pages_dict.get_mut(b"Kids") {
                kids.push(Object::Reference(page_id));
            }
            
            if let Ok(Object::Integer(count)) = pages_dict.get_mut(b"Count") {
                *count += 1;
                self.num_pages += 1;
            }
        }

        Ok(page_id)
    }

    pub fn remove_page(&mut self, page_num: usize) -> Result<()> {
        if page_num == 0 || page_num > self.num_pages {
            return Err(PdftkError::InvalidPageNumber(page_num));
        }

        let page_id = self.get_page(page_num)?;
        
        let cat_dict = self.document.catalog()?;
        let pages_id = cat_dict.get(b"Pages")
            .ok()
            .and_then(|p| p.as_reference().ok())
            .ok_or_else(|| PdftkError::PdfParse("Cannot find pages root".to_string()))?;

        if let Ok(Object::Dictionary(pages_dict)) = self.document.get_object_mut(pages_id) {
            if let Ok(Object::Array(kids)) = pages_dict.get_mut(b"Kids") {
                kids.retain(|kid| {
                    if let Ok(kid_ref) = kid.as_reference() {
                        kid_ref != page_id
                    } else {
                        true
                    }
                });
            }
            
            if let Ok(Object::Integer(count)) = pages_dict.get_mut(b"Count") {
                *count -= 1;
                self.num_pages -= 1;
            }
        }

        self.document.delete_object(page_id);
        Ok(())
    }

    pub fn save<P: AsRef<Path>>(&mut self, path: P) -> Result<()> {
        self.document.save(path)?;
        Ok(())
    }

    pub fn save_to_vec(&mut self) -> Result<Vec<u8>> {
        let mut buffer = Vec::new();
        self.document.save_to(&mut buffer)
            .map_err(|e| PdftkError::PdfWrite(e.to_string()))?;
        Ok(buffer)
    }

    pub fn compress(&mut self) {
        self.document.compress();
    }

    pub fn decompress(&mut self) {
        self.document.decompress();
    }

    pub fn prune_objects(&mut self) -> Vec<ObjectId> {
        self.document.prune_objects()
    }
}