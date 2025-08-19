package pdftk.org.bouncycastle.asn1.x509;
import java.util.Enumeration;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.DERSequence;
public class Targets
    extends ASN1Object
{
    private ASN1Sequence targets;
    public static Targets getInstance(Object obj)
    {
        if (obj instanceof Targets)
        {
            return (Targets)obj;
        }
        else if (obj != null)
        {
            return new Targets(ASN1Sequence.getInstance(obj));
        }
        return null;
    }
    private Targets(ASN1Sequence targets)
    {
        this.targets = targets;
    }
    public Targets(Target[] targets)
    {
        this.targets = new DERSequence(targets);
    }
    public Target[] getTargets()
    {
        Target[] targs = new Target[targets.size()];
        int count = 0;
        for (Enumeration e = targets.getObjects(); e.hasMoreElements();)
        {
            targs[count++] = Target.getInstance(e.nextElement());
        }
        return targs;
    }
    public ASN1Primitive toASN1Primitive()
    {
        return targets;
    }
}