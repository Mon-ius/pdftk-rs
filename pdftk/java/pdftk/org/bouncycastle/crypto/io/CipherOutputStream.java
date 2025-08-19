package pdftk.org.bouncycastle.crypto.io;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import pdftk.org.bouncycastle.crypto.BufferedBlockCipher;
import pdftk.org.bouncycastle.crypto.StreamCipher;
public class CipherOutputStream
    extends FilterOutputStream
{
    private BufferedBlockCipher bufferedBlockCipher;
    private StreamCipher streamCipher;
    private byte[] oneByte = new byte[1];
    private byte[] buf;
    public CipherOutputStream(
        OutputStream os,
        BufferedBlockCipher cipher)
    {
        super(os);
        this.bufferedBlockCipher = cipher;
        this.buf = new byte[cipher.getBlockSize()];
    }
    public CipherOutputStream(
        OutputStream os,
        StreamCipher cipher)
    {
        super(os);
        this.streamCipher = cipher;
    }
    public void write(
        int b)
        throws IOException
    {
        oneByte[0] = (byte)b;
        if (bufferedBlockCipher != null)
        {
            int len = bufferedBlockCipher.processBytes(oneByte, 0, 1, buf, 0);
            if (len != 0)
            {
                out.write(buf, 0, len);
            }
        }
        else
        {
            out.write(streamCipher.returnByte((byte)b));
        }
    }
    public void write(
        byte[] b)
        throws IOException
    {
        write(b, 0, b.length);
    }
    public void write(
        byte[] b,
        int off,
        int len)
        throws IOException
    {
        if (bufferedBlockCipher != null)
        {
            byte[] buf = new byte[bufferedBlockCipher.getOutputSize(len)];
            int outLen = bufferedBlockCipher.processBytes(b, off, len, buf, 0);
            if (outLen != 0)
            {
                out.write(buf, 0, outLen);
            }
        }
        else
        {
            byte[] buf = new byte[len];
            streamCipher.processBytes(b, off, len, buf, 0);
            out.write(buf, 0, len);
        }
    }
    public void flush()
        throws IOException
    {
        super.flush();
    }
    public void close()
        throws IOException
    {
        try
        {
            if (bufferedBlockCipher != null)
            {
                byte[] buf = new byte[bufferedBlockCipher.getOutputSize(0)];
                int outLen = bufferedBlockCipher.doFinal(buf, 0);
                if (outLen != 0)
                {
                    out.write(buf, 0, outLen);
                }
            }
        }
        catch (Exception e)
        {
            throw new IOException("Error closing stream: " + e.toString());
        }
        flush();
        super.close();
    }
}