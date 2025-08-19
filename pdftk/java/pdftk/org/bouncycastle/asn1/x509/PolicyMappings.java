package pdftk.org.bouncycastle.asn1.x509;
import java.util.Enumeration;
import java.util.Hashtable;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.DERSequence;
public class PolicyMappings
    extends ASN1Object
{
    ASN1Sequence seq = null;
    public static PolicyMappings getInstance(Object obj)
    {
        if (obj instanceof PolicyMappings)
        {
            return (PolicyMappings)obj;
        }
        if (obj != null)
        {
            return new PolicyMappings(ASN1Sequence.getInstance(obj));
        }
        return null;
    }
    private PolicyMappings(ASN1Sequence seq)
    {
        this.seq = seq;
    }
    public PolicyMappings(Hashtable mappings)
    {
        ASN1EncodableVector dev = new ASN1EncodableVector();
        Enumeration it = mappings.keys();
        while (it.hasMoreElements())
        {
            String idp = (String)it.nextElement();
            String sdp = (String)mappings.get(idp);
            ASN1EncodableVector dv = new ASN1EncodableVector();
            dv.add(new ASN1ObjectIdentifier(idp));
            dv.add(new ASN1ObjectIdentifier(sdp));
            dev.add(new DERSequence(dv));
        }
        seq = new DERSequence(dev);
    }
    public PolicyMappings(CertPolicyId issuerDomainPolicy, CertPolicyId subjectDomainPolicy)
    {
        ASN1EncodableVector dv = new ASN1EncodableVector();
        dv.add(issuerDomainPolicy);
        dv.add(subjectDomainPolicy);
        seq = new DERSequence(new DERSequence(dv));
    }
    public PolicyMappings(CertPolicyId[] issuerDomainPolicy, CertPolicyId[] subjectDomainPolicy)
    {
        ASN1EncodableVector dev = new ASN1EncodableVector();
        for (int i = 0; i != issuerDomainPolicy.length; i++)
        {
            ASN1EncodableVector dv = new ASN1EncodableVector();
            dv.add(issuerDomainPolicy[i]);
            dv.add(subjectDomainPolicy[i]);
            dev.add(new DERSequence(dv));
        }
        seq = new DERSequence(dev);
    }
    public ASN1Primitive toASN1Primitive()
    {
        return seq;
    }
}