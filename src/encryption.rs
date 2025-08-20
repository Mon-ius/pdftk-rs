use crate::error::{PdftkError, Result};
use rc4::{Rc4, KeyInit as Rc4KeyInit, StreamCipher};
use rc4::consts::U256;

#[derive(Debug, Clone, Copy)]
pub enum EncryptionLevel {
    None,
    Bits40,
    Bits128,
}

#[derive(Debug, Clone)]
pub struct Permissions {
    pub printing: bool,
    pub modify_contents: bool,
    pub copy_contents: bool,
    pub modify_annotations: bool,
    pub fill_forms: bool,
    pub screen_readers: bool,
    pub assembly: bool,
    pub degraded_printing: bool,
}

impl Default for Permissions {
    fn default() -> Self {
        Permissions {
            printing: true,
            modify_contents: true,
            copy_contents: true,
            modify_annotations: true,
            fill_forms: true,
            screen_readers: true,
            assembly: true,
            degraded_printing: true,
        }
    }
}

impl Permissions {
    pub fn none() -> Self {
        Permissions {
            printing: false,
            modify_contents: false,
            copy_contents: false,
            modify_annotations: false,
            fill_forms: false,
            screen_readers: false,
            assembly: false,
            degraded_printing: false,
        }
    }

    pub fn to_bits(&self) -> u32 {
        let mut bits = 0xFFFFF0C0u32;
        
        if self.printing { bits |= 1 << 2; }
        if self.modify_contents { bits |= 1 << 3; }
        if self.copy_contents { bits |= 1 << 4; }
        if self.modify_annotations { bits |= 1 << 5; }
        if self.fill_forms { bits |= 1 << 8; }
        if self.screen_readers { bits |= 1 << 9; }
        if self.assembly { bits |= 1 << 10; }
        if self.degraded_printing { bits |= 1 << 11; }
        
        bits
    }

    pub fn from_bits(bits: u32) -> Self {
        Permissions {
            printing: (bits & (1 << 2)) != 0,
            modify_contents: (bits & (1 << 3)) != 0,
            copy_contents: (bits & (1 << 4)) != 0,
            modify_annotations: (bits & (1 << 5)) != 0,
            fill_forms: (bits & (1 << 8)) != 0,
            screen_readers: (bits & (1 << 9)) != 0,
            assembly: (bits & (1 << 10)) != 0,
            degraded_printing: (bits & (1 << 11)) != 0,
        }
    }
}

pub struct Encryption {
    level: EncryptionLevel,
    owner_password: Vec<u8>,
    user_password: Vec<u8>,
    permissions: Permissions,
}

impl Encryption {
    pub fn new(level: EncryptionLevel, owner_password: String, user_password: String, permissions: Permissions) -> Self {
        Encryption {
            level,
            owner_password: Self::prepare_password(owner_password),
            user_password: Self::prepare_password(user_password),
            permissions,
        }
    }

    fn prepare_password(password: String) -> Vec<u8> {
        let mut pwd = password.into_bytes();
        pwd.truncate(32);
        while pwd.len() < 32 {
            pwd.push(PADDING[pwd.len()]);
        }
        pwd
    }

    pub fn compute_owner_key(&self) -> Result<Vec<u8>> {
        let mut hash = md5::compute(&self.owner_password).to_vec();

        if matches!(self.level, EncryptionLevel::Bits128) {
            for _ in 0..50 {
                hash = md5::compute(&hash).to_vec();
            }
        }

        let key_len = match self.level {
            EncryptionLevel::Bits40 => 5,
            EncryptionLevel::Bits128 => 16,
            EncryptionLevel::None => return Ok(Vec::new()),
        };

        let mut result = self.user_password.clone();
        
        if matches!(self.level, EncryptionLevel::Bits40) {
            let mut cipher = Rc4::<U256>::new_from_slice(&hash[..key_len])
                .map_err(|_| PdftkError::Encryption("Failed to create RC4 cipher".to_string()))?;
            cipher.apply_keystream(&mut result);
        } else {
            for i in 0..20 {
                let mut key = hash[..key_len].to_vec();
                for j in 0..key_len {
                    key[j] ^= i as u8;
                }
                let mut cipher = Rc4::<U256>::new_from_slice(&key)
                    .map_err(|_| PdftkError::Encryption("Failed to create RC4 cipher".to_string()))?;
                cipher.apply_keystream(&mut result);
            }
        }

        Ok(result)
    }

