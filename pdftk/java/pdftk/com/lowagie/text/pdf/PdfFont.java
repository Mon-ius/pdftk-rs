package pdftk.com.lowagie.text.pdf;
import pdftk.com.lowagie.text.ExceptionConverter;
class PdfFont implements Comparable {
    private BaseFont font;
    private float size;
    protected float hScale = 1;
    PdfFont(BaseFont bf, float size) {
        this.size = size;
        font = bf;
    }
    public int compareTo(Object object) {
        if (object == null) {
            return -1;
        }
        PdfFont pdfFont;
        try {
            pdfFont = (PdfFont) object;
            if (font != pdfFont.font) {
                return 1;
            }
            if (this.size() != pdfFont.size()) {
                return 2;
            }
            return 0;
        }
        catch(ClassCastException cce) {
            return -2;
        }
    }
    float size() {
	return size;
    }
    float width() {
        return width(' ');
    }
    float width(char character) {
	return font.getWidthPoint(character, size) * hScale;
    }
    float width(String s) {
	return font.getWidthPoint(s, size) * hScale;
    }
    BaseFont getFont() {
        return font;
    }
    static PdfFont getDefaultFont() {
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
            return new PdfFont(bf, 12);
        }
        catch (Exception ee) {
            throw new ExceptionConverter(ee);
        }
    }
    void setHorizontalScaling(float hScale) {
        this.hScale = hScale;
    }
}