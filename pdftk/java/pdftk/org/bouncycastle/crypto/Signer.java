package pdftk.org.bouncycastle.crypto;
public interface Signer
{
    public void init(boolean forSigning, CipherParameters param);
    public void update(byte b);
    public void update(byte[] in, int off, int len);
    public byte[] generateSignature()
        throws CryptoException, DataLengthException;
    public boolean verifySignature(byte[] signature);
    public void reset();
}