    pub fn compute_user_key(&self, file_id: &[u8]) -> Result<Vec<u8>> {
        let mut data = Vec::new();
        data.extend_from_slice(&self.user_password);
        data.extend_from_slice(&self.compute_owner_key()?);
        data.extend_from_slice(&self.permissions.to_bits().to_le_bytes());
        data.extend_from_slice(file_id);

        let mut hash = md5::compute(&data).to_vec();

        if matches!(self.level, EncryptionLevel::Bits128) {
            for _ in 0..50 {
                hash = md5::compute(&hash).to_vec();
            }
        }

        let key_len = match self.level {
            EncryptionLevel::Bits40 => 5,
            EncryptionLevel::Bits128 => 16,
            EncryptionLevel::None => return Ok(Vec::new()),
        };

        let result = if matches!(self.level, EncryptionLevel::Bits40) {
            let mut data = PADDING.to_vec();
            let mut cipher = Rc4::<U256>::new_from_slice(&hash[..key_len])
                .map_err(|_| PdftkError::Encryption("Failed to create RC4 cipher".to_string()))?;
            cipher.apply_keystream(&mut data);
            data
        } else {
            let mut temp_data = Vec::new();
            temp_data.extend_from_slice(&PADDING);
            temp_data.extend_from_slice(file_id);
            let mut data = md5::compute(&temp_data).to_vec();
            
            let mut cipher = Rc4::<U256>::new_from_slice(&hash[..key_len])
                .map_err(|_| PdftkError::Encryption("Failed to create RC4 cipher".to_string()))?;
            cipher.apply_keystream(&mut data);
            
            for i in 1..20 {
                let mut key = hash[..key_len].to_vec();
                for j in 0..key_len {
                    key[j] ^= i as u8;
                }
                let mut cipher = Rc4::<U256>::new_from_slice(&key)
                    .map_err(|_| PdftkError::Encryption("Failed to create RC4 cipher".to_string()))?;
                cipher.apply_keystream(&mut data);
            }
            
            data.resize(32, 0);
            data
        };

        Ok(result)
    }

    pub fn encrypt_string(&self, s: &[u8], obj_num: u32, gen_num: u16) -> Result<Vec<u8>> {
        let key = self.compute_object_key(obj_num, gen_num)?;
        let mut result = s.to_vec();
        
        let mut cipher = Rc4::<U256>::new_from_slice(&key)
            .map_err(|_| PdftkError::Encryption("Failed to create RC4 cipher".to_string()))?;
        cipher.apply_keystream(&mut result);
        
        Ok(result)
    }

    pub fn decrypt_string(&self, s: &[u8], obj_num: u32, gen_num: u16) -> Result<Vec<u8>> {
        self.encrypt_string(s, obj_num, gen_num)
    }

    fn compute_object_key(&self, obj_num: u32, gen_num: u16) -> Result<Vec<u8>> {
        let key_len = match self.level {
            EncryptionLevel::Bits40 => 5,
            EncryptionLevel::Bits128 => 16,
            EncryptionLevel::None => return Ok(Vec::new()),
        };
        
        let mut data = Vec::new();
        data.extend_from_slice(&self.user_password[..key_len]);
        data.extend_from_slice(&obj_num.to_le_bytes()[..3]);
        data.extend_from_slice(&gen_num.to_le_bytes()[..2]);
        
        let hash = md5::compute(&data);
        let key_size = (key_len + 5).min(16);
        
        Ok(hash[..key_size].to_vec())
    }
}

const PADDING: [u8; 32] = [
    0x28, 0xBF, 0x4E, 0x5E, 0x4E, 0x75, 0x8A, 0x41,
    0x64, 0x00, 0x4E, 0x56, 0xFF, 0xFA, 0x01, 0x08,
    0x2E, 0x2E, 0x00, 0xB6, 0xD0, 0x68, 0x3E, 0x80,
    0x2F, 0x0C, 0xA9, 0xFE, 0x64, 0x53, 0x69, 0x7A,
];