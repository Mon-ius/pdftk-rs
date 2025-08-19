package pdftk.com.lowagie.text.pdf;
import java.awt.Color;
import pdftk.com.lowagie.text.Element;
import pdftk.com.lowagie.text.DocumentException;
import pdftk.com.lowagie.text.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
public class TextField extends BaseField {
    private String defaultText;
    private String[] choices;
    private String[] choiceExports;
    private int choiceSelection;
    private int topFirst;
    private float extraMarginLeft;
    private float extraMarginTop;
    public TextField(PdfWriter writer, Rectangle box, String fieldName) {
        super(writer, box, fieldName);
    }
    public PdfAppearance getAppearance() throws IOException, DocumentException {
        PdfAppearance app = getBorderAppearance();
        app.beginVariableText();
        if (text == null || text.length() == 0) {
            app.endVariableText();
            return app;
        }
        BaseFont ufont = getRealFont();
        boolean borderExtra = borderStyle == PdfBorderDictionary.STYLE_BEVELED || borderStyle == PdfBorderDictionary.STYLE_INSET;
        float h = box.height() - borderWidth * 2;
        float bw2 = borderWidth;
        if (borderExtra) {
            h -= borderWidth * 2;
            bw2 *= 2;
        }
        h -= extraMarginTop;
        float offsetX = (borderExtra ? 2 * borderWidth : borderWidth);
        offsetX = Math.max(offsetX, 1);
        float offX = Math.min(bw2, offsetX);
        app.saveState();
        app.rectangle(offX, offX, box.width() - 2 * offX, box.height() - 2 * offX);
        app.clip();
        app.newPath();
        if (textColor == null)
            app.setGrayFill(0);
        else
            app.setColorFill(textColor);
        app.beginText();
        String ptext = text;
        if ((options & PASSWORD) != 0) {
            char[] pchar = new char[text.length()];
            for (int i = 0; i < text.length(); i++)
                pchar[i] = '*';
            ptext = new String(pchar);
        }
        if ((options & MULTILINE) != 0) {
            float usize = fontSize;
            float width = box.width() - 3 * offsetX - extraMarginLeft;
            ArrayList breaks = getHardBreaks(ptext);
            ArrayList lines = breaks;
            float factor = ufont.getFontDescriptor(BaseFont.BBOXURY, 1) - ufont.getFontDescriptor(BaseFont.BBOXLLY, 1);
            if (usize == 0) {
                usize = h / breaks.size() / factor;
                if (usize > 4) {
                    if (usize > 12)
                        usize = 12;
                    float step = Math.max((usize - 4) / 10, 0.2f);
                    for (; usize > 4; usize -= step) {
                        lines = breakLines(breaks, ufont, usize, width);
                        if (lines.size() * usize * factor <= h)
                            break;
                    }
                }
                if (usize <= 4) {
                    usize = 4;
                    lines = breakLines(breaks, ufont, usize, width);
                }
            }
            else
                lines = breakLines(breaks, ufont, usize, width);
            app.setFontAndSize(ufont, usize);
            app.setLeading(usize * factor);
            float offsetY = offsetX + h - ufont.getFontDescriptor(BaseFont.BBOXURY, usize);
            String nt = (String)lines.get(0);
            if (alignment == Element.ALIGN_RIGHT) {
                float wd = ufont.getWidthPoint(nt, usize);
                app.moveText(extraMarginLeft + box.width() - 2 * offsetX - wd, offsetY);
            }
            else if (alignment == Element.ALIGN_CENTER) {
                nt = nt.trim();
                float wd = ufont.getWidthPoint(nt, usize);
                app.moveText(extraMarginLeft + box.width() / 2  - wd / 2, offsetY);
            }
            else
                app.moveText(extraMarginLeft + 2 * offsetX, offsetY);
            app.showText(nt);
            int maxline = (int)(h / usize / factor) + 1;
            maxline = Math.min(maxline, lines.size());
            for (int k = 1; k < maxline; ++k) {
                nt = (String)lines.get(k);
                if (alignment == Element.ALIGN_RIGHT) {
                    float wd = ufont.getWidthPoint(nt, usize);
                    app.moveText(extraMarginLeft + box.width() - 2 * offsetX - wd - app.getXTLM(), 0);
                }
                else if (alignment == Element.ALIGN_CENTER) {
                    nt = nt.trim();
                    float wd = ufont.getWidthPoint(nt, usize);
                    app.moveText(extraMarginLeft + box.width() / 2  - wd / 2 - app.getXTLM(), 0);
                }
                app.newlineShowText(nt);
            }
        }
        else {
            float usize = fontSize;
            if (usize == 0) {
                float maxCalculatedSize = h / (ufont.getFontDescriptor(BaseFont.BBOXURX, 1) - ufont.getFontDescriptor(BaseFont.BBOXLLY, 1));
                float wd = ufont.getWidthPoint(ptext, 1);
                if (wd == 0)
                    usize = maxCalculatedSize;
                else
                    usize = (box.width() - extraMarginLeft - 2 * offsetX) / wd;
                if (usize > maxCalculatedSize)
                    usize = maxCalculatedSize;
                if (usize < 4)
                    usize = 4;
            }
            app.setFontAndSize(ufont, usize);
            float offsetY = offX + ((box.height() - 2*offX) - ufont.getFontDescriptor(BaseFont.ASCENT, usize)) / 2;
            if (offsetY < offX)
                offsetY = offX;
            if (offsetY - offX < -ufont.getFontDescriptor(BaseFont.DESCENT, usize)) {
                float ny = -ufont.getFontDescriptor(BaseFont.DESCENT, usize) + offX;
                float dy = box.height() - offX - ufont.getFontDescriptor(BaseFont.ASCENT, usize);
                offsetY = Math.min(ny, Math.max(offsetY, dy));
            }
            if ((options & COMB) != 0 && maxCharacterLength > 0) {
                int textLen = Math.min(maxCharacterLength, ptext.length());
                int position = 0;
                if (alignment == Element.ALIGN_RIGHT) {
                    position = maxCharacterLength - textLen;
                }
                else if (alignment == Element.ALIGN_CENTER) {
                    position = (maxCharacterLength - textLen) / 2;
                }
                float step = (box.width() - extraMarginLeft) / maxCharacterLength;
                float start = step / 2 + position * step;
                for (int k = 0; k < textLen; ++k) {
                    String c = ptext.substring(k, k + 1);
                    float wd = ufont.getWidthPoint(c, usize);
                    app.setTextMatrix(extraMarginLeft + start - wd / 2, offsetY - extraMarginTop);
                    app.showText(c);
                    start += step;
                }
            }
            else {
                if (alignment == Element.ALIGN_RIGHT) {
                    float wd = ufont.getWidthPoint(ptext, usize);
                    app.moveText(extraMarginLeft + box.width() - 2 * offsetX - wd, offsetY - extraMarginTop);
                }
                else if (alignment == Element.ALIGN_CENTER) {
                    float wd = ufont.getWidthPoint(ptext, usize);
                    app.moveText(extraMarginLeft + box.width() / 2  - wd / 2, offsetY - extraMarginTop);
                }
                else
                    app.moveText(extraMarginLeft + 2 * offsetX, offsetY - extraMarginTop);
                app.showText(ptext);
            }
        }
        app.endText();
        app.restoreState();
        app.endVariableText();
        return app;
    }
    PdfAppearance getListAppearance() throws IOException, DocumentException {
        PdfAppearance app = getBorderAppearance();
        app.beginVariableText();
        if (choices == null || choices.length == 0) {
            app.endVariableText();
            return app;
        }
        int topChoice = choiceSelection;
        if (topChoice >= choices.length) {
            topChoice = choices.length - 1;
        }
        if (topChoice < 0)
            topChoice = 0;
        BaseFont ufont = getRealFont();
        float usize = fontSize;
        if (usize == 0)
            usize = 12;
        boolean borderExtra = borderStyle == PdfBorderDictionary.STYLE_BEVELED || borderStyle == PdfBorderDictionary.STYLE_INSET;
        float h = box.height() - borderWidth * 2;
        if (borderExtra)
            h -= borderWidth * 2;
        float offsetX = (borderExtra ? 2 * borderWidth : borderWidth);
        float leading = ufont.getFontDescriptor(BaseFont.BBOXURY, usize) - ufont.getFontDescriptor(BaseFont.BBOXLLY, usize);
        int maxFit = (int)(h / leading) + 1;
        int first = 0;
        int last = 0;
        last = topChoice + maxFit / 2 + 1;
        first = last - maxFit;
        if (first < 0) {
            last += first;
            first = 0;
        }
        last = first + maxFit;
        if (last > choices.length)
            last = choices.length;
        topFirst = first;
        app.saveState();
        app.rectangle(offsetX, offsetX, box.width() - 2 * offsetX, box.height() - 2 * offsetX);
        app.clip();
        app.newPath();
        Color mColor;
        if (textColor == null)
            mColor = new GrayColor(0);
        else
            mColor = textColor;
        app.setColorFill(new Color(10, 36, 106));
        app.rectangle(offsetX, offsetX + h - (topChoice - first + 1) * leading, box.width() - 2 * offsetX, leading);
        app.fill();
        app.beginText();
        app.setFontAndSize(ufont, usize);
        app.setLeading(leading);
        app.moveText(offsetX * 2, offsetX + h - ufont.getFontDescriptor(BaseFont.BBOXURY, usize) + leading);
        app.setColorFill(mColor);
        for (int idx = first; idx < last; ++idx) {
            if (idx == topChoice) {
                app.setGrayFill(1);
                app.newlineShowText(choices[idx]);
                app.setColorFill(mColor);
            }
            else
                app.newlineShowText(choices[idx]);
        }
        app.endText();
        app.restoreState();
        app.endVariableText();
        return app;
    }
    public PdfFormField getTextField() throws IOException, DocumentException {
        if (maxCharacterLength <= 0)
            options &= ~COMB;
        if ((options & COMB) != 0)
            options &= ~MULTILINE;
        PdfFormField field = PdfFormField.createTextField(writer, false, false, maxCharacterLength);
        field.setWidget(box, PdfAnnotation.HIGHLIGHT_INVERT);
        switch (alignment) {
            case Element.ALIGN_CENTER:
                field.setQuadding(PdfFormField.Q_CENTER);
                break;
            case Element.ALIGN_RIGHT:
                field.setQuadding(PdfFormField.Q_RIGHT);
                break;
        }
        if (rotation != 0)
            field.setMKRotation(rotation);
        if (fieldName != null) {
            field.setFieldName(fieldName);
            field.setValueAsString(text);
            if (defaultText != null)
                field.setDefaultValueAsString(defaultText);
            if ((options & READ_ONLY) != 0)
                field.setFieldFlags(PdfFormField.FF_READ_ONLY);
            if ((options & REQUIRED) != 0)
                field.setFieldFlags(PdfFormField.FF_REQUIRED);
            if ((options & MULTILINE) != 0)
                field.setFieldFlags(PdfFormField.FF_MULTILINE);
            if ((options & DO_NOT_SCROLL) != 0)
                field.setFieldFlags(PdfFormField.FF_DONOTSCROLL);
            if ((options & PASSWORD) != 0)
                field.setFieldFlags(PdfFormField.FF_PASSWORD);
            if ((options & FILE_SELECTION) != 0)
                field.setFieldFlags(PdfFormField.FF_FILESELECT);
            if ((options & DO_NOT_SPELL_CHECK) != 0)
                field.setFieldFlags(PdfFormField.FF_DONOTSPELLCHECK);
            if ((options & COMB) != 0)
                field.setFieldFlags(PdfFormField.FF_COMB);
        }
        field.setBorderStyle(new PdfBorderDictionary(borderWidth, borderStyle, new PdfDashPattern(3)));
        PdfAppearance tp = getAppearance();
        field.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, tp);
        PdfAppearance da = (PdfAppearance)tp.getDuplicate();
        da.setFontAndSize(getRealFont(), fontSize);
        if (textColor == null)
            da.setGrayFill(0);
        else
            da.setColorFill(textColor);
        field.setDefaultAppearanceString(da);
        if (borderColor != null)
            field.setMKBorderColor(borderColor);
        if (backgroundColor != null)
            field.setMKBackgroundColor(backgroundColor);
        switch (visibility) {
            case HIDDEN:
                field.setFlags(PdfAnnotation.FLAGS_PRINT | PdfAnnotation.FLAGS_HIDDEN);
                break;
            case VISIBLE_BUT_DOES_NOT_PRINT:
                break;
            case HIDDEN_BUT_PRINTABLE:
                field.setFlags(PdfAnnotation.FLAGS_PRINT | PdfAnnotation.FLAGS_NOVIEW);
                break;
            default:
                field.setFlags(PdfAnnotation.FLAGS_PRINT);
                break;
        }
        return field;
    }
    public PdfFormField getComboField() throws IOException, DocumentException {
        return getChoiceField(false);
    }
    public PdfFormField getListField() throws IOException, DocumentException {
        return getChoiceField(true);
    }
    protected PdfFormField getChoiceField(boolean isList) throws IOException, DocumentException {
        options &= (~MULTILINE) & (~COMB);
        String uchoices[] = choices;
        if (uchoices == null)
            uchoices = new String[0];
        int topChoice = choiceSelection;
        if (topChoice >= uchoices.length)
            topChoice = uchoices.length - 1;
        if (text == null) text = "";
        if (topChoice >= 0)
            text = uchoices[topChoice];
        if (topChoice < 0)
            topChoice = 0;
        PdfFormField field = null;
        String mix[][] = null;
        if (choiceExports == null) {
            if (isList)
                field = PdfFormField.createList(writer, uchoices, topChoice);
            else
                field = PdfFormField.createCombo(writer, (options & EDIT) != 0, uchoices, topChoice);
        }
        else {
            mix = new String[uchoices.length][2];
            for (int k = 0; k < mix.length; ++k)
                mix[k][0] = mix[k][1] = uchoices[k];
            int top = Math.min(uchoices.length, choiceExports.length);
            for (int k = 0; k < top; ++k) {
                if (choiceExports[k] != null)
                    mix[k][0] = choiceExports[k];
            }
            if (isList)
                field = PdfFormField.createList(writer, mix, topChoice);
            else
                field = PdfFormField.createCombo(writer, (options & EDIT) != 0, mix, topChoice);
        }
        field.setWidget(box, PdfAnnotation.HIGHLIGHT_INVERT);
        if (rotation != 0)
            field.setMKRotation(rotation);
        if (fieldName != null) {
            field.setFieldName(fieldName);
            if (uchoices.length > 0) {
                if (mix != null) {
                    field.setValueAsString(mix[topChoice][0]);
                    field.setDefaultValueAsString(mix[topChoice][0]);
                }
                else {
                    field.setValueAsString(text);
                    field.setDefaultValueAsString(text);
                }
            }
            if ((options & READ_ONLY) != 0)
                field.setFieldFlags(PdfFormField.FF_READ_ONLY);
            if ((options & REQUIRED) != 0)
                field.setFieldFlags(PdfFormField.FF_REQUIRED);
            if ((options & DO_NOT_SPELL_CHECK) != 0)
                field.setFieldFlags(PdfFormField.FF_DONOTSPELLCHECK);
        }
        field.setBorderStyle(new PdfBorderDictionary(borderWidth, borderStyle, new PdfDashPattern(3)));
        PdfAppearance tp;
        if (isList) {
            tp = getListAppearance();
            if (topFirst > 0)
                field.put(PdfName.TI, new PdfNumber(topFirst));
        }
        else
            tp = getAppearance();
        field.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, tp);
        PdfAppearance da = (PdfAppearance)tp.getDuplicate();
        da.setFontAndSize(getRealFont(), fontSize);
        if (textColor == null)
            da.setGrayFill(0);
        else
            da.setColorFill(textColor);
        field.setDefaultAppearanceString(da);
        if (borderColor != null)
            field.setMKBorderColor(borderColor);
        if (backgroundColor != null)
            field.setMKBackgroundColor(backgroundColor);
        switch (visibility) {
            case HIDDEN:
                field.setFlags(PdfAnnotation.FLAGS_PRINT | PdfAnnotation.FLAGS_HIDDEN);
                break;
            case VISIBLE_BUT_DOES_NOT_PRINT:
                break;
            case HIDDEN_BUT_PRINTABLE:
                field.setFlags(PdfAnnotation.FLAGS_PRINT | PdfAnnotation.FLAGS_NOVIEW);
                break;
            default:
                field.setFlags(PdfAnnotation.FLAGS_PRINT);
                break;
        }
        return field;
    }
    public String getDefaultText() {
        return this.defaultText;
    }
    public void setDefaultText(String defaultText) {
        this.defaultText = defaultText;
    }
    public String[] getChoices() {
        return this.choices;
    }
    public void setChoices(String[] choices) {
        this.choices = choices;
    }
    public String[] getChoiceExports() {
        return this.choiceExports;
    }
    public void setChoiceExports(String[] choiceExports) {
        this.choiceExports = choiceExports;
    }
    public int getChoiceSelection() {
        return this.choiceSelection;
    }
    public void setChoiceSelection(int choiceSelection) {
        this.choiceSelection = choiceSelection;
    }
    int getTopFirst() {
        return topFirst;
    }
    public void setExtraMargin(float extraMarginLeft, float extraMarginTop) {
        this.extraMarginLeft = extraMarginLeft;
        this.extraMarginTop = extraMarginTop;
    }
}