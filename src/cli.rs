use clap::{Parser, Subcommand};
use libpdftk::{
    PdfDocument, Operations, PageRange, PageRotation,
    EncryptionLevel, Permissions, FormData, Attachment, Metadata,
    Result, PdftkError,
};
use std::path::PathBuf;
use std::fs;

#[derive(Parser)]
#[command(name = "pdftk-rs")]
#[command(author = "pdftk-rs authors")]
#[command(version = "0.1.0")]
#[command(about = "PDF toolkit - Rust implementation", long_about = None)]
struct Cli {
    #[arg(help = "Input PDF files")]
    input_files: Vec<String>,

    #[command(subcommand)]
    operation: Option<Operation>,

    #[arg(short = 'o', long, value_name = "FILE", help = "Output PDF file")]
    output: Option<PathBuf>,

    #[arg(long, help = "Owner password for output PDF")]
    owner_pw: Option<String>,

    #[arg(long, help = "User password for output PDF")]
    user_pw: Option<String>,

    #[arg(long, help = "Input password")]
    input_pw: Option<String>,

    #[arg(long, help = "Use 40-bit encryption")]
    encrypt_40bit: bool,

    #[arg(long, help = "Use 128-bit encryption")]
    encrypt_128bit: bool,

    #[arg(long, help = "Allow printing")]
    allow_printing: bool,

    #[arg(long, help = "Allow modifying contents")]
    allow_modify_contents: bool,

    #[arg(long, help = "Allow copying contents")]
    allow_copy_contents: bool,

    #[arg(long, help = "Allow modifying annotations")]
    allow_modify_annotations: bool,

    #[arg(long, help = "Allow filling forms")]
    allow_fill_forms: bool,

    #[arg(long, help = "Allow screen readers")]
    allow_screen_readers: bool,

    #[arg(long, help = "Allow assembly")]
    allow_assembly: bool,

    #[arg(long, help = "Allow degraded printing")]
    allow_degraded_printing: bool,

    #[arg(long, help = "Uncompress streams")]
    uncompress: bool,

    #[arg(long, help = "Compress streams")]
    compress: bool,

    #[arg(long, help = "Flatten forms")]
    flatten: bool,

    #[arg(long, help = "Drop XFA forms")]
    drop_xfa: bool,

    #[arg(long, help = "Drop XMP metadata")]
    drop_xmp: bool,

    #[arg(short = 'v', long, help = "Verbose output")]
    verbose: bool,
}

#[derive(Subcommand)]
enum Operation {
    Cat {
        #[arg(help = "Page ranges (e.g., 1-3,5,7-end)")]
        pages: Vec<String>,
    },
    Shuffle {
        #[arg(help = "Second input file for shuffling")]
        second_input: PathBuf,
    },
    Burst {
        #[arg(help = "Output pattern (use %d for page number)")]
        pattern: Option<String>,
    },
    Rotate {
        #[arg(help = "Pages to rotate")]
        pages: String,
        #[arg(help = "Rotation (north, east, south, west, or degrees)")]
        rotation: String,
    },
    Stamp {
        #[arg(help = "Stamp PDF file")]
        stamp_file: PathBuf,
        #[arg(long, help = "Apply stamp to all pages")]
        multi: bool,
    },
    Background {
        #[arg(help = "Background PDF file")]
        background_file: PathBuf,
        #[arg(long, help = "Apply background to all pages")]
        multi: bool,
    },
    FillForm {
        #[arg(help = "Form data file (FDF/XFDF)")]
        data_file: PathBuf,
    },
    DumpData {
        #[arg(long, help = "Output UTF-8 encoded data")]
        utf8: bool,
    },
    DumpDataFields {
        #[arg(long, help = "Output UTF-8 encoded data")]
        utf8: bool,
    },
    GenerateFdf,
    UpdateInfo {
        #[arg(help = "Info file")]
        info_file: PathBuf,
        #[arg(long, help = "Info file is UTF-8 encoded")]
        utf8: bool,
    },
    UpdateXmp {
        #[arg(help = "XMP metadata file")]
        xmp_file: PathBuf,
    },
    AttachFiles {
        #[arg(help = "Files to attach")]
        files: Vec<PathBuf>,
        #[arg(long, help = "Attach to specific page")]
        to_page: Option<usize>,
    },
    UnpackFiles {
        #[arg(help = "Output directory")]
        output_dir: Option<PathBuf>,
    },
    Filter {
        #[arg(help = "Pages to keep (e.g., 1-3,5,7-end)")]
        pages: String,
    },
    DumpDataAnnots,
}

