package pdftk.org.bouncycastle.crypto.tls;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import pdftk.org.bouncycastle.crypto.Digest;
import pdftk.org.bouncycastle.crypto.Mac;
import pdftk.org.bouncycastle.crypto.macs.HMac;
import pdftk.org.bouncycastle.crypto.params.KeyParameter;
import pdftk.org.bouncycastle.util.Arrays;
public class TlsMac
{
    protected TlsClientContext context;
    protected long seqNo;
    protected byte[] secret;
    protected Mac mac;
    public TlsMac(TlsClientContext context, Digest digest, byte[] key_block, int offset, int len)
    {
        this.context = context;
        this.seqNo = 0;
        KeyParameter param = new KeyParameter(key_block, offset, len);
        this.secret = Arrays.clone(param.getKey());
        boolean isTls = context.getServerVersion().getFullVersion() >= ProtocolVersion.TLSv10.getFullVersion();
        if (isTls)
        {
            this.mac = new HMac(digest);
        }
        else
        {
            this.mac = new SSL3Mac(digest);
        }
        this.mac.init(param);
    }
	public byte[] getMACSecret()
	{
		return this.secret;
	}
	public long getSequenceNumber()
	{
		return this.seqNo;
	}
	public void incSequenceNumber()
	{
		this.seqNo++;
	}
    public int getSize()
    {
        return mac.getMacSize();
    }
    public byte[] calculateMac(short type, byte[] message, int offset, int len)
    {
        ProtocolVersion serverVersion = context.getServerVersion();
        boolean isTls = serverVersion.getFullVersion() >= ProtocolVersion.TLSv10.getFullVersion();
        ByteArrayOutputStream bosMac = new ByteArrayOutputStream(isTls ? 13 : 11);
        try
        {
            TlsUtils.writeUint64(seqNo++, bosMac);
            TlsUtils.writeUint8(type, bosMac);
            if (isTls)
            {
                TlsUtils.writeVersion(serverVersion, bosMac);
            }
            TlsUtils.writeUint16(len, bosMac);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Internal error during mac calculation");
        }
        byte[] macHeader = bosMac.toByteArray();
        mac.update(macHeader, 0, macHeader.length);
        mac.update(message, offset, len);
        byte[] result = new byte[mac.getMacSize()];
        mac.doFinal(result, 0);
        return result;
    }
    public byte[] calculateMacConstantTime(short type, byte[] message, int offset, int len, int fullLength, byte[] dummyData)
    {
        byte[] result = calculateMac(type, message, offset, len);
        ProtocolVersion serverVersion = context.getServerVersion();
        boolean isTls = serverVersion.getFullVersion() >= ProtocolVersion.TLSv10.getFullVersion();
        if (isTls)
        {
            int db = 64, ds = 8;
            int L1 = 13 + fullLength;
            int L2 = 13 + len;
            int extra = ((L1 + ds) / db) - ((L2 + ds) / db);
            while (--extra >= 0)
            {
                mac.update(dummyData, 0, db);
            }
            mac.update(dummyData[0]);
            mac.reset();
        }
        return result;
    }
}