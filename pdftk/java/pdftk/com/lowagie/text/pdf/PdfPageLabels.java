package pdftk.com.lowagie.text.pdf;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.Iterator;
public class PdfPageLabels implements Comparator {
    public static int DECIMAL_ARABIC_NUMERALS = 0;
    public static int UPPERCASE_ROMAN_NUMERALS = 1;
    public static int LOWERCASE_ROMAN_NUMERALS = 2;
    public static int UPPERCASE_LETTERS = 3;
    public static int LOWERCASE_LETTERS = 4;
    public static int EMPTY = 5;
    static PdfName numberingStyle[] = new PdfName[]{PdfName.D, PdfName.R,
                new PdfName("r"), PdfName.A, new PdfName("a")};
    TreeMap map;
    public PdfPageLabels() {
        map = new TreeMap(this);
        addPageLabel(1, DECIMAL_ARABIC_NUMERALS, null, 1);
    }
    public int compare(Object obj, Object obj1) {
        int v1 = ((Integer)obj).intValue();
        int v2 = ((Integer)obj1).intValue();
        if (v1 < v2)
            return -1;
        if (v1 == v2)
            return 0;
        return 1;
    }
    public boolean equals(Object obj) {
        return true;
    }
    public void addPageLabel(int page, int numberStyle, String text, int firstPage) {
        if (page < 1 || firstPage < 1)
            throw new IllegalArgumentException("In a page label the page numbers must be greater or equal to 1.");
        PdfName pdfName = null;
        if (numberStyle >= 0 && numberStyle < numberingStyle.length)
            pdfName = numberingStyle[numberStyle];
        Integer iPage = new Integer(page);
        Object obj = new Object[]{iPage, pdfName, text, new Integer(firstPage)};
        map.put(iPage, obj);
    }
    public void addPageLabel(int page, int numberStyle, String text) {
        addPageLabel(page, numberStyle, text, 1);
    }
    public void addPageLabel(int page, int numberStyle) {
        addPageLabel(page, numberStyle, null, 1);
    }
    public void removePageLabel(int page) {
        if (page <= 1)
            return;
        map.remove(new Integer(page));
    }
    PdfDictionary getDictionary() {
        PdfDictionary dic = new PdfDictionary();
        PdfArray array = new PdfArray();
        for (Iterator it = map.values().iterator(); it.hasNext();) {
            Object obj[] = (Object[])it.next();
            PdfDictionary subDic = new PdfDictionary();
            PdfName pName = (PdfName)obj[1];
            if (pName != null)
                subDic.put(PdfName.S, pName);
            String text = (String)obj[2];
            if (text != null)
                subDic.put(PdfName.P, new PdfString(text, PdfObject.TEXT_UNICODE));
            int st = ((Integer)obj[3]).intValue();
            if (st != 1)
                subDic.put(PdfName.ST, new PdfNumber(st));
            array.add(new PdfNumber(((Integer)obj[0]).intValue() - 1));
            array.add(subDic);
        }
        dic.put(PdfName.NUMS, array);
        return dic;
    }
}