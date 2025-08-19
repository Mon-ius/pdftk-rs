package pdftk.org.bouncycastle.asn1.cmp;
import pdftk.org.bouncycastle.asn1.ASN1Encodable;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.DERSequence;
public class InfoTypeAndValue
    extends ASN1Object
{
    private ASN1ObjectIdentifier infoType;
    private ASN1Encodable       infoValue;
    private InfoTypeAndValue(ASN1Sequence seq)
    {
        infoType = ASN1ObjectIdentifier.getInstance(seq.getObjectAt(0));
        if (seq.size() > 1)
        {
            infoValue = (ASN1Encodable)seq.getObjectAt(1);
        }
    }
    public static InfoTypeAndValue getInstance(Object o)
    {
        if (o instanceof InfoTypeAndValue)
        {
            return (InfoTypeAndValue)o;
        }
        if (o != null)
        {
            return new InfoTypeAndValue(ASN1Sequence.getInstance(o));
        }
        return null;
    }
    public InfoTypeAndValue(
        ASN1ObjectIdentifier infoType)
    {
        this.infoType = infoType;
        this.infoValue = null;
    }
    public InfoTypeAndValue(
        ASN1ObjectIdentifier infoType,
        ASN1Encodable optionalValue)
    {
        this.infoType = infoType;
        this.infoValue = optionalValue;
    }
    public ASN1ObjectIdentifier getInfoType()
    {
        return infoType;
    }
    public ASN1Encodable getInfoValue()
    {
        return infoValue;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(infoType);
        if (infoValue != null)
        {
            v.add(infoValue);
        }
        return new DERSequence(v);
    }
}