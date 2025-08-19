package pdftk.org.bouncycastle.crypto.modes;
import pdftk.org.bouncycastle.crypto.BlockCipher;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.DataLengthException;
import pdftk.org.bouncycastle.crypto.InvalidCipherTextException;
public interface AEADBlockCipher
{
    public void init(boolean forEncryption, CipherParameters params)
        throws IllegalArgumentException;
    public String getAlgorithmName();
    public BlockCipher getUnderlyingCipher();
    public void processAADByte(byte in);
    public void processAADBytes(byte[] in, int inOff, int len);
    public int processByte(byte in, byte[] out, int outOff)
        throws DataLengthException;
    public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff)
        throws DataLengthException;
    public int doFinal(byte[] out, int outOff)
        throws IllegalStateException, InvalidCipherTextException;
    public byte[] getMac();
    public int getUpdateOutputSize(int len);
    public int getOutputSize(int len);
    public void reset();
}