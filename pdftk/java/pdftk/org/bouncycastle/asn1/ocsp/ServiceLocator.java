package pdftk.org.bouncycastle.asn1.ocsp;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.DERSequence;
import pdftk.org.bouncycastle.asn1.x500.X500Name;
public class ServiceLocator
    extends ASN1Object
{
    X500Name    issuer;
    ASN1Primitive locator;
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector    v = new ASN1EncodableVector();
        v.add(issuer);
        if (locator != null)
        {
            v.add(locator);
        }
        return new DERSequence(v);
    }
}