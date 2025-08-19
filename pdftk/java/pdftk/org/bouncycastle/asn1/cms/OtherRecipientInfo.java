package pdftk.org.bouncycastle.asn1.cms;
import pdftk.org.bouncycastle.asn1.ASN1Encodable;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERSequence;
public class OtherRecipientInfo
    extends ASN1Object
{
    private ASN1ObjectIdentifier    oriType;
    private ASN1Encodable           oriValue;
    public OtherRecipientInfo(
        ASN1ObjectIdentifier     oriType,
        ASN1Encodable            oriValue)
    {
        this.oriType = oriType;
        this.oriValue = oriValue;
    }
    public OtherRecipientInfo(
        ASN1Sequence seq)
    {
        oriType = ASN1ObjectIdentifier.getInstance(seq.getObjectAt(0));
        oriValue = seq.getObjectAt(1);
    }
    public static OtherRecipientInfo getInstance(
        ASN1TaggedObject    obj,
        boolean             explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }
    public static OtherRecipientInfo getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof OtherRecipientInfo)
        {
            return (OtherRecipientInfo)obj;
        }
        if (obj instanceof ASN1Sequence)
        {
            return new OtherRecipientInfo((ASN1Sequence)obj);
        }
        throw new IllegalArgumentException("Invalid OtherRecipientInfo: " + obj.getClass().getName());
    }
    public ASN1ObjectIdentifier getType()
    {
        return oriType;
    }
    public ASN1Encodable getValue()
    {
        return oriValue;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();
        v.add(oriType);
        v.add(oriValue);
        return new DERSequence(v);
    }
}