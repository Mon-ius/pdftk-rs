package pdftk.org.bouncycastle.crypto.tls;
public class AlwaysValidVerifyer implements CertificateVerifyer
{
    public boolean isValid(pdftk.org.bouncycastle.asn1.x509.Certificate[] certs)
    {
        return true;
    }
}