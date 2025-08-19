package pdftk.org.bouncycastle.asn1.isismtt.x509;
import java.util.Enumeration;
import pdftk.org.bouncycastle.asn1.ASN1Encodable;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1String;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERIA5String;
import pdftk.org.bouncycastle.asn1.DERObjectIdentifier;
import pdftk.org.bouncycastle.asn1.DERSequence;
import pdftk.org.bouncycastle.asn1.isismtt.ISISMTTObjectIdentifiers;
import pdftk.org.bouncycastle.asn1.x500.DirectoryString;
public class NamingAuthority
    extends ASN1Object
{
    public static final ASN1ObjectIdentifier id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern =
        new ASN1ObjectIdentifier(ISISMTTObjectIdentifiers.id_isismtt_at_namingAuthorities + ".1");
    private ASN1ObjectIdentifier namingAuthorityId;
    private String namingAuthorityUrl;
    private DirectoryString namingAuthorityText;
    public static NamingAuthority getInstance(Object obj)
    {
        if (obj == null || obj instanceof NamingAuthority)
        {
            return (NamingAuthority)obj;
        }
        if (obj instanceof ASN1Sequence)
        {
            return new NamingAuthority((ASN1Sequence)obj);
        }
        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }
    public static NamingAuthority getInstance(ASN1TaggedObject obj, boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }
    private NamingAuthority(ASN1Sequence seq)
    {
        if (seq.size() > 3)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }
        Enumeration e = seq.getObjects();
        if (e.hasMoreElements())
        {
            ASN1Encodable o = (ASN1Encodable)e.nextElement();
            if (o instanceof ASN1ObjectIdentifier)
            {
                namingAuthorityId = (ASN1ObjectIdentifier)o;
            }
            else if (o instanceof DERIA5String)
            {
                namingAuthorityUrl = DERIA5String.getInstance(o).getString();
            }
            else if (o instanceof ASN1String)
            {
                namingAuthorityText = DirectoryString.getInstance(o);
            }
            else
            {
                throw new IllegalArgumentException("Bad object encountered: "
                    + o.getClass());
            }
        }
        if (e.hasMoreElements())
        {
            ASN1Encodable o = (ASN1Encodable)e.nextElement();
            if (o instanceof DERIA5String)
            {
                namingAuthorityUrl = DERIA5String.getInstance(o).getString();
            }
            else if (o instanceof ASN1String)
            {
                namingAuthorityText = DirectoryString.getInstance(o);
            }
            else
            {
                throw new IllegalArgumentException("Bad object encountered: "
                    + o.getClass());
            }
        }
        if (e.hasMoreElements())
        {
            ASN1Encodable o = (ASN1Encodable)e.nextElement();
            if (o instanceof ASN1String)
            {
                namingAuthorityText = DirectoryString.getInstance(o);
            }
            else
            {
                throw new IllegalArgumentException("Bad object encountered: "
                    + o.getClass());
            }
        }
    }
    public ASN1ObjectIdentifier getNamingAuthorityId()
    {
        return namingAuthorityId;
    }
    public DirectoryString getNamingAuthorityText()
    {
        return namingAuthorityText;
    }
    public String getNamingAuthorityUrl()
    {
        return namingAuthorityUrl;
    }
    public NamingAuthority(DERObjectIdentifier namingAuthorityId,
                           String namingAuthorityUrl, DirectoryString namingAuthorityText)
    {
        this.namingAuthorityId = new ASN1ObjectIdentifier(namingAuthorityId.getId());
        this.namingAuthorityUrl = namingAuthorityUrl;
        this.namingAuthorityText = namingAuthorityText;
    }
    public NamingAuthority(ASN1ObjectIdentifier namingAuthorityId,
                           String namingAuthorityUrl, DirectoryString namingAuthorityText)
    {
        this.namingAuthorityId = namingAuthorityId;
        this.namingAuthorityUrl = namingAuthorityUrl;
        this.namingAuthorityText = namingAuthorityText;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        if (namingAuthorityId != null)
        {
            vec.add(namingAuthorityId);
        }
        if (namingAuthorityUrl != null)
        {
            vec.add(new DERIA5String(namingAuthorityUrl, true));
        }
        if (namingAuthorityText != null)
        {
            vec.add(namingAuthorityText);
        }
        return new DERSequence(vec);
    }
}