package pdftk.org.bouncycastle.asn1;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import pdftk.org.bouncycastle.util.Arrays;
import pdftk.org.bouncycastle.util.Strings;
public class DERUTCTime
    extends ASN1Primitive
{
    private byte[]      time;
    public static ASN1UTCTime getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof ASN1UTCTime)
        {
            return (ASN1UTCTime)obj;
        }
        if (obj instanceof DERUTCTime)
        {
            return new ASN1UTCTime(((DERUTCTime)obj).time);
        }
        if (obj instanceof byte[])
        {
            try
            {
                return (ASN1UTCTime)fromByteArray((byte[])obj);
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("encoding error in getInstance: " + e.toString());
            }
        }
        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }
    public static ASN1UTCTime getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        ASN1Object o = obj.getObject();
        if (explicit || o instanceof ASN1UTCTime)
        {
            return getInstance(o);
        }
        else
        {
            return new ASN1UTCTime(((ASN1OctetString)o).getOctets());
        }
    }
    public DERUTCTime(
        String  time)
    {
        this.time = Strings.toByteArray(time);
        try
        {
            this.getDate();
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException("invalid date string: " + e.getMessage());
        }
    }
    public DERUTCTime(
        Date time)
    {
        SimpleDateFormat dateF = new SimpleDateFormat("yyMMddHHmmss'Z'");
        dateF.setTimeZone(new SimpleTimeZone(0,"Z"));
        this.time = Strings.toByteArray(dateF.format(time));
    }
    DERUTCTime(
        byte[]  time)
    {
        this.time = time;
    }
    public Date getDate()
        throws ParseException
    {
        SimpleDateFormat dateF = new SimpleDateFormat("yyMMddHHmmssz");
        return dateF.parse(getTime());
    }
    public Date getAdjustedDate()
        throws ParseException
    {
        SimpleDateFormat dateF = new SimpleDateFormat("yyyyMMddHHmmssz");
        dateF.setTimeZone(new SimpleTimeZone(0, "Z"));
        return dateF.parse(getAdjustedTime());
    }
    public String getTime()
    {
        String stime = Strings.fromByteArray(time);
        if (stime.indexOf('-') < 0 && stime.indexOf('+') < 0)
        {
            if (stime.length() == 11)
            {
                return stime.substring(0, 10) + "00GMT+00:00";
            }
            else
            {
                return stime.substring(0, 12) + "GMT+00:00";
            }
        }
        else
        {
            int index = stime.indexOf('-');
            if (index < 0)
            {
                index = stime.indexOf('+');
            }
            String d = stime;
            if (index == stime.length() - 3)
            {
                d += "00";
            }
            if (index == 10)
            {
                return d.substring(0, 10) + "00GMT" + d.substring(10, 13) + ":" + d.substring(13, 15);
            }
            else
            {
                return d.substring(0, 12) + "GMT" + d.substring(12, 15) + ":" +  d.substring(15, 17);
            }
        }
    }
    public String getAdjustedTime()
    {
        String   d = this.getTime();
        if (d.charAt(0) < '5')
        {
            return "20" + d;
        }
        else
        {
            return "19" + d;
        }
    }
    boolean isConstructed()
    {
        return false;
    }
    int encodedLength()
    {
        int length = time.length;
        return 1 + StreamUtil.calculateBodyLength(length) + length;
    }
    void encode(
        ASN1OutputStream  out)
        throws IOException
    {
        out.write(BERTags.UTC_TIME);
        int length = time.length;
        out.writeLength(length);
        for (int i = 0; i != length; i++)
        {
            out.write((byte)time[i]);
        }
    }
    boolean asn1Equals(
        ASN1Primitive o)
    {
        if (!(o instanceof DERUTCTime))
        {
            return false;
        }
        return Arrays.areEqual(time, ((DERUTCTime)o).time);
    }
    public int hashCode()
    {
        return Arrays.hashCode(time);
    }
    public String toString()
    {
      return Strings.fromByteArray(time);
    }
}