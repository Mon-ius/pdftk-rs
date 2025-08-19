package pdftk.org.bouncycastle.asn1.x509;
import pdftk.org.bouncycastle.asn1.ASN1Choice;
import pdftk.org.bouncycastle.asn1.ASN1Object;
import pdftk.org.bouncycastle.asn1.ASN1Primitive;
import pdftk.org.bouncycastle.asn1.ASN1String;
import pdftk.org.bouncycastle.asn1.ASN1TaggedObject;
import pdftk.org.bouncycastle.asn1.DERBMPString;
import pdftk.org.bouncycastle.asn1.DERIA5String;
import pdftk.org.bouncycastle.asn1.DERUTF8String;
import pdftk.org.bouncycastle.asn1.DERVisibleString;
public class DisplayText
    extends ASN1Object
    implements ASN1Choice
{
   public static final int CONTENT_TYPE_IA5STRING = 0;
   public static final int CONTENT_TYPE_BMPSTRING = 1;
   public static final int CONTENT_TYPE_UTF8STRING = 2;
   public static final int CONTENT_TYPE_VISIBLESTRING = 3;
   public static final int DISPLAY_TEXT_MAXIMUM_SIZE = 200;
   int contentType;
   ASN1String contents;
   public DisplayText(int type, String text)
   {
      if (text.length() > DISPLAY_TEXT_MAXIMUM_SIZE)
      {
         text = text.substring (0, DISPLAY_TEXT_MAXIMUM_SIZE);
      }
      contentType = type;
      switch (type)
      {
         case CONTENT_TYPE_IA5STRING:
            contents = new DERIA5String(text);
            break;
         case CONTENT_TYPE_UTF8STRING:
            contents = new DERUTF8String(text);
            break;
         case CONTENT_TYPE_VISIBLESTRING:
            contents = new DERVisibleString(text);
            break;
         case CONTENT_TYPE_BMPSTRING:
            contents = new DERBMPString(text);
            break;
         default:
            contents = new DERUTF8String(text);
            break;
      }
   }
   public DisplayText(String text)
   {
      if (text.length() > DISPLAY_TEXT_MAXIMUM_SIZE)
      {
         text = text.substring(0, DISPLAY_TEXT_MAXIMUM_SIZE);
      }
      contentType = CONTENT_TYPE_UTF8STRING;
      contents = new DERUTF8String(text);
   }
   private DisplayText(ASN1String de)
   {
      contents = de;
   }
   public static DisplayText getInstance(Object obj)
   {
      if  (obj instanceof ASN1String)
      {
          return new DisplayText((ASN1String)obj);
      }
      else if (obj == null || obj instanceof DisplayText)
      {
          return (DisplayText)obj;
      }
      throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
   }
   public static DisplayText getInstance(
       ASN1TaggedObject obj,
       boolean          explicit)
   {
       return getInstance(obj.getObject());
   }
   public ASN1Primitive toASN1Primitive()
   {
      return (ASN1Primitive)contents;
   }
   public String getString()
   {
      return contents.getString();
   }
}