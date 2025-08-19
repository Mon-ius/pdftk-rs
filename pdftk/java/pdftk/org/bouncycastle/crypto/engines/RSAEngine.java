package pdftk.org.bouncycastle.crypto.engines;
import pdftk.org.bouncycastle.crypto.AsymmetricBlockCipher;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.DataLengthException;
public class RSAEngine
    implements AsymmetricBlockCipher
{
    private RSACoreEngine core;
    public void init(
        boolean             forEncryption,
        CipherParameters    param)
    {
        if (core == null)
        {
            core = new RSACoreEngine();
        }
        core.init(forEncryption, param);
    }
    public int getInputBlockSize()
    {
        return core.getInputBlockSize();
    }
    public int getOutputBlockSize()
    {
        return core.getOutputBlockSize();
    }
    public byte[] processBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
    {
        if (core == null)
        {
            throw new IllegalStateException("RSA engine not initialised");
        }
        return core.convertOutput(core.processBlock(core.convertInput(in, inOff, inLen)));
    }
}