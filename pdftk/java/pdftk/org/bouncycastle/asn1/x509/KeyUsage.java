package pdftk.org.bouncycastle.asn1.x509;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.DERBitString;
public class KeyUsage
    extends ASN1Object
{
    public static final int        digitalSignature = (1 << 7);
    public static final int        nonRepudiation   = (1 << 6);
    public static final int        keyEncipherment  = (1 << 5);
    public static final int        dataEncipherment = (1 << 4);
    public static final int        keyAgreement     = (1 << 3);
    public static final int        keyCertSign      = (1 << 2);
    public static final int        cRLSign          = (1 << 1);
    public static final int        encipherOnly     = (1 << 0);
    public static final int        decipherOnly     = (1 << 15);
    private DERBitString bitString;
    public static KeyUsage getInstance(Object obj)
    {
        if (obj instanceof KeyUsage)
        {
            return (KeyUsage)obj;
        }
        else if (obj != null)
        {
            return new KeyUsage(DERBitString.getInstance(obj));
        }
        return null;
    }
    public static KeyUsage fromExtensions(Extensions extensions)
    {
        return KeyUsage.getInstance(extensions.getExtensionParsedValue(Extension.keyUsage));
    }
    public KeyUsage(
        int usage)
    {
        this.bitString = new DERBitString(usage);
    }
    private KeyUsage(
        DERBitString bitString)
    {
        this.bitString = bitString;
    }
    public byte[] getBytes()
    {
        return bitString.getBytes();
    }
    public int getPadBits()
    {
        return bitString.getPadBits();
    }
    public String toString()
    {
        byte[] data = bitString.getBytes();
        if (data.length == 1)
        {
            return "KeyUsage: 0x" + Integer.toHexString(data[0] & 0xff);
        }
        return "KeyUsage: 0x" + Integer.toHexString((data[1] & 0xff) << 8 | (data[0] & 0xff));
    }
    public ASN1Primitive toASN1Primitive()
    {
        return bitString;
    }
}