package pdftk.org.bouncycastle.crypto.engines;
import pdftk.org.bouncycastle.crypto.BlockCipher;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.params.RC5Parameters;
public class RC564Engine
    implements BlockCipher
{
    private static final int wordSize = 64;
    private static final int bytesPerWord = wordSize / 8;
    private int _noRounds;
    private long _S[];
    private static final long P64 = 0xb7e151628aed2a6bL;
    private static final long Q64 = 0x9e3779b97f4a7c15L;
    private boolean forEncryption;
    public RC564Engine()
    {
        _noRounds     = 12;
        _S            = null;
    }
    public String getAlgorithmName()
    {
        return "RC5-64";
    }
    public int getBlockSize()
    {
        return 2 * bytesPerWord;
    }
    public void init(
        boolean             forEncryption,
        CipherParameters    params)
    {
        if (!(params instanceof RC5Parameters))
        {
            throw new IllegalArgumentException("invalid parameter passed to RC564 init - " + params.getClass().getName());
        }
        RC5Parameters       p = (RC5Parameters)params;
        this.forEncryption = forEncryption;
        _noRounds     = p.getRounds();
        setKey(p.getKey());
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
        long[]   L = new long[(key.length + (bytesPerWord - 1)) / bytesPerWord];
        for (int i = 0; i != key.length; i++)
        {
            L[i / bytesPerWord] += (long)(key[i] & 0xff) << (8 * (i % bytesPerWord));
        }
        _S            = new long[2*(_noRounds + 1)];
        _S[0] = P64;
        for (int i=1; i < _S.length; i++)
        {
            _S[i] = (_S[i-1] + Q64);
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
        long A = 0, B = 0;
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
        long A = bytesToWord(in, inOff) + _S[0];
        long B = bytesToWord(in, inOff + bytesPerWord) + _S[1];
        for (int i = 1; i <= _noRounds; i++)
        {
            A = rotateLeft(A ^ B, B) + _S[2*i];
            B = rotateLeft(B ^ A, A) + _S[2*i+1];
        }
        wordToBytes(A, out, outOff);
        wordToBytes(B, out, outOff + bytesPerWord);
        return 2 * bytesPerWord;
    }
    private int decryptBlock(
        byte[]  in,
        int     inOff,
        byte[]  out,
        int     outOff)
    {
        long A = bytesToWord(in, inOff);
        long B = bytesToWord(in, inOff + bytesPerWord);
        for (int i = _noRounds; i >= 1; i--)
        {
            B = rotateRight(B - _S[2*i+1], A) ^ A;
            A = rotateRight(A - _S[2*i],   B) ^ B;
        }
        wordToBytes(A - _S[0], out, outOff);
        wordToBytes(B - _S[1], out, outOff + bytesPerWord);
        return 2 * bytesPerWord;
    }
    private long rotateLeft(long x, long y)
    {
        return ((x << (y & (wordSize-1))) | (x >>> (wordSize - (y & (wordSize-1)))));
    }
    private long rotateRight(long x, long y)
    {
        return ((x >>> (y & (wordSize-1))) | (x << (wordSize - (y & (wordSize-1)))));
    }
    private long bytesToWord(
        byte[]  src,
        int     srcOff)
    {
        long    word = 0;
        for (int i = bytesPerWord - 1; i >= 0; i--)
        {
            word = (word << 8) + (src[i + srcOff] & 0xff);
        }
        return word;
    }
    private void wordToBytes(
        long    word,
        byte[]  dst,
        int     dstOff)
    {
        for (int i = 0; i < bytesPerWord; i++)
        {
            dst[i + dstOff] = (byte)word;
            word >>>= 8;
        }
    }
}