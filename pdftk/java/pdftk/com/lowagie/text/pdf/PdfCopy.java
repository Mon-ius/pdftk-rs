package pdftk.com.lowagie.text.pdf;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;
import pdftk.com.lowagie.text.Document;
import pdftk.com.lowagie.text.DocumentException;
import java.util.HashSet;
public class PdfCopy extends PdfWriter {
    static class IndirectReferences {
        PdfIndirectReference theRef = null;
        boolean hasCopied = false;
        IndirectReferences(PdfIndirectReference ref) {
            theRef = ref;
            hasCopied = false;
        }
        void setCopied() { hasCopied = true; }
        boolean getCopied() { return hasCopied; }
        PdfIndirectReference getRef() { return theRef; }
    };
    protected HashMap indirects = null;
    protected HashMap indirectMap = null;
    protected int currentObjectNum = 1;
    protected PdfReader reader = null;
    protected PdfIndirectReference topPageParent = null;
    protected ArrayList pageNumbersToRefs = new ArrayList();
	protected PdfIndirectReference m_new_bookmarks = null;
	protected PdfIndirectReference m_new_extensions = null;
	protected HashSet fullFormFieldNames = null;
	protected HashSet topFormFieldNames = null;
	protected class TopFormFieldData {
		HashMap newNamesRefs = null;
		HashMap newNamesKids = null;
		HashSet allNames = null;
		public TopFormFieldData() {
			newNamesRefs= new HashMap();
			newNamesKids= new HashMap();
			allNames= new HashSet();
		}
	};
	protected HashMap topFormFieldReadersData = null;
    protected static class RefKey {
        int num = 0;
        int gen = 0;
        RefKey(int num, int gen) {
            this.num = num;
            this.gen = gen;
        }
        RefKey(PdfIndirectReference ref) {
            num = ref.getNumber();
            gen = ref.getGeneration();
        }
        RefKey(PRIndirectReference ref) {
            num = ref.getNumber();
            gen = ref.getGeneration();
        }
        public int hashCode() {
            return (gen<<16)+num;
        }
        public boolean equals(Object o) {
            RefKey other = (RefKey)o;
            return this.gen == other.gen && this.num == other.num;
        }
        public String toString() {
            return "" + num + " " + gen;
        }
    }
    public PdfCopy(Document document, OutputStream os) throws DocumentException {
        super( os);
        document.addDocListener(getPdfDocument());
        getPdfDocument().setWriter(this);
        indirectMap = new HashMap();
		fullFormFieldNames = new HashSet();
		topFormFieldNames = new HashSet();
		topFormFieldReadersData = new HashMap();
    }
    public void open() {
        super.open();
        topPageParent = getPdfIndirectReference();
        getRoot().setLinearMode(topPageParent);
    }
    public PdfImportedPage getImportedPage(PdfReader reader, int pageNumber) throws IOException {
        if (currentPdfReaderInstance != null) {
            if (currentPdfReaderInstance.getReader() != reader) {
                try {
                    currentPdfReaderInstance.getReader().close();
                    currentPdfReaderInstance.getReaderFile().close();
                }
                catch (IOException ioe) {
                }
                currentPdfReaderInstance = reader.getPdfReaderInstance(this);
            }
        }
        else {
            currentPdfReaderInstance = reader.getPdfReaderInstance(this);
        }
        return currentPdfReaderInstance.getImportedPage(pageNumber);
    }
    protected PdfIndirectReference copyIndirect(PRIndirectReference in) throws IOException, BadPdfFormatException {
        RefKey key = new RefKey(in);
        IndirectReferences iRef = (IndirectReferences)indirects.get(key);
		boolean recurse_b= true;
        PdfIndirectReference retVal;
        if (iRef != null) {
            retVal = iRef.getRef();
            if (iRef.getCopied()) {
                return retVal;
            }
        }
        else {
            retVal = body.getPdfIndirectReference();
            iRef = new IndirectReferences(retVal);
            indirects.put(key, iRef);
		}
		PdfObject in_obj= (PdfObject)PdfReader.getPdfObject( in );
		if( in_obj!= null && in_obj.isDictionary() ) {
			PdfDictionary in_dict= (PdfDictionary)in_obj;
			PdfName type= (PdfName)in_dict.get( PdfName.TYPE );
			if( type!= null && type.isName() && type.equals( PdfName.PAGE ) ) {
				PdfObject parent_obj=
					(PdfObject)in_dict.get( PdfName.PARENT );
				if( parent_obj!= null && parent_obj.isIndirect() ) {
					PRIndirectReference parent_iref= (PRIndirectReference)parent_obj;
					RefKey parent_key= new RefKey( parent_iref );
					IndirectReferences parent_ref= (IndirectReferences)indirects.get( parent_key );
					if( parent_ref== null || !parent_ref.getCopied() ) {
						recurse_b= false;
					}
				}
			}
		}
		if( recurse_b ) {
			iRef.setCopied();
			PdfObject obj = copyObject((PdfObject)PdfReader.getPdfObjectRelease(in));
			addToBody(obj, retVal);
		}
        return retVal;
    }
    protected PdfDictionary copyDictionary(PdfDictionary in)
    throws IOException, BadPdfFormatException {
        PdfDictionary out = new PdfDictionary();
        PdfName type = (PdfName)in.get(PdfName.TYPE);
        for (Iterator it = in.getKeys().iterator(); it.hasNext();) {
            PdfName key = (PdfName)it.next();
            PdfObject value = in.get(key);
            if (type != null && PdfName.PAGE.equals(type)) {
                if (key.equals(PdfName.PARENT))
                    out.put(PdfName.PARENT, topPageParent);
                else if (!key.equals(PdfName.B))
                    out.put(key, copyObject(value));
            }
            else
                out.put(key, copyObject(value));
        }
        return out;
    }
    protected PdfStream copyStream(PRStream in) throws IOException, BadPdfFormatException {
        PRStream out = new PRStream(in, null);
        for (Iterator it = in.getKeys().iterator(); it.hasNext();) {
            PdfName key = (PdfName) it.next();
            PdfObject value = in.get(key);
            out.put(key, copyObject(value));
        }
        return out;
    }
    protected PdfArray copyArray(PdfArray in) throws IOException, BadPdfFormatException {
        PdfArray out = new PdfArray();
        for (Iterator i = in.getArrayList().iterator(); i.hasNext();) {
            PdfObject value = (PdfObject)i.next();
            out.add(copyObject(value));
        }
        return out;
    }
    protected PdfObject copyObject(PdfObject in) throws IOException,BadPdfFormatException {
        switch (in.type) {
            case PdfObject.DICTIONARY:
                return copyDictionary((PdfDictionary)in);
            case PdfObject.INDIRECT:
                return copyIndirect((PRIndirectReference)in);
            case PdfObject.ARRAY:
                return copyArray((PdfArray)in);
            case PdfObject.NUMBER:
            case PdfObject.NAME:
            case PdfObject.STRING:
            case PdfObject.m_NULL:
            case PdfObject.BOOLEAN:
                return in;
            case PdfObject.STREAM:
                return copyStream((PRStream)in);
            default:
                if (in.type < 0) {
                    String lit = ((PdfLiteral)in).toString();
                    if (lit.equals("true") || lit.equals("false")) {
                        return new PdfBoolean(lit);
                    }
                    return new PdfLiteral(lit);
                }
                System.err.println("CANNOT COPY type " + in.type);
                return null;
        }
    }
    protected int setFromIPage(PdfImportedPage iPage) {
        int pageNum = iPage.getPageNumber();
        PdfReaderInstance inst = currentPdfReaderInstance = iPage.getPdfReaderInstance();
        reader = inst.getReader();
        setFromReader(reader);
        return pageNum;
    }
    protected void setFromReader(PdfReader reader) {
        this.reader = reader;
        indirects = (HashMap)indirectMap.get(reader);
        if (indirects == null) {
            indirects = new HashMap();
            indirectMap.put(reader,indirects);
            PdfDictionary catalog = reader.getCatalog();
            PRIndirectReference ref = (PRIndirectReference)catalog.get(PdfName.PAGES);
            indirects.put(new RefKey(ref), new IndirectReferences(topPageParent));
        }
    }
    public void addPage(PdfImportedPage iPage) throws IOException, BadPdfFormatException, DocumentException {
        int pageNum = setFromIPage(iPage);
        PdfDictionary thePage = reader.getPageN(pageNum);
        PRIndirectReference origRef = reader.getPageOrigRef(pageNum);
        reader.releasePage(pageNum);
        RefKey key = new RefKey(origRef);
        PdfIndirectReference pageRef = null;
        IndirectReferences iRef = (IndirectReferences)indirects.get(key);
        if (iRef != null) {
            pageRef = iRef.getRef();
        }
        else {
            pageRef = body.getPdfIndirectReference();
            iRef = new IndirectReferences(pageRef);
            indirects.put(key, iRef);
        }
        pageReferences.add(pageRef);
        ++currentPageNumber;
        if (! iRef.getCopied()) {
            iRef.setCopied();
			if( !this.topFormFieldReadersData.containsKey( reader ) ) {
				this.topFormFieldReadersData.put( reader, new TopFormFieldData() );
			}
			TopFormFieldData readerData= (TopFormFieldData)topFormFieldReadersData.get(reader);
			{
				PdfArray annots= (PdfArray)PdfReader.getPdfObject(thePage.get(PdfName.ANNOTS));
				if( annots!= null && annots.isArray() ) {
					ArrayList annots_arr= annots.getArrayList();
					for( int ii= 0; ii< annots_arr.size(); ++ii ) {
						PdfObject annot_obj= (PdfObject)annots_arr.get(ii);
						if( annot_obj!= null && annot_obj.isIndirect() ) {
							PdfIndirectReference annot_ref= (PdfIndirectReference)annot_obj;
							if( annot_ref!= null ) {
								PdfDictionary annot= (PdfDictionary)PdfReader.getPdfObject(annot_ref);
								if( annot!= null && annot.isDictionary() ) {
									PdfName subtype= (PdfName)PdfReader.getPdfObject(annot.get(PdfName.SUBTYPE));
									if( subtype!= null && subtype.isName() && subtype.equals(PdfName.WIDGET) ) {
										String full_name= "";
										String top_name= "";
										boolean is_unicode_b= false;
										PdfString tt= (PdfString)PdfReader.getPdfObject(annot.get(PdfName.T));
										if( tt!= null && tt.isString() ) {
											top_name= tt.toString();
											is_unicode_b= ( is_unicode_b || PdfString.isUnicode( tt.getBytes() ) );
										}
										PdfIndirectReference parent_ref=
											(PdfIndirectReference)annot.get(PdfName.PARENT);
										while( parent_ref!= null && parent_ref.isIndirect() )
											{
												annot_ref= parent_ref;
												annot= (PdfDictionary)PdfReader.getPdfObject(annot_ref);
												parent_ref= (PdfIndirectReference)annot.get(PdfName.PARENT);
												tt= (PdfString)PdfReader.getPdfObject(annot.get(PdfName.T));
												if( tt!= null && tt.isString() ) {
													if( top_name.length()!= 0 ) {
														full_name+= top_name;
														full_name+= ".";
													}
													top_name= tt.toString();
												}
												is_unicode_b= ( is_unicode_b || PdfString.isUnicode( tt.getBytes() ) );
											}
										if( readerData.allNames.contains( top_name ) )
											{
												this.fullFormFieldNames.add( full_name+ top_name+ "." );
											}
										else {
											if( this.fullFormFieldNames.contains( full_name+ top_name+ "." ) ) {
												int new_parent_name_ii= 1;
												String new_parent_name= Integer.toString( new_parent_name_ii );
												while( this.fullFormFieldNames.contains( full_name+ top_name+ "."+ new_parent_name+ "." ) ||
													   this.topFormFieldNames.contains( new_parent_name ) &&
													   !readerData.newNamesKids.containsKey( new_parent_name ) )
													{
														new_parent_name= Integer.toString( ++new_parent_name_ii );
													}
												PdfIndirectReference new_parent_ref= null;
												PdfArray new_parent_kids= null;
												if( readerData.newNamesKids.containsKey( new_parent_name ) ) {
													new_parent_ref= (PdfIndirectReference)
														readerData.newNamesRefs.get( new_parent_name );
													new_parent_kids= (PdfArray)
														readerData.newNamesKids.get( new_parent_name );
												}
												else {
													PdfDictionary new_parent= new PdfDictionary();
													PdfString new_parent_name_pdf= new PdfString( new_parent_name );
													if( is_unicode_b ) {
														new_parent_name_pdf= new PdfString( new_parent_name, PdfObject.TEXT_UNICODE );
													}
													new_parent_ref= reader.getPRIndirectReference( new_parent );
													new_parent.put( PdfName.T, new_parent_name_pdf );
													new_parent_kids= new PdfArray();
													PdfIndirectReference new_parent_kids_ref=
														reader.getPRIndirectReference( new_parent_kids );
													new_parent.put(PdfName.KIDS, new_parent_kids_ref);
													readerData.newNamesRefs.put( new_parent_name, new_parent_ref );
													readerData.newNamesKids.put( new_parent_name, new_parent_kids );
													readerData.allNames.add( new_parent_name );
													this.topFormFieldNames.add( new_parent_name );
												}
												annot.put( PdfName.PARENT, new_parent_ref );
												new_parent_kids.add( annot_ref );
												this.fullFormFieldNames.add( full_name+ top_name+ "."+ new_parent_name+ "." );
											}
											else {
												readerData.allNames.add( top_name );
												this.topFormFieldNames.add( top_name );
												this.fullFormFieldNames.add( full_name+ top_name+ "." );
											}
										}
									}
								}
							}
						}
					}
				}
			}
            PdfDictionary newPage = copyDictionary(thePage);
			{
				PdfArray annots= (PdfArray)PdfReader.getPdfObject(thePage.get(PdfName.ANNOTS));
				if( annots!= null && annots.isArray() ) {
					ArrayList annots_arr= annots.getArrayList();
					for( int ii= 0; ii< annots_arr.size(); ++ii ) {
						PdfObject annot_obj= (PdfObject)annots_arr.get(ii);
						if( annot_obj!= null && annot_obj.isIndirect() ) {
							PdfIndirectReference annot_ref= (PdfIndirectReference)annots_arr.get(ii);
							if( annot_ref!= null ) {
								PdfDictionary annot= (PdfDictionary)PdfReader.getPdfObject(annot_ref);
								if( annot!= null && annot.isDictionary() ) {
									PdfName subtype= (PdfName)PdfReader.getPdfObject(annot.get(PdfName.SUBTYPE));
									if( subtype!= null && subtype.isName() && subtype.equals(PdfName.WIDGET) ) {
										PdfIndirectReference parent_ref=
											(PdfIndirectReference)annot.get(PdfName.PARENT);
										while( parent_ref!= null && parent_ref.isIndirect() ) {
											annot_ref= parent_ref;
											annot= (PdfDictionary)PdfReader.getPdfObject(annot_ref);
											parent_ref= (PdfIndirectReference)annot.get(PdfName.PARENT);
										}
										RefKey annot_key= new RefKey(annot_ref);
										IndirectReferences annot_iRef= (IndirectReferences)indirects.get(annot_key);
										PdfAcroForm acroForm= this.getAcroForm();
										acroForm.addDocumentField( annot_iRef.getRef() );
									}
								}
							}
						}
					}
				}
			}
			PdfDictionary catalog= reader.getCatalog();
			if( catalog!= null && catalog.isDictionary() ) {
				PdfDictionary acroForm= (PdfDictionary)PdfReader.getPdfObject(catalog.get(PdfName.ACROFORM));
				if( acroForm!= null && acroForm.isDictionary() ) {
					PdfDictionary dr= (PdfDictionary)PdfReader.getPdfObject(acroForm.get(PdfName.DR));
					if( dr!= null && dr.isDictionary() ) {
						PdfDictionary acroForm_target= this.getAcroForm();
						PdfDictionary dr_target= (PdfDictionary)PdfReader.getPdfObject(acroForm_target.get(PdfName.DR));
						if( dr_target== null ) {
							PdfDictionary dr_copy= copyDictionary( dr );
							acroForm_target.put( PdfName.DR, dr_copy );
						}
						else {
							for( Iterator it= dr.getKeys().iterator(); it.hasNext(); ) {
								PdfName dr_key= (PdfName)it.next();
								PdfObject dr_val= (PdfObject)dr.get(dr_key);
								if( !dr_target.contains( dr_key ) ) {
									dr_target.put( dr_key, copyObject( dr_val ) );
								}
							}
						}
					}
				}
			}
            newPage.put(PdfName.PARENT, topPageParent);
            addToBody(newPage, pageRef);
        }
        getRoot().addPage(pageRef);
        pageNumbersToRefs.add(pageRef);
    }
    public PdfIndirectReference getPageReference(int page) {
        if (page < 0 || page > pageNumbersToRefs.size())
            throw new IllegalArgumentException("Invalid page number " + page);
        return (PdfIndirectReference)pageNumbersToRefs.get(page - 1);
    }
    protected PdfDictionary getCatalog( PdfIndirectReference rootObj ) throws DocumentException {
		PdfDictionary catalog= getPdfDocument().getCatalog( rootObj );
			if( m_new_bookmarks!= null ) {
				catalog.put( PdfName.OUTLINES, m_new_bookmarks );
			}
			if( m_new_extensions!= null ) {
				catalog.put( PdfName.EXTENSIONS, m_new_extensions );
			}
            return catalog;
    }
	public void setOutlines( PdfIndirectReference outlines ) {
		m_new_bookmarks= outlines;
	}
	public void setExtensions( PdfIndirectReference extensions ) {
		m_new_extensions= extensions;
	}
    public void close() {
        if (open) {
            PdfReaderInstance ri = currentPdfReaderInstance;
            getPdfDocument().close();
            super.close();
            if (ri != null) {
                try {
                    ri.getReader().close();
                    ri.getReaderFile().close();
                }
                catch (IOException ioe) {
                }
            }
        }
    }
    public PdfIndirectReference add(PdfOutline outline) { return null; }
    public void addAnnotation(PdfAnnotation annot) {  }
    PdfIndirectReference add(PdfPage page, PdfContents contents) throws PdfException { return null; }
}