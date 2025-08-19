package pdftk.com.lowagie.text;
public interface DocListener extends ElementListener {
    public void open();
    public boolean setPageSize(Rectangle pageSize);
    public boolean setMargins(float marginLeft, float marginRight, float marginTop, float marginBottom);
    public boolean setMarginMirroring(boolean marginMirroring);
    public boolean newPage() throws DocumentException;
    public void resetPageCount();
    public void setPageCount(int pageN);
	public void clearTextWrap() throws DocumentException;
    public void close();
}