package pdftk.org.bouncycastle.asn1.x509;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERSequence;
import pdftk.org.bouncycastle.asn1.DERTaggedObject;
public class Holder
    extends ASN1Object
{
    public static final int V1_CERTIFICATE_HOLDER = 0;
    public static final int V2_CERTIFICATE_HOLDER = 1;
    IssuerSerial baseCertificateID;
    GeneralNames entityName;
    ObjectDigestInfo objectDigestInfo;
    private int version = V2_CERTIFICATE_HOLDER;
    public static Holder getInstance(Object obj)
    {
        if (obj instanceof Holder)
        {
            return (Holder)obj;
        }
        else if (obj instanceof ASN1TaggedObject)
        {
            return new Holder(ASN1TaggedObject.getInstance(obj));
        }
        else if (obj != null)
        {
            return new Holder(ASN1Sequence.getInstance(obj));
        }
        return null;
    }
    private Holder(ASN1TaggedObject tagObj)
    {
        switch (tagObj.getTagNo())
        {
        case 0:
            baseCertificateID = IssuerSerial.getInstance(tagObj, false);
            break;
        case 1:
            entityName = GeneralNames.getInstance(tagObj, false);
            break;
        default:
            throw new IllegalArgumentException("unknown tag in Holder");
        }
        version = 0;
    }
    private Holder(ASN1Sequence seq)
    {
        if (seq.size() > 3)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }
        for (int i = 0; i != seq.size(); i++)
        {
            ASN1TaggedObject tObj = ASN1TaggedObject.getInstance(seq
                .getObjectAt(i));
            switch (tObj.getTagNo())
            {
            case 0:
                baseCertificateID = IssuerSerial.getInstance(tObj, false);
                break;
            case 1:
                entityName = GeneralNames.getInstance(tObj, false);
                break;
            case 2:
                objectDigestInfo = ObjectDigestInfo.getInstance(tObj, false);
                break;
            default:
                throw new IllegalArgumentException("unknown tag in Holder");
            }
        }
        version = 1;
    }
    public Holder(IssuerSerial baseCertificateID)
    {
        this(baseCertificateID, V2_CERTIFICATE_HOLDER);
    }
    public Holder(IssuerSerial baseCertificateID, int version)
    {
        this.baseCertificateID = baseCertificateID;
        this.version = version;
    }
    public int getVersion()
    {
        return version;
    }
    public Holder(GeneralNames entityName)
    {
        this(entityName, V2_CERTIFICATE_HOLDER);
    }
    public Holder(GeneralNames entityName, int version)
    {
        this.entityName = entityName;
        this.version = version;
    }
    public Holder(ObjectDigestInfo objectDigestInfo)
    {
        this.objectDigestInfo = objectDigestInfo;
    }
    public IssuerSerial getBaseCertificateID()
    {
        return baseCertificateID;
    }
    public GeneralNames getEntityName()
    {
        return entityName;
    }
    public ObjectDigestInfo getObjectDigestInfo()
    {
        return objectDigestInfo;
    }
    public ASN1Primitive toASN1Primitive()
    {
        if (version == 1)
        {
            ASN1EncodableVector v = new ASN1EncodableVector();
            if (baseCertificateID != null)
            {
                v.add(new DERTaggedObject(false, 0, baseCertificateID));
            }
            if (entityName != null)
            {
                v.add(new DERTaggedObject(false, 1, entityName));
            }
            if (objectDigestInfo != null)
            {
                v.add(new DERTaggedObject(false, 2, objectDigestInfo));
            }
            return new DERSequence(v);
        }
        else
        {
            if (entityName != null)
            {
                return new DERTaggedObject(false, 1, entityName);
            }
            else
            {
                return new DERTaggedObject(false, 0, baseCertificateID);
            }
        }
    }
}