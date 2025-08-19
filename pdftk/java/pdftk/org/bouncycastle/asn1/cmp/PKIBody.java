package pdftk.org.bouncycastle.asn1.cmp;
import pdftk.org.bouncycastle.asn1.ASN1Choice;
import pdftk.org.bouncycastle.asn1.ASN1Encodable;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERTaggedObject;
import pdftk.org.bouncycastle.asn1.crmf.CertReqMessages;
import pdftk.org.bouncycastle.asn1.pkcs.CertificationRequest;
public class PKIBody
    extends ASN1Object
    implements ASN1Choice
{
    public static final int TYPE_INIT_REQ = 0;
    public static final int TYPE_INIT_REP = 1;
    public static final int TYPE_CERT_REQ = 2;
    public static final int TYPE_CERT_REP = 3;
    public static final int TYPE_P10_CERT_REQ = 4;
    public static final int TYPE_POPO_CHALL = 5;
    public static final int TYPE_POPO_REP = 6;
    public static final int TYPE_KEY_UPDATE_REQ = 7;
    public static final int TYPE_KEY_UPDATE_REP = 8;
    public static final int TYPE_KEY_RECOVERY_REQ = 9;
    public static final int TYPE_KEY_RECOVERY_REP = 10;
    public static final int TYPE_REVOCATION_REQ = 11;
    public static final int TYPE_REVOCATION_REP = 12;
    public static final int TYPE_CROSS_CERT_REQ = 13;
    public static final int TYPE_CROSS_CERT_REP = 14;
    public static final int TYPE_CA_KEY_UPDATE_ANN = 15;
    public static final int TYPE_CERT_ANN = 16;
    public static final int TYPE_REVOCATION_ANN = 17;
    public static final int TYPE_CRL_ANN = 18;
    public static final int TYPE_CONFIRM = 19;
    public static final int TYPE_NESTED = 20;
    public static final int TYPE_GEN_MSG = 21;
    public static final int TYPE_GEN_REP = 22;
    public static final int TYPE_ERROR = 23;
    public static final int TYPE_CERT_CONFIRM = 24;
    public static final int TYPE_POLL_REQ = 25;
    public static final int TYPE_POLL_REP = 26;
    private int tagNo;
    private ASN1Encodable body;
    public static PKIBody getInstance(Object o)
    {
        if (o == null || o instanceof PKIBody)
        {
            return (PKIBody)o;
        }
        if (o instanceof ASN1TaggedObject)
        {
            return new PKIBody((ASN1TaggedObject)o);
        }
        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }
    private PKIBody(ASN1TaggedObject tagged)
    {
        tagNo = tagged.getTagNo();
        body = getBodyForType(tagNo, tagged.getObject());
    }
    public PKIBody(
        int type,
        ASN1Encodable content)
    {
        tagNo = type;
        body = getBodyForType(type, content);
    }
    private static ASN1Encodable getBodyForType(
        int type,
        ASN1Encodable o)
    {
        switch (type)
        {
        case TYPE_INIT_REQ:
            return CertReqMessages.getInstance(o);
        case TYPE_INIT_REP:
            return CertRepMessage.getInstance(o);
        case TYPE_CERT_REQ:
            return CertReqMessages.getInstance(o);
        case TYPE_CERT_REP:
            return CertRepMessage.getInstance(o);
        case TYPE_P10_CERT_REQ:
            return CertificationRequest.getInstance(o);
        case TYPE_POPO_CHALL:
            return POPODecKeyChallContent.getInstance(o);
        case TYPE_POPO_REP:
            return POPODecKeyRespContent.getInstance(o);
        case TYPE_KEY_UPDATE_REQ:
            return CertReqMessages.getInstance(o);
        case TYPE_KEY_UPDATE_REP:
            return CertRepMessage.getInstance(o);
        case TYPE_KEY_RECOVERY_REQ:
            return CertReqMessages.getInstance(o);
        case TYPE_KEY_RECOVERY_REP:
            return KeyRecRepContent.getInstance(o);
        case TYPE_REVOCATION_REQ:
            return RevReqContent.getInstance(o);
        case TYPE_REVOCATION_REP:
            return RevRepContent.getInstance(o);
        case TYPE_CROSS_CERT_REQ:
            return CertReqMessages.getInstance(o);
        case TYPE_CROSS_CERT_REP:
            return CertRepMessage.getInstance(o);
        case TYPE_CA_KEY_UPDATE_ANN:
            return CAKeyUpdAnnContent.getInstance(o);
        case TYPE_CERT_ANN:
            return CMPCertificate.getInstance(o);
        case TYPE_REVOCATION_ANN:
            return RevAnnContent.getInstance(o);
        case TYPE_CRL_ANN:
            return CRLAnnContent.getInstance(o);
        case TYPE_CONFIRM:
            return PKIConfirmContent.getInstance(o);
        case TYPE_NESTED:
            return PKIMessages.getInstance(o);
        case TYPE_GEN_MSG:
            return GenMsgContent.getInstance(o);
        case TYPE_GEN_REP:
            return GenRepContent.getInstance(o);
        case TYPE_ERROR:
            return ErrorMsgContent.getInstance(o);
        case TYPE_CERT_CONFIRM:
            return CertConfirmContent.getInstance(o);
        case TYPE_POLL_REQ:
            return PollReqContent.getInstance(o);
        case TYPE_POLL_REP:
            return PollRepContent.getInstance(o);
        default:
            throw new IllegalArgumentException("unknown tag number: " + type);
        }
    }
    public int getType()
    {
        return tagNo;
    }
    public ASN1Encodable getContent()
    {
        return body;
    }
    public ASN1Primitive toASN1Primitive()
    {
        return new DERTaggedObject(true, tagNo, body);
    }
}