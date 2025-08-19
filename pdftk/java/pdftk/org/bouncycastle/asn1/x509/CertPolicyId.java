package pdftk.org.bouncycastle.asn1.x509;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
public class CertPolicyId
    extends ASN1Object
{
    private ASN1ObjectIdentifier id;
    private CertPolicyId(ASN1ObjectIdentifier id)
    {
        this.id = id;
    }
    public static CertPolicyId getInstance(Object o)
    {
        if (o instanceof CertPolicyId)
        {
            return (CertPolicyId)o;
        }
        else if (o != null)
        {
            return new CertPolicyId(ASN1ObjectIdentifier.getInstance(o));
        }
        return null;
    }
    public String getId()
    {
        return id.getId();
    }
    public ASN1Primitive toASN1Primitive()
    {
        return id;
    }
}