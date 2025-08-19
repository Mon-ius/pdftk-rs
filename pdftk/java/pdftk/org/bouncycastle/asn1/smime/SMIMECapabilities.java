package pdftk.org.bouncycastle.asn1.smime;
import java.util.Enumeration;
import java.util.Vector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.cms.Attribute;
import pdftk.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
public class SMIMECapabilities
    extends ASN1Object
{
    public static final ASN1ObjectIdentifier preferSignedData = PKCSObjectIdentifiers.preferSignedData;
    public static final ASN1ObjectIdentifier canNotDecryptAny = PKCSObjectIdentifiers.canNotDecryptAny;
    public static final ASN1ObjectIdentifier sMIMECapabilitesVersions = PKCSObjectIdentifiers.sMIMECapabilitiesVersions;
    public static final ASN1ObjectIdentifier dES_CBC = new ASN1ObjectIdentifier("1.3.14.3.2.7");
    public static final ASN1ObjectIdentifier dES_EDE3_CBC = PKCSObjectIdentifiers.des_EDE3_CBC;
    public static final ASN1ObjectIdentifier rC2_CBC = PKCSObjectIdentifiers.RC2_CBC;
    private ASN1Sequence         capabilities;
    public static SMIMECapabilities getInstance(
        Object o)
    {
        if (o == null || o instanceof SMIMECapabilities)
        {
            return (SMIMECapabilities)o;
        }
        if (o instanceof ASN1Sequence)
        {
            return new SMIMECapabilities((ASN1Sequence)o);
        }
        if (o instanceof Attribute)
        {
            return new SMIMECapabilities(
                (ASN1Sequence)(((Attribute)o).getAttrValues().getObjectAt(0)));
        }
        throw new IllegalArgumentException("unknown object in factory: " + o.getClass().getName());
    }
    public SMIMECapabilities(
        ASN1Sequence seq)
    {
        capabilities = seq;
    }
    public Vector getCapabilities(
        ASN1ObjectIdentifier capability)
    {
        Enumeration e = capabilities.getObjects();
        Vector      list = new Vector();
        if (capability == null)
        {
            while (e.hasMoreElements())
            {
                SMIMECapability  cap = SMIMECapability.getInstance(e.nextElement());
                list.addElement(cap);
            }
        }
        else
        {
            while (e.hasMoreElements())
            {
                SMIMECapability  cap = SMIMECapability.getInstance(e.nextElement());
                if (capability.equals(cap.getCapabilityID()))
                {
                    list.addElement(cap);
                }
            }
        }
        return list;
    }
    public ASN1Primitive toASN1Primitive()
    {
        return capabilities;
    }
}