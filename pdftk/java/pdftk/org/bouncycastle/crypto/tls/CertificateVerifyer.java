package pdftk.org.bouncycastle.crypto.tls;
public interface CertificateVerifyer
{
    public boolean isValid(pdftk.org.bouncycastle.asn1.x509.Certificate[] certs);
}