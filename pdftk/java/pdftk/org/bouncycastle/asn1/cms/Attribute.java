package pdftk.org.bouncycastle.asn1.cms;
import pdftk.org.bouncycastle.asn1.ASN1Encodable;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1Set;
import pdftk.org.bouncycastle.asn1.DERObjectIdentifier;
import pdftk.org.bouncycastle.asn1.DERSequence;
public class Attribute
    extends ASN1Object
{
    private ASN1ObjectIdentifier attrType;
    private ASN1Set             attrValues;
    public static Attribute getInstance(
        Object o)
    {
        if (o instanceof Attribute)
        {
            return (Attribute)o;
        }
        if (o != null)
        {
            return new Attribute(ASN1Sequence.getInstance(o));
        }
        return null;
    }
    private Attribute(
        ASN1Sequence seq)
    {
        attrType = (ASN1ObjectIdentifier)seq.getObjectAt(0);
        attrValues = (ASN1Set)seq.getObjectAt(1);
    }
    public Attribute(
        DERObjectIdentifier attrType,
        ASN1Set             attrValues)
    {
        this.attrType = new ASN1ObjectIdentifier(attrType.getId());
        this.attrValues = attrValues;
    }
    public Attribute(
        ASN1ObjectIdentifier attrType,
        ASN1Set             attrValues)
    {
        this.attrType = attrType;
        this.attrValues = attrValues;
    }
    public ASN1ObjectIdentifier getAttrType()
    {
        return attrType;
    }
    public ASN1Set getAttrValues()
    {
        return attrValues;
    }
    public ASN1Encodable[] getAttributeValues()
    {
        return attrValues.toArray();
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(attrType);
        v.add(attrValues);
        return new DERSequence(v);
    }
}