package pdftk.org.bouncycastle.crypto;
public interface SignerWithRecovery
    extends Signer
{
    public boolean hasFullMessage();
    public byte[] getRecoveredMessage();
    public void updateWithRecoveredMessage(byte[] signature)
        throws InvalidCipherTextException;
}