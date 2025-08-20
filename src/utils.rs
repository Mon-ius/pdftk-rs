use crate::error::{PdftkError, Result};
use std::fs::File;
use std::io::{Read, Write};
use std::path::Path;
use encoding_rs::WINDOWS_1252;

pub fn read_file_to_bytes<P: AsRef<Path>>(path: P) -> Result<Vec<u8>> {
    let mut file = File::open(path)?;
    let mut buffer = Vec::new();
    file.read_to_end(&mut buffer)?;
    Ok(buffer)
}

pub fn write_bytes_to_file<P: AsRef<Path>>(path: P, data: &[u8]) -> Result<()> {
    let mut file = File::create(path)?;
    file.write_all(data)?;
    Ok(())
}

pub fn utf8_to_pdfdoc(text: &str) -> Vec<u8> {
    let (encoded, _, _) = WINDOWS_1252.encode(text);
    encoded.to_vec()
}

pub fn pdfdoc_to_utf8(bytes: &[u8]) -> String {
    let (decoded, _, _) = WINDOWS_1252.decode(bytes);
    decoded.to_string()
}

pub fn escape_pdf_string(s: &str) -> String {
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

pub fn unescape_pdf_string(s: &str) -> String {
    let mut result = String::new();
    let mut chars = s.chars();
    
    while let Some(c) = chars.next() {
        if c == '\\' {
            if let Some(next) = chars.next() {
                match next {
                    '(' => result.push('('),
                    ')' => result.push(')'),
                    '\\' => result.push('\\'),
                    'n' => result.push('\n'),
                    'r' => result.push('\r'),
                    't' => result.push('\t'),
                    _ => {
                        result.push(c);
                        result.push(next);
                    }
                }
            } else {
                result.push(c);
            }
        } else {
            result.push(c);
        }
    }
    
    result
}

pub fn parse_page_range(input: &str, total_pages: usize) -> Result<Vec<usize>> {
    let mut pages = Vec::new();
    
    for part in input.split(',') {
        let part = part.trim();
        
        if part.is_empty() {
            continue;
        }
        
        if part == "end" {
            pages.push(total_pages);
        } else if part == "even" {
            pages.extend((1..=total_pages).filter(|n| n % 2 == 0));
        } else if part == "odd" {
            pages.extend((1..=total_pages).filter(|n| n % 2 != 0));
        } else if part.contains('-') {
            let range_parts: Vec<&str> = part.split('-').collect();
            if range_parts.len() != 2 {
                return Err(PdftkError::InvalidPageRange(part.to_string()));
            }
            
            let start = if range_parts[0].is_empty() {
                1
            } else if range_parts[0] == "end" {
                total_pages
            } else {
                range_parts[0].parse::<usize>()
                    .map_err(|_| PdftkError::InvalidPageRange(part.to_string()))?
            };
            
            let end = if range_parts[1].is_empty() {
                total_pages
            } else if range_parts[1] == "end" {
                total_pages
            } else {
                range_parts[1].parse::<usize>()
                    .map_err(|_| PdftkError::InvalidPageRange(part.to_string()))?
            };
            
            if start > end {
                pages.extend((end..=start).rev());
            } else {
                pages.extend(start..=end);
            }
        } else {
            let page = part.parse::<usize>()
                .map_err(|_| PdftkError::InvalidPageRange(part.to_string()))?;
            pages.push(page);
        }
    }
    
    pages.retain(|&p| p > 0 && p <= total_pages);
    
    Ok(pages)
}

pub fn parse_rotation(input: &str) -> Result<(i32, bool)> {
    let (rotation_str, absolute) = if input.ends_with("absolute") {
        (input.trim_end_matches("absolute").trim(), true)
    } else {
        (input, false)
    };
    
    let rotation = match rotation_str.to_lowercase().as_str() {
        "north" | "n" => 0,
        "east" | "e" | "right" | "r" => 90,
        "south" | "s" | "upside-down" | "u" => 180,
        "west" | "w" | "left" | "l" => 270,
        s => s.parse::<i32>()
            .map_err(|_| PdftkError::InvalidOperation(format!("Invalid rotation: {}", s)))?,
    };
    
    Ok((rotation % 360, absolute))
}

pub fn normalize_filename(filename: &str) -> String {
    filename
        .chars()
        .map(|c| match c {
            '/' | '\\' | ':' | '*' | '?' | '"' | '<' | '>' | '|' => '_',
            c => c,
        })
        .collect()
}

pub fn expand_page_range_with_qualifiers(input: &str, total_pages: usize) -> Result<Vec<(usize, Option<String>)>> {
    let mut pages_with_qualifiers = Vec::new();
    
    for part in input.split(' ') {
        let part = part.trim();
        
        if part.is_empty() {
            continue;
        }
        
        let (page_spec, qualifier) = if part.contains("north") {
            (part.trim_end_matches("north").trim(), Some("north".to_string()))
        } else if part.contains("south") {
            (part.trim_end_matches("south").trim(), Some("south".to_string()))
        } else if part.contains("east") {
            (part.trim_end_matches("east").trim(), Some("east".to_string()))
        } else if part.contains("west") {
            (part.trim_end_matches("west").trim(), Some("west".to_string()))
        } else if part.contains("left") {
            (part.trim_end_matches("left").trim(), Some("left".to_string()))
        } else if part.contains("right") {
            (part.trim_end_matches("right").trim(), Some("right".to_string()))
        } else {
            (part, None)
        };
        
        let pages = parse_page_range(page_spec, total_pages)?;
        for page in pages {
            pages_with_qualifiers.push((page, qualifier.clone()));
        }
    }
    
    Ok(pages_with_qualifiers)
}