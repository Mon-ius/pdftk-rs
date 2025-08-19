package pdftk.org.bouncycastle.asn1.cmp;
import java.math.BigInteger;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Integer;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERBitString;
import pdftk.org.bouncycastle.asn1.DERSequence;
public class PKIStatusInfo
    extends ASN1Object
{
    ASN1Integer      status;
    PKIFreeText     statusString;
    DERBitString    failInfo;
    public static PKIStatusInfo getInstance(
        ASN1TaggedObject obj,
        boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }
    public static PKIStatusInfo getInstance(
        Object obj)
    {
        if (obj instanceof PKIStatusInfo)
        {
            return (PKIStatusInfo)obj;
        }
        else if (obj != null)
        {
            return new PKIStatusInfo(ASN1Sequence.getInstance(obj));
        }
        return null;
    }
    private PKIStatusInfo(
        ASN1Sequence seq)
    {
        this.status = ASN1Integer.getInstance(seq.getObjectAt(0));
        this.statusString = null;
        this.failInfo = null;
        if (seq.size() > 2)
        {
            this.statusString = PKIFreeText.getInstance(seq.getObjectAt(1));
            this.failInfo = DERBitString.getInstance(seq.getObjectAt(2));
        }
        else if (seq.size() > 1)
        {
            Object obj = seq.getObjectAt(1);
            if (obj instanceof DERBitString)
            {
                this.failInfo = DERBitString.getInstance(obj);
            }
            else
            {
                this.statusString = PKIFreeText.getInstance(obj);
            }
        }
    }
    public PKIStatusInfo(PKIStatus status)
    {
        this.status = ASN1Integer.getInstance(status.toASN1Primitive());
    }
    public PKIStatusInfo(
        PKIStatus   status,
        PKIFreeText statusString)
    {
        this.status = ASN1Integer.getInstance(status.toASN1Primitive());
        this.statusString = statusString;
    }
    public PKIStatusInfo(
        PKIStatus      status,
        PKIFreeText    statusString,
        PKIFailureInfo failInfo)
    {
        this.status = ASN1Integer.getInstance(status.toASN1Primitive());;
        this.statusString = statusString;
        this.failInfo = failInfo;
    }
    public BigInteger getStatus()
    {
        return status.getValue();
    }
    public PKIFreeText getStatusString()
    {
        return statusString;
    }
    public DERBitString getFailInfo()
    {
        return failInfo;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();
        v.add(status);
        if (statusString != null)
        {
            v.add(statusString);
        }
        if (failInfo!= null)
        {
            v.add(failInfo);
        }
        return new DERSequence(v);
    }
}