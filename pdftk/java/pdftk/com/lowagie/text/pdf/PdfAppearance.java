package pdftk.com.lowagie.text.pdf;
import pdftk.com.lowagie.text.Rectangle;
import java.util.HashMap;
public class PdfAppearance extends PdfTemplate {
    public static final HashMap stdFieldFontNames = new HashMap();
    static {
        stdFieldFontNames.put("Courier-BoldOblique", new PdfName("CoBO"));
        stdFieldFontNames.put("Courier-Bold", new PdfName("CoBo"));
        stdFieldFontNames.put("Courier-Oblique", new PdfName("CoOb"));
        stdFieldFontNames.put("Courier", new PdfName("Cour"));
        stdFieldFontNames.put("Helvetica-BoldOblique", new PdfName("HeBO"));
        stdFieldFontNames.put("Helvetica-Bold", new PdfName("HeBo"));
        stdFieldFontNames.put("Helvetica-Oblique", new PdfName("HeOb"));
        stdFieldFontNames.put("Helvetica", new PdfName("Helv"));
        stdFieldFontNames.put("Symbol", new PdfName("Symb"));
        stdFieldFontNames.put("Times-BoldItalic", new PdfName("TiBI"));
        stdFieldFontNames.put("Times-Bold", new PdfName("TiBo"));
        stdFieldFontNames.put("Times-Italic", new PdfName("TiIt"));
        stdFieldFontNames.put("Times-Roman", new PdfName("TiRo"));
        stdFieldFontNames.put("ZapfDingbats", new PdfName("ZaDb"));
        stdFieldFontNames.put("HYSMyeongJo-Medium", new PdfName("HySm"));
        stdFieldFontNames.put("HYGoThic-Medium", new PdfName("HyGo"));
        stdFieldFontNames.put("HeiseiKakuGo-W5", new PdfName("KaGo"));
        stdFieldFontNames.put("HeiseiMin-W3", new PdfName("KaMi"));
        stdFieldFontNames.put("MHei-Medium", new PdfName("MHei"));
        stdFieldFontNames.put("MSung-Light", new PdfName("MSun"));
        stdFieldFontNames.put("STSong-Light", new PdfName("STSo"));
        stdFieldFontNames.put("MSungStd-Light", new PdfName("MSun"));
        stdFieldFontNames.put("STSongStd-Light", new PdfName("STSo"));
        stdFieldFontNames.put("HYSMyeongJoStd-Medium", new PdfName("HySm"));
        stdFieldFontNames.put("KozMinPro-Regular", new PdfName("KaMi"));
    }
    PdfAppearance() {
        super();
        separator = ' ';
    }
    PdfAppearance(PdfIndirectReference iref) {
        thisReference = iref;
    }
    PdfAppearance(PdfWriter wr) {
        super(wr);
        separator = ' ';
    }
    public void setFontAndSize(BaseFont bf, float size) {
        checkWriter();
        state.size = size;
        if (bf.getFontType() == BaseFont.FONT_TYPE_DOCUMENT) {
            state.fontDetails = new FontDetails(null, ((DocumentFont)bf).getIndirectReference(), bf);
        }
        else
            state.fontDetails = writer.addSimple(bf);
        PdfName psn = (PdfName)stdFieldFontNames.get(bf.getPostscriptFontName());
        if (psn == null) {
            psn = new PdfName(bf.getPostscriptFontName());
            bf.setSubset(false);
        }
        PageResources prs = getPageResources();
        prs.addFont(psn, state.fontDetails.getIndirectReference());
        content.append(psn.getBytes()).append(' ').append(size).append(" Tf").append_i(separator);
    }
    public PdfContentByte getDuplicate() {
        PdfAppearance tpl = new PdfAppearance();
        tpl.writer = writer;
        tpl.pdf = pdf;
        tpl.thisReference = thisReference;
        tpl.pageResources = pageResources;
        tpl.bBox = new Rectangle(bBox);
        tpl.group = group;
        tpl.layer = layer;
        if (matrix != null) {
            tpl.matrix = new PdfArray(matrix);
        }
        tpl.separator = separator;
        return tpl;
    }
}