package pdftk.org.bouncycastle.asn1;
public class ASN1ObjectIdentifier
    extends DERObjectIdentifier
{
    public ASN1ObjectIdentifier(String identifier)
    {
        super(identifier);
    }
    ASN1ObjectIdentifier(byte[] bytes)
    {
        super(bytes);
    }
    public ASN1ObjectIdentifier branch(String branchID)
    {
        return new ASN1ObjectIdentifier(getId() + "." + branchID);
    }
    public boolean on(ASN1ObjectIdentifier stem)
    {
        String id = getId(), stemId = stem.getId();
        return id.length() > stemId.length() && id.charAt(stemId.length()) == '.' && id.startsWith(stemId);
    }
}