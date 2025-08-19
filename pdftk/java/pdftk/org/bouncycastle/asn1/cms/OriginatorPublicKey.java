package pdftk.org.bouncycastle.asn1.cms;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERBitString;
import pdftk.org.bouncycastle.asn1.DERSequence;
import pdftk.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
public class OriginatorPublicKey
    extends ASN1Object
{
    private AlgorithmIdentifier algorithm;
    private DERBitString        publicKey;
    public OriginatorPublicKey(
        AlgorithmIdentifier algorithm,
        byte[]              publicKey)
    {
        this.algorithm = algorithm;
        this.publicKey = new DERBitString(publicKey);
    }
    public OriginatorPublicKey(
        ASN1Sequence seq)
    {
        algorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(0));
        publicKey = (DERBitString)seq.getObjectAt(1);
    }
    public static OriginatorPublicKey getInstance(
        ASN1TaggedObject    obj,
        boolean             explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }
    public static OriginatorPublicKey getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof OriginatorPublicKey)
        {
            return (OriginatorPublicKey)obj;
        }
        if (obj instanceof ASN1Sequence)
        {
            return new OriginatorPublicKey((ASN1Sequence)obj);
        }
        throw new IllegalArgumentException("Invalid OriginatorPublicKey: " + obj.getClass().getName());
    }
    public AlgorithmIdentifier getAlgorithm()
    {
        return algorithm;
    }
    public DERBitString getPublicKey()
    {
        return publicKey;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();
        v.add(algorithm);
        v.add(publicKey);
        return new DERSequence(v);
    }
}