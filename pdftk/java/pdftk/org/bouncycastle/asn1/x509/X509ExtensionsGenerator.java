package pdftk.org.bouncycastle.asn1.x509;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import pdftk.org.bouncycastle.asn1.ASN1Encodable;
import pdftk.org.bouncycastle.asn1.ASN1Encoding;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.DERObjectIdentifier;
import pdftk.org.bouncycastle.asn1.DEROctetString;
public class X509ExtensionsGenerator
{
    private Hashtable extensions = new Hashtable();
    private Vector extOrdering = new Vector();
    public void reset()
    {
        extensions = new Hashtable();
        extOrdering = new Vector();
    }
    public void addExtension(
        DERObjectIdentifier oid,
        boolean             critical,
        ASN1Encodable       value)
    {
        addExtension(new ASN1ObjectIdentifier(oid.getId()), critical, value);
    }
    public void addExtension(
        DERObjectIdentifier oid,
        boolean             critical,
        byte[]              value)
    {
        addExtension(new ASN1ObjectIdentifier(oid.getId()), critical, value);
    }
    public void addExtension(
        ASN1ObjectIdentifier oid,
        boolean             critical,
        ASN1Encodable       value)
    {
        try
        {
            this.addExtension(oid, critical, value.toASN1Primitive().getEncoded(ASN1Encoding.DER));
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("error encoding value: " + e);
        }
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
        extensions.put(oid, new X509Extension(critical, new DEROctetString(value)));
    }
    public boolean isEmpty()
    {
        return extOrdering.isEmpty();
    }
    public X509Extensions generate()
    {
        return new X509Extensions(extOrdering, extensions);
    }
}