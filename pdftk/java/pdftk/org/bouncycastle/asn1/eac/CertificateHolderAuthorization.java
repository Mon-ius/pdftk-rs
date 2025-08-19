package pdftk.org.bouncycastle.asn1.eac;
import java.io.IOException;
import java.util.Hashtable;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1InputStream;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.DERApplicationSpecific;
import pdftk.org.bouncycastle.util.Integers;
public class CertificateHolderAuthorization
    extends ASN1Object
{
    ASN1ObjectIdentifier oid;
    DERApplicationSpecific accessRights;
    public static final ASN1ObjectIdentifier id_role_EAC = EACObjectIdentifiers.bsi_de.branch("3.1.2.1");
    public static final int CVCA = 0xC0;
    public static final int DV_DOMESTIC = 0x80;
    public static final int DV_FOREIGN = 0x40;
    public static final int IS = 0;
    public static final int RADG4 = 0x02;
    public static final int RADG3 = 0x01;
    static Hashtable RightsDecodeMap = new Hashtable();
    static BidirectionalMap AuthorizationRole = new BidirectionalMap();
    static Hashtable ReverseMap = new Hashtable();
    static
    {
        RightsDecodeMap.put(Integers.valueOf(RADG4), "RADG4");
        RightsDecodeMap.put(Integers.valueOf(RADG3), "RADG3");
        AuthorizationRole.put(Integers.valueOf(CVCA), "CVCA");
        AuthorizationRole.put(Integers.valueOf(DV_DOMESTIC), "DV_DOMESTIC");
        AuthorizationRole.put(Integers.valueOf(DV_FOREIGN), "DV_FOREIGN");
        AuthorizationRole.put(Integers.valueOf(IS), "IS");
    }
    public static String GetRoleDescription(int i)
    {
        return (String)AuthorizationRole.get(Integers.valueOf(i));
    }
    public static int GetFlag(String description)
    {
        Integer i = (Integer)AuthorizationRole.getReverse(description);
        if (i == null)
        {
            throw new IllegalArgumentException("Unknown value " + description);
        }
        return i.intValue();
    }
    private void setPrivateData(ASN1InputStream cha)
        throws IOException
    {
        ASN1Primitive obj;
        obj = cha.readObject();
        if (obj instanceof ASN1ObjectIdentifier)
        {
            this.oid = (ASN1ObjectIdentifier)obj;
        }
        else
        {
            throw new IllegalArgumentException("no Oid in CerticateHolderAuthorization");
        }
        obj = cha.readObject();
        if (obj instanceof DERApplicationSpecific)
        {
            this.accessRights = (DERApplicationSpecific)obj;
        }
        else
        {
            throw new IllegalArgumentException("No access rights in CerticateHolderAuthorization");
        }
    }
    public CertificateHolderAuthorization(ASN1ObjectIdentifier oid, int rights)
        throws IOException
    {
        setOid(oid);
        setAccessRights((byte)rights);
    }
    public CertificateHolderAuthorization(DERApplicationSpecific aSpe)
        throws IOException
    {
        if (aSpe.getApplicationTag() == EACTags.CERTIFICATE_HOLDER_AUTHORIZATION_TEMPLATE)
        {
            setPrivateData(new ASN1InputStream(aSpe.getContents()));
        }
    }
    public int getAccessRights()
    {
        return accessRights.getContents()[0] & 0xff;
    }
    private void setAccessRights(byte rights)
    {
        byte[] accessRights = new byte[1];
        accessRights[0] = rights;
        this.accessRights = new DERApplicationSpecific(
            EACTags.getTag(EACTags.DISCRETIONARY_DATA), accessRights);
    }
    public ASN1ObjectIdentifier getOid()
    {
        return oid;
    }
    private void setOid(ASN1ObjectIdentifier oid)
    {
        this.oid = oid;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(oid);
        v.add(accessRights);
        return new DERApplicationSpecific(EACTags.CERTIFICATE_HOLDER_AUTHORIZATION_TEMPLATE, v);
    }
}