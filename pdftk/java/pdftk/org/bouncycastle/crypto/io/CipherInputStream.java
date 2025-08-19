package pdftk.org.bouncycastle.crypto.io;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import pdftk.org.bouncycastle.crypto.BufferedBlockCipher;
import pdftk.org.bouncycastle.crypto.StreamCipher;
public class CipherInputStream
    extends FilterInputStream
{
    private BufferedBlockCipher bufferedBlockCipher;
    private StreamCipher streamCipher;
    private byte[] buf;
    private byte[] inBuf;
    private int bufOff;
    private int maxBuf;
    private boolean finalized;
    private static final int INPUT_BUF_SIZE = 2048;
    public CipherInputStream(
        InputStream is,
        BufferedBlockCipher cipher)
    {
        super(is);
        this.bufferedBlockCipher = cipher;
        buf = new byte[cipher.getOutputSize(INPUT_BUF_SIZE)];
        inBuf = new byte[INPUT_BUF_SIZE];
    }
    public CipherInputStream(
        InputStream is,
        StreamCipher cipher)
    {
        super(is);
        this.streamCipher = cipher;
        buf = new byte[INPUT_BUF_SIZE];
        inBuf = new byte[INPUT_BUF_SIZE];
    }
    private int nextChunk()
        throws IOException
    {
        int available = super.available();
        if (available <= 0)
        {
            available = 1;
        }
        if (available > inBuf.length)
        {
            available = super.read(inBuf, 0, inBuf.length);
        }
        else
        {
            available = super.read(inBuf, 0, available);
        }
        if (available < 0)
        {
            if (finalized)
            {
                return -1;
            }
            try
            {
                if (bufferedBlockCipher != null)
                {
                    maxBuf = bufferedBlockCipher.doFinal(buf, 0);
                }
                else
                {
                    maxBuf = 0;
                }
            }
            catch (Exception e)
            {
                throw new IOException("error processing stream: " + e.toString());
            }
            bufOff = 0;
            finalized = true;
            if (bufOff == maxBuf)
            {
                return -1;
            }
        }
        else
        {
            bufOff = 0;
            try
            {
                if (bufferedBlockCipher != null)
                {
                    maxBuf = bufferedBlockCipher.processBytes(inBuf, 0, available, buf, 0);
                }
                else
                {
                    streamCipher.processBytes(inBuf, 0, available, buf, 0);
                    maxBuf = available;
                }
            }
            catch (Exception e)
            {
                throw new IOException("error processing stream: " + e.toString());
            }
            if (maxBuf == 0)
            {
                return nextChunk();
            }
        }
        return maxBuf;
    }
    public int read()
        throws IOException
    {
        if (bufOff == maxBuf)
        {
            if (nextChunk() < 0)
            {
                return -1;
            }
        }
        return buf[bufOff++] & 0xff;
    }
    public int read(
        byte[] b)
        throws IOException
    {
        return read(b, 0, b.length);
    }
    public int read(
        byte[] b,
        int off,
        int len)
        throws IOException
    {
        if (bufOff == maxBuf)
        {
            if (nextChunk() < 0)
            {
                return -1;
            }
        }
        int available = maxBuf - bufOff;
        if (len > available)
        {
            System.arraycopy(buf, bufOff, b, off, available);
            bufOff = maxBuf;
            return available;
        }
        else
        {
            System.arraycopy(buf, bufOff, b, off, len);
            bufOff += len;
            return len;
        }
    }
    public long skip(
        long n)
        throws IOException
    {
        if (n <= 0)
        {
            return 0;
        }
        int available = maxBuf - bufOff;
        if (n > available)
        {
            bufOff = maxBuf;
            return available;
        }
        else
        {
            bufOff += (int)n;
            return (int)n;
        }
    }
    public int available()
        throws IOException
    {
        return maxBuf - bufOff;
    }
    public void close()
        throws IOException
    {
        super.close();
    }
    public boolean markSupported()
    {
        return false;
    }
}