package pdftk.org.bouncycastle.asn1.x509;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.DERSequence;
public class AuthorityInformationAccess
    extends ASN1Object
{
    private AccessDescription[]    descriptions;
    public static AuthorityInformationAccess getInstance(
        Object  obj)
    {
        if (obj instanceof AuthorityInformationAccess)
        {
            return (AuthorityInformationAccess)obj;
        }
        if (obj != null)
        {
            return new AuthorityInformationAccess(ASN1Sequence.getInstance(obj));
        }
        return null;
    }
    private AuthorityInformationAccess(
        ASN1Sequence   seq)
    {
        if (seq.size() < 1)
        {
            throw new IllegalArgumentException("sequence may not be empty");
        }
        descriptions = new AccessDescription[seq.size()];
        for (int i = 0; i != seq.size(); i++)
        {
            descriptions[i] = AccessDescription.getInstance(seq.getObjectAt(i));
        }
    }
    public AuthorityInformationAccess(
        ASN1ObjectIdentifier oid,
        GeneralName location)
    {
        descriptions = new AccessDescription[1];
        descriptions[0] = new AccessDescription(oid, location);
    }
    public AccessDescription[] getAccessDescriptions()
    {
        return descriptions;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        for (int i = 0; i != descriptions.length; i++)
        {
            vec.add(descriptions[i]);
        }
        return new DERSequence(vec);
    }
    public String toString()
    {
        return ("AuthorityInformationAccess: Oid(" + this.descriptions[0].getAccessMethod().getId() + ")");
    }
}