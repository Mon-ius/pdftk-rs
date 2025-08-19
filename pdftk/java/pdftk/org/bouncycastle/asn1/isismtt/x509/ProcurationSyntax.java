package pdftk.org.bouncycastle.asn1.isismtt.x509;
import java.util.Enumeration;
import pdftk.org.bouncycastle.asn1.ASN1Encodable;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERPrintableString;
import pdftk.org.bouncycastle.asn1.DERSequence;
import pdftk.org.bouncycastle.asn1.DERTaggedObject;
import pdftk.org.bouncycastle.asn1.x500.DirectoryString;
import pdftk.org.bouncycastle.asn1.x509.GeneralName;
import pdftk.org.bouncycastle.asn1.x509.IssuerSerial;
public class ProcurationSyntax
    extends ASN1Object
{
    private String country;
    private DirectoryString typeOfSubstitution;
    private GeneralName thirdPerson;
    private IssuerSerial certRef;
    public static ProcurationSyntax getInstance(Object obj)
    {
        if (obj == null || obj instanceof ProcurationSyntax)
        {
            return (ProcurationSyntax)obj;
        }
        if (obj instanceof ASN1Sequence)
        {
            return new ProcurationSyntax((ASN1Sequence)obj);
        }
        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }
    private ProcurationSyntax(ASN1Sequence seq)
    {
        if (seq.size() < 1 || seq.size() > 3)
        {
            throw new IllegalArgumentException("Bad sequence size: " + seq.size());
        }
        Enumeration e = seq.getObjects();
        while (e.hasMoreElements())
        {
            ASN1TaggedObject o = ASN1TaggedObject.getInstance(e.nextElement());
            switch (o.getTagNo())
            {
                case 1:
                    country = DERPrintableString.getInstance(o, true).getString();
                    break;
                case 2:
                    typeOfSubstitution = DirectoryString.getInstance(o, true);
                    break;
                case 3:
                    ASN1Encodable signingFor = o.getObject();
                    if (signingFor instanceof ASN1TaggedObject)
                    {
                        thirdPerson = GeneralName.getInstance(signingFor);
                    }
                    else
                    {
                        certRef = IssuerSerial.getInstance(signingFor);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Bad tag number: " + o.getTagNo());
            }
        }
    }
    public ProcurationSyntax(
        String country,
        DirectoryString typeOfSubstitution,
        IssuerSerial certRef)
    {
        this.country = country;
        this.typeOfSubstitution = typeOfSubstitution;
        this.thirdPerson = null;
        this.certRef = certRef;
    }
    public ProcurationSyntax(
        String country,
        DirectoryString typeOfSubstitution,
        GeneralName thirdPerson)
    {
        this.country = country;
        this.typeOfSubstitution = typeOfSubstitution;
        this.thirdPerson = thirdPerson;
        this.certRef = null;
    }
    public String getCountry()
    {
        return country;
    }
    public DirectoryString getTypeOfSubstitution()
    {
        return typeOfSubstitution;
    }
    public GeneralName getThirdPerson()
    {
        return thirdPerson;
    }
    public IssuerSerial getCertRef()
    {
        return certRef;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        if (country != null)
        {
            vec.add(new DERTaggedObject(true, 1, new DERPrintableString(country, true)));
        }
        if (typeOfSubstitution != null)
        {
            vec.add(new DERTaggedObject(true, 2, typeOfSubstitution));
        }
        if (thirdPerson != null)
        {
            vec.add(new DERTaggedObject(true, 3, thirdPerson));
        }
        else
        {
            vec.add(new DERTaggedObject(true, 3, certRef));
        }
        return new DERSequence(vec);
    }
}