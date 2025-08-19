package pdftk.org.bouncycastle.asn1.pkcs;
import pdftk.org.bouncycastle.asn1.ASN1Encodable;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.DERSequence;
import pdftk.org.bouncycastle.asn1.DERTaggedObject;
public class CRLBag
    extends ASN1Object
{
    private ASN1ObjectIdentifier crlId;
    private ASN1Encodable crlValue;
    private CRLBag(
        ASN1Sequence seq)
    {
        this.crlId = (ASN1ObjectIdentifier)seq.getObjectAt(0);
        this.crlValue = ((DERTaggedObject)seq.getObjectAt(1)).getObject();
    }
    public static CRLBag getInstance(Object o)
    {
        if (o instanceof CRLBag)
        {
            return (CRLBag)o;
        }
        else if (o != null)
        {
            return new CRLBag(ASN1Sequence.getInstance(o));
        }
        return null;
    }
    public CRLBag(
        ASN1ObjectIdentifier crlId,
        ASN1Encodable crlValue)
    {
        this.crlId = crlId;
        this.crlValue = crlValue;
    }
    public ASN1ObjectIdentifier getcrlId()
    {
        return crlId;
    }
    public ASN1Encodable getCRLValue()
    {
        return crlValue;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();
        v.add(crlId);
        v.add(new DERTaggedObject(0, crlValue));
        return new DERSequence(v);
    }
}