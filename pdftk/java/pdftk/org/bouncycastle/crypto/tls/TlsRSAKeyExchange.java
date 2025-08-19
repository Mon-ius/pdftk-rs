package pdftk.org.bouncycastle.crypto.tls;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import pdftk.org.bouncycastle.asn1.x509.KeyUsage;
import pdftk.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import pdftk.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import pdftk.org.bouncycastle.crypto.params.RSAKeyParameters;
import pdftk.org.bouncycastle.crypto.util.PublicKeyFactory;
class TlsRSAKeyExchange implements TlsKeyExchange
{
    protected TlsClientContext context;
    protected AsymmetricKeyParameter serverPublicKey = null;
    protected RSAKeyParameters rsaServerPublicKey = null;
    protected byte[] premasterSecret;
    TlsRSAKeyExchange(TlsClientContext context)
    {
        this.context = context;
    }
    public void skipServerCertificate() throws IOException
    {
        throw new TlsFatalAlert(AlertDescription.unexpected_message);
    }
    public void processServerCertificate(Certificate serverCertificate) throws IOException
    {
        pdftk.org.bouncycastle.asn1.x509.Certificate x509Cert = serverCertificate.certs[0];
        SubjectPublicKeyInfo keyInfo = x509Cert.getSubjectPublicKeyInfo();
        try
        {
            this.serverPublicKey = PublicKeyFactory.createKey(keyInfo);
        }
        catch (RuntimeException e)
        {
            throw new TlsFatalAlert(AlertDescription.unsupported_certificate);
        }
        if (this.serverPublicKey.isPrivate())
        {
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }
        this.rsaServerPublicKey = validateRSAPublicKey((RSAKeyParameters)this.serverPublicKey);
        TlsUtils.validateKeyUsage(x509Cert, KeyUsage.keyEncipherment);
    }
    public void skipServerKeyExchange() throws IOException
    {
    }
    public void processServerKeyExchange(InputStream is)
        throws IOException
    {
        throw new TlsFatalAlert(AlertDescription.unexpected_message);
    }
    public void validateCertificateRequest(CertificateRequest certificateRequest)
        throws IOException
    {
        short[] types = certificateRequest.getCertificateTypes();
        for (int i = 0; i < types.length; ++i)
        {
            switch (types[i])
            {
                case ClientCertificateType.rsa_sign:
                case ClientCertificateType.dss_sign:
                case ClientCertificateType.ecdsa_sign:
                    break;
                default:
                    throw new TlsFatalAlert(AlertDescription.illegal_parameter);
            }
        }
    }
    public void skipClientCredentials() throws IOException
    {
    }
    public void processClientCredentials(TlsCredentials clientCredentials) throws IOException
    {
        if (!(clientCredentials instanceof TlsSignerCredentials))
        {
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }
    public void generateClientKeyExchange(OutputStream os) throws IOException
    {
        this.premasterSecret = TlsRSAUtils.generateEncryptedPreMasterSecret(context,
            this.rsaServerPublicKey, os);
    }
    public byte[] generatePremasterSecret() throws IOException
    {
        byte[] tmp = this.premasterSecret;
        this.premasterSecret = null;
        return tmp;
    }
    protected RSAKeyParameters validateRSAPublicKey(RSAKeyParameters key) throws IOException
    {
        if (!key.getExponent().isProbablePrime(2))
        {
            throw new TlsFatalAlert(AlertDescription.illegal_parameter);
        }
        return key;
    }
}