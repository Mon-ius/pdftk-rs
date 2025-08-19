package pdftk.org.bouncycastle.asn1.crmf;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERBitString;
import pdftk.org.bouncycastle.asn1.DERSequence;
import pdftk.org.bouncycastle.asn1.cmp.CMPObjectIdentifiers;
import pdftk.org.bouncycastle.asn1.cmp.PBMParameter;
import pdftk.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
public class PKMACValue
    extends ASN1Object
{
    private AlgorithmIdentifier  algId;
    private DERBitString        value;
    private PKMACValue(ASN1Sequence seq)
    {
        algId = AlgorithmIdentifier.getInstance(seq.getObjectAt(0));
        value = DERBitString.getInstance(seq.getObjectAt(1));
    }
    public static PKMACValue getInstance(Object o)
    {
        if (o instanceof PKMACValue)
        {
            return (PKMACValue)o;
        }
        if (o != null)
        {
            return new PKMACValue(ASN1Sequence.getInstance(o));
        }
        return null;
    }
    public static PKMACValue getInstance(ASN1TaggedObject obj, boolean isExplicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, isExplicit));
    }
    public PKMACValue(
        PBMParameter params,
        DERBitString value)
    {
        this(new AlgorithmIdentifier(
                    CMPObjectIdentifiers.passwordBasedMac, params), value);
    }
    public PKMACValue(
        AlgorithmIdentifier aid,
        DERBitString value)
    {
        this.algId = aid;
        this.value = value;
    }
    public AlgorithmIdentifier getAlgId()
    {
        return algId;
    }
    public DERBitString getValue()
    {
        return value;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(algId);
        v.add(value);
        return new DERSequence(v);
    }
}