package pdftk.org.bouncycastle.asn1.cms;
import pdftk.org.bouncycastle.asn1.ASN1Encodable;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.DERSequence;
public class OtherKeyAttribute
    extends ASN1Object
{
    private ASN1ObjectIdentifier keyAttrId;
    private ASN1Encodable        keyAttr;
    public static OtherKeyAttribute getInstance(
        Object o)
    {
        if (o == null || o instanceof OtherKeyAttribute)
        {
            return (OtherKeyAttribute)o;
        }
        if (o instanceof ASN1Sequence)
        {
            return new OtherKeyAttribute((ASN1Sequence)o);
        }
        throw new IllegalArgumentException("unknown object in factory: " + o.getClass().getName());
    }
    public OtherKeyAttribute(
        ASN1Sequence seq)
    {
        keyAttrId = (ASN1ObjectIdentifier)seq.getObjectAt(0);
        keyAttr = seq.getObjectAt(1);
    }
    public OtherKeyAttribute(
        ASN1ObjectIdentifier keyAttrId,
        ASN1Encodable        keyAttr)
    {
        this.keyAttrId = keyAttrId;
        this.keyAttr = keyAttr;
    }
    public ASN1ObjectIdentifier getKeyAttrId()
    {
        return keyAttrId;
    }
    public ASN1Encodable getKeyAttr()
    {
        return keyAttr;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(keyAttrId);
        v.add(keyAttr);
        return new DERSequence(v);
    }
}