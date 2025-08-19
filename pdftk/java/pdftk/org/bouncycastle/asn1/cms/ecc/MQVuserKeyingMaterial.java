package pdftk.org.bouncycastle.asn1.cms.ecc;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1OctetString;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERSequence;
import pdftk.org.bouncycastle.asn1.DERTaggedObject;
import pdftk.org.bouncycastle.asn1.cms.OriginatorPublicKey;
public class MQVuserKeyingMaterial
    extends ASN1Object
{
    private OriginatorPublicKey ephemeralPublicKey;
    private ASN1OctetString addedukm;
    public MQVuserKeyingMaterial(
        OriginatorPublicKey ephemeralPublicKey,
        ASN1OctetString addedukm)
    {
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.addedukm = addedukm;
    }
    private MQVuserKeyingMaterial(
        ASN1Sequence seq)
    {
        this.ephemeralPublicKey = OriginatorPublicKey.getInstance(
            seq.getObjectAt(0));
        if (seq.size() > 1)
        {
            this.addedukm = ASN1OctetString.getInstance(
                (ASN1TaggedObject)seq.getObjectAt(1), true);
        }
    }
    public static MQVuserKeyingMaterial getInstance(
        ASN1TaggedObject obj,
        boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }
    public static MQVuserKeyingMaterial getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof MQVuserKeyingMaterial)
        {
            return (MQVuserKeyingMaterial)obj;
        }
        if (obj instanceof ASN1Sequence)
        {
            return new MQVuserKeyingMaterial((ASN1Sequence)obj);
        }
        throw new IllegalArgumentException("Invalid MQVuserKeyingMaterial: " + obj.getClass().getName());
    }
    public OriginatorPublicKey getEphemeralPublicKey()
    {
        return ephemeralPublicKey;
    }
    public ASN1OctetString getAddedukm()
    {
        return addedukm;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(ephemeralPublicKey);
        if (addedukm != null)
        {
            v.add(new DERTaggedObject(true, 0, addedukm));
        }
        return new DERSequence(v);
    }
}