package pdftk.org.bouncycastle.crypto.paddings;
import java.security.SecureRandom;
import pdftk.org.bouncycastle.crypto.InvalidCipherTextException;
public class ISO10126d2Padding
    implements BlockCipherPadding
{
    SecureRandom    random;
    public void init(SecureRandom random)
        throws IllegalArgumentException
    {
        if (random != null)
        {
            this.random = random;
        }
        else
        {
            this.random = new SecureRandom();
        }
    }
    public String getPaddingName()
    {
        return "ISO10126-2";
    }
    public int addPadding(
        byte[]  in,
        int     inOff)
    {
        byte code = (byte)(in.length - inOff);
        while (inOff < (in.length - 1))
        {
            in[inOff] = (byte)random.nextInt();
            inOff++;
        }
        in[inOff] = code;
        return code;
    }
    public int padCount(byte[] in)
        throws InvalidCipherTextException
    {
        int count = in[in.length - 1] & 0xff;
        if (count > in.length)
        {
            throw new InvalidCipherTextException("pad block corrupted");
        }
        return count;
    }
}