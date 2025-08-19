package pdftk.com.lowagie.text.pdf;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import pdftk.com.lowagie.text.Anchor;
import pdftk.com.lowagie.text.Annotation;
import pdftk.com.lowagie.text.Chunk;
import pdftk.com.lowagie.text.DocListener;
import pdftk.com.lowagie.text.Document;
import pdftk.com.lowagie.text.DocumentException;
import pdftk.com.lowagie.text.Element;
import pdftk.com.lowagie.text.ExceptionConverter;
import pdftk.com.lowagie.text.List;
import pdftk.com.lowagie.text.ListItem;
import pdftk.com.lowagie.text.Meta;
import pdftk.com.lowagie.text.Paragraph;
import pdftk.com.lowagie.text.Phrase;
import pdftk.com.lowagie.text.Rectangle;
import pdftk.com.lowagie.text.StringCompare;
class PdfDocument extends Document implements DocListener {
    public static class PdfInfo extends PdfDictionary {
        PdfInfo() {
            super();
            addProducer();
            addCreationDate();
        }
        PdfInfo(String author, String title, String subject) {
            this();
            addTitle(title);
            addSubject(subject);
            addAuthor(author);
        }
        void addTitle(String title) {
            put(PdfName.TITLE, new PdfString(title));
        }
        void addSubject(String subject) {
            put(PdfName.SUBJECT, new PdfString(subject));
        }
        void addKeywords(String keywords) {
            put(PdfName.KEYWORDS, new PdfString(keywords));
        }
        void addAuthor(String author) {
            put(PdfName.AUTHOR, new PdfString(author));
        }
        void addCreator(String creator) {
            put(PdfName.CREATOR, new PdfString(creator));
        }
        void addProducer() {
            put(PdfName.PRODUCER, new PdfString(getVersion()));
        }
        void addCreationDate() {
            PdfString date = new PdfDate();
            put(PdfName.CREATIONDATE, date);
            put(PdfName.MODDATE, date);
        }
        void addkey(String key, String value) {
            if (key.equals("Producer") || key.equals("CreationDate"))
                return;
            put(new PdfName(key), new PdfString(value));
        }
    }
    static class PdfCatalog extends PdfDictionary {
        PdfWriter writer = null;
        PdfCatalog(PdfIndirectReference pages, PdfWriter writer) {
            super(CATALOG);
            this.writer = writer;
            put(PdfName.PAGES, pages);
        }
        PdfCatalog(PdfIndirectReference pages, PdfIndirectReference outlines, PdfWriter writer) {
            super(CATALOG);
            this.writer = writer;
            put(PdfName.PAGES, pages);
            put(PdfName.PAGEMODE, PdfName.USEOUTLINES);
            put(PdfName.OUTLINES, outlines);
        }
        void addNames(TreeMap localDestinations, ArrayList documentJavaScript, PdfWriter writer) {
            if (localDestinations.size() == 0 && documentJavaScript.size() == 0)
                return;
            try {
                PdfDictionary names = new PdfDictionary();
                if (localDestinations.size() > 0) {
                    PdfArray ar = new PdfArray();
                    for (Iterator i = localDestinations.keySet().iterator(); i.hasNext();) {
                        String name = (String)i.next();
                        Object obj[] = (Object[])localDestinations.get(name);
                        PdfIndirectReference ref = (PdfIndirectReference)obj[1];
                        ar.add(new PdfString(name));
                        ar.add(ref);
                    }
                    PdfDictionary dests = new PdfDictionary();
                    dests.put(PdfName.NAMES, ar);
                    names.put(PdfName.DESTS, writer.addToBody(dests).getIndirectReference());
                }
                if (documentJavaScript.size() > 0) {
                    String s[] = new String[documentJavaScript.size()];
                    for (int k = 0; k < s.length; ++k)
                        s[k] = Integer.toHexString(k);
                    Arrays.sort(s, new StringCompare());
                    PdfArray ar = new PdfArray();
                    for (int k = 0; k < s.length; ++k) {
                        ar.add(new PdfString(s[k]));
                        ar.add((PdfIndirectReference)documentJavaScript.get(k));
                    }
                    PdfDictionary js = new PdfDictionary();
                    js.put(PdfName.NAMES, ar);
                    names.put(PdfName.JAVASCRIPT, writer.addToBody(js).getIndirectReference());
                }
                put(PdfName.NAMES, writer.addToBody(names).getIndirectReference());
            }
            catch (IOException e) {
                throw new ExceptionConverter(e);
            }
        }
        void setOpenAction(PdfAction action) {
            put(PdfName.OPENACTION, action);
        }
        void setAdditionalActions(PdfDictionary actions) {
            try {
                put(PdfName.AA, writer.addToBody(actions).getIndirectReference());
            } catch (Exception e) {
                new ExceptionConverter(e);
            }
        }
        void setPageLabels(PdfPageLabels pageLabels) {
            put(PdfName.PAGELABELS, pageLabels.getDictionary());
        }
        void setAcroForm(PdfObject fields) {
            put(PdfName.ACROFORM, fields);
        }
    }
    private PdfIndirectReference thumb = null;
    static final String hangingPunctuation = ".,;:'";
    private PdfWriter writer = null;
    private PdfInfo info = new PdfInfo();
    private boolean firstPageEvent = true;
    private boolean isParagraph = true;
    private PdfLine line = null;
    private float indentLeft = 0;
    private float indentRight = 0;
    private float listIndentLeft = 0;
    private int alignment = Element.ALIGN_LEFT;
    private PdfContentByte text = null;
    private PdfContentByte graphics = null;
    private ArrayList lines = new ArrayList();
    private float leading = 0;
    private float currentHeight = 0;
    private float indentTop = 0;
    private float indentBottom = 0;
    private boolean pageEmpty = true;
    private int textEmptySize = 0;
    protected Rectangle nextPageSize = null;
    protected HashMap thisBoxSize = new HashMap();
    protected HashMap boxSize = new HashMap();
    protected PageResources pageResources = null;
    private float imageEnd = -1;
    private float imageIndentLeft = 0;
    private float imageIndentRight = 0;
    private ArrayList annotations = null;
    private ArrayList delayedAnnotations = new ArrayList();
    private PdfAcroForm acroForm = null;
    private PdfOutline rootOutline = null;
    private PdfOutline currentOutline = null;
    private PdfAction currentAction = null;
    private TreeMap localDestinations = new TreeMap(new StringCompare());
    private ArrayList documentJavaScript = new ArrayList();
    private int viewerPreferences = 0;
    private String openActionName = null;
    private PdfAction openActionAction = null;
    private PdfDictionary additionalActions = null;
    private PdfPageLabels pageLabels = null;
    private boolean isNewpage = false;
    private float paraIndent = 0;
    protected float nextMarginLeft = 0.0f;
    protected float nextMarginRight = 0.0f;
    protected float nextMarginTop = 0.0f;
    protected float nextMarginBottom = 0.0f;
    protected int duration = -1;
    protected PdfTransition transition = null;
    protected PdfDictionary pageAA = null;
    private boolean strictImageSequence = false;
    private int lastElementType = -1;
    protected int markPoint = 0;
    public PdfDocument()  {
        super();
        addProducer();
        addCreationDate();
    }
    public void setWriter(PdfWriter writer) throws DocumentException {
	if( this.writer!= null ) {
	    throw new DocumentException("You can only add a writer to a PdfDocument once.");
	}
	this.writer= writer;
    }
    public boolean setPageSize(Rectangle pageSize) {
        if (writer != null && writer.isPaused()) {
            return false;
        }
        nextPageSize = new Rectangle(pageSize);
        return true;
    }
    public void resetPageCount() {
        if (writer != null && writer.isPaused()) {
            return;
        }
        super.resetPageCount();
    }
    public void setPageCount(int pageN) {
        if (writer != null && writer.isPaused()) {
            return;
        }
        super.setPageCount(pageN);
    }
    public boolean setMargins(float marginLeft, float marginRight, float marginTop, float marginBottom) {
        if (writer != null && writer.isPaused()) {
            return false;
        }
        nextMarginLeft = marginLeft;
        nextMarginRight = marginRight;
        nextMarginTop = marginTop;
        nextMarginBottom = marginBottom;
        return true;
    }
    protected PdfArray rotateAnnotations() throws DocumentException {
        PdfArray array = new PdfArray();
        int rotation = pageSize.getRotation() % 360;
        int currentPage = writer.getCurrentPageNumber();
        for (int k = 0; k < annotations.size(); ++k) {
            PdfAnnotation dic = (PdfAnnotation)annotations.get(k);
            int page = dic.getPlaceInPage();
            if (page > currentPage) {
                delayedAnnotations.add(dic);
                continue;
            }
            if (dic.isForm()) {
                if (!dic.isUsed()) {
                    HashMap templates = dic.getTemplates();
                    if (templates != null)
                        getAcroForm().addFieldTemplates(templates);
                }
                PdfFormField field = (PdfFormField)dic;
                if (field.getParent() == null)
                    getAcroForm().addDocumentField(field.getIndirectReference());
            }
            if (dic.isAnnotation()) {
                array.add(dic.getIndirectReference());
                if (!dic.isUsed()) {
                    PdfRectangle rect = (PdfRectangle)dic.get(PdfName.RECT);
                    if (rect != null) {
                        switch (rotation) {
                            case 90:
                                dic.put(PdfName.RECT, new PdfRectangle(
                                pageSize.top() - rect.bottom(),
                                rect.left(),
                                pageSize.top() - rect.top(),
                                rect.right()));
                                break;
                            case 180:
                                dic.put(PdfName.RECT, new PdfRectangle(
                                pageSize.right() - rect.left(),
                                pageSize.top() - rect.bottom(),
                                pageSize.right() - rect.right(),
                                pageSize.top() - rect.top()));
                                break;
                            case 270:
                                dic.put(PdfName.RECT, new PdfRectangle(
                                rect.bottom(),
                                pageSize.right() - rect.left(),
                                rect.top(),
                                pageSize.right() - rect.right()));
                                break;
                        }
                    }
                }
            }
            if (!dic.isUsed()) {
                dic.setUsed();
                try {
                    writer.addToBody(dic, dic.getIndirectReference());
                }
                catch (IOException e) {
                    throw new ExceptionConverter(e);
                }
            }
        }
        return array;
    }
    public boolean newPage() throws DocumentException {
        lastElementType = -1;
        isNewpage = true;
        if (writer.getDirectContent().size() == 0 && writer.getDirectContentUnder().size() == 0 && (pageEmpty || (writer != null && writer.isPaused()))) {
            return false;
        }
        PdfPageEvent pageEvent = writer.getPageEvent();
        if (pageEvent != null)
            pageEvent.onEndPage(writer, this);
        super.newPage();
        imageIndentLeft = 0;
        imageIndentRight = 0;
        flushLines();
        pageResources.addDefaultColorDiff(writer.getDefaultColorspace());
        PdfDictionary resources = pageResources.getResources();
        if (writer.getPDFXConformance() != PdfWriter.PDFXNONE) {
            if (thisBoxSize.containsKey("art") && thisBoxSize.containsKey("trim"))
                throw new PdfXConformanceException("Only one of ArtBox or TrimBox can exist in the page.");
            if (!thisBoxSize.containsKey("art") && !thisBoxSize.containsKey("trim")) {
                if (thisBoxSize.containsKey("crop"))
                    thisBoxSize.put("trim", thisBoxSize.get("crop"));
                else
                    thisBoxSize.put("trim", new PdfRectangle(pageSize, pageSize.getRotation()));
            }
        }
        PdfPage page;
        int rotation = pageSize.getRotation();
        page = new PdfPage(new PdfRectangle(pageSize, rotation), thisBoxSize, resources, rotation);
        if (writer.isTagged())
            page.put(PdfName.STRUCTPARENTS, new PdfNumber(writer.getCurrentPageNumber() - 1));
        if (this.transition!=null) {
            page.put(PdfName.TRANS, this.transition.getTransitionDictionary());
            transition = null;
        }
        if (this.duration>0) {
            page.put(PdfName.DUR,new PdfNumber(this.duration));
            duration = 0;
        }
        if (pageAA != null) {
            try {
                page.put(PdfName.AA, writer.addToBody(pageAA).getIndirectReference());
            }
            catch (IOException ioe) {
                throw new ExceptionConverter(ioe);
            }
            pageAA = null;
        }
        if (writer.getUserunit() > 0f) {
        	page.put(PdfName.USERUNIT, new PdfNumber(writer.getUserunit()));
        }
        if (annotations.size() > 0) {
            PdfArray array = rotateAnnotations();
            if (array.size() != 0)
                page.put(PdfName.ANNOTS, array);
        }
        if (thumb != null) {
            page.put(PdfName.THUMB, thumb);
            thumb = null;
        }
        if (!open || close) {
            throw new PdfException("The document isn't open.");
        }
        if (text.size() > textEmptySize)
            text.endText();
        else
            text = null;
        writer.add(page, new PdfContents(writer.getDirectContentUnder(), graphics, text, writer.getDirectContent(), pageSize));
        initPage();
        isNewpage = false;
        return true;
    }
    public void open() {
        if (!open) {
            super.open();
            writer.open();
            rootOutline = new PdfOutline(writer);
            currentOutline = rootOutline;
        }
        try {
            initPage();
        }
        catch(DocumentException de) {
            throw new ExceptionConverter(de);
        }
    }
    void outlineTree(PdfOutline outline) throws IOException {
        outline.setIndirectReference(writer.getPdfIndirectReference());
        if (outline.parent() != null)
            outline.put(PdfName.PARENT, outline.parent().indirectReference());
        ArrayList kids = outline.getKids();
        int size = kids.size();
        for (int k = 0; k < size; ++k)
            outlineTree((PdfOutline)kids.get(k));
        for (int k = 0; k < size; ++k) {
            if (k > 0)
                ((PdfOutline)kids.get(k)).put(PdfName.PREV, ((PdfOutline)kids.get(k - 1)).indirectReference());
            if (k < size - 1)
                ((PdfOutline)kids.get(k)).put(PdfName.NEXT, ((PdfOutline)kids.get(k + 1)).indirectReference());
        }
        if (size > 0) {
            outline.put(PdfName.FIRST, ((PdfOutline)kids.get(0)).indirectReference());
            outline.put(PdfName.LAST, ((PdfOutline)kids.get(size - 1)).indirectReference());
        }
        for (int k = 0; k < size; ++k) {
            PdfOutline kid = (PdfOutline)kids.get(k);
            writer.addToBody(kid, kid.indirectReference());
        }
    }
    void writeOutlines() throws IOException {
        if (rootOutline.getKids().size() == 0)
            return;
        outlineTree(rootOutline);
        writer.addToBody(rootOutline, rootOutline.indirectReference());
    }
    void traverseOutlineCount(PdfOutline outline) {
        ArrayList kids = outline.getKids();
        PdfOutline parent = outline.parent();
        if (kids.size() == 0) {
            if (parent != null) {
                parent.setCount(parent.getCount() + 1);
            }
        }
        else {
            for (int k = 0; k < kids.size(); ++k) {
                traverseOutlineCount((PdfOutline)kids.get(k));
            }
            if (parent != null) {
                if (outline.isOpen()) {
                    parent.setCount(outline.getCount() + parent.getCount() + 1);
                }
                else {
                    parent.setCount(parent.getCount() + 1);
                    outline.setCount(-outline.getCount());
                }
            }
        }
    }
    void calculateOutlineCount() {
        if (rootOutline.getKids().size() == 0)
            return;
        traverseOutlineCount(rootOutline);
    }
    public void close() {
        if (close) {
            return;
        }
        try {
            boolean wasImage = false;
            newPage();
            if ( false || wasImage) newPage();
            if (annotations.size() > 0)
                throw new RuntimeException(annotations.size() + " annotations had invalid placement pages.");
            PdfPageEvent pageEvent = writer.getPageEvent();
            if (pageEvent != null)
                pageEvent.onCloseDocument(writer, this);
            super.close();
            writer.addLocalDestinations(localDestinations);
            calculateOutlineCount();
            writeOutlines();
        }
        catch(Exception e) {
            throw new ExceptionConverter(e);
        }
        writer.close();
    }
    PageResources getPageResources() {
        return pageResources;
    }
    public boolean add(Element element) throws DocumentException {
        if (writer != null && writer.isPaused()) {
            return false;
        }
        try {
            switch(element.type()) {
                case Element.HEADER:
                    info.addkey(((Meta)element).name(), ((Meta)element).content());
                    break;
                case Element.TITLE:
                    info.addTitle(((Meta)element).content());
                    break;
                case Element.SUBJECT:
                    info.addSubject(((Meta)element).content());
                    break;
                case Element.KEYWORDS:
                    info.addKeywords(((Meta)element).content());
                    break;
                case Element.AUTHOR:
                    info.addAuthor(((Meta)element).content());
                    break;
                case Element.CREATOR:
                    info.addCreator(((Meta)element).content());
                    break;
                case Element.PRODUCER:
                    info.addProducer();
                    break;
                case Element.CREATIONDATE:
                    info.addCreationDate();
                    break;
                case Element.CHUNK: {
                    if (line == null) {
                        carriageReturn();
                    }
                    PdfChunk chunk = new PdfChunk((Chunk) element, currentAction);
                    {
                        PdfChunk overflow;
                        while ((overflow = line.add(chunk)) != null) {
                            carriageReturn();
                            chunk = overflow;
                        }
                    }
                    pageEmpty = false;
                    if (chunk.isAttribute(Chunk.NEWPAGE)) {
                        newPage();
                    }
                    break;
                }
                case Element.ANCHOR: {
                    Anchor anchor = (Anchor) element;
                    String url = anchor.reference();
                    leading = anchor.leading();
                    if (url != null) {
                        currentAction = new PdfAction(url);
                    }
                    element.process(this);
                    currentAction = null;
                    break;
                }
                case Element.ANNOTATION: {
                    if (line == null) {
                        carriageReturn();
                    }
                    Annotation annot = (Annotation) element;
                    PdfAnnotation an = convertAnnotation(writer, annot);
                    annotations.add(an);
                    pageEmpty = false;
                    break;
                }
                case Element.PHRASE: {
                    leading = ((Phrase) element).leading();
                    element.process(this);
                    break;
                }
                case Element.PARAGRAPH: {
                    Paragraph paragraph = (Paragraph) element;
                    float spacingBefore = paragraph.spacingBefore();
                    if (spacingBefore != 0) {
                        leading = spacingBefore;
                        carriageReturn();
                        if (!pageEmpty) {
                            Chunk space = new Chunk(" ");
                            space.process(this);
                            carriageReturn();
                        }
                    }
                    alignment = paragraph.alignment();
                    leading = paragraph.leading();
                    carriageReturn();
                    if (currentHeight + line.height() + leading > indentTop() - indentBottom()) {
                        newPage();
                    }
                    indentLeft += paragraph.indentationLeft();
                    indentRight += paragraph.indentationRight();
                    carriageReturn();
                    paraIndent += paragraph.indentationLeft();
                    PdfPageEvent pageEvent = writer.getPageEvent();
                    if (pageEvent != null && isParagraph)
                        pageEvent.onParagraph(writer, this, indentTop() - currentHeight);
                        element.process(this);
                    paraIndent -= paragraph.indentationLeft();
                    float spacingAfter = paragraph.spacingAfter();
                    if (spacingAfter != 0) {
                        leading = spacingAfter;
                        carriageReturn();
                        if (currentHeight + line.height() + leading < indentTop() - indentBottom()) {
                            Chunk space = new Chunk(" ");
                            space.process(this);
                            carriageReturn();
                        }
                        leading = paragraph.leading();
                    }
                    if (pageEvent != null && isParagraph)
                        pageEvent.onParagraphEnd(writer, this, indentTop() - currentHeight);
                    alignment = Element.ALIGN_LEFT;
                    indentLeft -= paragraph.indentationLeft();
                    indentRight -= paragraph.indentationRight();
                    carriageReturn();
                    break;
                }
                case Element.LIST: {
                    List list = (List) element;
                    listIndentLeft += list.indentationLeft();
                    indentRight += list.indentationRight();
                    element.process(this);
                    listIndentLeft -= list.indentationLeft();
                    indentRight -= list.indentationRight();
                    break;
                }
                case Element.LISTITEM: {
                    ListItem listItem = (ListItem) element;
                    float spacingBefore = listItem.spacingBefore();
                    if (spacingBefore != 0) {
                        leading = spacingBefore;
                        carriageReturn();
                        if (!pageEmpty) {
                            Chunk space = new Chunk(" ");
                            space.process(this);
                            carriageReturn();
                        }
                    }
                    alignment = listItem.alignment();
                    listIndentLeft += listItem.indentationLeft();
                    indentRight += listItem.indentationRight();
                    leading = listItem.leading();
                    carriageReturn();
                    line.setListItem(listItem);
                    element.process(this);
                    float spacingAfter = listItem.spacingAfter();
                    if (spacingAfter != 0) {
                        leading = spacingAfter;
                        carriageReturn();
                        if (currentHeight + line.height() + leading < indentTop() - indentBottom()) {
                            Chunk space = new Chunk(" ");
                            space.process(this);
                            carriageReturn();
                        }
                        leading = listItem.leading();
                    }
                    carriageReturn();
                    listIndentLeft -= listItem.indentationLeft();
                    indentRight -= listItem.indentationRight();
                    break;
                }
                case Element.RECTANGLE: {
                    Rectangle rectangle = (Rectangle) element;
                    graphics.rectangle(rectangle);
                    pageEmpty = false;
                    break;
                }
                default:
                    return false;
            }
            lastElementType = element.type();
            return true;
        }
        catch(Exception e) {
            throw new DocumentException(e);
        }
    }
    private void initPage() throws DocumentException {
        markPoint = 0;
        annotations = delayedAnnotations;
        delayedAnnotations = new ArrayList();
        pageResources = new PageResources();
        writer.resetContent();
        pageN++;
        float oldleading = leading;
        int oldAlignment = alignment;
        if (marginMirroring && (getPageNumber() & 1) == 0) {
            marginRight = nextMarginLeft;
            marginLeft = nextMarginRight;
        }
        else {
            marginLeft = nextMarginLeft;
            marginRight = nextMarginRight;
        }
        marginTop = nextMarginTop;
        marginBottom = nextMarginBottom;
        imageEnd = -1;
        imageIndentRight = 0;
        imageIndentLeft = 0;
        graphics = new PdfContentByte(writer);
        text = new PdfContentByte(writer);
        text.beginText();
        text.moveText(left(), top());
        textEmptySize = text.size();
        text.reset();
        text.beginText();
        leading = 16;
        indentBottom = 0;
        indentTop = 0;
        currentHeight = 0;
        pageSize = nextPageSize;
        thisBoxSize = new HashMap(boxSize);
        if (pageSize.backgroundColor() != null
        || pageSize.hasBorders()
        || pageSize.borderColor() != null
        || pageSize.grayFill() > 0) {
            add(pageSize);
        }
        text.moveText(left(), top());
        pageEmpty = true;
        leading = oldleading;
        alignment = oldAlignment;
        carriageReturn();
        PdfPageEvent pageEvent = writer.getPageEvent();
        if (pageEvent != null) {
            if (firstPageEvent) {
                pageEvent.onOpenDocument(writer, this);
            }
            pageEvent.onStartPage(writer, this);
        }
        firstPageEvent = false;
    }
    private void carriageReturn() throws DocumentException {
        if (lines == null) {
            lines = new ArrayList();
        }
        if (line != null) {
            if (currentHeight + line.height() + leading < indentTop() - indentBottom()) {
                if (line.size() > 0) {
                    currentHeight += line.height();
                    lines.add(line);
                    pageEmpty = false;
                }
            }
            else {
                newPage();
            }
        }
        if (imageEnd > -1 && currentHeight > imageEnd) {
            imageEnd = -1;
            imageIndentRight = 0;
            imageIndentLeft = 0;
        }
        line = new PdfLine(indentLeft(), indentRight(), alignment, leading);
    }
    private void newLine() throws DocumentException {
        lastElementType = -1;
        carriageReturn();
        if (lines != null && lines.size() > 0) {
            lines.add(line);
            currentHeight += line.height();
        }
        line = new PdfLine(indentLeft(), indentRight(), alignment, leading);
    }
    private float flushLines() throws DocumentException {
        if (lines == null) {
            return 0;
        }
        boolean newline=false;
        if (line != null && line.size() > 0) {
            lines.add(line);
            line = new PdfLine(indentLeft(), indentRight(), alignment, leading);
            newline=true;
        }
        if (lines.size() == 0) {
            return 0;
        }
        Object currentValues[] = new Object[2];
        PdfFont currentFont = null;
        float displacement = 0;
        PdfLine l;
        PdfChunk chunk;
        Float lastBaseFactor = new Float(0);
        currentValues[1] = lastBaseFactor;
        for (Iterator i = lines.iterator(); i.hasNext(); ) {
            l = (PdfLine) i.next();
            if(isNewpage && newline) {
                newline=false;
                text.moveText(l.indentLeft() - indentLeft() + listIndentLeft + paraIndent,-l.height());
            }
            else {
                text.moveText(l.indentLeft() - indentLeft() + listIndentLeft, -l.height());
            }
            if (l.listSymbol() != null) {
                chunk = l.listSymbol();
                text.moveText(- l.listIndent(), 0);
                if (chunk.font().compareTo(currentFont) != 0) {
                    currentFont = chunk.font();
                    text.setFontAndSize(currentFont.getFont(), currentFont.size());
                }
                if (chunk.color() != null) {
                    Color color = chunk.color();
                    text.setColorFill(color);
                    text.showText(chunk.toString());
                    text.resetRGBColorFill();
                }
                else {
                    text.showText(chunk.toString());
                }
                text.moveText(l.listIndent(), 0);
            }
            currentValues[0] = currentFont;
            writeLineToContent(l, text, graphics, currentValues, writer.getSpaceCharRatio());
            currentFont = (PdfFont)currentValues[0];
            displacement += l.height();
            if (indentLeft() - listIndentLeft != l.indentLeft()) {
                text.moveText(indentLeft() - l.indentLeft() - listIndentLeft, 0);
            }
        }
        lines = new ArrayList();
        return displacement;
    }
    PdfInfo getInfo() {
        return info;
    }
    PdfCatalog getCatalog(PdfIndirectReference pages) throws DocumentException {
        PdfCatalog catalog;
        if (rootOutline.getKids().size() > 0) {
            catalog = new PdfCatalog(pages, rootOutline.indirectReference(), writer);
        }
        else
            catalog = new PdfCatalog(pages, writer);
        if (openActionName != null) {
            PdfAction action = getLocalGotoAction(openActionName);
            catalog.setOpenAction(action);
        }
        else if (openActionAction != null)
            catalog.setOpenAction(openActionAction);
        if (additionalActions != null)   {
            catalog.setAdditionalActions(additionalActions);
        }
        if (pageLabels != null)
            catalog.setPageLabels(pageLabels);
        catalog.addNames(localDestinations, documentJavaScript, writer);
        if (getAcroForm().isValid()) {
            try {
                catalog.setAcroForm(writer.addToBody(acroForm).getIndirectReference());
            }
            catch (IOException e) {
                throw new ExceptionConverter(e);
            }
        }
        return catalog;
    }
    public float getVerticalPosition(boolean ensureNewLine) {
        if (ensureNewLine) {
          ensureNewLine();
        }
        return top() -  currentHeight - indentTop;
    }
    private void ensureNewLine() {
      try {
        if ((lastElementType == Element.PHRASE) ||
            (lastElementType == Element.CHUNK)) {
          newLine();
          flushLines();
        }
      } catch (DocumentException ex) {
        throw new ExceptionConverter(ex);
        }
    }
    private float indentLeft() {
        return left(indentLeft + listIndentLeft + imageIndentLeft);
    }
    private float indentRight() {
        return right(indentRight + imageIndentRight);
    }
    private float indentTop() {
        return top(indentTop);
    }
    float indentBottom() {
        return bottom(indentBottom);
    }
    void addOutline(PdfOutline outline, String name) {
        localDestination(name, outline.getPdfDestination());
    }
    public PdfAcroForm getAcroForm() throws DocumentException {
	if( acroForm== null ) {
	    if( writer!= null )
		acroForm= new PdfAcroForm( writer );
	    else
		throw new DocumentException
		    ("Accessing PdfAcroForm before initializing writer.");
	}
        return acroForm;
    }
    public PdfOutline getRootOutline() {
        return rootOutline;
    }
    void writeLineToContent(PdfLine line, PdfContentByte text, PdfContentByte graphics, Object currentValues[], float ratio)  throws DocumentException {
        PdfFont currentFont = (PdfFont)(currentValues[0]);
        float lastBaseFactor = ((Float)(currentValues[1])).floatValue();
        PdfChunk chunk;
        int numberOfSpaces;
        int lineLen;
        boolean isJustified;
        float hangingCorrection = 0;
        float hScale = 1;
        float lastHScale = Float.NaN;
        float baseWordSpacing = 0;
        float baseCharacterSpacing = 0;
        numberOfSpaces = line.numberOfSpaces();
        lineLen = line.toString().length();
        isJustified = line.hasToBeJustified() && (numberOfSpaces != 0 || lineLen > 1);
        if (isJustified) {
            if (line.isNewlineSplit() && line.widthLeft() >= (lastBaseFactor * (ratio * numberOfSpaces + lineLen - 1))) {
                if (line.isRTL()) {
                    text.moveText(line.widthLeft() - lastBaseFactor * (ratio * numberOfSpaces + lineLen - 1), 0);
                }
                baseWordSpacing = ratio * lastBaseFactor;
                baseCharacterSpacing = lastBaseFactor;
            }
            else {
                float width = line.widthLeft();
                PdfChunk last = line.getChunk(line.size() - 1);
                if (last != null) {
                    String s = last.toString();
                    char c;
                    if (s.length() > 0 && hangingPunctuation.indexOf((c = s.charAt(s.length() - 1))) >= 0) {
                        float oldWidth = width;
                        width += last.font().width(c) * 0.4f;
                        hangingCorrection = width - oldWidth;
                    }
                }
                float baseFactor = width / (ratio * numberOfSpaces + lineLen - 1);
                baseWordSpacing = ratio * baseFactor;
                baseCharacterSpacing = baseFactor;
                lastBaseFactor = baseFactor;
            }
        }
        int lastChunkStroke = line.getLastStrokeChunk();
        int chunkStrokeIdx = 0;
        float xMarker = text.getXTLM();
        float baseXMarker = xMarker;
        float yMarker = text.getYTLM();
        boolean adjustMatrix = false;
        for (Iterator j = line.iterator(); j.hasNext(); ) {
            chunk = (PdfChunk) j.next();
            Color color = chunk.color();
            hScale = 1;
            if (chunkStrokeIdx <= lastChunkStroke) {
                float width;
                if (isJustified) {
                    width = chunk.getWidthCorrected(baseCharacterSpacing, baseWordSpacing);
                }
                else
                    width = chunk.width();
                if (chunk.isStroked()) {
                    PdfChunk nextChunk = line.getChunk(chunkStrokeIdx + 1);
                    if (chunk.isAttribute(Chunk.BACKGROUND)) {
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute(Chunk.BACKGROUND))
                            subtract = 0;
                        if (nextChunk == null)
                            subtract += hangingCorrection;
                        float fontSize = chunk.font().size();
                        float ascender = chunk.font().getFont().getFontDescriptor(BaseFont.ASCENT, fontSize);
                        float descender = chunk.font().getFont().getFontDescriptor(BaseFont.DESCENT, fontSize);
                        Object bgr[] = (Object[])chunk.getAttribute(Chunk.BACKGROUND);
                        graphics.setColorFill((Color)bgr[0]);
                        float extra[] = (float[])bgr[1];
                        graphics.rectangle(xMarker - extra[0],
                            yMarker + descender - extra[1] + chunk.getTextRise(),
                            width - subtract + extra[0] + extra[2],
                            ascender - descender + extra[1] + extra[3]);
                        graphics.fill();
                        graphics.setGrayFill(0);
                    }
                    if (chunk.isAttribute(Chunk.UNDERLINE)) {
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute(Chunk.UNDERLINE))
                            subtract = 0;
                        if (nextChunk == null)
                            subtract += hangingCorrection;
                        Object unders[][] = (Object[][])chunk.getAttribute(Chunk.UNDERLINE);
                        Color scolor = null;
                        for (int k = 0; k < unders.length; ++k) {
                            Object obj[] = unders[k];
                            scolor = (Color)obj[0];
                            float ps[] = (float[])obj[1];
                            if (scolor == null)
                                scolor = color;
                            if (scolor != null)
                                graphics.setColorStroke(scolor);
                            float fsize = chunk.font().size();
                            graphics.setLineWidth(ps[0] + fsize * ps[1]);
                            float shift = ps[2] + fsize * ps[3];
                            int cap2 = (int)ps[4];
                            if (cap2 != 0)
                                graphics.setLineCap(cap2);
                            graphics.moveTo(xMarker, yMarker + shift);
                            graphics.lineTo(xMarker + width - subtract, yMarker + shift);
                            graphics.stroke();
                            if (scolor != null)
                                graphics.resetGrayStroke();
                            if (cap2 != 0)
                                graphics.setLineCap(0);
                        }
                        graphics.setLineWidth(1);
                    }
                    if (chunk.isAttribute(Chunk.ACTION)) {
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute(Chunk.ACTION))
                            subtract = 0;
                        if (nextChunk == null)
                            subtract += hangingCorrection;
                        text.addAnnotation(new PdfAnnotation(writer, xMarker, yMarker, xMarker + width - subtract, yMarker + chunk.font().size(), (PdfAction)chunk.getAttribute(Chunk.ACTION)));
                    }
                    if (chunk.isAttribute(Chunk.REMOTEGOTO)) {
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute(Chunk.REMOTEGOTO))
                            subtract = 0;
                        if (nextChunk == null)
                            subtract += hangingCorrection;
                        Object obj[] = (Object[])chunk.getAttribute(Chunk.REMOTEGOTO);
                        String filename = (String)obj[0];
                        if (obj[1] instanceof String)
                            remoteGoto(filename, (String)obj[1], xMarker, yMarker, xMarker + width - subtract, yMarker + chunk.font().size());
                        else
                            remoteGoto(filename, ((Integer)obj[1]).intValue(), xMarker, yMarker, xMarker + width - subtract, yMarker + chunk.font().size());
                    }
                    if (chunk.isAttribute(Chunk.LOCALGOTO)) {
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute(Chunk.LOCALGOTO))
                            subtract = 0;
                        if (nextChunk == null)
                            subtract += hangingCorrection;
                        localGoto((String)chunk.getAttribute(Chunk.LOCALGOTO), xMarker, yMarker, xMarker + width - subtract, yMarker + chunk.font().size());
                    }
                    if (chunk.isAttribute(Chunk.LOCALDESTINATION)) {
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute(Chunk.LOCALDESTINATION))
                            subtract = 0;
                        if (nextChunk == null)
                            subtract += hangingCorrection;
                        localDestination((String)chunk.getAttribute(Chunk.LOCALDESTINATION), new PdfDestination(PdfDestination.XYZ, xMarker, yMarker + chunk.font().size(), 0));
                    }
                    if (chunk.isAttribute(Chunk.GENERICTAG)) {
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute(Chunk.GENERICTAG))
                            subtract = 0;
                        if (nextChunk == null)
                            subtract += hangingCorrection;
                        Rectangle rect = new Rectangle(xMarker, yMarker, xMarker + width - subtract, yMarker + chunk.font().size());
                        PdfPageEvent pev = writer.getPageEvent();
                        if (pev != null)
                            pev.onGenericTag(writer, this, rect, (String)chunk.getAttribute(Chunk.GENERICTAG));
                    }
                    if (chunk.isAttribute(Chunk.PDFANNOTATION)) {
                        float subtract = lastBaseFactor;
                        if (nextChunk != null && nextChunk.isAttribute(Chunk.PDFANNOTATION))
                            subtract = 0;
                        if (nextChunk == null)
                            subtract += hangingCorrection;
                        float fontSize = chunk.font().size();
                        float ascender = chunk.font().getFont().getFontDescriptor(BaseFont.ASCENT, fontSize);
                        float descender = chunk.font().getFont().getFontDescriptor(BaseFont.DESCENT, fontSize);
                        PdfAnnotation annot = PdfFormField.shallowDuplicate((PdfAnnotation)chunk.getAttribute(Chunk.PDFANNOTATION));
                        annot.put(PdfName.RECT, new PdfRectangle(xMarker, yMarker + descender, xMarker + width - subtract, yMarker + ascender));
                        text.addAnnotation(annot);
                    }
                    float params[] = (float[])chunk.getAttribute(Chunk.SKEW);
                    Float hs = (Float)chunk.getAttribute(Chunk.HSCALE);
                    if (params != null || hs != null) {
                        float b = 0, c = 0;
                        if (params != null) {
                            b = params[0];
                            c = params[1];
                        }
                        if (hs != null)
                            hScale = hs.floatValue();
                        text.setTextMatrix(hScale, b, c, 1, xMarker, yMarker);
                    }
                }
                xMarker += width;
                ++chunkStrokeIdx;
            }
            if (chunk.font().compareTo(currentFont) != 0) {
                currentFont = chunk.font();
                text.setFontAndSize(currentFont.getFont(), currentFont.size());
            }
            float rise = 0;
            Object textRender[] = (Object[])chunk.getAttribute(Chunk.TEXTRENDERMODE);
            int tr = 0;
            float strokeWidth = 1;
            Color strokeColor = null;
            Float fr = (Float)chunk.getAttribute(Chunk.SUBSUPSCRIPT);
            if (textRender != null) {
                tr = ((Integer)textRender[0]).intValue() & 3;
                if (tr != PdfContentByte.TEXT_RENDER_MODE_FILL)
                    text.setTextRenderingMode(tr);
                if (tr == PdfContentByte.TEXT_RENDER_MODE_STROKE || tr == PdfContentByte.TEXT_RENDER_MODE_FILL_STROKE) {
                    strokeWidth = ((Float)textRender[1]).floatValue();
                    if (strokeWidth != 1)
                        text.setLineWidth(strokeWidth);
                    strokeColor = (Color)textRender[2];
                    if (strokeColor == null)
                        strokeColor = color;
                    if (strokeColor != null)
                        text.setColorStroke(strokeColor);
                }
            }
            if (fr != null)
                rise = fr.floatValue();
            if (color != null)
                text.setColorFill(color);
            if (rise != 0)
                text.setTextRise(rise);
            else if (isJustified && numberOfSpaces > 0 && chunk.isSpecialEncoding()) {
                if (hScale != lastHScale) {
                    lastHScale = hScale;
                    text.setWordSpacing(baseWordSpacing / hScale);
                    text.setCharacterSpacing(baseCharacterSpacing / hScale);
                }
                String s = chunk.toString();
                int idx = s.indexOf(' ');
                if (idx < 0)
                    text.showText(chunk.toString());
                else {
                    float spaceCorrection = - baseWordSpacing * 1000f / chunk.font.size() / hScale;
                    PdfTextArray textArray = new PdfTextArray(s.substring(0, idx));
                    int lastIdx = idx;
                    while ((idx = s.indexOf(' ', lastIdx + 1)) >= 0) {
                        textArray.add(spaceCorrection);
                        textArray.add(s.substring(lastIdx, idx));
                        lastIdx = idx;
                    }
                    textArray.add(spaceCorrection);
                    textArray.add(s.substring(lastIdx));
                    text.showText(textArray);
                }
            }
            else {
                if (isJustified && hScale != lastHScale) {
                    lastHScale = hScale;
                    text.setWordSpacing(baseWordSpacing / hScale);
                    text.setCharacterSpacing(baseCharacterSpacing / hScale);
                }
                text.showText(chunk.toString());
            }
            if (rise != 0)
                text.setTextRise(0);
            if (color != null)
                text.resetRGBColorFill();
            if (tr != PdfContentByte.TEXT_RENDER_MODE_FILL)
                text.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_FILL);
            if (strokeColor != null)
                text.resetRGBColorStroke();
            if (strokeWidth != 1)
                text.setLineWidth(1);
            if (chunk.isAttribute(Chunk.SKEW) || chunk.isAttribute(Chunk.HSCALE)) {
                adjustMatrix = true;
                text.setTextMatrix(xMarker, yMarker);
            }
        }
        if (isJustified) {
            text.setWordSpacing(0);
            text.setCharacterSpacing(0);
            if (line.isNewlineSplit())
                lastBaseFactor = 0;
        }
        if (adjustMatrix)
            text.moveText(baseXMarker - text.getXTLM(), 0);
        currentValues[0] = currentFont;
        currentValues[1] = new Float(lastBaseFactor);
    }
    void localGoto(String name, float llx, float lly, float urx, float ury) {
        PdfAction action = getLocalGotoAction(name);
        annotations.add(new PdfAnnotation(writer, llx, lly, urx, ury, action));
    }
    PdfAction getLocalGotoAction(String name) {
        PdfAction action;
        Object obj[] = (Object[])localDestinations.get(name);
        if (obj == null)
            obj = new Object[3];
        if (obj[0] == null) {
            if (obj[1] == null) {
                obj[1] = writer.getPdfIndirectReference();
            }
            action = new PdfAction((PdfIndirectReference)obj[1]);
            obj[0] = action;
            localDestinations.put(name, obj);
        }
        else {
            action = (PdfAction)obj[0];
        }
        return action;
    }
    boolean localDestination(String name, PdfDestination destination) {
        Object obj[] = (Object[])localDestinations.get(name);
        if (obj == null)
            obj = new Object[3];
        if (obj[2] != null)
            return false;
        obj[2] = destination;
        localDestinations.put(name, obj);
        destination.addPage(writer.getCurrentPage());
        return true;
    }
    void remoteGoto(String filename, String name, float llx, float lly, float urx, float ury) {
        annotations.add(new PdfAnnotation(writer, llx, lly, urx, ury, new PdfAction(filename, name)));
    }
    void remoteGoto(String filename, int page, float llx, float lly, float urx, float ury) {
        writer.addAnnotation(new PdfAnnotation(writer, llx, lly, urx, ury, new PdfAction(filename, page)));
    }
    public void setViewerPreferences(int preferences) {
        viewerPreferences |= preferences;
    }
    void setAction(PdfAction action, float llx, float lly, float urx, float ury) {
        writer.addAnnotation(new PdfAnnotation(writer, llx, lly, urx, ury, action));
    }
    void setOpenAction(String name) {
        openActionName = name;
        openActionAction = null;
    }
    void setOpenAction(PdfAction action) {
        openActionAction = action;
        openActionName = null;
    }
    void addAdditionalAction(PdfName actionType, PdfAction action)  {
        if (additionalActions == null)  {
            additionalActions = new PdfDictionary();
        }
        if (action == null)
            additionalActions.remove(actionType);
        else
            additionalActions.put(actionType, action);
        if (additionalActions.size() == 0)
            additionalActions = null;
    }
    void setPageLabels(PdfPageLabels pageLabels) {
        this.pageLabels = pageLabels;
    }
    void addJavaScript(PdfAction js) {
        if (js.get(PdfName.JS) == null)
            throw new RuntimeException("Only JavaScript actions are allowed.");
        try {
            documentJavaScript.add(writer.addToBody(js).getIndirectReference());
        }
        catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }
    void setCropBoxSize(Rectangle crop) {
        setBoxSize("crop", crop);
    }
    void setBoxSize(String boxName, Rectangle size) {
        if (size == null)
            boxSize.remove(boxName);
        else
            boxSize.put(boxName, new PdfRectangle(size));
    }
    void addCalculationOrder(PdfFormField formField) throws DocumentException {
        getAcroForm().addCalculationOrder(formField);
    }
    void setSigFlags(int f) throws DocumentException {
        getAcroForm().setSigFlags(f);
    }
    void addFormFieldRaw(PdfFormField field) {
        annotations.add(field);
        ArrayList kids = field.getKids();
        if (kids != null) {
            for (int k = 0; k < kids.size(); ++k)
                addFormFieldRaw((PdfFormField)kids.get(k));
        }
    }
    void addAnnotation(PdfAnnotation annot) {
        pageEmpty = false;
        if (annot.isForm()) {
            PdfFormField field = (PdfFormField)annot;
            if (field.getParent() == null)
                addFormFieldRaw(field);
        }
        else
            annotations.add(annot);
    }
    void setDuration(int seconds) {
        if (seconds > 0)
            this.duration=seconds;
        else
            this.duration=-1;
    }
    void setTransition(PdfTransition transition) {
        this.transition=transition;
    }
    void setPageAction(PdfName actionType, PdfAction action) {
        if (pageAA == null) {
            pageAA = new PdfDictionary();
        }
        pageAA.put(actionType, action);
    }
    boolean isStrictImageSequence() {
        return this.strictImageSequence;
    }
    void setStrictImageSequence(boolean strictImageSequence) {
        this.strictImageSequence = strictImageSequence;
    }
    void setPageEmpty(boolean pageEmpty) {
        this.pageEmpty = pageEmpty;
    }
    ArrayList getDocumentJavaScript() {
        return documentJavaScript;
    }
    public boolean setMarginMirroring(boolean MarginMirroring) {
        if (writer != null && writer.isPaused()) {
            return false;
        }
        return super.setMarginMirroring(MarginMirroring);
    }
    static PdfAnnotation convertAnnotation(PdfWriter writer, Annotation annot) throws IOException {
         switch(annot.annotationType()) {
            case Annotation.URL_NET:
                return new PdfAnnotation(writer, annot.llx(), annot.lly(), annot.urx(), annot.ury(), new PdfAction((URL) annot.attributes().get(Annotation.URL)));
            case Annotation.URL_AS_STRING:
                return new PdfAnnotation(writer, annot.llx(), annot.lly(), annot.urx(), annot.ury(), new PdfAction((String) annot.attributes().get(Annotation.FILE)));
            case Annotation.FILE_DEST:
                return new PdfAnnotation(writer, annot.llx(), annot.lly(), annot.urx(), annot.ury(), new PdfAction((String) annot.attributes().get(Annotation.FILE), (String) annot.attributes().get(Annotation.DESTINATION)));
            case Annotation.SCREEN:
                boolean sparams[] = (boolean[])annot.attributes().get(Annotation.PARAMETERS);
                String fname = (String) annot.attributes().get(Annotation.FILE);
                String mimetype = (String) annot.attributes().get(Annotation.MIMETYPE);
                PdfFileSpecification fs;
                if (sparams[0])
                    fs = PdfFileSpecification.fileEmbedded(writer, fname, fname, null);
                else
                    fs = PdfFileSpecification.fileExtern(writer, fname);
                PdfAnnotation ann = PdfAnnotation.createScreen(writer, new Rectangle(annot.llx(), annot.lly(), annot.urx(), annot.ury()),
                        fname, fs, mimetype, sparams[1]);
                return ann;
            case Annotation.FILE_PAGE:
                return new PdfAnnotation(writer, annot.llx(), annot.lly(), annot.urx(), annot.ury(), new PdfAction((String) annot.attributes().get(Annotation.FILE), ((Integer) annot.attributes().get(Annotation.PAGE)).intValue()));
            case Annotation.NAMED_DEST:
                return new PdfAnnotation(writer, annot.llx(), annot.lly(), annot.urx(), annot.ury(), new PdfAction(((Integer) annot.attributes().get(Annotation.NAMED)).intValue()));
            case Annotation.LAUNCH:
                return new PdfAnnotation(writer, annot.llx(), annot.lly(), annot.urx(), annot.ury(), new PdfAction((String) annot.attributes().get(Annotation.APPLICATION),(String) annot.attributes().get(Annotation.PARAMETERS),(String) annot.attributes().get(Annotation.OPERATION),(String) annot.attributes().get(Annotation.DEFAULTDIR)));
            default:
                PdfDocument doc = writer.getPdfDocument();
                if (doc.line == null)
                    return null;
                PdfAnnotation an = new PdfAnnotation(writer, annot.llx(doc.indentRight() - doc.line.widthLeft()), annot.lly(doc.indentTop() - doc.currentHeight), annot.urx(doc.indentRight() - doc.line.widthLeft() + 20), annot.ury(doc.indentTop() - doc.currentHeight - 20), new PdfString(annot.title()), new PdfString(annot.content()));
                return an;
        }
    }
    int getMarkPoint() {
        return markPoint;
    }
    void incMarkPoint() {
        ++markPoint;
    }
}