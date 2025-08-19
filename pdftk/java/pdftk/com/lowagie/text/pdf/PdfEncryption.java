package pdftk.com.lowagie.text.pdf;
import pdftk.com.lowagie.text.pdf.crypto.ARCFOUREncryption;
import java.security.MessageDigest;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import pdftk.com.lowagie.text.ExceptionConverter;
public class PdfEncryption {
	public static final int STANDARD_ENCRYPTION_40 = 2;
	public static final int STANDARD_ENCRYPTION_128 = 3;
	public static final int AES_128 = 4;
	private static final byte[] pad = { (byte) 0x28, (byte) 0xBF, (byte) 0x4E,
			(byte) 0x5E, (byte) 0x4E, (byte) 0x75, (byte) 0x8A, (byte) 0x41,
			(byte) 0x64, (byte) 0x00, (byte) 0x4E, (byte) 0x56, (byte) 0xFF,
			(byte) 0xFA, (byte) 0x01, (byte) 0x08, (byte) 0x2E, (byte) 0x2E,
			(byte) 0x00, (byte) 0xB6, (byte) 0xD0, (byte) 0x68, (byte) 0x3E,
			(byte) 0x80, (byte) 0x2F, (byte) 0x0C, (byte) 0xA9, (byte) 0xFE,
			(byte) 0x64, (byte) 0x53, (byte) 0x69, (byte) 0x7A };
	private static final byte[] salt = { (byte) 0x73, (byte) 0x41, (byte) 0x6c,
			(byte) 0x54 };
	private static final byte[] metadataPad = { (byte) 255, (byte) 255,
			(byte) 255, (byte) 255 };
	byte key[] = null;
	int keySize = 0;
	byte mkey[] = null;
	byte extra[] = new byte[5];
	MessageDigest md5 = null;
	byte ownerKey[] = new byte[32];
	byte userKey[] = new byte[32];
	int permissions = 0;
	byte documentID[] = null;
	static long seq = System.currentTimeMillis();
	private int revision = 0;
	private ARCFOUREncryption arcfour = new ARCFOUREncryption();
	private int keyLength = 0;
	private boolean encryptMetadata = false;
	private boolean embeddedFilesOnly = false;
	private int cryptoMode = 0;
	public PdfEncryption() {
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			throw new ExceptionConverter(e);
		}
	}
	public PdfEncryption(PdfEncryption enc) {
		this();
		mkey = (byte[]) enc.mkey.clone();
		ownerKey = (byte[]) enc.ownerKey.clone();
		userKey = (byte[]) enc.userKey.clone();
		permissions = enc.permissions;
		if (enc.documentID != null)
			documentID = (byte[]) enc.documentID.clone();
		revision = enc.revision;
		keyLength = enc.keyLength;
		encryptMetadata = enc.encryptMetadata;
		embeddedFilesOnly = enc.embeddedFilesOnly;
	}
	public void setCryptoMode(int mode, int kl) {
		cryptoMode = mode;
		encryptMetadata = (mode & PdfWriter.DO_NOT_ENCRYPT_METADATA) == 0;
		embeddedFilesOnly = (mode & PdfWriter.EMBEDDED_FILES_ONLY) != 0;
		mode &= PdfWriter.ENCRYPTION_MASK;
		switch (mode) {
		case PdfWriter.STANDARD_ENCRYPTION_40:
			encryptMetadata = true;
			embeddedFilesOnly = false;
			keyLength = 40;
			revision = STANDARD_ENCRYPTION_40;
			break;
		case PdfWriter.STANDARD_ENCRYPTION_128:
			embeddedFilesOnly = false;
			if (kl > 0)
				keyLength = kl;
			else
				keyLength = 128;
			revision = STANDARD_ENCRYPTION_128;
			break;
		case PdfWriter.ENCRYPTION_AES_128:
			keyLength = 128;
			revision = AES_128;
			break;
		default:
		  throw new IllegalArgumentException("no.valid.encryption.mode");
		}
	}
	public int getCryptoMode() {
		return cryptoMode;
	}
	public boolean isMetadataEncrypted() {
		return encryptMetadata;
	}
	public boolean isEmbeddedFilesOnly() {
		return embeddedFilesOnly;
	}
	private byte[] padPassword(byte userPassword[]) {
		byte userPad[] = new byte[32];
		if (userPassword == null) {
			System.arraycopy(pad, 0, userPad, 0, 32);
		} else {
			System.arraycopy(userPassword, 0, userPad, 0, Math.min(
					userPassword.length, 32));
			if (userPassword.length < 32)
				System.arraycopy(pad, 0, userPad, userPassword.length,
						32 - userPassword.length);
		}
		return userPad;
	}
	private byte[] computeOwnerKey(byte userPad[], byte ownerPad[]) {
		byte ownerKey[] = new byte[32];
		byte digest[] = md5.digest(ownerPad);
		if (revision == STANDARD_ENCRYPTION_128 || revision == AES_128) {
			byte mkey[] = new byte[keyLength / 8];
			for (int k = 0; k < 50; ++k)
				System.arraycopy(md5.digest(digest), 0, digest, 0, mkey.length);
			System.arraycopy(userPad, 0, ownerKey, 0, 32);
			for (int i = 0; i < 20; ++i) {
				for (int j = 0; j < mkey.length; ++j)
					mkey[j] = (byte) (digest[j] ^ i);
				arcfour.prepareARCFOURKey(mkey);
				arcfour.encryptARCFOUR(ownerKey);
			}
		} else {
			arcfour.prepareARCFOURKey(digest, 0, 5);
			arcfour.encryptARCFOUR(userPad, ownerKey);
		}
		return ownerKey;
	}
	private void setupGlobalEncryptionKey(byte[] documentID, byte userPad[],
			byte ownerKey[], int permissions) {
		this.documentID = documentID;
		this.ownerKey = ownerKey;
		this.permissions = permissions;
		mkey = new byte[keyLength / 8];
		md5.reset();
		md5.update(userPad);
		md5.update(ownerKey);
		byte ext[] = new byte[4];
		ext[0] = (byte) permissions;
		ext[1] = (byte) (permissions >> 8);
		ext[2] = (byte) (permissions >> 16);
		ext[3] = (byte) (permissions >> 24);
		md5.update(ext, 0, 4);
		if (documentID != null)
			md5.update(documentID);
		if (!encryptMetadata)
			md5.update(metadataPad);
		byte digest[] = new byte[mkey.length];
		System.arraycopy(md5.digest(), 0, digest, 0, mkey.length);
		if (revision == STANDARD_ENCRYPTION_128 || revision == AES_128) {
			for (int k = 0; k < 50; ++k)
				System.arraycopy(md5.digest(digest), 0, digest, 0, mkey.length);
		}
		System.arraycopy(digest, 0, mkey, 0, mkey.length);
	}
	private void setupUserKey() {
		if (revision == STANDARD_ENCRYPTION_128 || revision == AES_128) {
			md5.update(pad);
			byte digest[] = md5.digest(documentID);
			System.arraycopy(digest, 0, userKey, 0, 16);
			for (int k = 16; k < 32; ++k)
				userKey[k] = 0;
			for (int i = 0; i < 20; ++i) {
				for (int j = 0; j < mkey.length; ++j)
					digest[j] = (byte) (mkey[j] ^ i);
				arcfour.prepareARCFOURKey(digest, 0, mkey.length);
				arcfour.encryptARCFOUR(userKey, 0, 16);
			}
		} else {
			arcfour.prepareARCFOURKey(mkey);
			arcfour.encryptARCFOUR(pad, userKey);
		}
	}
    public void setupAllKeys(byte userPassword[], byte ownerPassword[], int permissions) {
	if( ownerPassword== null || ownerPassword.length== 0 ) {
	    ownerPassword= new byte[userPassword.length];
	    System.arraycopy( userPassword, 0, ownerPassword, 0, userPassword.length );
	}
	permissions |= (revision == STANDARD_ENCRYPTION_128 || revision == AES_128) ? 0xfffff0c0 : 0xffffffc0;
        permissions &= 0xfffffffc;
        byte userPad[] = padPassword(userPassword);
        byte ownerPad[] = padPassword(ownerPassword);
        this.ownerKey = computeOwnerKey(userPad, ownerPad);
        documentID = createDocumentId();
        setupByUserPad(this.documentID, userPad, this.ownerKey, permissions);
    }
    public static byte[] createDocumentId() {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        }
        catch (Exception e) {
             throw new ExceptionConverter(e);
       }
        long time = System.currentTimeMillis();
        long mem = Runtime.getRuntime().freeMemory();
        String s = time + "+" + mem + "+" + (seq++);
        return md5.digest(s.getBytes());
    }
    public void setupByUserPassword(byte[] documentID, byte userPassword[], byte ownerKey[], int permissions) {
        setupByUserPad(documentID, padPassword(userPassword), ownerKey, permissions);
    }
    private void setupByUserPad(byte[] documentID, byte userPad[], byte ownerKey[], int permissions) {
        setupGlobalEncryptionKey(documentID, userPad, ownerKey, permissions);
        setupUserKey();
    }
    public void setupByOwnerPassword(byte[] documentID, byte ownerPassword[], byte userKey[], byte ownerKey[], int permissions) {
        setupByOwnerPad(documentID, padPassword(ownerPassword), userKey, ownerKey, permissions);
    }
    private void setupByOwnerPad(byte[] documentID, byte ownerPad[], byte userKey[], byte ownerKey[], int permissions) {
        byte userPad[] = computeOwnerKey(ownerKey, ownerPad);
        setupGlobalEncryptionKey(documentID, userPad, ownerKey, permissions);
        setupUserKey();
    }
	public void setupByEncryptionKey(byte[] key, int keylength) {
		mkey = new byte[keylength / 8];
		System.arraycopy(key, 0, mkey, 0, mkey.length);
	}
	public void setHashKey(int number, int generation) {
		md5.reset();
		extra[0] = (byte) number;
		extra[1] = (byte) (number >> 8);
		extra[2] = (byte) (number >> 16);
		extra[3] = (byte) generation;
		extra[4] = (byte) (generation >> 8);
		md5.update(mkey);
		md5.update(extra);
		if (revision == AES_128)
			md5.update(salt);
		key = md5.digest();
		keySize = mkey.length + 5;
		if (keySize > 16)
			keySize = 16;
	}
	public static PdfObject createInfoId(byte id[]) {
		ByteBuffer buf = new ByteBuffer(90);
		buf.append('[').append('<');
		for (int k = 0; k < 16; ++k)
			buf.appendHex(id[k]);
		buf.append('>').append('<');
		id = createDocumentId();
		for (int k = 0; k < 16; ++k)
			buf.appendHex(id[k]);
		buf.append('>').append(']');
		return new PdfLiteral(buf.toByteArray());
	}
    public PdfDictionary getEncryptionDictionary() {
      PdfDictionary dic = new PdfDictionary();
      dic.put(PdfName.FILTER, PdfName.STANDARD);
      dic.put(PdfName.O, new PdfLiteral(PdfContentByte.escapeString(ownerKey)));
      dic.put(PdfName.U, new PdfLiteral(PdfContentByte.escapeString(userKey)));
      dic.put(PdfName.P, new PdfNumber(permissions));
      dic.put(PdfName.R, new PdfNumber(revision));
      if (revision == STANDARD_ENCRYPTION_40) {
	dic.put(PdfName.V, new PdfNumber(1));
      }
      else if (revision == STANDARD_ENCRYPTION_128 && encryptMetadata) {
	dic.put(PdfName.V, new PdfNumber(2));
	dic.put(PdfName.LENGTH, new PdfNumber(128));
      }
      else {
	if (!encryptMetadata)
	  dic.put(PdfName.ENCRYPTMETADATA, PdfBoolean.PDFFALSE);
	dic.put(PdfName.R, new PdfNumber(AES_128));
	dic.put(PdfName.V, new PdfNumber(4));
	dic.put(PdfName.LENGTH, new PdfNumber(128));
	PdfDictionary stdcf = new PdfDictionary();
	stdcf.put(PdfName.LENGTH, new PdfNumber(16));
	if (embeddedFilesOnly) {
	  stdcf.put(PdfName.AUTHEVENT, PdfName.EFOPEN);
	  dic.put(PdfName.EFF, PdfName.STDCF);
	  dic.put(PdfName.STRF, PdfName.IDENTITY);
	  dic.put(PdfName.STMF, PdfName.IDENTITY);
	}
	else {
	  stdcf.put(PdfName.AUTHEVENT, PdfName.DOCOPEN);
	  dic.put(PdfName.STRF, PdfName.STDCF);
	  dic.put(PdfName.STMF, PdfName.STDCF);
	}
	if (revision == AES_128)
	  stdcf.put(PdfName.CFM, PdfName.AESV2);
	else
	  stdcf.put(PdfName.CFM, PdfName.V2);
	PdfDictionary cf = new PdfDictionary();
	cf.put(PdfName.STDCF, stdcf);
	dic.put(PdfName.CF, cf);
      }
      return dic;
    }
	public PdfObject getFileID() {
		return createInfoId(documentID);
	}
	public OutputStreamEncryption getEncryptionStream(OutputStream os) {
		return new OutputStreamEncryption(os, key, 0, keySize, revision);
	}
	public int calculateStreamSize(int n) {
		if (revision == AES_128)
			return (n & 0x7ffffff0) + 32;
		else
			return n;
	}
	public byte[] encryptByteArray(byte[] b) {
		try {
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			OutputStreamEncryption os2 = getEncryptionStream(ba);
			os2.write(b);
			os2.finish();
			return ba.toByteArray();
		} catch (IOException ex) {
			throw new ExceptionConverter(ex);
		}
	}
	public StandardDecryption getDecryptor() {
		return new StandardDecryption(key, 0, keySize, revision);
	}
	public byte[] decryptByteArray(byte[] b) {
		try {
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			StandardDecryption dec = getDecryptor();
			byte[] b2 = dec.update(b, 0, b.length);
			if (b2 != null)
				ba.write(b2);
			b2 = dec.finish();
			if (b2 != null)
				ba.write(b2);
			return ba.toByteArray();
		} catch (IOException ex) {
			throw new ExceptionConverter(ex);
		}
	}
	public byte[] computeUserPassword(byte[] ownerPassword) {
		byte[] userPad = computeOwnerKey(ownerKey, padPassword(ownerPassword));
		for (int i = 0; i < userPad.length; i++) {
			boolean match = true;
			for (int j = 0; j < userPad.length - i; j++) {
				if (userPad[i + j] != pad[j]) {
					match = false;
					break;
                }
			}
			if (!match) continue;
			byte[] userPassword = new byte[i];
			System.arraycopy(userPad, 0, userPassword, 0, i);
			return userPassword;
		}
		return userPad;
	}
    byte state[] = new byte[256];
    int x;
    int y;
    public void prepareKey() {
        prepareRC4Key(key, 0, keySize);
    }
    public void prepareRC4Key(byte kk[]) {
        prepareRC4Key(kk, 0, kk.length);
    }
    public void prepareRC4Key(byte kk[], int off, int len) {
        int index1 = 0;
        int index2 = 0;
        for (int k = 0; k < 256; ++k)
            state[k] = (byte)k;
        x = 0;
        y = 0;
        byte tmp;
        for (int k = 0; k < 256; ++k) {
            index2 = (kk[index1 + off] + state[k] + index2) & 255;
            tmp = state[k];
            state[k] = state[index2];
            state[index2] = tmp;
            index1 = (index1 + 1) % len;
        }
    }
    public void encryptRC4(byte dataIn[], int off, int len, byte dataOut[]) {
        int length = len + off;
        byte tmp;
        for (int k = off; k < length; ++k) {
            x = (x + 1) & 255;
            y = (state[x] + y) & 255;
            tmp = state[x];
            state[x] = state[y];
            state[y] = tmp;
            dataOut[k] = (byte)(dataIn[k] ^ state[(state[x] + state[y]) & 255]);
        }
    }
    public void encryptRC4(byte data[], int off, int len) {
        encryptRC4(data, off, len, data);
    }
    public void encryptRC4(byte dataIn[], byte dataOut[]) {
        encryptRC4(dataIn, 0, dataIn.length, dataOut);
    }
    public void encryptRC4(byte data[]) {
        encryptRC4(data, 0, data.length, data);
    }
}