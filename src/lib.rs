pub mod error;
pub mod pdf_document;
pub mod operations;
pub mod encryption;
pub mod forms;
pub mod attachments;
pub mod metadata;
pub mod utils;

pub use error::{PdftkError, Result};
pub use pdf_document::PdfDocument;
pub use operations::{Operations, PageRange, PageRotation};
pub use encryption::{EncryptionLevel, Permissions};
pub use forms::FormData;
pub use attachments::Attachment;
pub use metadata::Metadata;