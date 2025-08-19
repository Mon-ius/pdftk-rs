package pdftk.com.lowagie.text.pdf;
import pdftk.com.lowagie.text.Rectangle;
import pdftk.com.lowagie.text.ExceptionConverter;
import pdftk.com.lowagie.text.Phrase;
import pdftk.com.lowagie.text.Font;
import pdftk.com.lowagie.text.Element;
import pdftk.com.lowagie.text.DocumentException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CRL;
import java.security.PrivateKey;
import java.io.OutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.InputStream;
public class PdfSignatureAppearance {
    public static final PdfName SELF_SIGNED = PdfName.ADOBE_PPKLITE;
    public static final PdfName VERISIGN_SIGNED = PdfName.VERISIGN_PPKVS;
    public static final PdfName WINCER_SIGNED = PdfName.ADOBE_PPKMS;
    private static final float topSection = 0.3f;
    private static final float margin = 2;
    private Rectangle rect;
    private Rectangle pageRect;
    private PdfTemplate app[] = new PdfTemplate[5];
    private PdfTemplate frm;
    private PdfStamperImp writer;
    private String layer2Text;
    private String reason;
    private String location;
    private Calendar signDate;
    private String provider;
    private int page = 1;
    private String fieldName;
    private PrivateKey privKey;
    private Certificate[] certChain;
    private CRL[] crlList;
    private PdfName filter;
    private boolean newField;
    private ByteBuffer sigout;
    private OutputStream originalout;
    private File tempFile;
    private PdfDictionary cryptoDictionary;
    private PdfStamper stamper;
    private boolean preClosed = false;
    private PdfSigGenericPKCS sigStandard;
    private int range[];
    private RandomAccessFile raf;
    private byte bout[];
    private int boutLen;
    private byte externalDigest[];
    private byte externalRSAdata[];
    private String digestEncryptionAlgorithm;
    private HashMap exclusionLocations;
    PdfSignatureAppearance(PdfStamperImp writer) {
        this.writer = writer;
        signDate = new GregorianCalendar();
        fieldName = getNewSigName();
    }
    public void setLayer2Text(String text) {
        layer2Text = text;
    }
    public String getLayer2Text() {
        return layer2Text;
    }
    public void setLayer4Text(String text) {
        layer4Text = text;
    }
    public String getLayer4Text() {
        return layer4Text;
    }
    public Rectangle getRect() {
        return rect;
    }
    public boolean isInvisible() {
        return (rect == null || rect.width() == 0 || rect.height() == 0);
    }
    public void setCrypto(PrivateKey privKey, Certificate[] certChain, CRL[] crlList, PdfName filter) {
        this.privKey = privKey;
        this.certChain = certChain;
        this.crlList = crlList;
        this.filter = filter;
    }
    public void setVisibleSignature(Rectangle pageRect, int page, String fieldName) {
        if (fieldName != null) {
            AcroFields af = writer.getAcroFields();
            AcroFields.Item item = af.getFieldItem(fieldName);
            if (item != null)
                throw new IllegalArgumentException("The field " + fieldName + " already exists.");
            this.fieldName = fieldName;
        }
        if (page < 1 || page > writer.reader.getNumberOfPages())
            throw new IllegalArgumentException("Invalid page number: " + page);
        this.pageRect = new Rectangle(pageRect);
        this.pageRect.normalize();
        rect = new Rectangle(this.pageRect.width(), this.pageRect.height());
        this.page = page;
        newField = true;
    }
    public void setVisibleSignature(String fieldName) {
        AcroFields af = writer.getAcroFields();
        AcroFields.Item item = af.getFieldItem(fieldName);
        if (item == null)
            throw new IllegalArgumentException("The field " + fieldName + " does not exist.");
        PdfDictionary merged = (PdfDictionary)item.merged.get(0);
        if (!PdfName.SIG.equals(PdfReader.getPdfObject(merged.get(PdfName.FT))))
            throw new IllegalArgumentException("The field " + fieldName + " is not a signature field.");
        this.fieldName = fieldName;
        PdfArray r = (PdfArray)PdfReader.getPdfObject(merged.get(PdfName.RECT));
        ArrayList ar = r.getArrayList();
        float llx = ((PdfNumber)PdfReader.getPdfObject((PdfObject)ar.get(0))).floatValue();
        float lly = ((PdfNumber)PdfReader.getPdfObject((PdfObject)ar.get(1))).floatValue();
        float urx = ((PdfNumber)PdfReader.getPdfObject((PdfObject)ar.get(2))).floatValue();
        float ury = ((PdfNumber)PdfReader.getPdfObject((PdfObject)ar.get(3))).floatValue();
        pageRect = new Rectangle(llx, lly, urx, ury);
        pageRect.normalize();
        page = ((Integer)item.page.get(0)).intValue();
        int rotation = writer.reader.getPageRotation(page);
        Rectangle pageSize = writer.reader.getPageSizeWithRotation(page);
        switch (rotation) {
            case 90:
                pageRect = new Rectangle(
                pageRect.bottom(),
                pageSize.top() - pageRect.left(),
                pageRect.top(),
                pageSize.top() - pageRect.right());
                break;
            case 180:
                pageRect = new Rectangle(
                pageSize.right() - pageRect.left(),
                pageSize.top() - pageRect.bottom(),
                pageSize.right() - pageRect.right(),
                pageSize.top() - pageRect.top());
                break;
            case 270:
                pageRect = new Rectangle(
                pageSize.right() - pageRect.bottom(),
                pageRect.left(),
                pageSize.right() - pageRect.top(),
                pageRect.right());
                break;
        }
        if (rotation != 0)
            pageRect.normalize();
        rect = new Rectangle(this.pageRect.width(), this.pageRect.height());
    }
    public PdfTemplate getLayer(int layer) {
        if (layer < 0 || layer >= app.length)
            return null;
        PdfTemplate t = app[layer];
        if (t == null) {
            t = app[layer] = new PdfTemplate(writer);
            t.setBoundingBox(rect);
            writer.addDirectTemplateSimple(t, new PdfName("n" + layer));
        }
        return t;
    }
    public PdfTemplate getTopLayer() {
        if (frm == null) {
            frm = new PdfTemplate(writer);
            frm.setBoundingBox(rect);
            writer.addDirectTemplateSimple(frm, new PdfName("FRM"));
        }
        return frm;
    }
    public PdfTemplate getAppearance() throws DocumentException, IOException {
        if (app[0] == null) {
            PdfTemplate t = app[0] = new PdfTemplate(writer);
            t.setBoundingBox(new Rectangle(100, 100));
            writer.addDirectTemplateSimple(t, new PdfName("n0"));
            t.setLiteral("% DSBlank\n");
        }
        if (app[1] == null && !acro6Layers) {
            PdfTemplate t = app[1] = new PdfTemplate(writer);
            t.setBoundingBox(new Rectangle(100, 100));
            writer.addDirectTemplateSimple(t, new PdfName("n1"));
            t.setLiteral(questionMark);
        }
        if (app[2] == null) {
            String text;
            if (layer2Text == null) {
                StringBuffer buf = new StringBuffer();
                buf.append("Digitally signed by ").append(PdfPKCS7.getSubjectFields((X509Certificate)certChain[0]).getField("CN")).append("\n");
                SimpleDateFormat sd = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
                buf.append("Date: ").append(sd.format(signDate.getTime()));
                if (reason != null)
                    buf.append("\n").append("Reason: ").append(reason);
                if (location != null)
                    buf.append("\n").append("Location: ").append(location);
                text = buf.toString();
            }
            else
                text = layer2Text;
            PdfTemplate t = app[2] = new PdfTemplate(writer);
            t.setBoundingBox(rect);
            writer.addDirectTemplateSimple(t, new PdfName("n2"));
            Font font;
            if (layer2Font == null)
                font = new Font();
            else
                font = new Font(layer2Font);
            float size = font.size();
            if (size <= 0) {
                Rectangle sr = new Rectangle(rect.width() - 2 * margin, rect.height() * (1 - topSection) - 2 * margin);
                size = fitText(font, text, sr, 12, runDirection);
            }
            ColumnText ct = new ColumnText(t);
            ct.setRunDirection(runDirection);
            ct.setSimpleColumn(new Phrase(text, font), margin, 0, rect.width() - margin, rect.height() * (1 - topSection) - margin, size, Element.ALIGN_LEFT);
            ct.go();
        }
        if (app[3] == null && !acro6Layers) {
            PdfTemplate t = app[3] = new PdfTemplate(writer);
            t.setBoundingBox(new Rectangle(100, 100));
            writer.addDirectTemplateSimple(t, new PdfName("n3"));
            t.setLiteral("% DSBlank\n");
        }
        if (app[4] == null && !acro6Layers) {
            PdfTemplate t = app[4] = new PdfTemplate(writer);
            t.setBoundingBox(new Rectangle(0, rect.height() * (1 - topSection), rect.right(), rect.top()));
            writer.addDirectTemplateSimple(t, new PdfName("n4"));
            Font font;
            if (layer2Font == null)
                font = new Font();
            else
                font = new Font(layer2Font);
            float size = font.size();
            String text = "Signature Not Verified";
            if (layer4Text != null)
                text = layer4Text;
            Rectangle sr = new Rectangle(rect.width() - 2 * margin, rect.height() * topSection - 2 * margin);
            size = fitText(font, text, sr, 15, runDirection);
            ColumnText ct = new ColumnText(t);
            ct.setRunDirection(runDirection);
            ct.setSimpleColumn(new Phrase(text, font), margin, 0, rect.width() - margin, rect.height() - margin, size, Element.ALIGN_LEFT);
            ct.go();
        }
        int rotation = writer.reader.getPageRotation(page);
        Rectangle rotated = new Rectangle(rect);
        int n = rotation;
        while (n > 0) {
            rotated = rotated.rotate();
            n -= 90;
        }
        if (frm == null) {
            frm = new PdfTemplate(writer);
            frm.setBoundingBox(rotated);
            writer.addDirectTemplateSimple(frm, new PdfName("FRM"));
            float scale = Math.min(rect.width(), rect.height()) * 0.9f;
            float x = (rect.width() - scale) / 2;
            float y = (rect.height() - scale) / 2;
            scale /= 100;
            if (rotation == 90)
                frm.concatCTM(0, 1, -1, 0, rect.height(), 0);
            else if (rotation == 180)
                frm.concatCTM(-1, 0, 0, -1, rect.width(), rect.height());
            else if (rotation == 270)
                frm.concatCTM(0, -1, 1, 0, 0, rect.width());
            frm.addTemplate(app[0], 0, 0);
            if (!acro6Layers)
                frm.addTemplate(app[1], scale, 0, 0, scale, x, y);
            frm.addTemplate(app[2], 0, 0);
            if (!acro6Layers) {
                frm.addTemplate(app[3], scale, 0, 0, scale, x, y);
                frm.addTemplate(app[4], 0, 0);
            }
        }
        PdfTemplate napp = new PdfTemplate(writer);
        napp.setBoundingBox(rotated);
        writer.addDirectTemplateSimple(napp, null);
        napp.addTemplate(frm, 0, 0);
        return napp;
    }
    public static float fitText(Font font, String text, Rectangle rect, float maxFontSize, int runDirection) {
        try {
            ColumnText ct = null;
            int status = 0;
            if (maxFontSize <= 0) {
                int cr = 0;
                int lf = 0;
                char t[] = text.toCharArray();
                for (int k = 0; k < t.length; ++k) {
                    if (t[k] == '\n')
                        ++lf;
                    else if (t[k] == '\r')
                        ++cr;
                }
                int minLines = Math.max(cr, lf) + 1;
                maxFontSize = Math.abs(rect.height()) / minLines - 0.001f;
            }
            font.setSize(maxFontSize);
            Phrase ph = new Phrase(text, font);
            ct = new ColumnText(null);
            ct.setSimpleColumn(ph, rect.left(), rect.bottom(), rect.right(), rect.top(), maxFontSize, Element.ALIGN_LEFT);
            ct.setRunDirection(runDirection);
            status = ct.go(true);
            if ((status & ColumnText.NO_MORE_TEXT) != 0)
                return maxFontSize;
            float precision = 0.1f;
            float min = 0;
            float max = maxFontSize;
            float size = maxFontSize;
            for (int k = 0; k < 50; ++k) {
                size = (min + max) / 2;
                ct = new ColumnText(null);
                font.setSize(size);
                ct.setSimpleColumn(new Phrase(text, font), rect.left(), rect.bottom(), rect.right(), rect.top(), size, Element.ALIGN_LEFT);
                ct.setRunDirection(runDirection);
                status = ct.go(true);
                if ((status & ColumnText.NO_MORE_TEXT) != 0) {
                    if (max - min < size * precision)
                        return size;
                    min = size;
                }
                else
                    max = size;
            }
            return size;
        }
        catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }
    public void setExternalDigest(byte digest[], byte RSAdata[], String digestEncryptionAlgorithm) {
        externalDigest = digest;
        externalRSAdata = RSAdata;
        this.digestEncryptionAlgorithm = digestEncryptionAlgorithm;
    }
    public String getReason() {
        return this.reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }
    public String getLocation() {
        return this.location;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public String getProvider() {
        return this.provider;
    }
    public void setProvider(String provider) {
        this.provider = provider;
    }
    public java.security.PrivateKey getPrivKey() {
        return privKey;
    }
    public java.security.cert.Certificate[] getCertChain() {
        return this.certChain;
    }
    public java.security.cert.CRL[] getCrlList() {
        return this.crlList;
    }
    public pdftk.com.lowagie.text.pdf.PdfName getFilter() {
        return filter;
    }
    public boolean isNewField() {
        return this.newField;
    }
    public int getPage() {
        return page;
    }
    public java.lang.String getFieldName() {
        return fieldName;
    }
    public pdftk.com.lowagie.text.Rectangle getPageRect() {
        return pageRect;
    }
    public java.util.Calendar getSignDate() {
        return signDate;
    }
    public void setSignDate(java.util.Calendar signDate) {
        this.signDate = signDate;
    }
    pdftk.com.lowagie.text.pdf.ByteBuffer getSigout() {
        return sigout;
    }
    void setSigout(pdftk.com.lowagie.text.pdf.ByteBuffer sigout) {
        this.sigout = sigout;
    }
    java.io.OutputStream getOriginalout() {
        return originalout;
    }
    void setOriginalout(java.io.OutputStream originalout) {
        this.originalout = originalout;
    }
    public java.io.File getTempFile() {
        return tempFile;
    }
    void setTempFile(java.io.File tempFile) {
        this.tempFile = tempFile;
    }
    public String getNewSigName() {
        AcroFields af = writer.getAcroFields();
        String name = "Signature";
        int step = 0;
        boolean found = false;
        while (!found) {
            ++step;
            String n1 = name + step;
            if (af.getFieldItem(n1) != null)
                continue;
            n1 += ".";
            found = true;
            for (Iterator it = af.getFields().keySet().iterator(); it.hasNext();) {
                String fn = (String)it.next();
                if (fn.startsWith(n1)) {
                    found = false;
                    break;
                }
            }
        }
        name += step;
        return name;
    }
    public void preClose() throws IOException, DocumentException {
        preClose(null);
    }
    public void preClose(HashMap exclusionSizes) throws IOException, DocumentException {
        if (preClosed)
            throw new DocumentException("Document already pre closed.");
        preClosed = true;
        AcroFields af = writer.getAcroFields();
        String name = getFieldName();
        boolean fieldExists = !(isInvisible() || isNewField());
        int flags = 132;
        if (fieldExists) {
            flags = 0;
            ArrayList merged = af.getFieldItem(name).merged;
            PdfObject obj = PdfReader.getPdfObjectRelease(((PdfDictionary)merged.get(0)).get(PdfName.F));
            if (obj != null && obj.isNumber())
                flags = ((PdfNumber)obj).intValue();
            af.removeField(name);
        }
        writer.setSigFlags(3);
        PdfFormField sigField = PdfFormField.createSignature(writer);
        sigField.setFieldName(name);
        PdfIndirectReference refSig = writer.getPdfIndirectReference();
        sigField.put(PdfName.V, refSig);
        sigField.setFlags(flags);
        int pagen = getPage();
        if (!isInvisible()) {
            sigField.setWidget(getPageRect(), null);
            sigField.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, getAppearance());
        }
        else
            sigField.setWidget(new Rectangle(0, 0), null);
        sigField.setPage(pagen);
        writer.addAnnotation(sigField, pagen);
        exclusionLocations = new HashMap();
        if (cryptoDictionary == null) {
            if (PdfName.ADOBE_PPKLITE.equals(getFilter()))
                sigStandard = new PdfSigGenericPKCS.PPKLite(getProvider());
            else if (PdfName.ADOBE_PPKMS.equals(getFilter()))
                sigStandard = new PdfSigGenericPKCS.PPKMS(getProvider());
            else if (PdfName.VERISIGN_PPKVS.equals(getFilter()))
                sigStandard = new PdfSigGenericPKCS.VeriSign(getProvider());
            else
                throw new IllegalArgumentException("Unknown filter: " + getFilter());
            sigStandard.setExternalDigest(externalDigest, externalRSAdata, digestEncryptionAlgorithm);
            if (getReason() != null)
                sigStandard.setReason(getReason());
            if (getLocation() != null)
                sigStandard.setLocation(getLocation());
            if (getContact() != null)
                sigStandard.setContact(getContact());
            sigStandard.put(PdfName.M, new PdfDate(getSignDate()));
            sigStandard.setSignInfo(getPrivKey(), getCertChain(), getCrlList());
            PdfString contents = (PdfString)sigStandard.get(PdfName.CONTENTS);
            PdfLiteral lit = new PdfLiteral((contents.toString().length() + (PdfName.ADOBE_PPKLITE.equals(getFilter())?0:64)) * 2 + 2);
            exclusionLocations.put(PdfName.CONTENTS, lit);
            sigStandard.put(PdfName.CONTENTS, lit);
            lit = new PdfLiteral(80);
            exclusionLocations.put(PdfName.BYTERANGE, lit);
            sigStandard.put(PdfName.BYTERANGE, lit);
            if (signatureEvent != null)
                signatureEvent.getSignatureDictionary(sigStandard);
            writer.addToBody(sigStandard, refSig, false);
        }
        else {
            PdfLiteral lit = new PdfLiteral(80);
            exclusionLocations.put(PdfName.BYTERANGE, lit);
            cryptoDictionary.put(PdfName.BYTERANGE, lit);
            for (Iterator it = exclusionSizes.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry)it.next();
                PdfName key = (PdfName)entry.getKey();
                Integer v = (Integer)entry.getValue();
                lit = new PdfLiteral(v.intValue());
                exclusionLocations.put(key, lit);
                cryptoDictionary.put(key, lit);
            }
            if (signatureEvent != null)
                signatureEvent.getSignatureDictionary(cryptoDictionary);
            writer.addToBody(cryptoDictionary, refSig, false);
        }
        writer.close(stamper.getMoreInfo());
        range = new int[exclusionLocations.size() * 2];
        int byteRangePosition = ((PdfLiteral)exclusionLocations.get(PdfName.BYTERANGE)).getPosition();
        exclusionLocations.remove(PdfName.BYTERANGE);
        int idx = 1;
        for (Iterator it = exclusionLocations.values().iterator(); it.hasNext();) {
            PdfLiteral lit = (PdfLiteral)it.next();
            int n = lit.getPosition();
            range[idx++] = n;
            range[idx++] = lit.getPosLength() + n;
        }
        Arrays.sort(range, 1, range.length - 1);
        for (int k = 3; k < range.length - 2; k += 2)
            range[k] -= range[k - 1];
        if (tempFile == null) {
            bout = sigout.getBuffer();
            boutLen = sigout.size();
            range[range.length - 1] = boutLen - range[range.length - 2];
            ByteBuffer bf = new ByteBuffer();
            bf.append('[');
            for (int k = 0; k < range.length; ++k)
                bf.append(range[k]).append(' ');
            bf.append(']');
            System.arraycopy(bf.getBuffer(), 0, bout, byteRangePosition, bf.size());
        }
        else {
            try {
                raf = new RandomAccessFile(tempFile, "rw");
                int boutLen = (int)raf.length();
                range[range.length - 1] = boutLen - range[range.length - 2];
                ByteBuffer bf = new ByteBuffer();
                bf.append('[');
                for (int k = 0; k < range.length; ++k)
                    bf.append(range[k]).append(' ');
                bf.append(']');
                raf.seek(byteRangePosition);
                raf.write(bf.getBuffer(), 0, bf.size());
            }
            catch (IOException e) {
                try{raf.close();}catch(Exception ee){}
                try{tempFile.delete();}catch(Exception ee){}
                throw e;
            }
        }
    }
    public void close(PdfDictionary update) throws IOException, DocumentException {
        try {
            if (!preClosed)
                throw new DocumentException("preClose() must be called first.");
            ByteBuffer bf = new ByteBuffer();
            for (Iterator it = update.getKeys().iterator(); it.hasNext();) {
                PdfName key = (PdfName)it.next();
                PdfObject obj = update.get(key);
                PdfLiteral lit = (PdfLiteral)exclusionLocations.get(key);
                if (lit == null)
                    throw new IllegalArgumentException("The key " + key.toString() + " didn't reserve space in preClose().");
                bf.reset();
                obj.toPdf(null, bf);
                if (bf.size() > lit.getPosLength())
                    throw new IllegalArgumentException("The key " + key.toString() + " is too big. Is " + bf.size() + ", reserved " + lit.getPosLength());
                if (tempFile == null)
                    System.arraycopy(bf.getBuffer(), 0, bout, lit.getPosition(), bf.size());
                else {
                    raf.seek(lit.getPosition());
                    raf.write(bf.getBuffer(), 0, bf.size());
                }
            }
            if (update.size() != exclusionLocations.size())
                throw new IllegalArgumentException("The update dictionary has less keys than required.");
            if (tempFile == null) {
                originalout.write(bout, 0, boutLen);
            }
            else {
                if (originalout != null) {
                    raf.seek(0);
                    int length = (int)raf.length();
                    byte buf[] = new byte[8192];
                    while (length > 0) {
                        int r = raf.read(buf, 0, Math.min(buf.length, length));
                        if (r < 0)
                            throw new EOFException("Unexpected EOF");
                        originalout.write(buf, 0, r);
                        length -= r;
                    }
                }
            }
        }
        finally {
            if (tempFile != null) {
                try{raf.close();}catch(Exception ee){}
                if (originalout != null)
                    try{tempFile.delete();}catch(Exception ee){}
            }
            if (originalout != null)
                try{originalout.close();}catch(Exception e){}
        }
    }
    public InputStream getRangeStream() {
        return new PdfSignatureAppearance.RangeStream(raf, bout, range);
    }
    public pdftk.com.lowagie.text.pdf.PdfDictionary getCryptoDictionary() {
        return cryptoDictionary;
    }
    public void setCryptoDictionary(pdftk.com.lowagie.text.pdf.PdfDictionary cryptoDictionary) {
        this.cryptoDictionary = cryptoDictionary;
    }
    public pdftk.com.lowagie.text.pdf.PdfStamper getStamper() {
        return stamper;
    }
    void setStamper(pdftk.com.lowagie.text.pdf.PdfStamper stamper) {
        this.stamper = stamper;
    }
    public boolean isPreClosed() {
        return preClosed;
    }
    public pdftk.com.lowagie.text.pdf.PdfSigGenericPKCS getSigStandard() {
        return sigStandard;
    }
    public String getContact() {
        return this.contact;
    }
    public void setContact(String contact) {
        this.contact = contact;
    }
    public Font getLayer2Font() {
        return this.layer2Font;
    }
    public void setLayer2Font(Font layer2Font) {
        this.layer2Font = layer2Font;
    }
    public boolean isAcro6Layers() {
        return this.acro6Layers;
    }
    public void setAcro6Layers(boolean acro6Layers) {
        this.acro6Layers = acro6Layers;
    }
    public void setRunDirection(int runDirection) {
        if (runDirection < PdfWriter.RUN_DIRECTION_DEFAULT || runDirection > PdfWriter.RUN_DIRECTION_RTL)
            throw new RuntimeException("Invalid run direction: " + runDirection);
        this.runDirection = runDirection;
    }
    public int getRunDirection() {
        return runDirection;
    }
    public SignatureEvent getSignatureEvent() {
        return this.signatureEvent;
    }
    public void setSignatureEvent(SignatureEvent signatureEvent) {
        this.signatureEvent = signatureEvent;
    }
    public float getImageScale() {
        return this.imageScale;
    }
    public void setImageScale(float imageScale) {
        this.imageScale = imageScale;
    }
    public static final String questionMark =
        "% DSUnknown\n" +
        "q\n" +
        "1 G\n" +
        "1 g\n" +
        "0.1 0 0 0.1 9 0 cm\n" +
        "0 J 0 j 4 M []0 d\n" +
        "1 i \n" +
        "0 g\n" +
        "313 292 m\n" +
        "313 404 325 453 432 529 c\n" +
        "478 561 504 597 504 645 c\n" +
        "504 736 440 760 391 760 c\n" +
        "286 760 271 681 265 626 c\n" +
        "265 625 l\n" +
        "100 625 l\n" +
        "100 828 253 898 381 898 c\n" +
        "451 898 679 878 679 650 c\n" +
        "679 555 628 499 538 435 c\n" +
        "488 399 467 376 467 292 c\n" +
        "313 292 l\n" +
        "h\n" +
        "308 214 170 -164 re\n" +
        "f\n" +
        "0.44 G\n" +
        "1.2 w\n" +
        "1 1 0.4 rg\n" +
        "287 318 m\n" +
        "287 430 299 479 406 555 c\n" +
        "451 587 478 623 478 671 c\n" +
        "478 762 414 786 365 786 c\n" +
        "260 786 245 707 239 652 c\n" +
        "239 651 l\n" +
        "74 651 l\n" +
        "74 854 227 924 355 924 c\n" +
        "425 924 653 904 653 676 c\n" +
        "653 581 602 525 512 461 c\n" +
        "462 425 441 402 441 318 c\n" +
        "287 318 l\n" +
        "h\n" +
        "282 240 170 -164 re\n" +
        "B\n" +
        "Q\n";
    private String contact;
    private Font layer2Font;
    private String layer4Text;
    private boolean acro6Layers;
    private int runDirection = PdfWriter.RUN_DIRECTION_NO_BIDI;
    private SignatureEvent signatureEvent;
    private float imageScale;
    private static class RangeStream extends InputStream {
        private byte b[] = new byte[1];
        private RandomAccessFile raf;
        private byte bout[];
        private int range[];
        private int rangePosition = 0;
        private RangeStream(RandomAccessFile raf, byte bout[], int range[]) {
            this.raf = raf;
            this.bout = bout;
            this.range = range;
        }
        public int read() throws IOException {
            int n = read(b);
            if (n != 1)
                return -1;
            return b[0] & 0xff;
        }
        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if ((off < 0) || (off > b.length) || (len < 0) ||
            ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }
            if (rangePosition >= range[range.length - 2] + range[range.length - 1]) {
                return -1;
            }
            for (int k = 0; k < range.length; k += 2) {
                int start = range[k];
                int end = start + range[k + 1];
                if (rangePosition < start)
                    rangePosition = start;
                if (rangePosition >= start && rangePosition < end) {
                    int lenf = Math.min(len, end - rangePosition);
                    if (raf == null)
                        System.arraycopy(bout, rangePosition, b, off, lenf);
                    else {
                        raf.seek(rangePosition);
                        raf.readFully(b, off, lenf);
                    }
                    rangePosition += lenf;
                    return lenf;
                }
            }
            return -1;
        }
    }
    public interface SignatureEvent {
        public void getSignatureDictionary(PdfDictionary sig);
    }
}