package pdftk.org.bouncycastle.crypto.macs;
import pdftk.org.bouncycastle.crypto.BlockCipher;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.Mac;
import pdftk.org.bouncycastle.crypto.modes.CBCBlockCipher;
import pdftk.org.bouncycastle.crypto.paddings.ISO7816d4Padding;
public class CMac implements Mac
{
    private static final byte CONSTANT_128 = (byte)0x87;
    private static final byte CONSTANT_64 = (byte)0x1b;
    private byte[] ZEROES;
    private byte[] mac;
    private byte[] buf;
    private int bufOff;
    private BlockCipher cipher;
    private int macSize;
    private byte[] L, Lu, Lu2;
    public CMac(BlockCipher cipher)
    {
        this(cipher, cipher.getBlockSize() * 8);
    }
    public CMac(BlockCipher cipher, int macSizeInBits)
    {
        if ((macSizeInBits % 8) != 0)
        {
            throw new IllegalArgumentException("MAC size must be multiple of 8");
        }
        if (macSizeInBits > (cipher.getBlockSize() * 8))
        {
            throw new IllegalArgumentException(
                "MAC size must be less or equal to "
                    + (cipher.getBlockSize() * 8));
        }
        if (cipher.getBlockSize() != 8 && cipher.getBlockSize() != 16)
        {
            throw new IllegalArgumentException(
                "Block size must be either 64 or 128 bits");
        }
        this.cipher = new CBCBlockCipher(cipher);
        this.macSize = macSizeInBits / 8;
        mac = new byte[cipher.getBlockSize()];
        buf = new byte[cipher.getBlockSize()];
        ZEROES = new byte[cipher.getBlockSize()];
        bufOff = 0;
    }
    public String getAlgorithmName()
    {
        return cipher.getAlgorithmName();
    }
    private static byte[] doubleLu(byte[] in)
    {
        int FirstBit = (in[0] & 0xFF) >> 7;
        byte[] ret = new byte[in.length];
        for (int i = 0; i < in.length - 1; i++)
        {
            ret[i] = (byte)((in[i] << 1) + ((in[i + 1] & 0xFF) >> 7));
        }
        ret[in.length - 1] = (byte)(in[in.length - 1] << 1);
        if (FirstBit == 1)
        {
            ret[in.length - 1] ^= in.length == 16 ? CONSTANT_128 : CONSTANT_64;
        }
        return ret;
    }
    public void init(CipherParameters params)
    {
        if (params != null)
        {
            cipher.init(true, params);
            L = new byte[ZEROES.length];
            cipher.processBlock(ZEROES, 0, L, 0);
            Lu = doubleLu(L);
            Lu2 = doubleLu(Lu);
        }
        reset();
    }
    public int getMacSize()
    {
        return macSize;
    }
    public void update(byte in)
    {
        if (bufOff == buf.length)
        {
            cipher.processBlock(buf, 0, mac, 0);
            bufOff = 0;
        }
        buf[bufOff++] = in;
    }
    public void update(byte[] in, int inOff, int len)
    {
        if (len < 0)
        {
            throw new IllegalArgumentException(
                "Can't have a negative input length!");
        }
        int blockSize = cipher.getBlockSize();
        int gapLen = blockSize - bufOff;
        if (len > gapLen)
        {
            System.arraycopy(in, inOff, buf, bufOff, gapLen);
            cipher.processBlock(buf, 0, mac, 0);
            bufOff = 0;
            len -= gapLen;
            inOff += gapLen;
            while (len > blockSize)
            {
                cipher.processBlock(in, inOff, mac, 0);
                len -= blockSize;
                inOff += blockSize;
            }
        }
        System.arraycopy(in, inOff, buf, bufOff, len);
        bufOff += len;
    }
    public int doFinal(byte[] out, int outOff)
    {
        int blockSize = cipher.getBlockSize();
        byte[] lu;
        if (bufOff == blockSize)
        {
            lu = Lu;
        }
        else
        {
            new ISO7816d4Padding().addPadding(buf, bufOff);
            lu = Lu2;
        }
        for (int i = 0; i < mac.length; i++)
        {
            buf[i] ^= lu[i];
        }
        cipher.processBlock(buf, 0, mac, 0);
        System.arraycopy(mac, 0, out, outOff, macSize);
        reset();
        return macSize;
    }
    public void reset()
    {
        for (int i = 0; i < buf.length; i++)
        {
            buf[i] = 0;
        }
        bufOff = 0;
        cipher.reset();
    }
}