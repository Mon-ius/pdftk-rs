package pdftk.com.lowagie.text.pdf;
import java.util.HashMap;
public interface SimpleXMLDocHandler {
    public void startElement(String tag, HashMap h);
    public void endElement(String tag);
    public void startDocument();
    public void endDocument();
    public void text(String str);
}