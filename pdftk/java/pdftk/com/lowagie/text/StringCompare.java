package pdftk.com.lowagie.text;
import java.util.Comparator;
public class StringCompare implements Comparator {
    public int compare(Object o1, Object o2) {
        return ((String)o1).compareTo((String)o2);
    }
}