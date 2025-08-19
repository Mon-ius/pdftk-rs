package pdftk.com.lowagie.text.pdf.interfaces;
import java.security.cert.Certificate;
import pdftk.com.lowagie.text.DocumentException;
public interface PdfEncryptionSettings {
    public void setEncryption(byte userPassword[], byte ownerPassword[], int permissions, int encryptionType) throws DocumentException;
    public void setEncryption(Certificate[] certs, int[] permissions, int encryptionType) throws DocumentException;
}