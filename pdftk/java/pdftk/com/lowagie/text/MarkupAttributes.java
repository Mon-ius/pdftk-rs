package pdftk.com.lowagie.text;
import java.util.Properties;
import java.util.Set;
public interface MarkupAttributes extends pdftk.com.lowagie.text.Element {
    public void setMarkupAttribute(String name, String value);
    public void setMarkupAttributes(Properties markupAttributes);
    public String getMarkupAttribute(String name);
    public Set getMarkupAttributeNames();
    public Properties getMarkupAttributes();
}