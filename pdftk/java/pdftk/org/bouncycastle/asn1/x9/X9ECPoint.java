package pdftk.org.bouncycastle.asn1.x9;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1OctetString;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.DEROctetString;
import pdftk.org.bouncycastle.math.ec.ECCurve;
import pdftk.org.bouncycastle.math.ec.ECPoint;
public class X9ECPoint
    extends ASN1Object
{
    ECPoint p;
    public X9ECPoint(
        ECPoint p)
    {
        this.p = p;
    }
    public X9ECPoint(
        ECCurve          c,
        ASN1OctetString  s)
    {
        this.p = c.decodePoint(s.getOctets());
    }
    public ECPoint getPoint()
    {
        return p;
    }
    public ASN1Primitive toASN1Primitive()
    {
        return new DEROctetString(p.getEncoded());
    }
}