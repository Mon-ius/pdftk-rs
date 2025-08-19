package pdftk.org.bouncycastle.crypto.tls;
import java.io.IOException;
public interface TlsCipherFactory
{
    TlsCipher createCipher(TlsClientContext context, int encryptionAlgorithm, int digestAlgorithm) throws IOException;
}