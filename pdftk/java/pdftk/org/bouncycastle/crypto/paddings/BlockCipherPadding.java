package pdftk.org.bouncycastle.crypto.paddings;
import java.security.SecureRandom;
import pdftk.org.bouncycastle.crypto.InvalidCipherTextException;
public interface BlockCipherPadding
{
    public void init(SecureRandom random)
        throws IllegalArgumentException;
    public String getPaddingName();
    public int addPadding(byte[] in, int inOff);
    public int padCount(byte[] in)
        throws InvalidCipherTextException;
}