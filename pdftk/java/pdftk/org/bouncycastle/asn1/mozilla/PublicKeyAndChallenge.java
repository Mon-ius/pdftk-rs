package pdftk.org.bouncycastle.asn1.mozilla;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.DERIA5String;
import pdftk.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
public class PublicKeyAndChallenge
    extends ASN1Object
{
    private ASN1Sequence         pkacSeq;
    private SubjectPublicKeyInfo spki;
    private DERIA5String         challenge;
    public static PublicKeyAndChallenge getInstance(Object obj)
    {
        if (obj instanceof PublicKeyAndChallenge)
        {
            return (PublicKeyAndChallenge)obj;
        }
        else if (obj != null)
        {
            return new PublicKeyAndChallenge(ASN1Sequence.getInstance(obj));
        }
        return null;
    }
    private PublicKeyAndChallenge(ASN1Sequence seq)
    {
        pkacSeq = seq;
        spki = SubjectPublicKeyInfo.getInstance(seq.getObjectAt(0));
        challenge = DERIA5String.getInstance(seq.getObjectAt(1));
    }
    public ASN1Primitive toASN1Primitive()
    {
        return pkacSeq;
    }
    public SubjectPublicKeyInfo getSubjectPublicKeyInfo()
    {
        return spki;
    }
    public DERIA5String getChallenge()
    {
        return challenge;
    }
}