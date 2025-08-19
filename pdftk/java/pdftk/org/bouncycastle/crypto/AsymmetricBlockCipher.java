package pdftk.org.bouncycastle.crypto;
public interface AsymmetricBlockCipher
{
    public void init(boolean forEncryption, CipherParameters param);
    public int getInputBlockSize();
    public int getOutputBlockSize();
    public byte[] processBlock(byte[] in, int inOff, int len)
        throws InvalidCipherTextException;
}