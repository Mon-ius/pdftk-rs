package pdftk.org.bouncycastle.crypto;
public interface DerivationFunction
{
    public void init(DerivationParameters param);
    public Digest getDigest();
    public int generateBytes(byte[] out, int outOff, int len)
        throws DataLengthException, IllegalArgumentException;
}