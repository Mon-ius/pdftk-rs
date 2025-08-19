package pdftk.org.bouncycastle.crypto.tls;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import pdftk.org.bouncycastle.asn1.x509.KeyUsage;
import pdftk.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import pdftk.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import pdftk.org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import pdftk.org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import pdftk.org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import pdftk.org.bouncycastle.crypto.params.ECDomainParameters;
import pdftk.org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import pdftk.org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import pdftk.org.bouncycastle.crypto.params.ECPublicKeyParameters;
import pdftk.org.bouncycastle.crypto.util.PublicKeyFactory;
import pdftk.org.bouncycastle.util.BigIntegers;
class TlsECDHKeyExchange implements TlsKeyExchange
{
    protected TlsClientContext context;
    protected int keyExchange;
    protected TlsSigner tlsSigner;
    protected AsymmetricKeyParameter serverPublicKey;
    protected ECPublicKeyParameters ecAgreeServerPublicKey;
    protected TlsAgreementCredentials agreementCredentials;
    protected ECPrivateKeyParameters ecAgreeClientPrivateKey = null;
    TlsECDHKeyExchange(TlsClientContext context, int keyExchange)
    {
        switch (keyExchange)
        {
            case KeyExchangeAlgorithm.ECDHE_RSA:
                this.tlsSigner = new TlsRSASigner();
                break;
            case KeyExchangeAlgorithm.ECDHE_ECDSA:
                this.tlsSigner = new TlsECDSASigner();
                break;
            case KeyExchangeAlgorithm.ECDH_RSA:
            case KeyExchangeAlgorithm.ECDH_ECDSA:
                this.tlsSigner = null;
                break;
            default:
                throw new IllegalArgumentException("unsupported key exchange algorithm");
        }
        this.context = context;
        this.keyExchange = keyExchange;
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
        if (tlsSigner == null)
        {
            try
            {
                this.ecAgreeServerPublicKey = validateECPublicKey((ECPublicKeyParameters)this.serverPublicKey);
            }
            catch (ClassCastException e)
            {
                throw new TlsFatalAlert(AlertDescription.certificate_unknown);
            }
            TlsUtils.validateKeyUsage(x509Cert, KeyUsage.keyAgreement);
        }
        else
        {
            if (!tlsSigner.isValidPublicKey(this.serverPublicKey))
            {
                throw new TlsFatalAlert(AlertDescription.certificate_unknown);
            }
            TlsUtils.validateKeyUsage(x509Cert, KeyUsage.digitalSignature);
        }
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
                case ClientCertificateType.rsa_fixed_ecdh:
                case ClientCertificateType.ecdsa_fixed_ecdh:
                    break;
                default:
                    throw new TlsFatalAlert(AlertDescription.illegal_parameter);
            }
        }
    }
    public void skipClientCredentials() throws IOException
    {
        this.agreementCredentials = null;
    }
    public void processClientCredentials(TlsCredentials clientCredentials) throws IOException
    {
        if (clientCredentials instanceof TlsAgreementCredentials)
        {
            this.agreementCredentials = (TlsAgreementCredentials)clientCredentials;
        }
        else if (clientCredentials instanceof TlsSignerCredentials)
        {
        }
        else
        {
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }
    public void generateClientKeyExchange(OutputStream os) throws IOException
    {
        if (agreementCredentials == null)
        {
            generateEphemeralClientKeyExchange(ecAgreeServerPublicKey.getParameters(), os);
        }
    }
    public byte[] generatePremasterSecret() throws IOException
    {
        if (agreementCredentials != null)
        {
            return agreementCredentials.generateAgreement(ecAgreeServerPublicKey);
        }
        return calculateECDHBasicAgreement(ecAgreeServerPublicKey, ecAgreeClientPrivateKey);
    }
    protected boolean areOnSameCurve(ECDomainParameters a, ECDomainParameters b)
    {
        return a.getCurve().equals(b.getCurve()) && a.getG().equals(b.getG())
            && a.getN().equals(b.getN()) && a.getH().equals(b.getH());
    }
    protected byte[] externalizeKey(ECPublicKeyParameters keyParameters) throws IOException
    {
        return keyParameters.getQ().getEncoded();
    }
    protected AsymmetricCipherKeyPair generateECKeyPair(ECDomainParameters ecParams)
    {
        ECKeyPairGenerator keyPairGenerator = new ECKeyPairGenerator();
        ECKeyGenerationParameters keyGenerationParameters = new ECKeyGenerationParameters(ecParams,
            context.getSecureRandom());
        keyPairGenerator.init(keyGenerationParameters);
        return keyPairGenerator.generateKeyPair();
    }
    protected void generateEphemeralClientKeyExchange(ECDomainParameters ecParams, OutputStream os)
        throws IOException
    {
        AsymmetricCipherKeyPair ecAgreeClientKeyPair = generateECKeyPair(ecParams);
        this.ecAgreeClientPrivateKey = (ECPrivateKeyParameters)ecAgreeClientKeyPair.getPrivate();
        byte[] keData = externalizeKey((ECPublicKeyParameters)ecAgreeClientKeyPair.getPublic());
        TlsUtils.writeOpaque8(keData, os);
    }
    protected byte[] calculateECDHBasicAgreement(ECPublicKeyParameters publicKey,
        ECPrivateKeyParameters privateKey)
    {
        ECDHBasicAgreement basicAgreement = new ECDHBasicAgreement();
        basicAgreement.init(privateKey);
        BigInteger agreement = basicAgreement.calculateAgreement(publicKey);
        return BigIntegers.asUnsignedByteArray(agreement);
    }
    protected ECPublicKeyParameters validateECPublicKey(ECPublicKeyParameters key)
        throws IOException
    {
        return key;
    }
}