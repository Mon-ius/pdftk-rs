package pdftk.org.bouncycastle.crypto.engines;
import pdftk.org.bouncycastle.crypto.BlockCipher;
import pdftk.org.bouncycastle.crypto.CipherParameters;
import pdftk.org.bouncycastle.crypto.DataLengthException;
import pdftk.org.bouncycastle.crypto.params.KeyParameter;
public class RC6Engine
    implements BlockCipher
{
    private static final int wordSize = 32;
    private static final int bytesPerWord = wordSize / 8;
    private static final int _noRounds = 20;
    private int _S[];
    private static final int    P32 = 0xb7e15163;
    private static final int    Q32 = 0x9e3779b9;
    private static final int    LGW = 5;
    private boolean forEncryption;
    public RC6Engine()
    {
        _S            = null;
    }
    public String getAlgorithmName()
    {
        return "RC6";
    }
    public int getBlockSize()
    {
        return 4 * bytesPerWord;
    }
    public void init(
        boolean             forEncryption,
        CipherParameters    params)
    {
        if (!(params instanceof KeyParameter))
        {
            throw new IllegalArgumentException("invalid parameter passed to RC6 init - " + params.getClass().getName());
        }
        KeyParameter       p = (KeyParameter)params;
        this.forEncryption = forEncryption;
        setKey(p.getKey());
    }
    public int processBlock(
        byte[]  in,
        int     inOff,
        byte[]  out,
        int     outOff)
    {
        int blockSize = getBlockSize();
        if (_S == null)
        {
            throw new IllegalStateException("RC6 engine not initialised");
        }
        if ((inOff + blockSize) > in.length)
        {
            throw new DataLengthException("input buffer too short");
        }
        if ((outOff + blockSize) > out.length)
        {
            throw new DataLengthException("output buffer too short");
        }
        return (forEncryption)
            ?   encryptBlock(in, inOff, out, outOff)
            :   decryptBlock(in, inOff, out, outOff);
    }
    public void reset()
    {
    }
    private void setKey(
        byte[]      key)
    {
        int c = (key.length + (bytesPerWord - 1)) / bytesPerWord;
        if (c == 0)
        {
            c = 1;
        }
        int[]   L = new int[(key.length + bytesPerWord - 1) / bytesPerWord];
        for (int i = key.length - 1; i >= 0; i--)
        {
            L[i / bytesPerWord] = (L[i / bytesPerWord] << 8) + (key[i] & 0xff);
        }
        _S            = new int[2+2*_noRounds+2];
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
        int A = 0;
        int B = 0;
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
        int A = bytesToWord(in, inOff);
        int B = bytesToWord(in, inOff + bytesPerWord);
        int C = bytesToWord(in, inOff + bytesPerWord*2);
        int D = bytesToWord(in, inOff + bytesPerWord*3);
        B += _S[0];
        D += _S[1];
        for (int i = 1; i <= _noRounds; i++)
        {
            int t = 0,u = 0;
            t = B*(2*B+1);
            t = rotateLeft(t,5);
            u = D*(2*D+1);
            u = rotateLeft(u,5);
            A ^= t;
            A = rotateLeft(A,u);
            A += _S[2*i];
            C ^= u;
            C = rotateLeft(C,t);
            C += _S[2*i+1];
            int temp = A;
            A = B;
            B = C;
            C = D;
            D = temp;
        }
        A += _S[2*_noRounds+2];
        C += _S[2*_noRounds+3];
        wordToBytes(A, out, outOff);
        wordToBytes(B, out, outOff + bytesPerWord);
        wordToBytes(C, out, outOff + bytesPerWord*2);
        wordToBytes(D, out, outOff + bytesPerWord*3);
        return 4 * bytesPerWord;
    }
    private int decryptBlock(
        byte[]  in,
        int     inOff,
        byte[]  out,
        int     outOff)
    {
        int A = bytesToWord(in, inOff);
        int B = bytesToWord(in, inOff + bytesPerWord);
        int C = bytesToWord(in, inOff + bytesPerWord*2);
        int D = bytesToWord(in, inOff + bytesPerWord*3);
        C -= _S[2*_noRounds+3];
        A -= _S[2*_noRounds+2];
        for (int i = _noRounds; i >= 1; i--)
        {
            int t=0,u = 0;
            int temp = D;
            D = C;
            C = B;
            B = A;
            A = temp;
            t = B*(2*B+1);
            t = rotateLeft(t, LGW);
            u = D*(2*D+1);
            u = rotateLeft(u, LGW);
            C -= _S[2*i+1];
            C = rotateRight(C,t);
            C ^= u;
            A -= _S[2*i];
            A = rotateRight(A,u);
            A ^= t;
        }
        D -= _S[1];
        B -= _S[0];
        wordToBytes(A, out, outOff);
        wordToBytes(B, out, outOff + bytesPerWord);
        wordToBytes(C, out, outOff + bytesPerWord*2);
        wordToBytes(D, out, outOff + bytesPerWord*3);
        return 4 * bytesPerWord;
    }
    private int rotateLeft(int x, int y)
    {
        return (x << y) | (x >>> -y);
    }
    private int rotateRight(int x, int y)
    {
        return (x >>> y) | (x << -y);
    }
    private int bytesToWord(
        byte[]  src,
        int     srcOff)
    {
        int    word = 0;
        for (int i = bytesPerWord - 1; i >= 0; i--)
        {
            word = (word << 8) + (src[i + srcOff] & 0xff);
        }
        return word;
    }
    private void wordToBytes(
        int    word,
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