fn main() -> Result<()> {
    let cli = Cli::parse();

    if cli.input_files.is_empty() && cli.operation.is_some() {
        eprintln!("Error: No input files specified");
        std::process::exit(1);
    }

    let mut input_docs = Vec::new();
    for input_file in &cli.input_files {
        let doc = if let Some(ref password) = cli.input_pw {
            PdfDocument::open(input_file, Some(password.clone()))?
        } else {
            PdfDocument::open(input_file, None)?
        };
        input_docs.push(doc);
    }

    let mut output_doc = PdfDocument::new();
    let result = match cli.operation {
        Some(Operation::Cat { pages }) => {
            if pages.is_empty() {
                let inputs: Vec<(PdfDocument, PageRange)> = input_docs.into_iter()
                    .map(|doc| (doc, PageRange::All))
                    .collect();
                Operations::cat(inputs, &mut output_doc)?;
            } else {
                let mut inputs = Vec::new();
                for (doc, page_spec) in input_docs.into_iter().zip(pages.iter()) {
                    let range = PageRange::parse(page_spec)?;
                    inputs.push((doc, range));
                }
                Operations::cat(inputs, &mut output_doc)?;
            }
            Some(output_doc)
        }
        Some(Operation::Shuffle { second_input }) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("Shuffle requires exactly one input file".to_string()));
            }
            let second_doc = if let Some(ref password) = cli.input_pw {
                PdfDocument::open(second_input, Some(password.clone()))?
            } else {
                PdfDocument::open(second_input, None)?
            };
            Operations::shuffle(&input_docs[0], &second_doc, &mut output_doc)?;
            Some(output_doc)
        }
        Some(Operation::Burst { pattern }) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("Burst requires exactly one input file".to_string()));
            }
            let pattern = pattern.unwrap_or_else(|| "page_%d.pdf".to_string());
            let output_files = Operations::burst(&input_docs[0], &pattern)?;
            if cli.verbose {
                for file in output_files {
                    println!("Created: {}", file);
                }
            }
            None
        }
        Some(Operation::Rotate { pages, rotation }) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("Rotate requires exactly one input file".to_string()));
            }
            let range = PageRange::parse(&pages)?;
            let (degrees, absolute) = libpdftk::utils::parse_rotation(&rotation)?;
            let rotation = PageRotation::from_degrees(degrees);
            let mut doc = input_docs.into_iter().next().unwrap();
            Operations::rotate_pages(&mut doc, range, rotation, absolute)?;
            Some(doc)
        }
        Some(Operation::Stamp { stamp_file, multi }) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("Stamp requires exactly one input file".to_string()));
            }
            let stamp_doc = if let Some(ref password) = cli.input_pw {
                PdfDocument::open(stamp_file, Some(password.clone()))?
            } else {
                PdfDocument::open(stamp_file, None)?
            };
            Operations::stamp(&input_docs[0], &stamp_doc, &mut output_doc, multi)?;
            Some(output_doc)
        }
        Some(Operation::Background { background_file, multi }) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("Background requires exactly one input file".to_string()));
            }
            let bg_doc = if let Some(ref password) = cli.input_pw {
                PdfDocument::open(background_file, Some(password.clone()))?
            } else {
                PdfDocument::open(background_file, None)?
            };
            Operations::stamp(&bg_doc, &input_docs[0], &mut output_doc, multi)?;
            Some(output_doc)
        }
        Some(Operation::FillForm { data_file }) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("FillForm requires exactly one input file".to_string()));
            }
            let data = fs::read_to_string(&data_file)?;
            let form_data = if data_file.extension().and_then(|s| s.to_str()) == Some("xfdf") {
                FormData::from_xfdf(&data)?
            } else {
                FormData::from_fdf(data.as_bytes())?
            };
            let mut doc = input_docs.into_iter().next().unwrap();
            form_data.fill_pdf(&mut doc)?;
            if cli.flatten {
                FormData::flatten_forms(&mut doc)?;
            }
            Some(doc)
        }
        Some(Operation::DumpData { utf8 }) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("DumpData requires exactly one input file".to_string()));
            }
            let dump_output = Operations::dump_data(&input_docs[0], utf8)?;
            println!("{}", dump_output);
            None
        }
        Some(Operation::DumpDataFields { utf8: _ }) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("DumpDataFields requires exactly one input file".to_string()));
            }
            let form_data = FormData::extract_from_pdf(&input_docs[0])?;
            for (name, field) in form_data.fields {
                println!("Field: {}", name);
                println!("  Value: {}", field.value);
                println!("  Type: {:?}", field.field_type);
                if let Some(default) = field.default_value {
                    println!("  Default: {}", default);
                }
                println!();
            }
            None
        }
        Some(Operation::GenerateFdf) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("GenerateFdf requires exactly one input file".to_string()));
            }
            let form_data = FormData::extract_from_pdf(&input_docs[0])?;
            let fdf = form_data.to_fdf()?;
            if let Some(ref output) = cli.output {
                fs::write(output, fdf)?;
            } else {
                println!("{}", String::from_utf8_lossy(&fdf));
            }
            None
        }
        Some(Operation::UpdateInfo { info_file, utf8: _ }) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("UpdateInfo requires exactly one input file".to_string()));
            }
            let info_text = fs::read_to_string(info_file)?;
            let metadata = Metadata::from_text(&info_text)?;
            let mut doc = input_docs.into_iter().next().unwrap();
            metadata.apply_to_pdf(&mut doc)?;
            Some(doc)
        }
        Some(Operation::UpdateXmp { xmp_file }) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("UpdateXmp requires exactly one input file".to_string()));
            }
            let xmp_data = fs::read_to_string(xmp_file)?;
            let mut doc = input_docs.into_iter().next().unwrap();
            libpdftk::metadata::update_xmp_metadata(&mut doc, &xmp_data)?;
            Some(doc)
        }
        Some(Operation::AttachFiles { files, to_page }) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("AttachFiles requires exactly one input file".to_string()));
            }
            let mut doc = input_docs.into_iter().next().unwrap();
            let mut attachments = Vec::new();
            for file in files {
                attachments.push(Attachment::from_file(file)?);
            }
            if let Some(page_num) = to_page {
                if attachments.len() != 1 {
                    return Err(PdftkError::InvalidOperation("Can only attach one file to a specific page".to_string()));
                }
                libpdftk::attachments::attach_file_to_page(&mut doc, page_num, attachments.into_iter().next().unwrap())?;
            } else {
                libpdftk::attachments::attach_files(&mut doc, attachments)?;
            }
            Some(doc)
        }
        Some(Operation::UnpackFiles { output_dir }) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("UnpackFiles requires exactly one input file".to_string()));
            }
            let output_dir = output_dir.unwrap_or_else(|| PathBuf::from("."));
            let files = libpdftk::attachments::unpack_files(&input_docs[0], &output_dir)?;
            if cli.verbose {
                for file in files {
                    println!("Extracted: {}", file);
                }
            }
            None
        }
        Some(Operation::Filter { pages }) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("Filter requires exactly one input file".to_string()));
            }
            let range = PageRange::parse(&pages)?;
            output_doc = Operations::filter(&input_docs[0], range)?;
            Some(output_doc)
        }
        Some(Operation::DumpDataAnnots) => {
            if input_docs.len() != 1 {
                return Err(PdftkError::InvalidOperation("DumpDataAnnots requires exactly one input file".to_string()));
            }
            let annots_output = Operations::dump_data_annots(&input_docs[0])?;
            println!("{}", annots_output);
            None
        }
        None => {
            if input_docs.len() == 1 {
                Some(input_docs.into_iter().next().unwrap())
            } else if !input_docs.is_empty() {
                let inputs: Vec<(PdfDocument, PageRange)> = input_docs.into_iter()
                    .map(|doc| (doc, PageRange::All))
                    .collect();
                Operations::cat(inputs, &mut output_doc)?;
                Some(output_doc)
            } else {
                None
            }
        }
    };

    if let Some(mut doc) = result {
        if cli.compress {
            doc.compress();
        } else if cli.uncompress {
            doc.decompress();
        }

        if cli.drop_xfa {
            doc.prune_objects();
        }

        let _encryption_level = if cli.encrypt_128bit {
            EncryptionLevel::Bits128
        } else if cli.encrypt_40bit {
            EncryptionLevel::Bits40
        } else {
            EncryptionLevel::None
        };

        if !matches!(_encryption_level, EncryptionLevel::None) {
            let _permissions = Permissions {
                printing: cli.allow_printing,
                modify_contents: cli.allow_modify_contents,
                copy_contents: cli.allow_copy_contents,
                modify_annotations: cli.allow_modify_annotations,
                fill_forms: cli.allow_fill_forms,
                screen_readers: cli.allow_screen_readers,
                assembly: cli.allow_assembly,
                degraded_printing: cli.allow_degraded_printing,
            };

            let _owner_pw = cli.owner_pw.unwrap_or_default();
            let _user_pw = cli.user_pw.unwrap_or_default();
        }

        if let Some(output_path) = cli.output {
            doc.save(output_path)?;
            if cli.verbose {
                println!("Output written successfully");
            }
        } else {
            let output = doc.save_to_vec()?;
            std::io::Write::write_all(&mut std::io::stdout(), &output)?;
        }
    }

    Ok(())
}