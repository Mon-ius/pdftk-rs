package pdftk.org.bouncycastle.asn1.eac;
import java.io.IOException;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1InputStream;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.BERTags;
import pdftk.org.bouncycastle.asn1.DERApplicationSpecific;
import pdftk.org.bouncycastle.asn1.DEROctetString;
public class CertificateBody
    extends ASN1Object
{
    ASN1InputStream seq;
    private DERApplicationSpecific certificateProfileIdentifier;
    private DERApplicationSpecific certificationAuthorityReference;
    private PublicKeyDataObject publicKey;
    private DERApplicationSpecific certificateHolderReference;
    private CertificateHolderAuthorization certificateHolderAuthorization;
    private DERApplicationSpecific certificateEffectiveDate;
    private DERApplicationSpecific certificateExpirationDate;
    private int certificateType = 0;
    private static final int CPI = 0x01;
    private static final int CAR = 0x02;
    private static final int PK = 0x04;
    private static final int CHR = 0x08;
    private static final int CHA = 0x10;
    private static final int CEfD = 0x20;
    private static final int CExD = 0x40;
    public static final int profileType = 0x7f;
    public static final int requestType = 0x0D;
    private void setIso7816CertificateBody(DERApplicationSpecific appSpe)
        throws IOException
    {
        byte[] content;
        if (appSpe.getApplicationTag() == EACTags.CERTIFICATE_CONTENT_TEMPLATE)
        {
            content = appSpe.getContents();
        }
        else
        {
            throw new IOException("Bad tag : not an iso7816 CERTIFICATE_CONTENT_TEMPLATE");
        }
        ASN1InputStream aIS = new ASN1InputStream(content);
        ASN1Primitive obj;
        while ((obj = aIS.readObject()) != null)
        {
            DERApplicationSpecific aSpe;
            if (obj instanceof DERApplicationSpecific)
            {
                aSpe = (DERApplicationSpecific)obj;
            }
            else
            {
                throw new IOException("Not a valid iso7816 content : not a DERApplicationSpecific Object :" + EACTags.encodeTag(appSpe) + obj.getClass());
            }
            switch (aSpe.getApplicationTag())
            {
            case EACTags.INTERCHANGE_PROFILE:
                setCertificateProfileIdentifier(aSpe);
                break;
            case EACTags.ISSUER_IDENTIFICATION_NUMBER:
                setCertificationAuthorityReference(aSpe);
                break;
            case EACTags.CARDHOLDER_PUBLIC_KEY_TEMPLATE:
                setPublicKey(PublicKeyDataObject.getInstance(aSpe.getObject(BERTags.SEQUENCE)));
                break;
            case EACTags.CARDHOLDER_NAME:
                setCertificateHolderReference(aSpe);
                break;
            case EACTags.CERTIFICATE_HOLDER_AUTHORIZATION_TEMPLATE:
                setCertificateHolderAuthorization(new CertificateHolderAuthorization(aSpe));
                break;
            case EACTags.APPLICATION_EFFECTIVE_DATE:
                setCertificateEffectiveDate(aSpe);
                break;
            case EACTags.APPLICATION_EXPIRATION_DATE:
                setCertificateExpirationDate(aSpe);
                break;
            default:
                certificateType = 0;
                throw new IOException("Not a valid iso7816 DERApplicationSpecific tag " + aSpe.getApplicationTag());
            }
        }
    }
    public CertificateBody(
        DERApplicationSpecific certificateProfileIdentifier,
        CertificationAuthorityReference certificationAuthorityReference,
        PublicKeyDataObject publicKey,
        CertificateHolderReference certificateHolderReference,
        CertificateHolderAuthorization certificateHolderAuthorization,
        PackedDate certificateEffectiveDate,
        PackedDate certificateExpirationDate
    )
    {
        setCertificateProfileIdentifier(certificateProfileIdentifier);
        setCertificationAuthorityReference(new DERApplicationSpecific(
            EACTags.ISSUER_IDENTIFICATION_NUMBER, certificationAuthorityReference.getEncoded()));
        setPublicKey(publicKey);
        setCertificateHolderReference(new DERApplicationSpecific(
				EACTags.CARDHOLDER_NAME, certificateHolderReference.getEncoded()));
        setCertificateHolderAuthorization(certificateHolderAuthorization);
        try
        {
            setCertificateEffectiveDate(new DERApplicationSpecific(
                false, EACTags.APPLICATION_EFFECTIVE_DATE, new DEROctetString(certificateEffectiveDate.getEncoding())));
            setCertificateExpirationDate(new DERApplicationSpecific(
                false, EACTags.APPLICATION_EXPIRATION_DATE, new DEROctetString(certificateExpirationDate.getEncoding())));
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("unable to encode dates: " + e.getMessage());
        }
    }
    private CertificateBody(DERApplicationSpecific obj)
        throws IOException
    {
        setIso7816CertificateBody(obj);
    }
    private ASN1Primitive profileToASN1Object()
        throws IOException
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(certificateProfileIdentifier);
        v.add(certificationAuthorityReference);
        v.add(new DERApplicationSpecific(false, EACTags.CARDHOLDER_PUBLIC_KEY_TEMPLATE, publicKey));
        v.add(certificateHolderReference);
        v.add(certificateHolderAuthorization);
        v.add(certificateEffectiveDate);
        v.add(certificateExpirationDate);
        return new DERApplicationSpecific(EACTags.CERTIFICATE_CONTENT_TEMPLATE, v);
    }
    private void setCertificateProfileIdentifier(DERApplicationSpecific certificateProfileIdentifier)
	throws IllegalArgumentException {
		if (certificateProfileIdentifier.getApplicationTag() == EACTags.INTERCHANGE_PROFILE) {
			this.certificateProfileIdentifier = certificateProfileIdentifier;
			certificateType |= CPI;
		}
		else
			throw new IllegalArgumentException("Not an Iso7816Tags.INTERCHANGE_PROFILE tag :"+ EACTags.encodeTag(certificateProfileIdentifier));
	}
    private void setCertificateHolderReference(DERApplicationSpecific certificateHolderReference)
	throws IllegalArgumentException {
		if (certificateHolderReference.getApplicationTag() == EACTags.CARDHOLDER_NAME) {
			this.certificateHolderReference = certificateHolderReference;
			certificateType |= CHR;
		}
		else
			throw new IllegalArgumentException("Not an Iso7816Tags.CARDHOLDER_NAME tag");
	}
	private void setCertificationAuthorityReference(
			DERApplicationSpecific certificationAuthorityReference)
				throws IllegalArgumentException {
		if (certificationAuthorityReference.getApplicationTag() == EACTags.ISSUER_IDENTIFICATION_NUMBER) {
			this.certificationAuthorityReference = certificationAuthorityReference;
			certificateType |= CAR;
		}
		else
			throw new IllegalArgumentException("Not an Iso7816Tags.ISSUER_IDENTIFICATION_NUMBER tag");
	}
	private void setPublicKey(PublicKeyDataObject publicKey)
    {
		this.publicKey = PublicKeyDataObject.getInstance(publicKey);
        this.certificateType |= PK;
	}
    private ASN1Primitive requestToASN1Object()
        throws IOException
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(certificateProfileIdentifier);
        v.add(new DERApplicationSpecific(false, EACTags.CARDHOLDER_PUBLIC_KEY_TEMPLATE, publicKey));
        v.add(certificateHolderReference);
        return new DERApplicationSpecific(EACTags.CERTIFICATE_CONTENT_TEMPLATE, v);
    }
    public ASN1Primitive toASN1Primitive()
    {
        try
        {
            if (certificateType == profileType)
            {
                return profileToASN1Object();
            }
            if (certificateType == requestType)
            {
                return requestToASN1Object();
            }
        }
        catch (IOException e)
        {
            return null;
        }
        return null;
    }
    public int getCertificateType()
    {
        return certificateType;
    }
    public static CertificateBody getInstance(Object obj)
        throws IOException
    {
        if (obj instanceof CertificateBody)
        {
            return (CertificateBody)obj;
        }
        else if (obj != null)
        {
            return new CertificateBody(DERApplicationSpecific.getInstance(obj));
        }
        return null;
    }
    public PackedDate getCertificateEffectiveDate()
    {
        if ((this.certificateType & CertificateBody.CEfD) ==
            CertificateBody.CEfD)
        {
            return new PackedDate(certificateEffectiveDate.getContents());
        }
        return null;
    }
    private void setCertificateEffectiveDate(DERApplicationSpecific ced)
        throws IllegalArgumentException
    {
        if (ced.getApplicationTag() == EACTags.APPLICATION_EFFECTIVE_DATE)
        {
            this.certificateEffectiveDate = ced;
            certificateType |= CEfD;
        }
        else
        {
            throw new IllegalArgumentException("Not an Iso7816Tags.APPLICATION_EFFECTIVE_DATE tag :" + EACTags.encodeTag(ced));
        }
    }
    public PackedDate getCertificateExpirationDate()
        throws IOException
    {
        if ((this.certificateType & CertificateBody.CExD) ==
            CertificateBody.CExD)
        {
            return new PackedDate(certificateExpirationDate.getContents());
        }
        throw new IOException("certificate Expiration Date not set");
    }
    private void setCertificateExpirationDate(DERApplicationSpecific ced)
        throws IllegalArgumentException
    {
        if (ced.getApplicationTag() == EACTags.APPLICATION_EXPIRATION_DATE)
        {
            this.certificateExpirationDate = ced;
            certificateType |= CExD;
        }
        else
        {
            throw new IllegalArgumentException("Not an Iso7816Tags.APPLICATION_EXPIRATION_DATE tag");
        }
    }
    public CertificateHolderAuthorization getCertificateHolderAuthorization()
        throws IOException
    {
        if ((this.certificateType & CertificateBody.CHA) ==
            CertificateBody.CHA)
        {
            return certificateHolderAuthorization;
        }
        throw new IOException("Certificate Holder Authorisation not set");
    }
    private void setCertificateHolderAuthorization(
        CertificateHolderAuthorization cha)
    {
        this.certificateHolderAuthorization = cha;
        certificateType |= CHA;
    }
    public CertificateHolderReference getCertificateHolderReference()
    {
        return new CertificateHolderReference(certificateHolderReference.getContents());
    }
    public DERApplicationSpecific getCertificateProfileIdentifier()
    {
        return certificateProfileIdentifier;
    }
    public CertificationAuthorityReference getCertificationAuthorityReference()
        throws IOException
    {
        if ((this.certificateType & CertificateBody.CAR) ==
            CertificateBody.CAR)
        {
            return new CertificationAuthorityReference(certificationAuthorityReference.getContents());
        }
        throw new IOException("Certification authority reference not set");
    }
    public PublicKeyDataObject getPublicKey()
    {
        return publicKey;
    }
}