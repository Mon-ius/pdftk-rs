package pdftk.org.bouncycastle.asn1.x509;
import pdftk.org.bouncycastle.asn1.ASN1Choice;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERTaggedObject;
public class Target
    extends ASN1Object
    implements ASN1Choice
{
    public static final int targetName = 0;
    public static final int targetGroup = 1;
    private GeneralName targName;
    private GeneralName targGroup;
    public static Target getInstance(Object obj)
    {
        if (obj == null || obj instanceof Target)
        {
            return (Target) obj;
        }
        else if (obj instanceof ASN1TaggedObject)
        {
            return new Target((ASN1TaggedObject)obj);
        }
        throw new IllegalArgumentException("unknown object in factory: "
            + obj.getClass());
    }
    private Target(ASN1TaggedObject tagObj)
    {
        switch (tagObj.getTagNo())
        {
        case targetName:
            targName = GeneralName.getInstance(tagObj, true);
            break;
        case targetGroup:
            targGroup = GeneralName.getInstance(tagObj, true);
            break;
        default:
            throw new IllegalArgumentException("unknown tag: " + tagObj.getTagNo());
        }
    }
    public Target(int type, GeneralName name)
    {
        this(new DERTaggedObject(type, name));
    }
    public GeneralName getTargetGroup()
    {
        return targGroup;
    }
    public GeneralName getTargetName()
    {
        return targName;
    }
    public ASN1Primitive toASN1Primitive()
    {
        if (targName != null)
        {
            return new DERTaggedObject(true, 0, targName);
        }
        else
        {
            return new DERTaggedObject(true, 1, targGroup);
        }
    }
}