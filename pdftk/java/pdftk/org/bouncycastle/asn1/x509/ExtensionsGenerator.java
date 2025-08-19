package pdftk.org.bouncycastle.asn1.x509;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import pdftk.org.bouncycastle.asn1.ASN1Encodable;
import pdftk.org.bouncycastle.asn1.ASN1Encoding;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.DEROctetString;
public class ExtensionsGenerator
{
    private Hashtable extensions = new Hashtable();
    private Vector extOrdering = new Vector();
    public void reset()
    {
        extensions = new Hashtable();
        extOrdering = new Vector();
    }
    public void addExtension(
        ASN1ObjectIdentifier oid,
        boolean              critical,
        ASN1Encodable        value)
        throws IOException
    {
        this.addExtension(oid, critical, value.toASN1Primitive().getEncoded(ASN1Encoding.DER));
    }
    public void addExtension(
        ASN1ObjectIdentifier oid,
        boolean             critical,
        byte[]              value)
    {
        if (extensions.containsKey(oid))
        {
            throw new IllegalArgumentException("extension " + oid + " already added");
        }
        extOrdering.addElement(oid);
        extensions.put(oid, new Extension(oid, critical, new DEROctetString(value)));
    }
    public boolean isEmpty()
    {
        return extOrdering.isEmpty();
    }
    public Extensions generate()
    {
        Extension[] exts = new Extension[extOrdering.size()];
        for (int i = 0; i != extOrdering.size(); i++)
        {
            exts[i] = (Extension)extensions.get(extOrdering.elementAt(i));
        }
        return new Extensions(exts);
    }
}