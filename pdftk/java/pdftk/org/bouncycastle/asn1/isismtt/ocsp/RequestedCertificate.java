package pdftk.org.bouncycastle.asn1.isismtt.ocsp;
import java.io.IOException;
import pdftk.org.bouncycastle.asn1.ASN1Choice;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1OctetString;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DEROctetString;
import pdftk.org.bouncycastle.asn1.DERTaggedObject;
import pdftk.org.bouncycastle.asn1.x509.Certificate;
public class RequestedCertificate
    extends ASN1Object
    implements ASN1Choice
{
    public static final int certificate = -1;
    public static final int publicKeyCertificate = 0;
    public static final int attributeCertificate = 1;
    private Certificate cert;
    private byte[] publicKeyCert;
    private byte[] attributeCert;
    public static RequestedCertificate getInstance(Object obj)
    {
        if (obj == null || obj instanceof RequestedCertificate)
        {
            return (RequestedCertificate)obj;
        }
        if (obj instanceof ASN1Sequence)
        {
            return new RequestedCertificate(Certificate.getInstance(obj));
        }
        if (obj instanceof ASN1TaggedObject)
        {
            return new RequestedCertificate((ASN1TaggedObject)obj);
        }
        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }
    public static RequestedCertificate getInstance(ASN1TaggedObject obj, boolean explicit)
    {
        if (!explicit)
        {
            throw new IllegalArgumentException("choice item must be explicitly tagged");
        }
        return getInstance(obj.getObject());
    }
    private RequestedCertificate(ASN1TaggedObject tagged)
    {
        if (tagged.getTagNo() == publicKeyCertificate)
        {
            publicKeyCert = ASN1OctetString.getInstance(tagged, true).getOctets();
        }
        else if (tagged.getTagNo() == attributeCertificate)
        {
            attributeCert = ASN1OctetString.getInstance(tagged, true).getOctets();
        }
        else
        {
            throw new IllegalArgumentException("unknown tag number: " + tagged.getTagNo());
        }
    }
    public RequestedCertificate(Certificate certificate)
    {
        this.cert = certificate;
    }
    public RequestedCertificate(int type, byte[] certificateOctets)
    {
        this(new DERTaggedObject(type, new DEROctetString(certificateOctets)));
    }
    public int getType()
    {
        if (cert != null)
        {
            return certificate;
        }
        if (publicKeyCert != null)
        {
            return publicKeyCertificate;
        }
        return attributeCertificate;
    }
    public byte[] getCertificateBytes()
    {
        if (cert != null)
        {
            try
            {
                return cert.getEncoded();
            }
            catch (IOException e)
            {
                throw new IllegalStateException("can't decode certificate: " + e);
            }
        }
        if (publicKeyCert != null)
        {
            return publicKeyCert;
        }
        return attributeCert;
    }
    public ASN1Primitive toASN1Primitive()
    {
        if (publicKeyCert != null)
        {
            return new DERTaggedObject(0, new DEROctetString(publicKeyCert));
        }
        if (attributeCert != null)
        {
            return new DERTaggedObject(1, new DEROctetString(attributeCert));
        }
        return cert.toASN1Primitive();
    }
}