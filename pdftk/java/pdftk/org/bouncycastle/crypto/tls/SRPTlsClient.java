package pdftk.org.bouncycastle.crypto.tls;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import pdftk.org.bouncycastle.util.Arrays;
import pdftk.org.bouncycastle.util.Integers;
public abstract class SRPTlsClient
    implements TlsClient
{
    public static final Integer EXT_SRP = Integers.valueOf(ExtensionType.srp);
    protected TlsCipherFactory cipherFactory;
    protected byte[] identity;
    protected byte[] password;
    protected TlsClientContext context;
    protected int selectedCompressionMethod;
    protected int selectedCipherSuite;
    public SRPTlsClient(byte[] identity, byte[] password)
    {
        this(new DefaultTlsCipherFactory(), identity, password);
    }
    public SRPTlsClient(TlsCipherFactory cipherFactory, byte[] identity, byte[] password)
    {
        this.cipherFactory = cipherFactory;
        this.identity = Arrays.clone(identity);
        this.password = Arrays.clone(password);
    }
    public void init(TlsClientContext context)
    {
        this.context = context;
    }
    public ProtocolVersion getClientVersion()
    {
        return ProtocolVersion.TLSv10;
    }
    public int[] getCipherSuites()
    {
        return new int[] {
            CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA,
            CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA,
            CipherSuite.TLS_SRP_SHA_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_SRP_SHA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA, };
    }
    public Hashtable getClientExtensions() throws IOException
    {
        Hashtable clientExtensions = new Hashtable();
        ByteArrayOutputStream srpData = new ByteArrayOutputStream();
        TlsUtils.writeOpaque8(this.identity, srpData);
        clientExtensions.put(EXT_SRP, srpData.toByteArray());
        return clientExtensions;
    }
    public short[] getCompressionMethods()
    {
        return new short[] { CompressionMethod.NULL };
    }
    public void notifyServerVersion(ProtocolVersion serverVersion) throws IOException
    {
        if (!ProtocolVersion.TLSv10.equals(serverVersion))
        {
            throw new TlsFatalAlert(AlertDescription.illegal_parameter);
        }
    }
    public void notifySessionID(byte[] sessionID)
    {
    }
    public void notifySelectedCipherSuite(int selectedCipherSuite)
    {
        this.selectedCipherSuite = selectedCipherSuite;
    }
    public void notifySelectedCompressionMethod(short selectedCompressionMethod)
    {
        this.selectedCompressionMethod = selectedCompressionMethod;
    }
    public void notifySecureRenegotiation(boolean secureRenegotiation) throws IOException
    {
        if (!secureRenegotiation)
        {
        }
    }
    public void processServerExtensions(Hashtable serverExtensions)
    {
    }
    public TlsKeyExchange getKeyExchange() throws IOException
    {
        switch (selectedCipherSuite)
        {
            case CipherSuite.TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA:
            case CipherSuite.TLS_SRP_SHA_WITH_AES_128_CBC_SHA:
            case CipherSuite.TLS_SRP_SHA_WITH_AES_256_CBC_SHA:
                return createSRPKeyExchange(KeyExchangeAlgorithm.SRP);
            case CipherSuite.TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA:
            case CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA:
            case CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA:
                return createSRPKeyExchange(KeyExchangeAlgorithm.SRP_RSA);
            case CipherSuite.TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA:
            case CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA:
            case CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA:
                return createSRPKeyExchange(KeyExchangeAlgorithm.SRP_DSS);
            default:
                throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }
    public TlsCompression getCompression() throws IOException
    {
        switch (selectedCompressionMethod)
        {
            case CompressionMethod.NULL:
                return new TlsNullCompression();
            default:
                throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }
    public TlsCipher getCipher() throws IOException
    {
        switch (selectedCipherSuite)
        {
            case CipherSuite.TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA:
            case CipherSuite.TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA:
            case CipherSuite.TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA:
                return cipherFactory.createCipher(context, EncryptionAlgorithm._3DES_EDE_CBC,
                    DigestAlgorithm.SHA);
            case CipherSuite.TLS_SRP_SHA_WITH_AES_128_CBC_SHA:
            case CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA:
            case CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA:
                return cipherFactory.createCipher(context, EncryptionAlgorithm.AES_128_CBC,
                    DigestAlgorithm.SHA);
            case CipherSuite.TLS_SRP_SHA_WITH_AES_256_CBC_SHA:
            case CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA:
            case CipherSuite.TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA:
                return cipherFactory.createCipher(context, EncryptionAlgorithm.AES_256_CBC,
                    DigestAlgorithm.SHA);
            default:
                throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }
    protected TlsKeyExchange createSRPKeyExchange(int keyExchange)
    {
        return new TlsSRPKeyExchange(context, keyExchange, identity, password);
    }
}