package pdftk.com.lowagie.text.pdf;
import java.awt.Font;
import pdftk.com.lowagie.text.pdf.BaseFont;
import pdftk.com.lowagie.text.pdf.DefaultFontMapper;
public class AsianFontMapper extends DefaultFontMapper {
	public static String ChineseSimplifiedFont = "STSong-Light";
	public static String ChineseSimplifiedEncoding_H = "UniGB-UCS2-H";
	public static String ChineseSimplifiedEncoding_V = "UniGB-UCS2-V";
	public static String ChineseTraditionalFont_MHei = "MHei-Medium";
	public static String ChineseTraditionalFont_MSung = "MSung-Light";
	public static String ChineseTraditionalEncoding_H = "UniCNS-UCS2-H";
	public static String ChineseTraditionalEncoding_V = "UniCNS-UCS2-V";
	public static String JapaneseFont_Go = "HeiseiKakuGo-W5";
	public static String JapaneseFont_Min = "HeiseiMin-W3";
	public static String JapaneseEncoding_H = "UniJIS-UCS2-H";
	public static String JapaneseEncoding_V = "UniJIS-UCS2-V";
	public static String JapaneseEncoding_HW_H = "UniJIS-UCS2-HW-H";
	public static String JapaneseEncoding_HW_V = "UniJIS-UCS2-HW-V";
	public static String KoreanFont_GoThic = "HYGoThic-Medium";
	public static String KoreanFont_SMyeongJo = "HYSMyeongJo-Medium";
	public static String KoreanEncoding_H = "UniKS-UCS2-H";
	public static String KoreanEncoding_V = "UniKS-UCS2-V";
	private String defaultFont;
	private String encoding;
	public AsianFontMapper(String font, String encoding) {
		super();
		this.defaultFont = font;
		this.encoding = encoding;
	}
	public BaseFont awtToPdf(Font font) {
		try {
			BaseFontParameters p = getBaseFontParameters(font.getFontName());
			if (p != null){
				return BaseFont.createFont(p.fontName, p.encoding, p.embedded, p.cached, p.ttfAfm, p.pfb);
			}else{
				return BaseFont.createFont(defaultFont, encoding, true);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}