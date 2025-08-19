package pdftk.org.bouncycastle.crypto;
public interface Wrapper
{
    public void init(boolean forWrapping, CipherParameters param);
    public String getAlgorithmName();
    public byte[] wrap(byte[] in, int inOff, int inLen);
    public byte[] unwrap(byte[] in, int inOff, int inLen)
        throws InvalidCipherTextException;
}