package pdftk.com.lowagie.text.pdf;
import java.io.IOException;
import java.io.OutputStream;
public class PdfString extends PdfObject {
    protected String value= null;
    protected byte[] originalBytes= null;
    protected String encoding= null;
    protected int objNum = 0;
    protected int objGen = 0;
    protected boolean hexWriting = false;
    public PdfString() {
        super(STRING);
    }
    public PdfString(String value) {
        super(STRING);
        this.value = value;
    }
    public PdfString(String value, String encoding) {
        super(STRING);
        this.value = value;
        this.encoding = encoding;
    }
    public PdfString(byte[] bytes) {
        super(STRING);
	this.bytes = bytes;
    }
    public PdfString(byte[] bytes, String encoding) {
        super(STRING);
	this.bytes = bytes;
	this.encoding = encoding;
    }
    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        byte b[] = getBytes();
        PdfEncryption crypto = null;
        if (writer != null)
            crypto = writer.getEncryption();
        if (crypto != null && !crypto.isEmbeddedFilesOnly())
            b = crypto.encryptByteArray(b);
        if (hexWriting) {
            ByteBuffer buf = new ByteBuffer();
            buf.append('<');
            int len = b.length;
            for (int k = 0; k < len; ++k)
                buf.appendHex(b[k]);
            buf.append('>');
            os.write(buf.toByteArray());
        }
        else
            os.write(PdfContentByte.escapeString(b));
    }
    public String toString() {
        return getValue();
    }
    public byte[] getBytes() {
        if( bytes== null && value!= null ) {
	    if( encoding== null ) {
		if( PdfEncodings.isPdfDocEncoding( value ) ) {
		    encoding= TEXT_PDFDOCENCODING;
		}
		else {
		    encoding= TEXT_UNICODE;
		}
	    }
	    bytes= PdfEncodings.convertToBytes( value, encoding );
        }
        return bytes;
    }
    private String getValue() {
	if( value== null && bytes!= null ) {
	    if( encoding== null ) {
		if( isUnicode( bytes ) ) {
		    encoding= TEXT_UNICODE;
		}
		else {
		    encoding= TEXT_PDFDOCENCODING;
		}
	    }
	    value= PdfEncodings.convertToString( bytes, encoding );
	}
	return value;
    }
    public String toUnicodeString() {
	return getValue();
    }
    public String getEncoding() {
        return encoding;
    }
    void setObjNum(int objNum, int objGen) {
        this.objNum = objNum;
        this.objGen = objGen;
    }
    void decrypt(PdfReader reader) {
        PdfEncryption decrypt = reader.getDecrypt();
        if (decrypt != null) {
	    getBytes();
	    originalBytes= new byte[ bytes.length ];
	    System.arraycopy( bytes, 0, originalBytes, 0, bytes.length );
            decrypt.setHashKey(objNum, objGen);
            bytes = decrypt.decryptByteArray(bytes);
	    value = null;
	    encoding = null;
        }
    }
    public byte[] getOriginalBytes() {
        if( originalBytes!= null ) {
	    return originalBytes;
	}
	return getBytes();
    }
    public PdfString setHexWriting(boolean hexWriting) {
        this.hexWriting = hexWriting;
        return this;
    }
    public boolean isHexWriting() {
        return hexWriting;
    }
    public static boolean isUnicode( byte[] bb ) {
	return( bb.length >= 2 && bb[0] == (byte)254 && bb[1] == (byte)255 );
    }
}