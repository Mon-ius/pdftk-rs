package pdftk.org.bouncycastle.asn1.nist;
import java.util.Enumeration;
import java.util.Hashtable;
import pdftk.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import pdftk.org.bouncycastle.asn1.sec.SECNamedCurves;
import pdftk.org.bouncycastle.asn1.sec.SECObjectIdentifiers;
import pdftk.org.bouncycastle.asn1.x9.X9ECParameters;
import pdftk.org.bouncycastle.util.Strings;
public class NISTNamedCurves
{
    static final Hashtable objIds = new Hashtable();
    static final Hashtable names = new Hashtable();
    static void defineCurve(String name, ASN1ObjectIdentifier oid)
    {
        objIds.put(name, oid);
        names.put(oid, name);
    }
    static
    {
        defineCurve("B-571", SECObjectIdentifiers.sect571r1);
        defineCurve("B-409", SECObjectIdentifiers.sect409r1);
        defineCurve("B-283", SECObjectIdentifiers.sect283r1);
        defineCurve("B-233", SECObjectIdentifiers.sect233r1);
        defineCurve("B-163", SECObjectIdentifiers.sect163r2);
        defineCurve("P-521", SECObjectIdentifiers.secp521r1);
        defineCurve("P-384", SECObjectIdentifiers.secp384r1);
        defineCurve("P-256", SECObjectIdentifiers.secp256r1);
        defineCurve("P-224", SECObjectIdentifiers.secp224r1);
        defineCurve("P-192", SECObjectIdentifiers.secp192r1);
    }
    public static X9ECParameters getByName(
        String  name)
    {
        ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier)objIds.get(Strings.toUpperCase(name));
        if (oid != null)
        {
            return getByOID(oid);
        }
        return null;
    }
    public static X9ECParameters getByOID(
        ASN1ObjectIdentifier  oid)
    {
        return SECNamedCurves.getByOID(oid);
    }
    public static ASN1ObjectIdentifier getOID(
        String  name)
    {
        return (ASN1ObjectIdentifier)objIds.get(Strings.toUpperCase(name));
    }
    public static String getName(
        ASN1ObjectIdentifier  oid)
    {
        return (String)names.get(oid);
    }
    public static Enumeration getNames()
    {
        return objIds.keys();
    }
}