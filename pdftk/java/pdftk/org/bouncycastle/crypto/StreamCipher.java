package pdftk.org.bouncycastle.crypto;
public interface StreamCipher
{
    public void init(boolean forEncryption, CipherParameters params)
        throws IllegalArgumentException;
    public String getAlgorithmName();
    public byte returnByte(byte in);
    public void processBytes(byte[] in, int inOff, int len, byte[] out, int outOff)
        throws DataLengthException;
    public void reset();
}