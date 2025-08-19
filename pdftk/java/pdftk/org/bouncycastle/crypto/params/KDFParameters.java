package pdftk.org.bouncycastle.crypto.params;
import pdftk.org.bouncycastle.crypto.DerivationParameters;
public class KDFParameters
    implements DerivationParameters
{
    byte[]  iv;
    byte[]  shared;
    public KDFParameters(
        byte[]  shared,
        byte[]  iv)
    {
        this.shared = shared;
        this.iv = iv;
    }
    public byte[] getSharedSecret()
    {
        return shared;
    }
    public byte[] getIV()
    {
        return iv;
    }
}