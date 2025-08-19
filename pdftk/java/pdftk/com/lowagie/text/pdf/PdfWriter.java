package pdftk.com.lowagie.text.pdf;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.HashSet;
import pdftk.com.lowagie.text.DocWriter;
import pdftk.com.lowagie.text.DocumentException;
import pdftk.com.lowagie.text.ExceptionConverter;
import pdftk.com.lowagie.text.Rectangle;
public class PdfWriter extends DocWriter {
	public static final int GENERATION_MAX = 65535;
    public static class PdfBody {
        static class PdfCrossReference implements Comparable {
            private int type = 0;
            private int offset = 0;
            private int refnum = 0;
            private int generation = 0;
            PdfCrossReference(int refnum, int offset, int generation) {
                type = 0;
                this.offset = offset;
                this.refnum = refnum;
                this.generation = generation;
            }
            PdfCrossReference(int refnum, int offset) {
                type = 1;
                this.offset = offset;
                this.refnum = refnum;
                this.generation = 0;
            }
            PdfCrossReference(int type, int refnum, int offset, int generation) {
                this.type = type;
                this.offset = offset;
                this.refnum = refnum;
                this.generation = generation;
            }
            int getRefnum() {
                return refnum;
            }
            public void toPdf(OutputStream os) throws IOException {
                String s = "0000000000" + offset;
                StringBuffer off = new StringBuffer(s.substring(s.length() - 10));
                s = "00000" + generation;
                String gen = s.substring(s.length() - 5);
                if (generation == GENERATION_MAX) {
                    os.write(getISOBytes(off.append(' ').append(gen).append(" f \n").toString()));
                }
                else
                    os.write(getISOBytes(off.append(' ').append(gen).append(" n \n").toString()));
            }
            public void toPdf(int midSize, OutputStream os) throws IOException {
                os.write((byte)type);
                while (--midSize >= 0)
                    os.write((byte)((offset >>> (8 * midSize)) & 0xff));
                os.write((byte)((generation >>> 8) & 0xff));
                os.write((byte)(generation & 0xff));
            }
            public int compareTo(Object o) {
                PdfCrossReference other = (PdfCrossReference)o;
                return (refnum < other.refnum ? -1 : (refnum==other.refnum ? 0 : 1));
            }
            public boolean equals(Object obj) {
                if (obj instanceof PdfCrossReference) {
                    PdfCrossReference other = (PdfCrossReference)obj;
                    return (refnum == other.refnum);
                }
                else
                    return false;
            }
        }
        private TreeSet xrefs = null;
        private int refnum = 0;
        private int position = 0;
        private PdfWriter writer = null;
        PdfBody(PdfWriter writer) {
            xrefs = new TreeSet();
            xrefs.add(new PdfCrossReference(0, 0, GENERATION_MAX));
            position = writer.getOs().getCounter();
            refnum = 1;
            this.writer = writer;
        }
        void setRefnum(int refnum) {
            this.refnum = refnum;
        }
        private static final int OBJSINSTREAM = 200;
        private ByteBuffer index = null;
        private ByteBuffer streamObjects = null;
        private int currentObjNum = 0;
        private int numObj = 0;
        private PdfWriter.PdfBody.PdfCrossReference addToObjStm(PdfObject obj, int nObj) throws IOException {
            if (numObj >= OBJSINSTREAM)
                flushObjStm();
            if (index == null) {
                index = new ByteBuffer();
                streamObjects = new ByteBuffer();
                currentObjNum = getIndirectReferenceNumber();
                numObj = 0;
            }
            int p = streamObjects.size();
            int idx = numObj++;
            PdfEncryption enc = writer.crypto;
            writer.crypto = null;
            obj.toPdf(writer, streamObjects);
            writer.crypto = enc;
            streamObjects.append(' ');
            index.append(nObj).append(' ').append(p).append(' ');
            return new PdfWriter.PdfBody.PdfCrossReference(2, nObj, currentObjNum, idx);
        }
        private void flushObjStm() throws IOException {
            if (numObj == 0)
                return;
            int first = index.size();
            index.append(streamObjects);
            PdfStream stream = new PdfStream(index.toByteArray());
            stream.flateCompress();
            stream.put(PdfName.TYPE, PdfName.OBJSTM);
            stream.put(PdfName.N, new PdfNumber(numObj));
            stream.put(PdfName.FIRST, new PdfNumber(first));
            add(stream, currentObjNum);
            index = null;
            streamObjects = null;
            numObj = 0;
        }
        PdfIndirectObject add(PdfObject object) throws IOException {
            return add(object, getIndirectReferenceNumber());
        }
        PdfIndirectObject add(PdfObject object, boolean inObjStm) throws IOException {
            return add(object, getIndirectReferenceNumber(), inObjStm);
        }
        PdfIndirectReference getPdfIndirectReference() {
            return new PdfIndirectReference( 0, getIndirectReferenceNumber() );
        }
        int getIndirectReferenceNumber() {
            int n = refnum++;
            xrefs.add(new PdfCrossReference(n, 0, GENERATION_MAX));
            return n;
        }
        PdfIndirectObject add(PdfObject object, PdfIndirectReference ref) throws IOException {
            return add(object, ref.getNumber());
        }
        PdfIndirectObject add(PdfObject object, PdfIndirectReference ref, boolean inObjStm) throws IOException {
            return add(object, ref.getNumber(), inObjStm);
        }
        PdfIndirectObject add(PdfObject object, int refNumber) throws IOException {
            return add(object, refNumber, true);
        }
        PdfIndirectObject add(PdfObject object, int refNumber, boolean inObjStm) throws IOException {
            if (inObjStm && object.canBeInObjStm() && writer.isFullCompression()) {
                PdfCrossReference pxref = addToObjStm(object, refNumber);
                PdfIndirectObject indirect = new PdfIndirectObject(refNumber, object, writer);
                if (!xrefs.add(pxref)) {
                    xrefs.remove(pxref);
                    xrefs.add(pxref);
                }
                return indirect;
            }
            else {
                PdfIndirectObject indirect = new PdfIndirectObject(refNumber, object, writer);
				writer.getOs().write('\n');
                PdfCrossReference pxref = new PdfCrossReference(refNumber, position+ 1);
                if (!xrefs.add(pxref)) {
                    xrefs.remove(pxref);
                    xrefs.add(pxref);
                }
                indirect.writeTo(writer.getOs());
                position = writer.getOs().getCounter();
                return indirect;
            }
        }
        int offset() {
            return position;
        }
        int size() {
            return Math.max(((PdfCrossReference)xrefs.last()).getRefnum() + 1, refnum);
        }
        void writeCrossReferenceTable(OutputStream os, PdfIndirectReference root, PdfIndirectReference info, PdfIndirectReference encryption, PdfObject fileID, int prevxref) throws IOException {
            int refNumber = 0;
            if (writer.isFullCompression()) {
                flushObjStm();
                refNumber = getIndirectReferenceNumber();
                xrefs.add(new PdfCrossReference(refNumber, position));
            }
            PdfCrossReference entry = (PdfCrossReference)xrefs.first();
            int first = entry.getRefnum();
            int len = 0;
            ArrayList sections = new ArrayList();
            for (Iterator i = xrefs.iterator(); i.hasNext(); ) {
                entry = (PdfCrossReference)i.next();
                if (first + len == entry.getRefnum())
                    ++len;
                else {
                    sections.add(new Integer(first));
                    sections.add(new Integer(len));
                    first = entry.getRefnum();
                    len = 1;
                }
            }
            sections.add(new Integer(first));
            sections.add(new Integer(len));
            if (writer.isFullCompression()) {
                int mid = 4;
                int mask = 0xff000000;
                for (; mid > 1; --mid) {
                    if ((mask & position) != 0)
                        break;
                    mask >>>= 8;
                }
                ByteBuffer buf = new ByteBuffer();
                for (Iterator i = xrefs.iterator(); i.hasNext(); ) {
                    entry = (PdfCrossReference) i.next();
                    entry.toPdf(mid, buf);
                }
                PdfStream xr = new PdfStream(buf.toByteArray());
                buf = null;
                xr.flateCompress();
                xr.put(PdfName.SIZE, new PdfNumber(size()));
                xr.put(PdfName.ROOT, root);
                if (info != null) {
                    xr.put(PdfName.INFO, info);
                }
                if (encryption != null)
                    xr.put(PdfName.ENCRYPT, encryption);
                if (fileID != null)
                    xr.put(PdfName.ID, fileID);
                xr.put(PdfName.W, new PdfArray(new int[]{1, mid, 2}));
                xr.put(PdfName.TYPE, PdfName.XREF);
                PdfArray idx = new PdfArray();
                for (int k = 0; k < sections.size(); ++k)
                    idx.add(new PdfNumber(((Integer)sections.get(k)).intValue()));
                xr.put(PdfName.INDEX, idx);
                if (prevxref > 0)
                    xr.put(PdfName.PREV, new PdfNumber(prevxref));
                PdfEncryption enc = writer.crypto;
                writer.crypto = null;
                PdfIndirectObject indirect = new PdfIndirectObject(refNumber, xr, writer);
                indirect.writeTo(writer.getOs());
                writer.crypto = enc;
            }
            else {
                os.write(getISOBytes("xref\n"));
                Iterator i = xrefs.iterator();
                for (int k = 0; k < sections.size(); k += 2) {
                    first = ((Integer)sections.get(k)).intValue();
                    len = ((Integer)sections.get(k + 1)).intValue();
                    os.write(getISOBytes(String.valueOf(first)));
                    os.write(getISOBytes(" "));
                    os.write(getISOBytes(String.valueOf(len)));
                    os.write('\n');
                    while (len-- > 0) {
                        entry = (PdfCrossReference) i.next();
                        entry.toPdf(os);
                    }
                }
            }
        }
    }
    static class PdfTrailer extends PdfDictionary {
        int offset;
        PdfTrailer(int size, int offset, PdfIndirectReference root, PdfIndirectReference info, PdfIndirectReference encryption, PdfObject fileID, int prevxref) {
            this.offset = offset;
            put(PdfName.SIZE, new PdfNumber(size));
            put(PdfName.ROOT, root);
            if (info != null) {
                put(PdfName.INFO, info);
            }
            if (encryption != null)
                put(PdfName.ENCRYPT, encryption);
            if (fileID != null)
                put(PdfName.ID, fileID);
            if (prevxref > 0)
                put(PdfName.PREV, new PdfNumber(prevxref));
        }
        public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
            os.write(getISOBytes("trailer\n"));
            super.toPdf(writer, os);
            os.write(getISOBytes("\nstartxref\n"));
            os.write(getISOBytes(String.valueOf(offset)));
            os.write(getISOBytes("\n%%EOF\n"));
        }
    }
    public static final int PageLayoutSinglePage = 1;
    public static final int PageLayoutOneColumn = 2;
    public static final int PageLayoutTwoColumnLeft = 4;
    public static final int PageLayoutTwoColumnRight = 8;
	public static final int PageLayoutTwoPageLeft = 16;
	public static final int PageLayoutTwoPageRight = 32;
    public static final int PageModeUseNone = 16;
    public static final int PageModeUseOutlines = 32;
    public static final int PageModeUseThumbs = 64;
    public static final int PageModeFullScreen = 128;
    public static final int PageModeUseOC = 1 << 20;
    public static final int PageModeUseAttachments = 2048;
    public static final int HideToolbar = 256;
    public static final int HideMenubar = 512;
    public static final int HideWindowUI = 1024;
    public static final int FitWindow = 2048;
    public static final int CenterWindow = 4096;
    public static final int DisplayDocTitle = 1 << 17;
    public static final int NonFullScreenPageModeUseNone = 8192;
    public static final int NonFullScreenPageModeUseOutlines = 16384;
    public static final int NonFullScreenPageModeUseThumbs = 32768;
    public static final int NonFullScreenPageModeUseOC = 1 << 19;
    public static final int DirectionL2R = 1 << 16;
    public static final int DirectionR2L = 1 << 17;
    public static final int PrintScalingNone = 1 << 21;
    static final int ViewerPreferencesMask = 0xffff00;
    public static final int AllowPrinting = 4 + 2048;
    public static final int AllowModifyContents = 8;
    public static final int AllowCopy = 16;
    public static final int AllowModifyAnnotations = 32;
    public static final int AllowFillIn = 256;
    public static final int AllowScreenReaders = 512;
    public static final int AllowAssembly = 1024;
    public static final int AllowDegradedPrinting = 4;
    public static final boolean STRENGTH40BITS = false;
    public static final boolean STRENGTH128BITS = true;
    public static final PdfName DOCUMENT_CLOSE = PdfName.WC;
    public static final PdfName WILL_SAVE = PdfName.WS;
    public static final PdfName DID_SAVE = PdfName.DS;
    public static final PdfName WILL_PRINT = PdfName.WP;
    public static final PdfName DID_PRINT = PdfName.DP;
    public static final PdfName PAGE_OPEN = PdfName.O;
    public static final PdfName PAGE_CLOSE = PdfName.C;
    public static final int SIGNATURE_EXISTS = 1;
    public static final int SIGNATURE_APPEND_ONLY = 2;
    public static final char VERSION_1_0 = '0';
    public static final char VERSION_1_1 = '1';
    public static final char VERSION_1_2 = '2';
    public static final char VERSION_1_3 = '3';
    public static final char VERSION_1_4 = '4';
    public static final char VERSION_1_5 = '5';
    public static final char VERSION_1_6 = '6';
    public static final char VERSION_1_7 = '7';
     public static final PdfName PDF_VERSION_1_2 = new PdfName("1.2");
     public static final PdfName PDF_VERSION_1_3 = new PdfName("1.3");
     public static final PdfName PDF_VERSION_1_4 = new PdfName("1.4");
     public static final PdfName PDF_VERSION_1_5 = new PdfName("1.5");
     public static final PdfName PDF_VERSION_1_6 = new PdfName("1.6");
     public static final PdfName PDF_VERSION_1_7 = new PdfName("1.7");
    private static final int VPOINT = 7;
    protected byte[] HEADER = getISOBytes("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3");
    protected int prevxref = 0;
    private PdfPages root = null;
    protected PdfDictionary imageDictionary = new PdfDictionary();
    protected HashMap formXObjects = new HashMap();
    protected int formXObjectsCounter = 1;
    protected int fontNumber = 1;
    protected int colorNumber = 1;
    protected int patternNumber = 1;
    private PdfContentByte directContent = null;
    private PdfContentByte directContentUnder = null;
    protected HashMap documentFonts = new HashMap();
    protected HashMap documentColors = new HashMap();
    protected HashMap documentPatterns = new HashMap();
    protected HashMap documentShadings = new HashMap();
    protected HashMap documentShadingPatterns = new HashMap();
    protected ColorDetails patternColorspaceRGB = null;
    protected ColorDetails patternColorspaceGRAY = null;
    protected ColorDetails patternColorspaceCMYK = null;
    protected HashMap documentSpotPatterns = new HashMap();
    protected HashMap documentExtGState = new HashMap();
    protected HashMap documentProperties = new HashMap();
    protected HashSet documentOCG = new HashSet();
    protected ArrayList documentOCGorder = new ArrayList();
    protected PdfOCProperties OCProperties = null;
    protected PdfArray OCGRadioGroup = new PdfArray();
    protected PdfDictionary defaultColorspace = new PdfDictionary();
    protected float userunit = 0f;
    public static final int PDFXNONE = 0;
    public static final int PDFX1A2001 = 1;
    public static final int PDFX32002 = 2;
    private int pdfxConformance = PDFXNONE;
    static final int PDFXKEY_COLOR = 1;
    static final int PDFXKEY_CMYK = 2;
    static final int PDFXKEY_RGB = 3;
    static final int PDFXKEY_FONT = 4;
    static final int PDFXKEY_IMAGE = 5;
    static final int PDFXKEY_GSTATE = 6;
    static final int PDFXKEY_LAYER = 7;
    protected PdfBody body = null;
    private PdfDocument pdf = null;
    private PdfPageEvent pageEvent = null;
    protected HashMap importedPages = new HashMap();
    protected PdfReaderInstance currentPdfReaderInstance = null;
    protected ArrayList pageReferences = new ArrayList();
    protected int currentPageNumber = 1;
    protected PdfDictionary group = null;
    public static final float SPACE_CHAR_RATIO_DEFAULT = 2.5f;
    public static final float NO_SPACE_CHAR_RATIO = 10000000f;
    public static final int RUN_DIRECTION_DEFAULT = 0;
    public static final int RUN_DIRECTION_NO_BIDI = 1;
    public static final int RUN_DIRECTION_LTR = 2;
    public static final int RUN_DIRECTION_RTL = 3;
    protected int runDirection = RUN_DIRECTION_NO_BIDI;
    private float spaceCharRatio = SPACE_CHAR_RATIO_DEFAULT;
    private PdfDictionary extraCatalog = null;
    public boolean filterStreams = false;
    public boolean compressStreams = false;
    protected byte[] xmpMetadata = null;
    protected boolean fullCompression = false;
    protected boolean tagged = false;
    protected PdfObject fileID = null;
    protected PdfStructureTreeRoot structureTreeRoot = null;
    protected PdfWriter() {
    }
    protected PdfWriter(OutputStream os) {
        super(os);
    }
    PdfIndirectReference add(PdfPage page, PdfContents contents) throws PdfException {
        if (!open) {
            throw new PdfException("The document isn't open.");
        }
        PdfIndirectObject object;
        try {
            object = addToBody(contents);
        }
        catch(IOException ioe) {
            throw new ExceptionConverter(ioe);
        }
        page.add(object.getIndirectReference());
        if (group != null) {
            page.put(PdfName.GROUP, group);
            group = null;
        }
        getRoot().addPage(page);
        currentPageNumber++;
        return null;
    }
    PdfIndirectReference getImageReference(PdfName name) {
        return (PdfIndirectReference) imageDictionary.get(name);
    }
    public void open() {
        super.open();
        try {
            os.write(HEADER);
            body = new PdfBody(this);
            if (pdfxConformance == PDFX32002) {
                PdfDictionary sec = new PdfDictionary();
                sec.put(PdfName.GAMMA, new PdfArray(new float[]{2.2f,2.2f,2.2f}));
                sec.put(PdfName.MATRIX, new PdfArray(new float[]{0.4124f,0.2126f,0.0193f,0.3576f,0.7152f,0.1192f,0.1805f,0.0722f,0.9505f}));
                sec.put(PdfName.WHITEPOINT, new PdfArray(new float[]{0.9505f,1f,1.089f}));
                PdfArray arr = new PdfArray(PdfName.CALRGB);
                arr.add(sec);
                setDefaultColorspace(PdfName.DEFAULTRGB, addToBody(arr).getIndirectReference());
            }
        }
        catch(IOException ioe) {
            throw new ExceptionConverter(ioe);
        }
    }
    private static void getOCGOrder(PdfArray order, PdfLayer layer) {
        if (!layer.isOnPanel())
            return;
        if (layer.getTitle() == null)
            order.add(layer.getRef());
        ArrayList children = layer.getChildren();
        if (children == null)
            return;
        PdfArray kids = new PdfArray();
        if (layer.getTitle() != null)
            kids.add(new PdfString(layer.getTitle(), PdfObject.TEXT_UNICODE));
        for (int k = 0; k < children.size(); ++k) {
            getOCGOrder(kids, (PdfLayer)children.get(k));
        }
        if (kids.size() > 0)
            order.add(kids);
    }
    private void addASEvent(PdfName event, PdfName category) {
        PdfArray arr = new PdfArray();
        for (Iterator it = documentOCG.iterator(); it.hasNext();) {
            PdfLayer layer = (PdfLayer)it.next();
            PdfDictionary usage = (PdfDictionary)layer.get(PdfName.USAGE);
            if (usage != null && usage.get(category) != null)
                arr.add(layer.getRef());
        }
        if (arr.size() == 0)
            return;
        PdfDictionary d = (PdfDictionary)OCProperties.get(PdfName.D);
        PdfArray arras = (PdfArray)d.get(PdfName.AS);
        if (arras == null) {
            arras = new PdfArray();
            d.put(PdfName.AS, arras);
        }
        PdfDictionary as = new PdfDictionary();
        as.put(PdfName.EVENT, event);
        as.put(PdfName.CATEGORY, new PdfArray(category));
        as.put(PdfName.OCGS, arr);
        arras.add(as);
    }
    private void fillOCProperties(boolean erase) {
        if (OCProperties == null)
            OCProperties = new PdfOCProperties();
        if (erase) {
            OCProperties.remove(PdfName.OCGS);
            OCProperties.remove(PdfName.D);
        }
        if (OCProperties.get(PdfName.OCGS) == null) {
            PdfArray gr = new PdfArray();
            for (Iterator it = documentOCG.iterator(); it.hasNext();) {
                PdfLayer layer = (PdfLayer)it.next();
                gr.add(layer.getRef());
            }
            OCProperties.put(PdfName.OCGS, gr);
        }
        if (OCProperties.get(PdfName.D) != null)
            return;
        ArrayList docOrder = new ArrayList(documentOCGorder);
        for (Iterator it = docOrder.iterator(); it.hasNext();) {
            PdfLayer layer = (PdfLayer)it.next();
            if (layer.getParent() != null)
                it.remove();
        }
        PdfArray order = new PdfArray();
        for (Iterator it = docOrder.iterator(); it.hasNext();) {
            PdfLayer layer = (PdfLayer)it.next();
            getOCGOrder(order, layer);
        }
        PdfDictionary d = new PdfDictionary();
        OCProperties.put(PdfName.D, d);
        d.put(PdfName.ORDER, order);
        PdfArray gr = new PdfArray();
        for (Iterator it = documentOCG.iterator(); it.hasNext();) {
            PdfLayer layer = (PdfLayer)it.next();
            if (!layer.isOn())
                gr.add(layer.getRef());
        }
        if (gr.size() > 0)
            d.put(PdfName.OFF, gr);
        if (OCGRadioGroup.size() > 0)
            d.put(PdfName.RBGROUPS, OCGRadioGroup);
        addASEvent(PdfName.VIEW, PdfName.ZOOM);
        addASEvent(PdfName.VIEW, PdfName.VIEW);
        addASEvent(PdfName.PRINT, PdfName.PRINT);
        addASEvent(PdfName.EXPORT, PdfName.EXPORT);
        d.put(PdfName.LISTMODE, PdfName.VISIBLEPAGES);
    }
    protected PdfDictionary getCatalog(PdfIndirectReference rootObj) throws DocumentException
    {
        PdfDictionary catalog = pdf.getCatalog(rootObj);
        if (tagged) {
            try {
                getStructureTreeRoot().buildTree();
            }
            catch (Exception e) {
                throw new ExceptionConverter(e);
            }
            catalog.put(PdfName.STRUCTTREEROOT, structureTreeRoot.getReference());
            PdfDictionary mi = new PdfDictionary();
            mi.put(PdfName.MARKED, PdfBoolean.PDFTRUE);
            catalog.put(PdfName.MARKINFO, mi);
        }
        if (documentOCG.size() == 0)
            return catalog;
        fillOCProperties(false);
        catalog.put(PdfName.OCPROPERTIES, OCProperties);
        return catalog;
    }
    protected void addSharedObjectsToBody() throws IOException {
        for (Iterator it = documentFonts.values().iterator(); it.hasNext();) {
            FontDetails details = (FontDetails)it.next();
            details.writeFont(this);
        }
        for (Iterator it = formXObjects.values().iterator(); it.hasNext();) {
            Object objs[] = (Object[])it.next();
            PdfTemplate template = (PdfTemplate)objs[1];
            if (template != null && template.getIndirectReference() instanceof PRIndirectReference)
                continue;
            if (template != null && template.getType() == PdfTemplate.TYPE_TEMPLATE) {
                addToBody(template.getFormXObject(), template.getIndirectReference());
            }
        }
        for (Iterator it = importedPages.values().iterator(); it.hasNext();) {
            currentPdfReaderInstance = (PdfReaderInstance)it.next();
            currentPdfReaderInstance.writeAllPages();
        }
        currentPdfReaderInstance = null;
        for (Iterator it = documentColors.values().iterator(); it.hasNext();) {
            ColorDetails color = (ColorDetails)it.next();
            addToBody(color.getSpotColor(this), color.getIndirectReference());
        }
        for (Iterator it = documentPatterns.keySet().iterator(); it.hasNext();) {
            PdfPatternPainter pat = (PdfPatternPainter)it.next();
            addToBody(pat.getPattern(), pat.getIndirectReference());
        }
        for (Iterator it = documentShadingPatterns.keySet().iterator(); it.hasNext();) {
            PdfShadingPattern shadingPattern = (PdfShadingPattern)it.next();
            shadingPattern.addToBody();
        }
        for (Iterator it = documentShadings.keySet().iterator(); it.hasNext();) {
            PdfShading shading = (PdfShading)it.next();
            shading.addToBody();
        }
        for (Iterator it = documentExtGState.keySet().iterator(); it.hasNext();) {
            PdfDictionary gstate = (PdfDictionary)it.next();
            PdfObject obj[] = (PdfObject[])documentExtGState.get(gstate);
            addToBody(gstate, (PdfIndirectReference)obj[1]);
        }
        for (Iterator it = documentProperties.keySet().iterator(); it.hasNext();) {
            Object prop = it.next();
            PdfObject[] obj = (PdfObject[])documentProperties.get(prop);
            if (prop instanceof PdfLayerMembership){
                PdfLayerMembership layer = (PdfLayerMembership)prop;
                addToBody(layer.getPdfObject(), layer.getRef());
            }
            else if ((prop instanceof PdfDictionary) && !(prop instanceof PdfLayer)){
                addToBody((PdfDictionary)prop, (PdfIndirectReference)obj[1]);
            }
        }
        for (Iterator it = documentOCG.iterator(); it.hasNext();) {
            PdfOCG layer = (PdfOCG)it.next();
            addToBody(layer.getPdfObject(), layer.getRef());
        }
    }
    public void close() {
        if (open) {
            if ((currentPageNumber - 1) != pageReferences.size())
                throw new RuntimeException("The page " + pageReferences.size() +
                " was requested but the document has only " + (currentPageNumber - 1) + " pages.");
            pdf.close();
            try {
                addSharedObjectsToBody();
                PdfIndirectReference rootRef = getRoot().writePageTree();
                PdfDictionary catalog = getCatalog(rootRef);
                if (xmpMetadata != null) {
                	PdfStream xmp = new PdfStream(xmpMetadata);
                	xmp.put(PdfName.TYPE, PdfName.METADATA);
                	xmp.put(PdfName.SUBTYPE, PdfName.XML);
                	catalog.put(PdfName.METADATA, body.add(xmp).getIndirectReference());
                }
                PdfDictionary info = getInfo();
                if (pdfxConformance != PDFXNONE) {
                    if (info.get(PdfName.GTS_PDFXVERSION) == null) {
                        if (pdfxConformance == PDFX1A2001) {
                            info.put(PdfName.GTS_PDFXVERSION, new PdfString("PDF/X-1:2001"));
                            info.put(new PdfName("GTS_PDFXConformance"), new PdfString("PDF/X-1a:2001"));
                        }
                        else if (pdfxConformance == PDFX32002)
                            info.put(PdfName.GTS_PDFXVERSION, new PdfString("PDF/X-3:2002"));
                    }
                    if (info.get(PdfName.TITLE) == null) {
                        info.put(PdfName.TITLE, new PdfString("Pdf document"));
                    }
                    if (info.get(PdfName.CREATOR) == null) {
                        info.put(PdfName.CREATOR, new PdfString("Unknown"));
                    }
                    if (info.get(PdfName.TRAPPED) == null) {
                        info.put(PdfName.TRAPPED, new PdfName("False"));
                    }
                    getExtraCatalog();
                    if (extraCatalog.get(PdfName.OUTPUTINTENTS) == null) {
                        PdfDictionary out = new PdfDictionary(PdfName.OUTPUTINTENT);
                        out.put(PdfName.OUTPUTCONDITION, new PdfString("SWOP CGATS TR 001-1995"));
                        out.put(PdfName.OUTPUTCONDITIONIDENTIFIER, new PdfString("CGATS TR 001"));
                        out.put(PdfName.REGISTRYNAME, new PdfString("http:
                        out.put(PdfName.INFO, new PdfString(""));
                        out.put(PdfName.S, PdfName.GTS_PDFX);
                        extraCatalog.put(PdfName.OUTPUTINTENTS, new PdfArray(out));
                    }
                }
                if (extraCatalog != null) {
                    catalog.mergeDifferent(extraCatalog);
                }
                PdfIndirectObject indirectCatalog = addToBody(catalog, false);
                PdfIndirectObject infoObj = addToBody(info, false);
                PdfIndirectReference encryption = null;
                body.flushObjStm();
                if (crypto != null) {
                    PdfIndirectObject encryptionObject = addToBody(crypto.getEncryptionDictionary(), false);
                    encryption = encryptionObject.getIndirectReference();
                    fileID = crypto.getFileID();
                }
				else if (fileID == null)
                    fileID = PdfEncryption.createInfoId(PdfEncryption.createDocumentId());
                body.writeCrossReferenceTable(os, indirectCatalog.getIndirectReference(),
                    infoObj.getIndirectReference(), encryption,  fileID, prevxref);
                if (fullCompression) {
                    os.write(getISOBytes("startxref\n"));
                    os.write(getISOBytes(String.valueOf(body.offset())));
                    os.write(getISOBytes("\n%%EOF\n"));
                }
                else {
                    PdfTrailer trailer = new PdfTrailer(body.size(),
                    body.offset(),
                    indirectCatalog.getIndirectReference(),
                    infoObj.getIndirectReference(),
                    encryption,
                    fileID, prevxref);
                    trailer.toPdf(this, os);
                }
                super.close();
            }
            catch(IOException ioe) {
                throw new ExceptionConverter(ioe);
            }
            catch(DocumentException de) {
                throw new ExceptionConverter(de);
            }
        }
    }
    public float getVerticalPosition(boolean ensureNewLine) {
        return pdf.getVerticalPosition(ensureNewLine);
    }
    boolean isPaused() {
        return m_pause;
    }
    public PdfContentByte getDirectContent() {
        if (!open)
            throw new RuntimeException("The document is not open.");
		if( directContent== null )
			directContent = new PdfContentByte(this);
        return directContent;
    }
    public PdfContentByte getDirectContentUnder() {
        if (!open)
            throw new RuntimeException("The document is not open.");
		if( directContentUnder== null )
			directContentUnder = new PdfContentByte(this);
        return directContentUnder;
    }
    void resetContent() {
        getDirectContent().reset();
        getDirectContentUnder().reset();
    }
    public PdfAcroForm getAcroForm() throws DocumentException {
        return pdf.getAcroForm();
    }
    public PdfOutline getRootOutline() {
        return getDirectContent().getRootOutline();
    }
    public OutputStreamCounter getOs() {
        return os;
    }
    FontDetails addSimple(BaseFont bf) {
        if (bf.getFontType() == BaseFont.FONT_TYPE_DOCUMENT) {
            return new FontDetails(new PdfName("F" + (fontNumber++)), ((DocumentFont)bf).getIndirectReference(), bf);
        }
        FontDetails ret = (FontDetails)documentFonts.get(bf);
        if (ret == null) {
            checkPDFXConformance(this, PDFXKEY_FONT, bf);
            ret = new FontDetails(new PdfName("F" + (fontNumber++)), body.getPdfIndirectReference(), bf);
            documentFonts.put(bf, ret);
        }
        return ret;
    }
    void eliminateFontSubset(PdfDictionary fonts) {
        for (Iterator it = documentFonts.values().iterator(); it.hasNext();) {
            FontDetails ft = (FontDetails)it.next();
            if (fonts.get(ft.getFontName()) != null)
                ft.setSubset(false);
        }
    }
    PdfName getColorspaceName() {
        return new PdfName("CS" + (colorNumber++));
    }
    ColorDetails addSimple(PdfSpotColor spc) {
        ColorDetails ret = (ColorDetails)documentColors.get(spc);
        if (ret == null) {
            ret = new ColorDetails(getColorspaceName(), body.getPdfIndirectReference(), spc);
            documentColors.put(spc, ret);
        }
        return ret;
    }
    ColorDetails addSimplePatternColorspace(Color color) {
        int type = ExtendedColor.getType(color);
        if (type == ExtendedColor.TYPE_PATTERN || type == ExtendedColor.TYPE_SHADING)
            throw new RuntimeException("An uncolored tile pattern can not have another pattern or shading as color.");
        try {
            switch (type) {
                case ExtendedColor.TYPE_RGB:
                    if (patternColorspaceRGB == null) {
                        patternColorspaceRGB = new ColorDetails(getColorspaceName(), body.getPdfIndirectReference(), null);
                        PdfArray array = new PdfArray(PdfName.PATTERN);
                        array.add(PdfName.DEVICERGB);
                        addToBody(array, patternColorspaceRGB.getIndirectReference());
                    }
                    return patternColorspaceRGB;
                case ExtendedColor.TYPE_CMYK:
                    if (patternColorspaceCMYK == null) {
                        patternColorspaceCMYK = new ColorDetails(getColorspaceName(), body.getPdfIndirectReference(), null);
                        PdfArray array = new PdfArray(PdfName.PATTERN);
                        array.add(PdfName.DEVICECMYK);
                        addToBody(array, patternColorspaceCMYK.getIndirectReference());
                    }
                    return patternColorspaceCMYK;
                case ExtendedColor.TYPE_GRAY:
                    if (patternColorspaceGRAY == null) {
                        patternColorspaceGRAY = new ColorDetails(getColorspaceName(), body.getPdfIndirectReference(), null);
                        PdfArray array = new PdfArray(PdfName.PATTERN);
                        array.add(PdfName.DEVICEGRAY);
                        addToBody(array, patternColorspaceGRAY.getIndirectReference());
                    }
                    return patternColorspaceGRAY;
                case ExtendedColor.TYPE_SEPARATION: {
                    ColorDetails details = addSimple(((SpotColor)color).getPdfSpotColor());
                    ColorDetails patternDetails = (ColorDetails)documentSpotPatterns.get(details);
                    if (patternDetails == null) {
                        patternDetails = new ColorDetails(getColorspaceName(), body.getPdfIndirectReference(), null);
                        PdfArray array = new PdfArray(PdfName.PATTERN);
                        array.add(details.getIndirectReference());
                        addToBody(array, patternDetails.getIndirectReference());
                        documentSpotPatterns.put(details, patternDetails);
                    }
                    return patternDetails;
                }
                default:
                    throw new RuntimeException("Invalid color type in PdfWriter.addSimplePatternColorspace().");
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    void addSimpleShadingPattern(PdfShadingPattern shading) {
        if (!documentShadingPatterns.containsKey(shading)) {
            shading.setName(patternNumber);
            ++patternNumber;
            documentShadingPatterns.put(shading, null);
            addSimpleShading(shading.getShading());
        }
    }
    void addSimpleShading(PdfShading shading) {
        if (!documentShadings.containsKey(shading)) {
            documentShadings.put(shading, null);
            shading.setName(documentShadings.size());
        }
    }
    PdfObject[] addSimpleExtGState(PdfDictionary gstate) {
        if (!documentExtGState.containsKey(gstate)) {
            checkPDFXConformance(this, PDFXKEY_GSTATE, gstate);
            documentExtGState.put(gstate, new PdfObject[]{new PdfName("GS" + (documentExtGState.size() + 1)), getPdfIndirectReference()});
        }
        return (PdfObject[])documentExtGState.get(gstate);
    }
    void registerLayer(PdfOCG layer) {
        checkPDFXConformance(this, PDFXKEY_LAYER, null);
        if (layer instanceof PdfLayer) {
            PdfLayer la = (PdfLayer)layer;
            if (la.getTitle() == null) {
                if (!documentOCG.contains(layer)) {
                    documentOCG.add(layer);
                    documentOCGorder.add(layer);
                }
            }
            else {
                documentOCGorder.add(layer);
            }
        }
        else
            throw new IllegalArgumentException("Only PdfLayer is accepted.");
    }
    PdfObject[] addSimpleProperty(Object prop, PdfIndirectReference refi) {
        if (!documentProperties.containsKey(prop)) {
            if (prop instanceof PdfOCG)
				checkPDFXConformance(this, PDFXKEY_LAYER, null);
            documentProperties.put(prop, new PdfObject[]{new PdfName("Pr" + (documentProperties.size() + 1)), refi});
        }
        return (PdfObject[])documentProperties.get(prop);
    }
    boolean propertyExists(Object prop) {
        return documentProperties.containsKey(prop);
    }
    public PdfDocument getPdfDocument() {
		if( this.pdf== null )
			this.pdf = new PdfDocument();
        return pdf;
    }
    public PdfIndirectReference getPdfIndirectReference() {
        return body.getPdfIndirectReference();
    }
    int getIndirectReferenceNumber() {
        return body.getIndirectReferenceNumber();
    }
    PdfName addSimplePattern(PdfPatternPainter painter) {
        PdfName name = (PdfName)documentPatterns.get(painter);
        try {
            if ( name == null ) {
                name = new PdfName("P" + patternNumber);
                ++patternNumber;
                documentPatterns.put(painter, name);
            }
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
        return name;
    }
    PdfName addDirectTemplateSimple(PdfTemplate template, PdfName forcedName) {
        PdfIndirectReference ref = template.getIndirectReference();
        Object obj[] = (Object[])formXObjects.get(ref);
        PdfName name = null;
        try {
            if (obj == null) {
                if (forcedName == null) {
                    name = new PdfName("Xf" + formXObjectsCounter);
                    ++formXObjectsCounter;
                }
                else
                    name = forcedName;
                if (template.getType() == PdfTemplate.TYPE_IMPORTED)
                    template = null;
                formXObjects.put(ref, new Object[]{name, template});
            }
            else
                name = (PdfName)obj[0];
        }
        catch (Exception e) {
            throw new ExceptionConverter(e);
        }
        return name;
    }
    public void setPageEvent(PdfPageEvent pageEvent) {
        this.pageEvent = pageEvent;
    }
    public PdfPageEvent getPageEvent() {
        return pageEvent;
    }
    void addLocalDestinations(TreeMap dest) throws IOException {
        for (Iterator i = dest.keySet().iterator(); i.hasNext();) {
            String name = (String)i.next();
            Object obj[] = (Object[])dest.get(name);
            PdfDestination destination = (PdfDestination)obj[2];
            if (destination == null)
                throw new RuntimeException("The name '" + name + "' has no local destination.");
            if (obj[1] == null)
                obj[1] = getPdfIndirectReference();
            addToBody(destination, (PdfIndirectReference)obj[1]);
        }
    }
    public int getPageNumber() {
        return getPdfDocument().getPageNumber();
    }
    public void setViewerPreferences(int preferences) {
        getPdfDocument().setViewerPreferences(preferences);
    }
    public static final int INVALID_ENCRYPTION = -1;
    public static final int STANDARD_ENCRYPTION_40 = 0;
    public static final int STANDARD_ENCRYPTION_128 = 1;
    public static final int ENCRYPTION_AES_128 = 2;
    static final int ENCRYPTION_MASK = 7;
    public static final int DO_NOT_ENCRYPT_METADATA = 8;
    public static final int EMBEDDED_FILES_ONLY = 24;
    public static final int ALLOW_PRINTING = 4 + 2048;
    public static final int ALLOW_MODIFY_CONTENTS = 8;
    public static final int ALLOW_COPY = 16;
    public static final int ALLOW_MODIFY_ANNOTATIONS = 32;
    public static final int ALLOW_FILL_IN = 256;
    public static final int ALLOW_SCREENREADERS = 512;
    public static final int ALLOW_ASSEMBLY = 1024;
    public static final int ALLOW_DEGRADED_PRINTING = 4;
    protected PdfEncryption crypto = null;
    PdfEncryption getEncryption() {
        return crypto;
    }
    public void setEncryption(byte userPassword[], byte ownerPassword[], int permissions, boolean strength128Bits) throws DocumentException {
        setEncryption(userPassword, ownerPassword, permissions, strength128Bits ? STANDARD_ENCRYPTION_128 : STANDARD_ENCRYPTION_40);
    }
    public void setEncryption(byte userPassword[], byte ownerPassword[], int permissions, int encryptionType) throws DocumentException {
        if (getPdfDocument().isOpen())
            throw new DocumentException("Encryption can only be added before opening the document.");
        crypto = new PdfEncryption();
        crypto.setCryptoMode(encryptionType, 0);
        crypto.setupAllKeys(userPassword, ownerPassword, permissions);
    }
    public PdfIndirectObject addToBody(PdfObject object) throws IOException {
        PdfIndirectObject iobj = body.add(object);
        return iobj;
    }
    public PdfIndirectObject addToBody(PdfObject object, boolean inObjStm) throws IOException {
        PdfIndirectObject iobj = body.add(object, inObjStm);
        return iobj;
    }
    public PdfIndirectObject addToBody(PdfObject object, PdfIndirectReference ref) throws IOException {
        PdfIndirectObject iobj = body.add(object, ref);
        return iobj;
    }
    public PdfIndirectObject addToBody(PdfObject object, PdfIndirectReference ref, boolean inObjStm) throws IOException {
        PdfIndirectObject iobj = body.add(object, ref, inObjStm);
        return iobj;
    }
    public PdfIndirectObject addToBody(PdfObject object, int refNumber) throws IOException {
        PdfIndirectObject iobj = body.add(object, refNumber);
        return iobj;
    }
    public PdfIndirectObject addToBody(PdfObject object, int refNumber, boolean inObjStm) throws IOException {
        PdfIndirectObject iobj = body.add(object, refNumber, inObjStm);
        return iobj;
    }
    public void setOpenAction(String name) {
        getPdfDocument().setOpenAction(name);
    }
    public void setAdditionalAction(PdfName actionType, PdfAction action) throws PdfException {
        if (!(actionType.equals(DOCUMENT_CLOSE) ||
        actionType.equals(WILL_SAVE) ||
        actionType.equals(DID_SAVE) ||
        actionType.equals(WILL_PRINT) ||
        actionType.equals(DID_PRINT))) {
            throw new PdfException("Invalid additional action type: " + actionType.toString());
        }
        getPdfDocument().addAdditionalAction(actionType, action);
    }
    public void setOpenAction(PdfAction action) {
        getPdfDocument().setOpenAction(action);
    }
    public void setPageLabels(PdfPageLabels pageLabels) {
        getPdfDocument().setPageLabels(pageLabels);
    }
    RandomAccessFileOrArray getReaderFile(PdfReader reader) throws IOException {
        return currentPdfReaderInstance.getReaderFile();
    }
    protected int getNewObjectNumber(PdfReader reader, int number, int generation) {
        return currentPdfReaderInstance.getNewObjectNumber(number, generation);
    }
    public PdfImportedPage getImportedPage(PdfReader reader, int pageNumber) throws IOException {
        PdfReaderInstance inst = (PdfReaderInstance)importedPages.get(reader);
        if (inst == null) {
            inst = reader.getPdfReaderInstance(this);
            importedPages.put(reader, inst);
        }
        return inst.getImportedPage(pageNumber);
    }
    public void addJavaScript(PdfAction js) {
        getPdfDocument().addJavaScript(js);
    }
    public void addJavaScript(String code, boolean unicode) {
        addJavaScript(PdfAction.javaScript(code, this, unicode));
    }
    public void addJavaScript(String code) {
        addJavaScript(code, false);
    }
    public void setCropBoxSize(Rectangle crop) {
        getPdfDocument().setCropBoxSize(crop);
    }
    public PdfIndirectReference getPageReference(int page) {
        --page;
        if (page < 0)
            throw new IndexOutOfBoundsException("The page numbers start at 1.");
        PdfIndirectReference ref;
        if (page < pageReferences.size()) {
            ref = (PdfIndirectReference)pageReferences.get(page);
            if (ref == null) {
                ref = body.getPdfIndirectReference();
                pageReferences.set(page, ref);
            }
        }
        else {
            int empty = page - pageReferences.size();
            for (int k = 0; k < empty; ++k)
                pageReferences.add(null);
            ref = body.getPdfIndirectReference();
            pageReferences.add(ref);
        }
        return ref;
    }
    PdfIndirectReference getCurrentPage() {
        return getPageReference(currentPageNumber);
    }
    int getCurrentPageNumber() {
        return currentPageNumber;
    }
    public void addCalculationOrder(PdfFormField annot) throws DocumentException {
        getPdfDocument().addCalculationOrder(annot);
    }
    public void setSigFlags(int f) throws DocumentException {
        getPdfDocument().setSigFlags(f);
    }
    public void addAnnotation(PdfAnnotation annot) {
        getPdfDocument().addAnnotation(annot);
    }
    void addAnnotation(PdfAnnotation annot, int page) {
        addAnnotation(annot);
    }
    public void setPdfVersion(char version) {
        if (HEADER.length > VPOINT)
            HEADER[VPOINT] = (byte)version;
    }
    public int reorderPages(int order[]) throws DocumentException {
        return getRoot().reorderPages(order);
    }
    public float getSpaceCharRatio() {
        return spaceCharRatio;
    }
    public void setSpaceCharRatio(float spaceCharRatio) {
        if (spaceCharRatio < 0.001f)
            this.spaceCharRatio = 0.001f;
        else
            this.spaceCharRatio = spaceCharRatio;
    }
    public void setRunDirection(int runDirection) {
        if (runDirection < RUN_DIRECTION_NO_BIDI || runDirection > RUN_DIRECTION_RTL)
            throw new RuntimeException("Invalid run direction: " + runDirection);
        this.runDirection = runDirection;
    }
    public int getRunDirection() {
        return runDirection;
    }
    public void setDuration(int seconds) {
        getPdfDocument().setDuration(seconds);
    }
    public void setTransition(PdfTransition transition) {
        getPdfDocument().setTransition(transition);
    }
    public void freeReader(PdfReader reader) throws IOException {
        currentPdfReaderInstance = (PdfReaderInstance)importedPages.get(reader);
        if (currentPdfReaderInstance == null)
            return;
        currentPdfReaderInstance.writeAllPages();
        currentPdfReaderInstance = null;
        importedPages.remove(reader);
    }
    public void setPageAction(PdfName actionType, PdfAction action) throws PdfException {
        if (!actionType.equals(PAGE_OPEN) && !actionType.equals(PAGE_CLOSE))
            throw new PdfException("Invalid page additional action type: " + actionType.toString());
        getPdfDocument().setPageAction(actionType, action);
    }
    public int getCurrentDocumentSize() {
        return body.offset() + body.size() * 20 + 0x48;
    }
    public boolean isStrictImageSequence() {
        return getPdfDocument().isStrictImageSequence();
    }
    public void setStrictImageSequence(boolean strictImageSequence) {
        getPdfDocument().setStrictImageSequence(strictImageSequence);
    }
    public void setPageEmpty(boolean pageEmpty) {
        getPdfDocument().setPageEmpty(pageEmpty);
    }
    public PdfDictionary getInfo() {
        return getPdfDocument().getInfo();
    }
    public PdfDictionary getExtraCatalog() {
        if (extraCatalog == null)
            extraCatalog = new PdfDictionary();
        return this.extraCatalog;
    }
     public void setLinearPageMode() {
		 getRoot().setLinearMode(null);
    }
    public PdfDictionary getGroup() {
        return this.group;
    }
    public void setGroup(PdfDictionary group) {
        this.group = group;
    }
    public void setPDFXConformance(int pdfxConformance) {
        if (this.pdfxConformance == pdfxConformance)
            return;
        if (getPdfDocument().isOpen())
            throw new PdfXConformanceException("PDFX conformance can only be set before opening the document.");
        if (crypto != null)
            throw new PdfXConformanceException("A PDFX conforming document cannot be encrypted.");
        if (pdfxConformance != PDFXNONE)
            setPdfVersion(VERSION_1_3);
        this.pdfxConformance = pdfxConformance;
    }
    public int getPDFXConformance() {
        return pdfxConformance;
    }
    static void checkPDFXConformance(PdfWriter writer, int key, Object obj1) {
        if (writer == null || writer.pdfxConformance == PDFXNONE)
            return;
        int conf = writer.pdfxConformance;
        switch (key) {
            case PDFXKEY_COLOR:
                switch (conf) {
                    case PDFX1A2001:
                        if (obj1 instanceof ExtendedColor) {
                            ExtendedColor ec = (ExtendedColor)obj1;
                            switch (ec.getType()) {
                                case ExtendedColor.TYPE_CMYK:
                                case ExtendedColor.TYPE_GRAY:
                                    return;
                                case ExtendedColor.TYPE_RGB:
                                    throw new PdfXConformanceException("Colorspace RGB is not allowed.");
                                case ExtendedColor.TYPE_SEPARATION:
                                    SpotColor sc = (SpotColor)ec;
                                    checkPDFXConformance(writer, PDFXKEY_COLOR, sc.getPdfSpotColor().getAlternativeCS());
                                    break;
                                case ExtendedColor.TYPE_SHADING:
                                    ShadingColor xc = (ShadingColor)ec;
                                    checkPDFXConformance(writer, PDFXKEY_COLOR, xc.getPdfShadingPattern().getShading().getColorSpace());
                                    break;
                                case ExtendedColor.TYPE_PATTERN:
                                    PatternColor pc = (PatternColor)ec;
                                    checkPDFXConformance(writer, PDFXKEY_COLOR, pc.getPainter().getDefaultColor());
                                    break;
                            }
                        }
                        else if (obj1 instanceof Color)
                            throw new PdfXConformanceException("Colorspace RGB is not allowed.");
                        break;
                }
                break;
            case PDFXKEY_CMYK:
                break;
            case PDFXKEY_RGB:
                if (conf == PDFX1A2001)
                    throw new PdfXConformanceException("Colorspace RGB is not allowed.");
                break;
            case PDFXKEY_FONT:
                if (!((BaseFont)obj1).isEmbedded())
                    throw new PdfXConformanceException("All the fonts must be embedded.");
                break;
            case PDFXKEY_IMAGE:
				break;
            case PDFXKEY_GSTATE:
                PdfDictionary gs = (PdfDictionary)obj1;
                PdfObject obj = gs.get(PdfName.BM);
                if (obj != null && !PdfGState.BM_NORMAL.equals(obj) && !PdfGState.BM_COMPATIBLE.equals(obj))
                    throw new PdfXConformanceException("Blend mode " + obj.toString() + " not allowed.");
                obj = gs.get(PdfName.CA);
                double v = 0.0;
                if (obj != null && (v = ((PdfNumber)obj).doubleValue()) != 1.0)
                    throw new PdfXConformanceException("Transparency is not allowed: /CA = " + v);
                obj = gs.get(PdfName.ca);
                v = 0.0;
                if (obj != null && (v = ((PdfNumber)obj).doubleValue()) != 1.0)
                    throw new PdfXConformanceException("Transparency is not allowed: /ca = " + v);
                break;
            case PDFXKEY_LAYER:
                throw new PdfXConformanceException("Layers are not allowed.");
        }
    }
    public void setOutputIntents(String outputConditionIdentifier, String outputCondition, String registryName, String info, byte destOutputProfile[]) throws IOException {
        getExtraCatalog();
        PdfDictionary out = new PdfDictionary(PdfName.OUTPUTINTENT);
        if (outputCondition != null)
            out.put(PdfName.OUTPUTCONDITION, new PdfString(outputCondition, PdfObject.TEXT_UNICODE));
        if (outputConditionIdentifier != null)
            out.put(PdfName.OUTPUTCONDITIONIDENTIFIER, new PdfString(outputConditionIdentifier, PdfObject.TEXT_UNICODE));
        if (registryName != null)
            out.put(PdfName.REGISTRYNAME, new PdfString(registryName, PdfObject.TEXT_UNICODE));
        if (info != null)
            out.put(PdfName.INFO, new PdfString(registryName, PdfObject.TEXT_UNICODE));
        if (destOutputProfile != null) {
            PdfStream stream = new PdfStream(destOutputProfile);
            stream.flateCompress();
            out.put(PdfName.DESTOUTPUTPROFILE, addToBody(stream).getIndirectReference());
        }
        out.put(PdfName.S, PdfName.GTS_PDFX);
        extraCatalog.put(PdfName.OUTPUTINTENTS, new PdfArray(out));
    }
    private static String getNameString(PdfDictionary dic, PdfName key) {
        PdfObject obj = PdfReader.getPdfObject(dic.get(key));
        if (obj == null || !obj.isString())
            return null;
        return ((PdfString)obj).toUnicodeString();
    }
    public boolean setOutputIntents(PdfReader reader, boolean checkExistence) throws IOException {
        PdfDictionary catalog = reader.getCatalog();
        PdfArray outs = (PdfArray)PdfReader.getPdfObject(catalog.get(PdfName.OUTPUTINTENTS));
        if (outs == null)
            return false;
        ArrayList arr = outs.getArrayList();
        if (arr.size() == 0)
            return false;
        PdfDictionary out = (PdfDictionary)PdfReader.getPdfObject((PdfObject)arr.get(0));
        PdfObject obj = PdfReader.getPdfObject(out.get(PdfName.S));
        if (obj == null || !PdfName.GTS_PDFX.equals(obj))
            return false;
        if (checkExistence)
            return true;
        PRStream stream = (PRStream)PdfReader.getPdfObject(out.get(PdfName.DESTOUTPUTPROFILE));
        byte destProfile[] = null;
        if (stream != null) {
            destProfile = PdfReader.getStreamBytes(stream);
        }
        setOutputIntents(getNameString(out, PdfName.OUTPUTCONDITIONIDENTIFIER), getNameString(out, PdfName.OUTPUTCONDITION),
            getNameString(out, PdfName.REGISTRYNAME), getNameString(out, PdfName.INFO), destProfile);
        return true;
    }
    public void setBoxSize(String boxName, Rectangle size) {
		getPdfDocument().setBoxSize(boxName, size);
    }
    public PdfDictionary getDefaultColorspace() {
        return defaultColorspace;
    }
    public void setDefaultColorspace(PdfName key, PdfObject cs) {
        if (cs == null || cs.isNull())
            defaultColorspace.remove(key);
        defaultColorspace.put(key, cs);
    }
    public boolean isFullCompression() {
        return this.fullCompression;
    }
    public void setFullCompression() {
        this.fullCompression = true;
        setPdfVersion(VERSION_1_5);
    }
    public PdfOCProperties getOCProperties() {
        fillOCProperties(true);
        return OCProperties;
    }
    public void addOCGRadioGroup(ArrayList group) {
        PdfArray ar = new PdfArray();
        for (int k = 0; k < group.size(); ++k) {
            PdfLayer layer = (PdfLayer)group.get(k);
            if (layer.getTitle() == null)
                ar.add(layer.getRef());
        }
        if (ar.size() == 0)
            return;
        OCGRadioGroup.add(ar);
    }
	public float getUserunit() {
		return userunit;
	}
	public void setUserunit(float userunit) throws DocumentException {
		if (userunit < 1f || userunit > 75000f) throw new DocumentException("UserUnit should be a value between 1 and 75000.");
		this.userunit = userunit;
        setPdfVersion(VERSION_1_6);
	}
	public void setXmpMetadata(byte[] xmpMetadata) {
		this.xmpMetadata = xmpMetadata;
	}
    public void releaseTemplate(PdfTemplate tp) throws IOException {
        PdfIndirectReference ref = tp.getIndirectReference();
        Object[] objs = (Object[])formXObjects.get(ref);
        if (objs == null || objs[1] == null)
            return;
        PdfTemplate template = (PdfTemplate)objs[1];
        if (template.getIndirectReference() instanceof PRIndirectReference)
            return;
        if (template.getType() == PdfTemplate.TYPE_TEMPLATE) {
            addToBody(template.getFormXObject(), template.getIndirectReference());
            objs[1] = null;
        }
    }
    public void setTagged() {
        if (open)
            throw new IllegalArgumentException("Tagging must be set before opening the document.");
        tagged = true;
    }
    public boolean isTagged() {
        return tagged;
    }
    public void setFileID( PdfObject fileID ) {
		this.fileID = fileID;
    }
    public PdfObject getFileID() {
		return( this.fileID );
    }
    public PdfStructureTreeRoot getStructureTreeRoot() {
        if (tagged && structureTreeRoot == null)
            structureTreeRoot = new PdfStructureTreeRoot(this);
        return structureTreeRoot;
    }
	public PdfPages getRoot() {
		if( root== null )
			root = new PdfPages(this);
		return root;
	}
}