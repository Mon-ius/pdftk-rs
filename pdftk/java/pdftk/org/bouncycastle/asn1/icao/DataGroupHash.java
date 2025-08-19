package pdftk.org.bouncycastle.asn1.icao;
import java.util.Enumeration;
import pdftk.org.bouncycastle.asn1.ASN1EncodableVector;
import pdftk.org.bouncycastle.asn1.ASN1Integer;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1OctetString;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1Sequence;
import pdftk.org.bouncycastle.asn1.DERSequence;
public class DataGroupHash
    extends ASN1Object
{
    ASN1Integer dataGroupNumber;
    ASN1OctetString    dataGroupHashValue;
    public static DataGroupHash getInstance(
        Object obj)
    {
        if (obj instanceof DataGroupHash)
        {
            return (DataGroupHash)obj;
        }
        else if (obj != null)
        {
            return new DataGroupHash(ASN1Sequence.getInstance(obj));
        }
        return null;
    }
    private DataGroupHash(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();
        dataGroupNumber = ASN1Integer.getInstance(e.nextElement());
        dataGroupHashValue = ASN1OctetString.getInstance(e.nextElement());
    }
    public DataGroupHash(
        int dataGroupNumber,
        ASN1OctetString     dataGroupHashValue)
    {
        this.dataGroupNumber = new ASN1Integer(dataGroupNumber);
        this.dataGroupHashValue = dataGroupHashValue;
    }
    public int getDataGroupNumber()
    {
        return dataGroupNumber.getValue().intValue();
    }
    public ASN1OctetString getDataGroupHashValue()
    {
        return dataGroupHashValue;
    }
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        seq.add(dataGroupNumber);
        seq.add(dataGroupHashValue);
        return new DERSequence(seq);
    }
}