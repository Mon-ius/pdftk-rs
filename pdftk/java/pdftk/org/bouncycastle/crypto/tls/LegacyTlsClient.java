package pdftk.org.bouncycastle.crypto.tls;
import java.io.IOException;
public class LegacyTlsClient extends DefaultTlsClient
{
    protected CertificateVerifyer verifyer;
    public LegacyTlsClient(CertificateVerifyer verifyer)
    {
        super();
        this.verifyer = verifyer;
    }
    public TlsAuthentication getAuthentication() throws IOException
    {
        return new LegacyTlsAuthentication(verifyer);
    }
}