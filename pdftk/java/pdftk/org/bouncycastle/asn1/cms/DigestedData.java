package pdftk.org.bouncycastle.asn1.cms;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Integer;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1OctetString;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.BERSequence;
import pdftk.org.bouncycastle.asn1.DEROctetString;
import pdftk.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
public class DigestedData
    extends ASN1Object
{
    private ASN1Integer           version;
    private AlgorithmIdentifier  digestAlgorithm;
    private ContentInfo          encapContentInfo;
    private ASN1OctetString      digest;
    public DigestedData(
        AlgorithmIdentifier digestAlgorithm,
        ContentInfo encapContentInfo,
        byte[]      digest)
    {
        this.version = new ASN1Integer(0);
        this.digestAlgorithm = digestAlgorithm;
        this.encapContentInfo = encapContentInfo;
        this.digest = new DEROctetString(digest);
    }
    private DigestedData(
        ASN1Sequence seq)
    {
        this.version = (ASN1Integer)seq.getObjectAt(0);
        this.digestAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(1));
        this.encapContentInfo = ContentInfo.getInstance(seq.getObjectAt(2));
        this.digest = ASN1OctetString.getInstance(seq.getObjectAt(3));
    }
    public static DigestedData getInstance(
        ASN1TaggedObject _ato,
        boolean _explicit)
    {
        return getInstance(ASN1Sequence.getInstance(_ato, _explicit));
    }
    public static DigestedData getInstance(
        Object obj)
    {
        if (obj instanceof DigestedData)
        {
            return (DigestedData)obj;
        }
        if (obj != null)
        {
            return new DigestedData(ASN1Sequence.getInstance(obj));
        }
        return null;
    }
    public ASN1Integer getVersion()
    {
        return version;
    }
    public AlgorithmIdentifier getDigestAlgorithm()
    {
        return digestAlgorithm;
    }
    public ContentInfo getEncapContentInfo()
    {
        return encapContentInfo;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(version);
        v.add(digestAlgorithm);
        v.add(encapContentInfo);
        v.add(digest);
        return new BERSequence(v);
    }
    public byte[] getDigest()
    {
        return digest.getOctets();
    }
}