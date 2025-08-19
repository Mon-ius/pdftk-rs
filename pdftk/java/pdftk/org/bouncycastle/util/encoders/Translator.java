package pdftk.org.bouncycastle.util.encoders;
public interface Translator
{
    public int getEncodedBlockSize();
    public int encode(byte[] in, int inOff, int length, byte[] out, int outOff);
    public int getDecodedBlockSize();
    public int decode(byte[] in, int inOff, int length, byte[] out, int outOff);
}