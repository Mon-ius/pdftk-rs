package pdftk.org.bouncycastle.asn1.eac;
import java.io.IOException;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1InputStream;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.ASN1ParsingException;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.DERApplicationSpecific;
import pdftk.org.bouncycastle.asn1.DEROctetString;
public class CVCertificate
    extends ASN1Object
{
    private CertificateBody certificateBody;
    private byte[] signature;
    private int valid;
    private static int bodyValid = 0x01;
    private static int signValid = 0x02;
    public static final byte version_1 = 0x0;
    public static String ReferenceEncoding = "ISO-8859-1";
    private void setPrivateData(DERApplicationSpecific appSpe)
        throws IOException
    {
        valid = 0;
        if (appSpe.getApplicationTag() == EACTags.CARDHOLDER_CERTIFICATE)
        {
            ASN1InputStream content = new ASN1InputStream(appSpe.getContents());
            ASN1Primitive tmpObj;
            while ((tmpObj = content.readObject()) != null)
            {
                DERApplicationSpecific aSpe;
                if (tmpObj instanceof DERApplicationSpecific)
                {
                    aSpe = (DERApplicationSpecific)tmpObj;
                    switch (aSpe.getApplicationTag())
                    {
                    case EACTags.CERTIFICATE_CONTENT_TEMPLATE:
                        certificateBody = CertificateBody.getInstance(aSpe);
                        valid |= bodyValid;
                        break;
                    case EACTags.STATIC_INTERNAL_AUTHENTIFICATION_ONE_STEP:
                        signature = aSpe.getContents();
                        valid |= signValid;
                        break;
                    default:
                        throw new IOException("Invalid tag, not an Iso7816CertificateStructure :" + aSpe.getApplicationTag());
                    }
                }
                else
                {
                    throw new IOException("Invalid Object, not an Iso7816CertificateStructure");
                }
            }
        }
        else
        {
            throw new IOException("not a CARDHOLDER_CERTIFICATE :" + appSpe.getApplicationTag());
        }
    }
    public CVCertificate(ASN1InputStream aIS)
        throws IOException
    {
        initFrom(aIS);
    }
    private void initFrom(ASN1InputStream aIS)
        throws IOException
    {
        ASN1Primitive obj;
        while ((obj = aIS.readObject()) != null)
        {
            if (obj instanceof DERApplicationSpecific)
            {
                setPrivateData((DERApplicationSpecific)obj);
            }
            else
            {
                throw new IOException("Invalid Input Stream for creating an Iso7816CertificateStructure");
            }
        }
    }
    private CVCertificate(DERApplicationSpecific appSpe)
        throws IOException
    {
        setPrivateData(appSpe);
    }
    public CVCertificate(CertificateBody body, byte[] signature)
        throws IOException
    {
        certificateBody = body;
        this.signature = signature;
        valid |= bodyValid;
        valid |= signValid;
    }
    public static CVCertificate getInstance(Object obj)
    {
        if (obj instanceof CVCertificate)
        {
            return (CVCertificate)obj;
        }
        else if (obj != null)
        {
            try
            {
                return new CVCertificate(DERApplicationSpecific.getInstance(obj));
            }
            catch (IOException e)
            {
                throw new ASN1ParsingException("unable to parse data: " + e.getMessage(), e);
            }
        }
        return null;
    }
    public byte[] getSignature()
    {
        return signature;
    }
    public CertificateBody getBody()
    {
        return certificateBody;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        if (valid != (signValid | bodyValid))
        {
            return null;
        }
        v.add(certificateBody);
        try
        {
            v.add(new DERApplicationSpecific(false, EACTags.STATIC_INTERNAL_AUTHENTIFICATION_ONE_STEP, new DEROctetString(signature)));
        }
        catch (IOException e)
        {
            throw new IllegalStateException("unable to convert signature!");
        }
        return new DERApplicationSpecific(EACTags.CARDHOLDER_CERTIFICATE, v);
    }
    public ASN1ObjectIdentifier getHolderAuthorization()
        throws IOException
    {
        CertificateHolderAuthorization cha = certificateBody.getCertificateHolderAuthorization();
        return cha.getOid();
    }
    public PackedDate getEffectiveDate()
        throws IOException
    {
        return certificateBody.getCertificateEffectiveDate();
    }
    public int getCertificateType()
    {
        return this.certificateBody.getCertificateType();
    }
    public PackedDate getExpirationDate()
        throws IOException
    {
        return certificateBody.getCertificateExpirationDate();
    }
    public int getRole()
        throws IOException
    {
        CertificateHolderAuthorization cha = certificateBody.getCertificateHolderAuthorization();
        return cha.getAccessRights();
    }
    public CertificationAuthorityReference getAuthorityReference()
        throws IOException
    {
        return certificateBody.getCertificationAuthorityReference();
    }
    public CertificateHolderReference getHolderReference()
        throws IOException
    {
        return certificateBody.getCertificateHolderReference();
    }
    public int getHolderAuthorizationRole()
        throws IOException
    {
        int rights = certificateBody.getCertificateHolderAuthorization().getAccessRights();
        return rights & 0xC0;
    }
    public Flags getHolderAuthorizationRights()
        throws IOException
    {
        return new Flags(certificateBody.getCertificateHolderAuthorization().getAccessRights() & 0x1F);
    }
}