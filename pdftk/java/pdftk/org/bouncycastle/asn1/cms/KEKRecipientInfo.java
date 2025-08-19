package pdftk.org.bouncycastle.asn1.cms;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Integer;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1OctetString;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERSequence;
import pdftk.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
public class KEKRecipientInfo
    extends ASN1Object
{
    private ASN1Integer          version;
    private KEKIdentifier       kekid;
    private AlgorithmIdentifier keyEncryptionAlgorithm;
    private ASN1OctetString     encryptedKey;
    public KEKRecipientInfo(
        KEKIdentifier       kekid,
        AlgorithmIdentifier keyEncryptionAlgorithm,
        ASN1OctetString     encryptedKey)
    {
        this.version = new ASN1Integer(4);
        this.kekid = kekid;
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
        this.encryptedKey = encryptedKey;
    }
    public KEKRecipientInfo(
        ASN1Sequence seq)
    {
        version = (ASN1Integer)seq.getObjectAt(0);
        kekid = KEKIdentifier.getInstance(seq.getObjectAt(1));
        keyEncryptionAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(2));
        encryptedKey = (ASN1OctetString)seq.getObjectAt(3);
    }
    public static KEKRecipientInfo getInstance(
        ASN1TaggedObject    obj,
        boolean             explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }
    public static KEKRecipientInfo getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof KEKRecipientInfo)
        {
            return (KEKRecipientInfo)obj;
        }
        if(obj instanceof ASN1Sequence)
        {
            return new KEKRecipientInfo((ASN1Sequence)obj);
        }
        throw new IllegalArgumentException("Invalid KEKRecipientInfo: " + obj.getClass().getName());
    }
    public ASN1Integer getVersion()
    {
        return version;
    }
    public KEKIdentifier getKekid()
    {
        return kekid;
    }
    public AlgorithmIdentifier getKeyEncryptionAlgorithm()
    {
        return keyEncryptionAlgorithm;
    }
    public ASN1OctetString getEncryptedKey()
    {
        return encryptedKey;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();
        v.add(version);
        v.add(kekid);
        v.add(keyEncryptionAlgorithm);
        v.add(encryptedKey);
        return new DERSequence(v);
    }
}