use thiserror::Error;

#[derive(Error, Debug)]
pub enum PdftkError {
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),
    
    #[error("PDF parsing error: {0}")]
    PdfParse(String),
    
    #[error("Invalid page range: {0}")]
    InvalidPageRange(String),
    
    #[error("Invalid page number: {0}")]
    InvalidPageNumber(usize),
    
    #[error("Password required")]
    PasswordRequired,
    
    #[error("Invalid password")]
    InvalidPassword,
    
    #[error("Encryption error: {0}")]
    Encryption(String),
    
    #[error("Form field not found: {0}")]
    FormFieldNotFound(String),
    
    #[error("Invalid operation: {0}")]
    InvalidOperation(String),
    
    #[error("File not found: {0}")]
    FileNotFound(String),
    
    #[error("Unsupported PDF version: {0}")]
    UnsupportedPdfVersion(String),
    
    #[error("Invalid attachment")]
    InvalidAttachment,
    
    #[error("PDF write error: {0}")]
    PdfWrite(String),
    
    #[error("Unknown error: {0}")]
    Unknown(String),
    
    #[error("Lopdf error: {0}")]
    Lopdf(#[from] lopdf::Error),
}

pub type Result<T> = std::result::Result<T, PdftkError>;