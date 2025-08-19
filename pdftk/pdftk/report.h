void
ReportAcroFormFields( ostream& ofs,
											itext::PdfReader* reader_p,
											bool utf8_b );
void
ReportAnnots( ostream& ofs,
							itext::PdfReader* reader_p,
							bool utf8_b );
void
ReportOnPdf( ostream& ofs,
						 itext::PdfReader* reader_p,
						 bool utf8_b );
bool
UpdateInfo( itext::PdfReader* reader_p,
						istream& ifs,
						bool utf8_b );
class PdfBookmark {
public:
	static const string m_prefix;
	static const string m_begin_mark;
	static const string m_title_label;
	static const string m_level_label;
	static const string m_page_number_label;
	string m_title;
	int m_level;
	int m_page_num;
	PdfBookmark();
	bool valid();
};
ostream& operator<<( ostream& ss, const PdfBookmark& bb );
int
ReadOutlines( vector<PdfBookmark>& bookmark_data,
							itext::PdfDictionary* outline_p,
							int level,
							itext::PdfReader* reader_p,
							bool utf8_b );
int
BuildBookmarks( itext::PdfWriter* writer_p,
								vector<PdfBookmark>::const_iterator& it,
								vector<PdfBookmark>::const_iterator it_end,
								itext::PdfDictionary* parent_p,
								itext::PdfIndirectReference* parent_ref_p,
								itext::PdfDictionary* after_child_p,
								itext::PdfIndirectReference* after_child_ref_p,
								itext::PdfDictionary*& final_child_p,
								itext::PdfIndirectReference*& final_child_ref_p,
								int parent_level,
								int& num_bookmarks_total,
								int page_num_offset,
								int level_offset,
								bool utf8_b );