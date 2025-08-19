package pdftk.org.bouncycastle.asn1.ess;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1OctetString;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.DEROctetString;
public class ContentIdentifier
    extends ASN1Object
{
     ASN1OctetString value;
    public static ContentIdentifier getInstance(Object o)
    {
        if (o instanceof ContentIdentifier)
        {
            return (ContentIdentifier) o;
        }
        else if (o != null)
        {
            return new ContentIdentifier(ASN1OctetString.getInstance(o));
        }
        return null;
    }
    private ContentIdentifier(
        ASN1OctetString value)
    {
        this.value = value;
    }
    public ContentIdentifier(
        byte[] value)
    {
        this(new DEROctetString(value));
    }
    public ASN1OctetString getValue()
    {
        return value;
    }
    public ASN1Primitive toASN1Primitive()
    {
        return value;
    }
}