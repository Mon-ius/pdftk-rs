package pdftk.org.bouncycastle.asn1.x509;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Enumerated;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERBitString;
import pdftk.org.bouncycastle.asn1.DERSequence;
public class ObjectDigestInfo
    extends ASN1Object
{
    public final static int publicKey = 0;
    public final static int publicKeyCert = 1;
    public final static int otherObjectDigest = 2;
    ASN1Enumerated digestedObjectType;
    ASN1ObjectIdentifier otherObjectTypeID;
    AlgorithmIdentifier digestAlgorithm;
    DERBitString objectDigest;
    public static ObjectDigestInfo getInstance(
        Object obj)
    {
        if (obj instanceof ObjectDigestInfo)
        {
            return (ObjectDigestInfo)obj;
        }
        if (obj != null)
        {
            return new ObjectDigestInfo(ASN1Sequence.getInstance(obj));
        }
        return null;
    }
    public static ObjectDigestInfo getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }
    public ObjectDigestInfo(
        int digestedObjectType,
        ASN1ObjectIdentifier otherObjectTypeID,
        AlgorithmIdentifier digestAlgorithm,
        byte[] objectDigest)
    {
        this.digestedObjectType = new ASN1Enumerated(digestedObjectType);
        if (digestedObjectType == otherObjectDigest)
        {
            this.otherObjectTypeID = otherObjectTypeID;
        }
        this.digestAlgorithm = digestAlgorithm;
        this.objectDigest = new DERBitString(objectDigest);
    }
    private ObjectDigestInfo(
        ASN1Sequence seq)
    {
        if (seq.size() > 4 || seq.size() < 3)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }
        digestedObjectType = ASN1Enumerated.getInstance(seq.getObjectAt(0));
        int offset = 0;
        if (seq.size() == 4)
        {
            otherObjectTypeID = ASN1ObjectIdentifier.getInstance(seq.getObjectAt(1));
            offset++;
        }
        digestAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(1 + offset));
        objectDigest = DERBitString.getInstance(seq.getObjectAt(2 + offset));
    }
    public ASN1Enumerated getDigestedObjectType()
    {
        return digestedObjectType;
    }
    public ASN1ObjectIdentifier getOtherObjectTypeID()
    {
        return otherObjectTypeID;
    }
    public AlgorithmIdentifier getDigestAlgorithm()
    {
        return digestAlgorithm;
    }
    public DERBitString getObjectDigest()
    {
        return objectDigest;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(digestedObjectType);
        if (otherObjectTypeID != null)
        {
            v.add(otherObjectTypeID);
        }
        v.add(digestAlgorithm);
        v.add(objectDigest);
        return new DERSequence(v);
    }
}