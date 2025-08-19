package pdftk.org.bouncycastle.asn1.x509;
import java.io.IOException;
import java.util.Enumeration;
import pdftk.org.bouncycastle.asn1.ASN1Encodable;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1InputStream;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERBitString;
import pdftk.org.bouncycastle.asn1.DERSequence;
public class SubjectPublicKeyInfo
    extends ASN1Object
{
    private AlgorithmIdentifier     algId;
    private DERBitString            keyData;
    public static SubjectPublicKeyInfo getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }
    public static SubjectPublicKeyInfo getInstance(
        Object  obj)
    {
        if (obj instanceof SubjectPublicKeyInfo)
        {
            return (SubjectPublicKeyInfo)obj;
        }
        else if (obj != null)
        {
            return new SubjectPublicKeyInfo(ASN1Sequence.getInstance(obj));
        }
        return null;
    }
    public SubjectPublicKeyInfo(
        AlgorithmIdentifier algId,
        ASN1Encodable       publicKey)
        throws IOException
    {
        this.keyData = new DERBitString(publicKey);
        this.algId = algId;
    }
    public SubjectPublicKeyInfo(
        AlgorithmIdentifier algId,
        byte[]              publicKey)
    {
        this.keyData = new DERBitString(publicKey);
        this.algId = algId;
    }
    public SubjectPublicKeyInfo(
        ASN1Sequence  seq)
    {
        if (seq.size() != 2)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                    + seq.size());
        }
        Enumeration         e = seq.getObjects();
        this.algId = AlgorithmIdentifier.getInstance(e.nextElement());
        this.keyData = DERBitString.getInstance(e.nextElement());
    }
    public AlgorithmIdentifier getAlgorithm()
    {
        return algId;
    }
    public AlgorithmIdentifier getAlgorithmId()
    {
        return algId;
    }
    public ASN1Primitive parsePublicKey()
        throws IOException
    {
        ASN1InputStream         aIn = new ASN1InputStream(keyData.getBytes());
        return aIn.readObject();
    }
    public ASN1Primitive getPublicKey()
        throws IOException
    {
        ASN1InputStream         aIn = new ASN1InputStream(keyData.getBytes());
        return aIn.readObject();
    }
    public DERBitString getPublicKeyData()
    {
        return keyData;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();
        v.add(algId);
        v.add(keyData);
        return new DERSequence(v);
    }
}