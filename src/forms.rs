use crate::error::{PdftkError, Result};
use crate::pdf_document::PdfDocument;
use lopdf::{Object, Dictionary};
use std::collections::HashMap;
use serde::{Serialize, Deserialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FormField {
    pub name: String,
    pub value: String,
    pub field_type: FieldType,
    pub options: Vec<String>,
    pub default_value: Option<String>,
    pub required: bool,
    pub readonly: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum FieldType {
    Text,
    Checkbox,
    RadioButton,
    ComboBox,
    ListBox,
    Button,
    Signature,
}

#[derive(Debug, Clone)]
pub struct FormData {
    pub fields: HashMap<String, FormField>,
}

impl FormData {
    pub fn new() -> Self {
        FormData {
            fields: HashMap::new(),
        }
    }

    pub fn from_fdf(_fdf_data: &[u8]) -> Result<Self> {
        let form_data = FormData::new();
        Ok(form_data)
    }

    pub fn from_xfdf(_xfdf_data: &str) -> Result<Self> {
        let form_data = FormData::new();
        Ok(form_data)
    }

    pub fn to_fdf(&self) -> Result<Vec<u8>> {
        let mut fdf = Vec::new();
        
        fdf.extend_from_slice(b"%FDF-1.2\n");
        fdf.extend_from_slice(b"1 0 obj\n<< /FDF << /Fields [\n");
        
        for (name, field) in &self.fields {
            fdf.extend_from_slice(b"<< /T (");
            fdf.extend_from_slice(Self::escape_pdf_string(name).as_bytes());
            fdf.extend_from_slice(b") /V (");
            fdf.extend_from_slice(Self::escape_pdf_string(&field.value).as_bytes());
            fdf.extend_from_slice(b") >>\n");
        }
        
        fdf.extend_from_slice(b"] >> >>\nendobj\n");
        fdf.extend_from_slice(b"trailer\n<< /Root 1 0 R >>\n");
        fdf.extend_from_slice(b"%%EOF\n");
        
        Ok(fdf)
    }

    pub fn to_xfdf(&self) -> Result<String> {
        let mut xfdf = String::new();
        
        xfdf.push_str("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xfdf.push_str("<xfdf xmlns=\"http://ns.adobe.com/xfdf/\" xml:space=\"preserve\">\n");
        xfdf.push_str("  <fields>\n");
        
        for (name, field) in &self.fields {
            xfdf.push_str(&format!("    <field name=\"{}\">\n", Self::escape_xml(name)));
            xfdf.push_str(&format!("      <value>{}</value>\n", Self::escape_xml(&field.value)));
            xfdf.push_str("    </field>\n");
        }
        
        xfdf.push_str("  </fields>\n");
        xfdf.push_str("</xfdf>\n");
        
        Ok(xfdf)
    }

    fn escape_pdf_string(s: &str) -> String {
        s.chars()
            .map(|c| match c {
                '(' => "\\(".to_string(),
                ')' => "\\)".to_string(),
                '\\' => "\\\\".to_string(),
                '\n' => "\\n".to_string(),
                '\r' => "\\r".to_string(),
                '\t' => "\\t".to_string(),
                c => c.to_string(),
            })
            .collect()
    }

    fn escape_xml(s: &str) -> String {
        s.chars()
            .map(|c| match c {
                '<' => "&lt;".to_string(),
                '>' => "&gt;".to_string(),
                '&' => "&amp;".to_string(),
                '"' => "&quot;".to_string(),
                '\'' => "&apos;".to_string(),
                c => c.to_string(),
            })
            .collect()
    }

    pub fn extract_from_pdf(doc: &PdfDocument) -> Result<Self> {
        let mut form_data = FormData::new();
        
        if let Ok(cat_dict) = doc.document.catalog() {
                if let Ok(Object::Reference(acroform_ref)) = cat_dict.get(b"AcroForm") {
                    if let Ok(Object::Dictionary(acroform)) = doc.document.get_object(*acroform_ref) {
                        if let Ok(Object::Array(fields)) = acroform.get(b"Fields") {
                            for field_ref in fields {
                                if let Object::Reference(ref_id) = field_ref {
                                    form_data.extract_field(doc, *ref_id)?;
                                }
                            }
                        }
                    }
                }
        }
        
        Ok(form_data)
    }

    fn extract_field(&mut self, doc: &PdfDocument, field_id: lopdf::ObjectId) -> Result<()> {
        if let Ok(Object::Dictionary(field_dict)) = doc.document.get_object(field_id) {
            let name = Self::get_field_name(field_dict)?;
            let value = Self::get_field_value(field_dict)?;
            let field_type = Self::get_field_type(field_dict)?;
            
            let field = FormField {
                name: name.clone(),
                value,
                field_type,
                options: Vec::new(),
                default_value: Self::get_field_default_value(field_dict).ok(),
                required: false,
                readonly: false,
            };
            
            self.fields.insert(name, field);
        }
        
        Ok(())
    }

    fn get_field_name(dict: &Dictionary) -> Result<String> {
        dict.get(b"T")
            .ok()
            .and_then(|obj| {
                if let Object::String(s, _) = obj {
                    Some(String::from_utf8_lossy(s).to_string())
                } else {
                    None
                }
            })
            .ok_or_else(|| PdftkError::FormFieldNotFound("Field name not found".to_string()))
    }

    fn get_field_value(dict: &Dictionary) -> Result<String> {
        Ok(dict.get(b"V")
            .ok()
            .and_then(|obj| {
                if let Object::String(s, _) = obj {
                    Some(String::from_utf8_lossy(s).to_string())
                } else {
                    None
                }
            })
            .unwrap_or_else(|| String::new()))
    }

    fn get_field_default_value(dict: &Dictionary) -> Result<String> {
        dict.get(b"DV")
            .ok()
            .and_then(|obj| {
                if let Object::String(s, _) = obj {
                    Some(String::from_utf8_lossy(s).to_string())
                } else {
                    None
                }
            })
            .ok_or_else(|| PdftkError::FormFieldNotFound("Default value not found".to_string()))
    }

    fn get_field_type(dict: &Dictionary) -> Result<FieldType> {
        Ok(dict.get(b"FT")
            .ok()
            .and_then(|obj| {
                if let Object::Name(name) = obj {
                    match name.as_slice() {
                        b"Tx" => Some(FieldType::Text),
                        b"Btn" => Some(FieldType::Button),
                        b"Ch" => Some(FieldType::ComboBox),
                        b"Sig" => Some(FieldType::Signature),
                        _ => None,
                    }
                } else {
                    None
                }
            })
            .unwrap_or(FieldType::Text))
    }

    pub fn fill_pdf(&self, doc: &mut PdfDocument) -> Result<()> {
        let field_ids: Vec<lopdf::ObjectId> = if let Ok(cat_dict) = doc.document.catalog() {
            if let Ok(Object::Reference(acroform_ref)) = cat_dict.get(b"AcroForm") {
                if let Ok(Object::Dictionary(acroform)) = doc.document.get_object(*acroform_ref) {
                    if let Ok(Object::Array(fields)) = acroform.get(b"Fields") {
                        fields.iter()
                            .filter_map(|field_ref| {
                                if let Object::Reference(ref_id) = field_ref {
                                    Some(*ref_id)
                                } else {
                                    None
                                }
                            })
                            .collect()
                    } else {
                        Vec::new()
                    }
                } else {
                    Vec::new()
                }
            } else {
                Vec::new()
            }
        } else {
            Vec::new()
        };
        
        for field_id in field_ids {
            self.fill_field(doc, field_id)?;
        }
        
        Ok(())
    }

    fn fill_field(&self, doc: &mut PdfDocument, field_id: lopdf::ObjectId) -> Result<()> {
        if let Ok(Object::Dictionary(field_dict)) = doc.document.get_object_mut(field_id) {
            if let Some(name) = Self::get_field_name(field_dict).ok() {
                if let Some(field) = self.fields.get(&name) {
                    field_dict.set("V", Object::String(field.value.clone().into_bytes(), lopdf::StringFormat::Literal));
                }
            }
        }
        
        Ok(())
    }

    pub fn flatten_forms(_doc: &mut PdfDocument) -> Result<()> {
        Ok(())
    }
}