package pdftk.org.bouncycastle.asn1.util;
import pdftk.org.bouncycastle.asn1.ASN1Encodable;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
public class DERDump
    extends ASN1Dump
{
    public static String dumpAsString(
        ASN1Primitive obj)
    {
        StringBuffer buf = new StringBuffer();
        _dumpAsString("", false, obj, buf);
        return buf.toString();
    }
    public static String dumpAsString(
        ASN1Encodable obj)
    {
        StringBuffer buf = new StringBuffer();
        _dumpAsString("", false, obj.toASN1Primitive(), buf);
        return buf.toString();
    }
}