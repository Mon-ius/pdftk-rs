package pdftk.org.bouncycastle.asn1.esf;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.DERSequence;
public class SigPolicyQualifiers
    extends ASN1Object
{
    ASN1Sequence qualifiers;
    public static SigPolicyQualifiers getInstance(
        Object obj)
    {
        if (obj instanceof SigPolicyQualifiers)
        {
            return (SigPolicyQualifiers) obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new SigPolicyQualifiers(ASN1Sequence.getInstance(obj));
        }
        return null;
    }
    private SigPolicyQualifiers(
        ASN1Sequence seq)
    {
        qualifiers = seq;
    }
    public SigPolicyQualifiers(
        SigPolicyQualifierInfo[] qualifierInfos)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        for (int i=0; i < qualifierInfos.length; i++)
        {
            v.add(qualifierInfos[i]);
        }
        qualifiers = new DERSequence(v);
    }
    public int size()
    {
        return qualifiers.size();
    }
    public SigPolicyQualifierInfo getInfoAt(
        int i)
    {
        return SigPolicyQualifierInfo.getInstance(qualifiers.getObjectAt(i));
    }
    public ASN1Primitive toASN1Primitive()
    {
        return qualifiers;
    }
}