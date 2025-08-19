package pdftk.org.bouncycastle.asn1.ua;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
public interface UAObjectIdentifiers
{
    static final ASN1ObjectIdentifier UaOid = new ASN1ObjectIdentifier("1.2.804.2.1.1.1");
    static final ASN1ObjectIdentifier dstu4145le = UaOid.branch("1.3.1.1");
    static final ASN1ObjectIdentifier dstu4145be = UaOid.branch("1.3.1.1.1.1");
}