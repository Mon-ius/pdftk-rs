package pdftk.org.bouncycastle.crypto.engines;
import pdftk.org.bouncycastle.crypto.BlockCipher;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.params.KeyParameter;
import pdftk.org.bouncycastle.crypto.params.RC5Parameters;
public class RC532Engine
    implements BlockCipher
{
    private int _noRounds;
    private int _S[];
    private static final int P32 = 0xb7e15163;
    private static final int Q32 = 0x9e3779b9;
    private boolean forEncryption;
    public RC532Engine()
    {
        _noRounds     = 12;
        _S            = null;
    }
    public String getAlgorithmName()
    {
        return "RC5-32";
    }
    public int getBlockSize()
    {
        return 2 * 4;
    }
    public void init(
        boolean             forEncryption,
        CipherParameters    params)
    {
        if (params instanceof RC5Parameters)
        {
            RC5Parameters       p = (RC5Parameters)params;
            _noRounds     = p.getRounds();
            setKey(p.getKey());
        }
        else if (params instanceof KeyParameter)
        {
            KeyParameter       p = (KeyParameter)params;
            setKey(p.getKey());
        }
        else
        {
            throw new IllegalArgumentException("invalid parameter passed to RC532 init - " + params.getClass().getName());
        }
        this.forEncryption = forEncryption;
    }
    public int processBlock(
        byte[]  in,
        int     inOff,
        byte[]  out,
        int     outOff)
    {
        return (forEncryption) ? encryptBlock(in, inOff, out, outOff)
                                    : decryptBlock(in, inOff, out, outOff);
    }
    public void reset()
    {
    }
    private void setKey(
        byte[]      key)
    {
        int[]   L = new int[(key.length + (4 - 1)) / 4];
        for (int i = 0; i != key.length; i++)
        {
            L[i / 4] += (key[i] & 0xff) << (8 * (i % 4));
        }
        _S            = new int[2*(_noRounds + 1)];
        _S[0] = P32;
        for (int i=1; i < _S.length; i++)
        {
            _S[i] = (_S[i-1] + Q32);
        }
        int iter;
        if (L.length > _S.length)
        {
            iter = 3 * L.length;
        }
        else
        {
            iter = 3 * _S.length;
        }
        int A = 0, B = 0;
        int i = 0, j = 0;
        for (int k = 0; k < iter; k++)
        {
            A = _S[i] = rotateLeft(_S[i] + A + B, 3);
            B =  L[j] = rotateLeft(L[j] + A + B, A+B);
            i = (i+1) % _S.length;
            j = (j+1) %  L.length;
        }
    }
    private int encryptBlock(
        byte[]  in,
        int     inOff,
        byte[]  out,
        int     outOff)
    {
        int A = bytesToWord(in, inOff) + _S[0];
        int B = bytesToWord(in, inOff + 4) + _S[1];
        for (int i = 1; i <= _noRounds; i++)
        {
            A = rotateLeft(A ^ B, B) + _S[2*i];
            B = rotateLeft(B ^ A, A) + _S[2*i+1];
        }
        wordToBytes(A, out, outOff);
        wordToBytes(B, out, outOff + 4);
        return 2 * 4;
    }
    private int decryptBlock(
        byte[]  in,
        int     inOff,
        byte[]  out,
        int     outOff)
    {
        int A = bytesToWord(in, inOff);
        int B = bytesToWord(in, inOff + 4);
        for (int i = _noRounds; i >= 1; i--)
        {
            B = rotateRight(B - _S[2*i+1], A) ^ A;
            A = rotateRight(A - _S[2*i],   B) ^ B;
        }
        wordToBytes(A - _S[0], out, outOff);
        wordToBytes(B - _S[1], out, outOff + 4);
        return 2 * 4;
    }
    private int rotateLeft(int x, int y)
    {
        return ((x << (y & (32-1))) | (x >>> (32 - (y & (32-1)))));
    }
    private int rotateRight(int x, int y)
    {
        return ((x >>> (y & (32-1))) | (x << (32 - (y & (32-1)))));
    }
    private int bytesToWord(
        byte[]  src,
        int     srcOff)
    {
        return (src[srcOff] & 0xff) | ((src[srcOff + 1] & 0xff) << 8)
            | ((src[srcOff + 2] & 0xff) << 16) | ((src[srcOff + 3] & 0xff) << 24);
    }
    private void wordToBytes(
        int    word,
        byte[]  dst,
        int     dstOff)
    {
        dst[dstOff] = (byte)word;
        dst[dstOff + 1] = (byte)(word >> 8);
        dst[dstOff + 2] = (byte)(word >> 16);
        dst[dstOff + 3] = (byte)(word >> 24);
    }
}