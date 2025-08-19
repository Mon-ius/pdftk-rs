#pragma GCC java_exceptions
#include <gcj/cni.h>
#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <map>
#include <vector>
#include <set>
#include <algorithm>
#include <java/lang/System.h>
#include <java/lang/Throwable.h>
#include <java/lang/String.h>
#include <java/io/IOException.h>
#include <java/io/PrintStream.h>
#include <java/io/FileOutputStream.h>
#include <java/util/Set.h>
#include <java/util/Vector.h>
#include <java/util/ArrayList.h>
#include <java/util/Iterator.h>
#include <java/util/HashMap.h>
#include "pdftk/com/lowagie/text/Document.h"
#include "pdftk/com/lowagie/text/Rectangle.h"
#include "pdftk/com/lowagie/text/pdf/PdfObject.h"
#include "pdftk/com/lowagie/text/pdf/PdfName.h"
#include "pdftk/com/lowagie/text/pdf/PdfString.h"
#include "pdftk/com/lowagie/text/pdf/PdfNumber.h"
#include "pdftk/com/lowagie/text/pdf/PdfBoolean.h"
#include "pdftk/com/lowagie/text/pdf/PdfArray.h"
#include "pdftk/com/lowagie/text/pdf/PdfDictionary.h"
#include "pdftk/com/lowagie/text/pdf/PdfDestination.h"
#include "pdftk/com/lowagie/text/pdf/PdfOutline.h"
#include "pdftk/com/lowagie/text/pdf/PdfCopy.h"
#include "pdftk/com/lowagie/text/pdf/PdfReader.h"
#include "pdftk/com/lowagie/text/pdf/PdfImportedPage.h"
#include "pdftk/com/lowagie/text/pdf/PdfWriter.h"
#include "pdftk/com/lowagie/text/pdf/PdfStamperImp.h"
#include "pdftk/com/lowagie/text/pdf/PdfNameTree.h"
#include "pdftk/com/lowagie/text/pdf/FdfReader.h"
#include "pdftk/com/lowagie/text/pdf/AcroFields.h"
#include "pdftk/com/lowagie/text/pdf/PdfIndirectReference.h"
#include "pdftk/com/lowagie/text/pdf/PdfIndirectObject.h"
#include "pdftk/com/lowagie/text/pdf/PdfFileSpecification.h"
#include "pdftk/com/lowagie/text/pdf/PRStream.h"
using namespace std;
namespace java {
	using namespace java::lang;
	using namespace java::io;
	using namespace java::util;
}
namespace itext {
	using namespace pdftk::com::lowagie::text;
	using namespace pdftk::com::lowagie::text::pdf;
}
#include "pdftk.h"
#include "report.h"
static const string g_uninitString= "PdftkEmptyString";
class PdfInfo {
public:
	static const string m_prefix;
	static const string m_begin_mark;
	static const string m_key_label;
	static const string m_value_label;
	string m_key;
	string m_value;
	PdfInfo() : m_key( g_uninitString ), m_value( g_uninitString ) {}
	bool valid() { return( m_key!= g_uninitString && m_value!= g_uninitString ); }
};
const string PdfInfo::m_prefix= "Info";
const string PdfInfo::m_begin_mark= "InfoBegin";
const string PdfInfo::m_key_label= "InfoKey:";
const string PdfInfo::m_value_label= "InfoValue:";
ostream& operator<<( ostream& ss, const PdfInfo& ii ) {
	ss << PdfInfo::m_begin_mark << endl;
	ss << PdfInfo::m_key_label << " " << ii.m_key << endl;
	ss << PdfInfo::m_value_label << " " << ii.m_value << endl;
	return ss;
}
const string PdfBookmark::m_prefix= "Bookmark";
const string PdfBookmark::m_begin_mark= "BookmarkBegin";
const string PdfBookmark::m_title_label= "BookmarkTitle:";
const string PdfBookmark::m_level_label= "BookmarkLevel:";
const string PdfBookmark::m_page_number_label= "BookmarkPageNumber:";
PdfBookmark::PdfBookmark() : m_title( g_uninitString ),
														 m_level(-1), m_page_num(-1) {}
bool PdfBookmark::valid() {
	return( 0< m_level && 0<= m_page_num && m_title!= g_uninitString );
}
ostream& operator<<( ostream& ss, const PdfBookmark& bb ) {
	ss << PdfBookmark::m_begin_mark << endl;
	ss << PdfBookmark::m_title_label << " " << bb.m_title << endl;
	ss << PdfBookmark::m_level_label << " " << bb.m_level << endl;
	ss << PdfBookmark::m_page_number_label << " " << bb.m_page_num << endl;
	return ss;
}
class PdfPageLabel {
public:
	static const string m_prefix;
	static const string m_begin_mark;
};
const string PdfPageLabel::m_prefix= "PageLabel";
const string PdfPageLabel::m_begin_mark= "PageLabelBegin";
class PdfPageMedia {
public:
	static const string m_prefix;
	static const string m_begin_mark;
};
const string PdfPageMedia::m_prefix= "PageMedia";
const string PdfPageMedia::m_begin_mark= "PageMediaBegin";
class PdfData {
public:
	vector<PdfInfo> m_info;
	vector<PdfBookmark> m_bookmarks;
	int m_num_pages;
	string m_id_0;
	string m_id_1;
	PdfData() : m_info(), m_bookmarks(), m_num_pages(-1),
							m_id_0( g_uninitString ), m_id_1( g_uninitString ) {}
};
ostream& operator<<( ostream& ss, const PdfData& dd ) {
	for( vector<PdfInfo>::const_iterator vit= dd.m_info.begin(); vit!= dd.m_info.end(); ++vit ) {
		ss << *vit;
	}
	ss << "PdfID0: " << dd.m_id_0 << endl;
	ss << "PdfID1: " << dd.m_id_1 << endl;
	ss << "NumberOfPages: " << dd.m_num_pages << endl;
	for( vector<PdfBookmark>::const_iterator vit= dd.m_bookmarks.begin();
			 vit!= dd.m_bookmarks.end(); ++vit )
		{
			ss << *vit;
		}
	return ss;
}
static void
OutputXmlString( ostream& ofs,
								 java::lang::String* jss_p )
{
	if( jss_p ) {
		for( jint ii= 0; ii< jss_p->length(); ++ii ) {
			jchar wc= jss_p->charAt(ii);
			if( 0x20<= wc && wc<= 0x7e ) {
				switch( wc ) {
				case '<' :
					ofs << "&lt;";
					break;
				case '>':
					ofs << "&gt;";
					break;
				case '&':
					ofs << "&amp;";
					break;
				case '"':
					ofs << "&quot;";
					break;
				default:
					ofs << (char)wc;
					break;
				}
			}
			else {
				ofs << "&#" << (unsigned int)wc << ";";
			}
		}
	}
}
static void
OutputUtf8String( ostream& ofs,
									java::lang::String* jss_p )
{
	if( jss_p ) {
		int fn_buff_len = JvGetStringUTFLength( jss_p );
		char* fn_buff= (char*)malloc( fn_buff_len* sizeof(char) );
		JvGetStringUTFRegion( jss_p, 0, jss_p->length(), fn_buff );
		for( int ii= 0; ii< fn_buff_len; ++ii ) {
			ofs << fn_buff[ii];
		}
		free( fn_buff );
	}
}
static void
OutputPdfString( ostream& ofs,
								 itext::PdfString* pdfss_p,
								 bool utf8_b )
{
	if( pdfss_p && pdfss_p->isString() ) {
		java::lang::String* jss_p= pdfss_p->toUnicodeString();
		if( utf8_b ) {
			OutputUtf8String( ofs, jss_p );
		}
		else {
			OutputXmlString( ofs, jss_p );
		}
	}
}
static void
OutputPdfName( ostream& ofs,
							 itext::PdfName* pdfnn_p )
{
	if( pdfnn_p && pdfnn_p->isName() ) {
		java::String* jnn_p= new java::String( pdfnn_p->getBytes() );
		jnn_p= itext::PdfName::decodeName( jnn_p );
		OutputXmlString( ofs, jnn_p );
	}
}
static int
GetPageNumber( itext::PdfDictionary* dict_p,
							 itext::PdfReader* reader_p,
							 map< itext::PdfDictionary*, int >& cache )
{
	{
		map< itext::PdfDictionary*, int >::const_iterator it=
			cache.find( dict_p );
		if( it!= cache.end() )
			return it->second;
	}
	int ret_val= 0;
	if( dict_p && dict_p->contains( itext::PdfName::PARENT ) ) {
		itext::PdfDictionary* parent_p= (itext::PdfDictionary*)
			reader_p->getPdfObject( dict_p->get( itext::PdfName::PARENT ) );
		if( parent_p && parent_p->isDictionary() ) {
			jint sum_pages= GetPageNumber( parent_p, reader_p, cache );
			itext::PdfArray* parent_kids_p= (itext::PdfArray*)
				reader_p->getPdfObject( parent_p->get( itext::PdfName::KIDS ) );
			if( parent_kids_p && parent_kids_p->isArray() ) {
				java::ArrayList* kids_p= parent_kids_p->getArrayList();
				if( kids_p ) {
					for( jint kids_ii= 0; kids_ii< kids_p->size(); ++kids_ii ) {
						itext::PdfDictionary* kid_p= (itext::PdfDictionary*)
							reader_p->getPdfObject( (itext::PdfDictionary*)(kids_p->get(kids_ii)) );
						if( kid_p && kid_p->isDictionary() ) {
							if( kid_p== dict_p )
								ret_val= sum_pages;
							itext::PdfName* kid_type_p= (itext::PdfName*)
								reader_p->getPdfObject( kid_p->get( itext::PdfName::TYPE ) );
							if( kid_type_p && kid_type_p->isName() ) {
								if( kid_type_p->equals( itext::PdfName::PAGE ) ) {
									cache[ kid_p ]= sum_pages;
									sum_pages+= 1;
								}
								else if( kid_type_p->equals( itext::PdfName::PAGES ) ) {
									itext::PdfNumber* count_p= (itext::PdfNumber*)
										reader_p->getPdfObject( kid_p->get( itext::PdfName::COUNT ) );
									if( count_p && count_p->isNumber() ) {
										sum_pages+= count_p->intValue();
									}
									else {
										cerr << "pdftk Error in GetPageNumber(): invalid count;" << endl;
									}
								}
								else {
									cerr << "pdftk Error in GetPageNumber(): unexpected kid type;" << endl;
								}
							}
							else {
								cerr << "pdftk Error in GetPageNumber(): invalid kid_type_p;" << endl;
							}
						}
						else {
							cerr << "pdftk Error in GetPageNumber(): invalid kid_p;" << endl;
						}
					}
				}
				else {
					cerr << "pdftk Error in GetPageNumber(): invalid kids_p;" << endl;
				}
			}
			else {
				cerr << "pdftk Error in GetPageNumber(): invalid kids array;" << endl;
			}
		}
		else {
			cerr << "pdftk Error in GetPageNumber(): invalid parent;" << endl;
		}
	}
	else {
		ret_val= 0;
		cache[ dict_p ]= ret_val;
	}
	return ret_val;
}
int
ReadOutlines( vector<PdfBookmark>& bookmark_data,
							itext::PdfDictionary* outline_p,
							int level,
							itext::PdfReader* reader_p,
							bool utf8_b )
{
	int ret_val= 0;
	map< itext::PdfDictionary*, int > cache;
	while( outline_p ) {
		PdfBookmark bookmark;
		itext::PdfString* title_p= (itext::PdfString*)
			reader_p->getPdfObject( outline_p->get( itext::PdfName::TITLE ) );
		if( title_p && title_p->isString() ) {
			ostringstream oss;
			OutputPdfString( oss, title_p, utf8_b );
			bookmark.m_title= oss.str();
		}
		else {
			ret_val= 1;
		}
		bookmark.m_level= level+ 1;
		{
			bool fail_b= false;
			itext::PdfObject* destination_p= 0; {
				if( outline_p->contains( itext::PdfName::DEST ) ) {
					destination_p=
						reader_p->getPdfObject( outline_p->get( itext::PdfName::DEST ) );
				}
				else if( outline_p->contains( itext::PdfName::A ) ) {
					itext::PdfDictionary* action_p= (itext::PdfDictionary*)
						reader_p->getPdfObject( outline_p->get( itext::PdfName::A ) );
					if( action_p && action_p->isDictionary() ) {
						itext::PdfName* s_p= (itext::PdfName*)
							reader_p->getPdfObject( action_p->get( itext::PdfName::S ) );
						if( s_p && s_p->isName() ) {
							if( s_p->equals( itext::PdfName::GOTO ) ) {
								destination_p=
									reader_p->getPdfObject( action_p->get( itext::PdfName::D ) );
							}
							else {
							}
						}
						else {
							fail_b= true;
						}
					}
					else {
						fail_b= true;
					}
				}
				else {
					fail_b= true;
				}
			}
			if( destination_p && destination_p->isArray() ) {
				java::ArrayList* array_list_p= ((itext::PdfArray*)destination_p)->getArrayList();
				if( array_list_p && !array_list_p->isEmpty() ) {
					itext::PdfDictionary* page_p= (itext::PdfDictionary*)
						reader_p->getPdfObject( (itext::PdfObject*)(array_list_p->get(0)) );
					if( page_p && page_p->isDictionary() ) {
						bookmark.m_page_num= GetPageNumber(page_p, reader_p, cache)+ 1;
					}
					else {
						fail_b= true;
					}
				}
				else {
					fail_b= true;
				}
			}
			else {
				fail_b= true;
			}
			if( fail_b ) {
				bookmark.m_page_num= 0;
			}
		}
		if( 0< bookmark.m_level )
			bookmark_data.push_back( bookmark );
		if( outline_p->contains( itext::PdfName::FIRST ) ) {
			itext::PdfDictionary* child_p= (itext::PdfDictionary*)
				reader_p->getPdfObject( outline_p->get( itext::PdfName::FIRST ) );
			if( child_p && child_p->isDictionary() ) {
				ret_val+= ReadOutlines( bookmark_data, child_p, level+ 1, reader_p, utf8_b );
			}
		}
		if( outline_p->contains( itext::PdfName::NEXT ) ) {
			itext::PdfDictionary* sibling_p= (itext::PdfDictionary*)
				reader_p->getPdfObject( outline_p->get( itext::PdfName::NEXT ) );
			if( sibling_p && sibling_p->isDictionary() ) {
				outline_p= sibling_p;
			}
			else
				outline_p= 0;
		}
		else
			outline_p= 0;
	}
	return ret_val;
}
static void
ReportOutlines( ostream& ofs,
								itext::PdfDictionary* outline_p,
								itext::PdfReader* reader_p,
								bool utf8_b )
{
	vector<PdfBookmark> bookmark_data;
	ReadOutlines( bookmark_data,
								outline_p,
								0,
								reader_p,
								utf8_b );
	for( vector<PdfBookmark>::iterator it= bookmark_data.begin();
			 it!= bookmark_data.end(); ++it )
		{
			ofs << *it;
		}
}
static void
ReportInfo( ostream& ofs,
						itext::PdfReader* reader_p,
						itext::PdfDictionary* info_p,
						bool utf8_b )
{
	if( info_p && info_p->isDictionary() ) {
		java::Set* keys_p= info_p->getKeys();
		for( java::Iterator* it= keys_p->iterator(); it->hasNext(); ) {
			itext::PdfName* key_p= (itext::PdfName*)it->next();
			int key_len= JvGetArrayLength( key_p->getBytes() )- 1;
			itext::PdfObject* value_p= (itext::PdfObject*)reader_p->getPdfObject( info_p->get( key_p ) );
			if( 0< key_len &&
					value_p->isString() &&
					0< ((itext::PdfString*)value_p)->toUnicodeString()->length() )
				{
					ofs << PdfInfo::m_begin_mark << endl;
					ofs << PdfInfo::m_key_label << " ";
					OutputPdfName( ofs, key_p );
					ofs << endl;
					ofs << PdfInfo::m_value_label << " ";
					OutputPdfString( ofs, (itext::PdfString*)value_p, utf8_b );
					ofs << endl;
				}
		}
	}
	else {
	}
}
static void
ReportPageLabels( ostream& ofs,
									itext::PdfDictionary* numtree_node_p,
									itext::PdfReader* reader_p,
									bool utf8_b )
{
	itext::PdfArray* nums_p= (itext::PdfArray*)
		reader_p->getPdfObject( numtree_node_p->get( itext::PdfName::NUMS ) );
	if( nums_p && nums_p->isArray() ) {
		java::ArrayList* labels_p= nums_p->getArrayList();
		if( labels_p ) {
			for( jint labels_ii= 0; labels_ii< labels_p->size(); labels_ii+=2 ) {
				itext::PdfNumber* index_p= (itext::PdfNumber*)
					reader_p->getPdfObject( (itext::PdfNumber*)(labels_p->get(labels_ii)) );
				itext::PdfDictionary* label_p= (itext::PdfDictionary*)
					reader_p->getPdfObject( (itext::PdfDictionary*)(labels_p->get(labels_ii+ 1)) );
				if( index_p && index_p->isNumber() &&
						label_p && label_p->isDictionary() )
					{
						ofs << PdfPageLabel::m_begin_mark << endl;
						ofs << "PageLabelNewIndex: " << (long)(index_p->intValue())+ 1 << endl;
						{
							ofs << "PageLabelStart: ";
							itext::PdfNumber* start_p= (itext::PdfNumber*)
								reader_p->getPdfObject( label_p->get( itext::PdfName::ST ) );
							if( start_p && start_p->isNumber() ) {
								ofs << (long)(start_p->intValue()) << endl;
							}
							else {
								ofs << "1" << endl;
							}
						}
						{
							itext::PdfString* prefix_p= (itext::PdfString*)
								reader_p->getPdfObject( label_p->get( itext::PdfName::P ) );
							if( prefix_p && prefix_p->isString() ) {
								ofs << "PageLabelPrefix: ";
								OutputPdfString( ofs, prefix_p, utf8_b );
								ofs << endl;
							}
						}
						{
							itext::PdfName* r_p= new itext::PdfName(JvNewStringUTF("r"));
							itext::PdfName* a_p= new itext::PdfName(JvNewStringUTF("a"));
							itext::PdfName* style_p= (itext::PdfName*)
								reader_p->getPdfObject( label_p->get( itext::PdfName::S ) );
							ofs << "PageLabelNumStyle: ";
							if( style_p && style_p->isName() ) {
								if( style_p->equals( itext::PdfName::D ) ) {
									ofs << "DecimalArabicNumerals" << endl;
								}
								else if( style_p->equals( itext::PdfName::R ) ) {
									ofs << "UppercaseRomanNumerals" << endl;
								}
								else if( style_p->equals( r_p ) ) {
									ofs << "LowercaseRomanNumerals" << endl;
								}
								else if( style_p->equals( itext::PdfName::A ) ) {
									ofs << "UppercaseLetters" << endl;
								}
								else if( style_p->equals( a_p ) ) {
									ofs << "LowercaseLetters" << endl;
								}
								else {
									ofs << "[PDFTK ERROR]" << endl;
								}
							}
							else {
								ofs << "NoNumber" << endl;
							}
						}
					}
				else {
					ofs << "[PDFTK ERROR: INVALID label_p IN ReportPageLabelNode]" << endl;
				}
			}
		}
		else {
			ofs << "[PDFTK ERROR: INVALID labels_p IN ReportPageLabelNode]" << endl;
		}
	}
	else {
		itext::PdfArray* kids_p= (itext::PdfArray*)
			reader_p->getPdfObject( numtree_node_p->get( itext::PdfName::KIDS ) );
		if( kids_p && kids_p->isArray() ) {
			java::ArrayList* kids_ar_p= kids_p->getArrayList();
			if( kids_ar_p ) {
				for( jint kids_ii= 0; kids_ii< kids_ar_p->size(); ++kids_ii ) {
					itext::PdfDictionary* kid_p= (itext::PdfDictionary*)
						reader_p->getPdfObject( (itext::PdfDictionary*)kids_ar_p->get(kids_ii) );
					if( kid_p && kid_p->isDictionary() ) {
						ReportPageLabels( ofs, kid_p, reader_p, utf8_b );
					}
					else {
						ofs << "[PDFTK ERROR: INVALID kid_p]" << endl;
					}
				}
			}
			else {
				ofs << "[PDFTK ERROR: INVALID kids_ar_p]" << endl;
			}
		}
		else {
			ofs << "[PDFTK ERROR: INVALID PAGE LABEL NUMBER TREE]" << endl;
		}
	}
}
class FormField {
	public:
	string m_ft;
	string m_tt;
	string m_tu;
	int m_ff;
	set< string > m_vv;
	string m_dv;
	int m_qq;
	string m_ds;
	string m_rv;
	int m_maxlen;
	set< string > m_states;
	string m_state;
	FormField() : m_ft(), m_tt(), m_tu(), m_ff(0), m_vv(), m_dv(),
								m_qq(0), m_ds(), m_rv(),
								m_maxlen(0),
								m_states(), m_state() {}
};
static void
	OutputFormField( ostream& ofs,
									 const FormField& ff )
{
	ofs << "---" << endl;
	ofs << "FieldType: " << ff.m_ft << endl;
	ofs << "FieldName: " << ff.m_tt << endl;
	if( !ff.m_tu.empty() )
		ofs << "FieldNameAlt: " << ff.m_tu << endl;
	ofs << "FieldFlags: " << ff.m_ff << endl;
	if( !ff.m_vv.empty() ) {
		for( set< string >::const_iterator it= ff.m_vv.begin(); it!= ff.m_vv.end(); ++it )
			ofs << "FieldValue: " << (*it) << endl;
	}
	if( !ff.m_dv.empty() )
		ofs << "FieldValueDefault: " << ff.m_dv << endl;
	ofs << "FieldJustification: ";
	switch( ff.m_qq ) {
	case 0:
		ofs << "Left";
		break;
	case 1:
		ofs << "Center";
		break;
	case 2:
		ofs << "Right";
		break;
	default:
		ofs << ff.m_qq;
		break;
	}
	ofs << endl;
	if( !ff.m_ds.empty() )
		ofs << "FieldStyleDefault: " << ff.m_ds << endl;
	if( !ff.m_rv.empty() )
		ofs << "FieldValueRichText: " << ff.m_rv << endl;
	if( 0< ff.m_maxlen )
		ofs << "FieldMaxLength: " << ff.m_maxlen << endl;
	for( set< string >::const_iterator it= ff.m_states.begin();
			 it!= ff.m_states.end(); ++it )
		{
			ofs << "FieldStateOption: " << *it << endl;
		}
}
static bool
ReportAcroFormFields( ostream& ofs,
											itext::PdfArray* kids_array_p,
											FormField& acc_state,
											itext::PdfReader* reader_p,
											bool utf8_b )
{
	FormField prev_state= acc_state;
	bool ret_val_b= false;
	java::ArrayList* kids_p= kids_array_p->getArrayList();
	if( kids_p ) {
		for( jint kids_ii= 0; kids_ii< kids_p->size(); ++kids_ii ) {
			itext::PdfDictionary* kid_p= (itext::PdfDictionary*)
				reader_p->getPdfObject( (itext::PdfDictionary*)(kids_p->get(kids_ii)) );
			if( kid_p && kid_p->isDictionary() ) {
				if( kid_p->contains( itext::PdfName::FT ) ) {
					itext::PdfName* ft_p= (itext::PdfName*)
						reader_p->getPdfObject( kid_p->get( itext::PdfName::FT ) );
					if( ft_p && ft_p->isName() ) {
						if( ft_p->equals( itext::PdfName::BTN ) ) {
							acc_state.m_ft= "Button";
						}
						else if( ft_p->equals( itext::PdfName::TX ) ) {
							acc_state.m_ft= "Text";
						}
						else if( ft_p->equals( itext::PdfName::CH ) ) {
							acc_state.m_ft= "Choice";
						}
						else if( ft_p->equals( itext::PdfName::SIG ) ) {
							acc_state.m_ft= "Signature";
						}
						else {
							cerr << "pdftk Warning in ReportAcroFormFields(): unexpected field type;" << endl;
						}
					}
				}
				if( kid_p->contains( itext::PdfName::T ) ) {
					itext::PdfString* pdfs_p= (itext::PdfString*)
						reader_p->getPdfObject( kid_p->get( itext::PdfName::T ) );
					if( pdfs_p && pdfs_p->isString() ) {
						ostringstream name_oss;
						OutputPdfString( name_oss, pdfs_p, utf8_b );
						if( !acc_state.m_tt.empty() ) {
							acc_state.m_tt+= ".";
						}
						acc_state.m_tt+= name_oss.str();
					}
				}
				if( kid_p->contains( itext::PdfName::TU ) ) {
					itext::PdfString* pdfs_p= (itext::PdfString*)
						reader_p->getPdfObject( kid_p->get( itext::PdfName::TU ) );
					if( pdfs_p && pdfs_p->isString() ) {
						ostringstream name_oss;
						OutputPdfString( name_oss, pdfs_p, utf8_b );
						acc_state.m_tu= name_oss.str();
					}
				}
				else {
					acc_state.m_tu.erase();
				}
				if( kid_p->contains( itext::PdfName::FF ) ) {
					itext::PdfNumber* pdfs_p= (itext::PdfNumber*)
						reader_p->getPdfObject( kid_p->get( itext::PdfName::FF ) );
					if( pdfs_p && pdfs_p->isNumber() ) {
						acc_state.m_ff= pdfs_p->intValue();
					}
				}
				if( kid_p->contains( itext::PdfName::V ) ) {
					itext::PdfObject* pdfs_p=
						reader_p->getPdfObject( kid_p->get( itext::PdfName::V ) );
					if( pdfs_p && pdfs_p->isString() ) {
						ostringstream name_oss;
						OutputPdfString( name_oss, (itext::PdfString*)pdfs_p, utf8_b );
						acc_state.m_vv.insert( name_oss.str() );
					}
					else if( pdfs_p && pdfs_p->isName() ) {
						ostringstream name_oss;
						OutputPdfName( name_oss, (itext::PdfName*)pdfs_p );
						acc_state.m_vv.insert( name_oss.str() );
					}
					else if( pdfs_p && pdfs_p->isArray() ) {
						java::ArrayList* vv_p= ((itext::PdfArray*)pdfs_p)->getArrayList();
						for( jint vv_ii= 0; vv_ii< vv_p->size(); ++vv_ii ) {
							itext::PdfObject* pdfs_p= (itext::PdfObject*)
								reader_p->getPdfObject( (itext::PdfObject*)(vv_p->get(vv_ii)) );
							if( pdfs_p && pdfs_p->isString() ) {
								ostringstream name_oss;
								OutputPdfString( name_oss, (itext::PdfString*)pdfs_p, utf8_b );
								acc_state.m_vv.insert( name_oss.str() );
							}
							else if( pdfs_p && pdfs_p->isName() ) {
								ostringstream name_oss;
								OutputPdfName( name_oss, (itext::PdfName*)pdfs_p );
								acc_state.m_vv.insert( name_oss.str() );
							}
						}
					}
				}
				if( kid_p->contains( itext::PdfName::DV ) ) {
					itext::PdfString* pdfs_p= (itext::PdfString*)
						reader_p->getPdfObject( kid_p->get( itext::PdfName::DV ) );
					if( pdfs_p && pdfs_p->isString() ) {
						ostringstream name_oss;
						OutputPdfString( name_oss, pdfs_p, utf8_b );
						acc_state.m_dv= name_oss.str();
					}
				}
				if( kid_p->contains( itext::PdfName::Q ) ) {
					itext::PdfNumber* pdfs_p= (itext::PdfNumber*)
						reader_p->getPdfObject( kid_p->get( itext::PdfName::Q ) );
					if( pdfs_p && pdfs_p->isNumber() ) {
						acc_state.m_qq= pdfs_p->intValue();
					}
				}
				if( kid_p->contains( itext::PdfName::DS ) ) {
					itext::PdfString* pdfs_p= (itext::PdfString*)
						reader_p->getPdfObject( kid_p->get( itext::PdfName::DS ) );
					if( pdfs_p && pdfs_p->isString() ) {
						ostringstream name_oss;
						OutputPdfString( name_oss, pdfs_p, utf8_b );
						acc_state.m_ds= name_oss.str();
					}
				}
				else {
					acc_state.m_ds.erase();
				}
				if( kid_p->contains( itext::PdfName::RV ) ) {
					itext::PdfObject* pdfo_p= (itext::PdfObject*)
						reader_p->getPdfObject( kid_p->get( itext::PdfName::RV ) );
					if( pdfo_p && pdfo_p->isString() ) {
						itext::PdfString* pdfs_p= (itext::PdfString*)pdfo_p;
						ostringstream name_oss;
						OutputPdfString( name_oss, pdfs_p, utf8_b );
						acc_state.m_rv= name_oss.str();
					}
					else if( pdfo_p && pdfo_p->isStream() ) {
						itext::PRStream* pdfs_p= (itext::PRStream*)pdfo_p;
						jbyteArray pdfsa_p= pdfs_p->getBytes();
						acc_state.m_rv= (char*)(elements(pdfsa_p));
					}
				}
				else {
					acc_state.m_rv.erase();
				}
				if( kid_p->contains( itext::PdfName::MAXLEN ) ) {
					itext::PdfNumber* pdfs_p= (itext::PdfNumber*)
						reader_p->getPdfObject( kid_p->get( itext::PdfName::MAXLEN ) );
					if( pdfs_p && pdfs_p->isNumber() ) {
						acc_state.m_maxlen= pdfs_p->intValue();
					}
				}
				if( kid_p->contains( itext::PdfName::AP ) ) {
					itext::PdfDictionary* ap_p= (itext::PdfDictionary*)
						reader_p->getPdfObject( kid_p->get( itext::PdfName::AP ) );
					if( ap_p && ap_p->isDictionary() ) {
						if( ap_p->contains( itext::PdfName::N ) ) {
							itext::PdfObject* n_p=
								reader_p->getPdfObject( ap_p->get( itext::PdfName::N ) );
							if( n_p && n_p->isDictionary() ) {
								java::Set* n_set_p= ((itext::PdfDictionary*)n_p)->getKeys();
								for( java::Iterator* it= n_set_p->iterator(); it->hasNext(); ) {
									itext::PdfName* key_p= (itext::PdfName*)it->next();
									ostringstream oss;
									OutputPdfName( oss, key_p );
									acc_state.m_states.insert( oss.str() );
								}
							}
						}
						if( ap_p->contains( itext::PdfName::D ) ) {
							itext::PdfObject* n_p=
								reader_p->getPdfObject( ap_p->get( itext::PdfName::D ) );
							if( n_p && n_p->isDictionary() ) {
								java::Set* n_set_p= ((itext::PdfDictionary*)n_p)->getKeys();
								for( java::Iterator* it= n_set_p->iterator(); it->hasNext(); ) {
									itext::PdfName* key_p= (itext::PdfName*)it->next();
									ostringstream oss;
									OutputPdfName( oss, key_p );
									acc_state.m_states.insert( oss.str() );
								}
							}
						}
						if( ap_p->contains( itext::PdfName::R ) ) {
							itext::PdfObject* n_p=
								reader_p->getPdfObject( ap_p->get( itext::PdfName::N ) );
							if( n_p && n_p->isDictionary() ) {
								java::Set* n_set_p= ((itext::PdfDictionary*)n_p)->getKeys();
								for( java::Iterator* it= n_set_p->iterator(); it->hasNext(); ) {
									itext::PdfName* key_p= (itext::PdfName*)it->next();
									ostringstream oss;
									OutputPdfName( oss, key_p );
									acc_state.m_states.insert( oss.str() );
								}
							}
						}
					}
				}
				if( kid_p->contains( itext::PdfName::OPT ) ) {
					itext::PdfArray* kid_opts_p= (itext::PdfArray*)
						reader_p->getPdfObject( kid_p->get( itext::PdfName::OPT ) );
					if( kid_opts_p && kid_opts_p->isArray() ) {
						java::ArrayList* opts_p= kid_opts_p->getArrayList();
						for( jint opts_ii= 0; opts_ii< opts_p->size(); ++opts_ii ) {
							itext::PdfString* opt_p= (itext::PdfString*)
								reader_p->getPdfObject( (itext::PdfObject*)(opts_p->get(opts_ii)) );
							if( opt_p && opt_p->isString() ) {
								ostringstream name_oss;
								OutputPdfString( name_oss, opt_p, utf8_b );
								acc_state.m_states.insert( name_oss.str() );
							}
						}
					}
				}
				if( kid_p->contains( itext::PdfName::KIDS ) ) {
					itext::PdfArray* kid_kids_p= (itext::PdfArray*)
						reader_p->getPdfObject( kid_p->get( itext::PdfName::KIDS )  );
					if( kid_kids_p && kid_kids_p->isArray() ) {
						bool kids_have_names_b=
							ReportAcroFormFields( ofs, kid_kids_p, acc_state, reader_p, utf8_b );
						if( !kids_have_names_b &&
								kid_p->contains( itext::PdfName::T ) )
							{
								OutputFormField( ofs, acc_state );
							}
						acc_state= prev_state;
					}
					else {
					}
				}
				else if( kid_p->contains( itext::PdfName::T ) ) {
					OutputFormField( ofs, acc_state );
					acc_state= prev_state;
					ret_val_b= true;
				}
			}
		}
	}
	else {
		cerr << "pdftk Warning in ReportAcroFormFields(): unable to get ArrayList;" << endl;
	}
	return ret_val_b;
}
void
ReportAcroFormFields( ostream& ofs,
											itext::PdfReader* reader_p,
											bool utf8_b )
{
	itext::PdfDictionary* catalog_p= reader_p->catalog;
	if( catalog_p && catalog_p->isDictionary() ) {
		itext::PdfDictionary* acro_form_p= (itext::PdfDictionary*)
			reader_p->getPdfObject( catalog_p->get( itext::PdfName::ACROFORM ) );
		if( acro_form_p && acro_form_p->isDictionary() ) {
			itext::PdfArray* fields_p= (itext::PdfArray*)
				reader_p->getPdfObject( acro_form_p->get( itext::PdfName::FIELDS ) );
			if( fields_p && fields_p->isArray() ) {
				FormField root_field_state;
				ReportAcroFormFields( ofs, fields_p, root_field_state, reader_p, utf8_b );
			}
		}
	}
	else {
		cerr << "pdftk Error in ReportAcroFormFields(): unable to access PDF catalog;" << endl;
	}
}
void
ReportOnPdf( ostream& ofs,
						 itext::PdfReader* reader_p,
						 bool utf8_b )
{
	{
		itext::PdfDictionary* trailer_p= reader_p->getTrailer();
		if( trailer_p && trailer_p->isDictionary() ) {
			{
				itext::PdfDictionary* info_p= (itext::PdfDictionary*)
					reader_p->getPdfObject( trailer_p->get( itext::PdfName::INFO ) );
				if( info_p && info_p->isDictionary() ) {
					ReportInfo( ofs, reader_p, info_p, utf8_b );
				}
				else {
					cerr << "Warning: no info dictionary found" << endl;
				}
			}
			{
				itext::PdfArray* id_p= (itext::PdfArray*)
					reader_p->getPdfObject( trailer_p->get( itext::PdfName::ID ) );
				if( id_p && id_p->isArray() ) {
					java::ArrayList* id_al_p= id_p->getArrayList();
					if( id_al_p ) {
						for( jint ii= 0; ii< id_al_p->size(); ++ii ) {
							ofs << "PdfID" << (int)ii << ": ";
							itext::PdfString* id_ss_p= (itext::PdfString*)
								reader_p->getPdfObject( (itext::PdfObject*)id_al_p->get(ii) );
							if( id_ss_p && id_ss_p->isString() ) {
								jbyteArray bb= id_ss_p->getBytes();
								if( bb && bb->length ) {
									jbyte* bb_ss= elements( bb );
									char buff[8]= "";
									for( jint ii= 0; ii< bb->length; ++ii ) {
										sprintf( buff, "%02x", (unsigned char)bb_ss[ii] );
										ofs << buff;
									}
								}
							}
							else {
								cerr << "pdftk Error in ReportOnPdf(): invalid pdf id array string;" << endl;
							}
							ofs << endl;
						}
					}
					else {
						cerr << "pdftk Error in ReportOnPdf(): invalid ID ArrayList" << endl;
					}
				}
			}
		}
		else {
			cerr << "pdftk Error in ReportOnPdf(): invalid trailer;" << endl;
		}
	}
	jint numPages= reader_p->getNumberOfPages();
	{
		itext::PdfDictionary* catalog_p= reader_p->catalog;
		if( catalog_p && catalog_p->isDictionary() ) {
			ofs << "NumberOfPages: " << (unsigned int)numPages << endl;
			itext::PdfDictionary* outlines_p= (itext::PdfDictionary*)
				reader_p->getPdfObject( catalog_p->get( itext::PdfName::OUTLINES ) );
			if( outlines_p && outlines_p->isDictionary() ) {
				itext::PdfDictionary* top_outline_p= (itext::PdfDictionary*)
					reader_p->getPdfObject( outlines_p->get( itext::PdfName::FIRST ) );
				if( top_outline_p && top_outline_p->isDictionary() ) {
					ReportOutlines( ofs, top_outline_p, reader_p, utf8_b );
				}
				else {
				}
			}
		}
		else {
			cerr << "pdftk Error in ReportOnPdf(): couldn't find catalog;" << endl;
		}
	}
	{
		for( jint ii= 1; ii<= numPages; ++ii ) {
			itext::PdfDictionary* page_p= reader_p->getPageN( ii );
			ofs << PdfPageMedia::m_begin_mark << endl;
			ofs << "PageMediaNumber: " << (unsigned int)ii << endl;
			ofs << "PageMediaRotation: " << (unsigned int)(reader_p->getPageRotation( page_p )) << endl;
			itext::Rectangle* page_rect_p= reader_p->getPageSize( page_p );
			if( page_rect_p ) {
				ofs << "PageMediaRect: "
						<< (float)(page_rect_p->left()) << " "
						<< (float)(page_rect_p->bottom()) << " "
						<< (float)(page_rect_p->right()) << " "
						<< (float)(page_rect_p->top()) << endl;
				ofs << "PageMediaDimensions: "
						<< (float)(page_rect_p->right()- page_rect_p->left()) << " "
						<< (float)(page_rect_p->top()- page_rect_p->bottom()) << endl;
			}
			itext::Rectangle* page_crop_p= reader_p->getBoxSize( page_p, itext::PdfName::CROPBOX );
			if( page_crop_p &&
					!( page_crop_p->left()== page_rect_p->left() &&
						 page_crop_p->bottom()== page_rect_p->bottom() &&
						 page_crop_p->right()== page_rect_p->right() &&
						 page_crop_p->top()== page_rect_p->top() ) )
				{
					ofs << "PageMediaCropRect: "
							<< (float)(page_crop_p->left()) << " "
							<< (float)(page_crop_p->bottom()) << " "
							<< (float)(page_crop_p->right()) << " "
							<< (float)(page_crop_p->top()) << endl;
				}
			itext::PdfString* stamptkData_p= page_p->getAsString( itext::PdfName::STAMPTKDATA );
			if( stamptkData_p ) {
				ofs << "PageMediaStamptkData: ";
				OutputPdfString( ofs, stamptkData_p, utf8_b );
				ofs << endl;
			}
			reader_p->releasePage( ii );
		}
	}
	{
		itext::PdfDictionary* catalog_p= reader_p->catalog;
		if( catalog_p && catalog_p->isDictionary() ) {
			itext::PdfDictionary* pagelabels_p= (itext::PdfDictionary*)
				reader_p->getPdfObject( catalog_p->get( itext::PdfName::PAGELABELS ) );
			if( pagelabels_p && pagelabels_p->isDictionary() ) {
				ReportPageLabels( ofs, pagelabels_p, reader_p, utf8_b );
			}
		}
		else {
			cerr << "pdftk Error in ReportOnPdf(): couldn't find catalog (2);" << endl;
		}
	}
}
static const char empty_string[]= "";
static const char*
BufferString( const char* buff, int buff_ii= 0 )
{
	if( isspace( buff[buff_ii] ) )
		++buff_ii;
	return( buff[buff_ii] ? (buff+ buff_ii) : empty_string );
}
static int
BufferInt( const char* buff, int buff_ii= 0 )
{
	int ret_val= 0;
	if( isspace( buff[buff_ii] ) )
		++buff_ii;
	while( buff[buff_ii] && '0'<= buff[buff_ii] && buff[buff_ii]<= '9' ) {
		ret_val*= 10;
		ret_val+= (buff[buff_ii]- '0');
		++buff_ii;
	}
	return ret_val;
}
static bool
LoadString( string& ss, const char* buff, const char* label ) {
	int label_len= strlen( label );
	if( strncmp( buff, label, label_len )== 0 ) {
		if( ss== g_uninitString ) {
			ss= BufferString( buff, label_len );
		}
		else {
			cerr << "pdftk Warning: " << label << " (" << ss << ") already loaded when reading new " << label << " (" << BufferString( buff, label_len ) << ") -- skipping newer item" << endl;
		}
		return true;
	}
	return false;
}
static bool
LoadInt( int& ii, const char* buff, const char* label ) {
	int label_len= strlen( label );
	if( strncmp( buff, label, label_len )== 0 ) {
		if( ii< 0 ) {
			ii= BufferInt( buff, label_len );
		}
		else {
			cerr << "pdftk Warning: " << label << " (" << ii << ") not empty when reading new " << label << " (" << BufferInt( buff, label_len ) << ") -- skipping newer item" << endl;
		}
		return true;
	}
	return false;
}
static int
LoadDataFile( istream& ifs,
							PdfData* pdf_data_p )
{
	if( ifs ) {
		const int buff_size= 4096;
		char buff[buff_size];
		char buff_prev[buff_size];
		int buff_prev_len= 0;
		PdfInfo info;
		bool info_b= false;
		PdfBookmark bookmark;
		bool bookmark_b= false;
		while( ifs ) {
			ifs.getline( buff, buff_size );
			if( !ifs ||
					strncmp( buff, PdfInfo::m_begin_mark.c_str(), PdfInfo::m_begin_mark.length() )== 0 ||
					strncmp( buff, PdfBookmark::m_begin_mark.c_str(), PdfBookmark::m_begin_mark.length() )== 0 ||
					strncmp( buff, PdfPageLabel::m_begin_mark.c_str(), PdfPageLabel::m_begin_mark.length() )== 0 ||
					buff_prev_len && strncmp( buff, buff_prev, buff_prev_len )!= 0 )
			{
				if( info_b ) {
					if( info.valid() ) {
						pdf_data_p->m_info.push_back( info );
					}
					else {
						cerr << "pdftk Warning: data info record not valid -- skipped; data:" << endl;
						cerr << info;
					}
				}
				else if( bookmark_b ) {
					if( bookmark.valid() ) {
						pdf_data_p->m_bookmarks.push_back( bookmark );
					}
					else {
						cerr << "pdftk Warning: data bookmark record not valid -- skipped; data:" << endl;
						cerr << bookmark;
					}
				}
				buff_prev[0]= 0;
				buff_prev_len= 0;
				info= PdfInfo();
				info_b= false;
				bookmark= PdfBookmark();
				bookmark_b= false;
			}
			if( buff[0]== 0 || buff[0]== '#' ) {
				continue;
			}
			else if( strncmp( buff, PdfInfo::m_prefix.c_str(), PdfInfo::m_prefix.length() )== 0 ) {
				buff_prev_len= PdfInfo::m_prefix.length();
				info_b= true;
				if( strncmp( buff, PdfInfo::m_begin_mark.c_str(), PdfInfo::m_begin_mark.length() )== 0 ||
						LoadString( info.m_key, buff, PdfInfo::m_key_label.c_str() ) ||
						LoadString( info.m_value, buff, PdfInfo::m_value_label.c_str() ) )
					{
					}
				else {
					cerr << "pdftk Warning: unexpected Info case in LoadDataFile(); continuing" << endl;
				}
			}
			else if( strncmp( buff, PdfBookmark::m_prefix.c_str(), PdfBookmark::m_prefix.length() )== 0 ) {
				buff_prev_len= PdfBookmark::m_prefix.length();
				bookmark_b= true;
				if( strncmp( buff, PdfBookmark::m_begin_mark.c_str(), PdfBookmark::m_begin_mark.length() )== 0 ||
						LoadString( bookmark.m_title, buff, PdfBookmark::m_title_label.c_str() ) ||
						LoadInt( bookmark.m_level, buff, PdfBookmark::m_level_label.c_str() ) ||
						LoadInt( bookmark.m_page_num, buff, PdfBookmark::m_page_number_label.c_str() ) )
					{
					}
				else {
					cerr << "pdftk Warning: unexpected Bookmark case in LoadDataFile(); continuing" << endl;
				}
			}
			else if( strncmp( buff, PdfPageLabel::m_prefix.c_str(), PdfPageLabel::m_prefix.length() )== 0 ) {
				buff_prev_len= 0;
			}
			else if( strncmp( buff, PdfPageMedia::m_prefix.c_str(), PdfPageMedia::m_prefix.length() )== 0 ) {
				buff_prev_len= 0;
			}
			else if( strncmp( buff, "PdfID", 5 )== 0 ) {
				buff_prev_len= 0;
				if( LoadString( pdf_data_p->m_id_0, buff, "PdfID0:" ) ||
						LoadString( pdf_data_p->m_id_1, buff, "PdfID1:" ) )
					{
					}
				else {
					cerr << "pdftk Warning: unexpected PdfID case in LoadDataFile(); continuing" << endl;
				}
			}
			else if( LoadInt( pdf_data_p->m_num_pages, buff, "NumberOfPages:" ) ) {
				buff_prev_len= 0;
			}
			else {
				cerr << "pdftk Warning: unexpected case 1 in LoadDataFile(); continuing" << endl;
			}
			if( buff_prev_len )
				strncpy( buff_prev, buff, buff_prev_len );
			else
				buff_prev[0]= 0;
		}
		if( buff_prev_len!= 0 ) {
			cerr << "pdftk Warning in LoadDataFile(): incomplete record;" << endl;
		}
	}
	else {
		cerr << "pdftk Error in LoadDataFile(): invalid istream;" << endl;
	}
	return 0;
}
void
XmlStringToJcharArray( jchar* jvs,
											 jsize jvs_size,
											 jsize* jvs_len_p,
											 string ss )
{
	*jvs_len_p= 0;
	jsize jvs_i= 0;
	bool inside_entity_b= false;
	string buff;
	for( string::const_iterator it= ss.begin(); it!= ss.end() && jvs_i< jvs_size- 1; ++it ) {
		if( inside_entity_b ) {
			buff+= *it;
			if( *it== ';' ) {
				if( buff== "&lt;" ) {
					jvs[jvs_i++]= (jchar)'<';
				}
				else if( buff== "&gt;" ) {
					jvs[jvs_i++]= (jchar)'>';
				}
				else if( buff== "&amp;" ) {
					jvs[jvs_i++]= (jchar)'&';
				}
				else if( buff== "&quot;" ) {
					jvs[jvs_i++]= (jchar)'"';
				}
				else {
					bool numerical_b= true;
					int number= 0;
					string::const_iterator jt= buff.begin();
					if( *jt!= '&' )
						numerical_b= false;
					if( *(++jt)!= '#' )
						numerical_b= false;
					while( ++jt!= buff.end() ) {
						if( '0'<= *jt && *jt<= '9' ) {
							number= number* 10+ *jt- '0';
						}
						else if( *jt== ';' && ++jt== buff.end() ) {
							break;
						}
						else {
							numerical_b= false;
							break;
						}
					}
					if( numerical_b ) {
						jvs[jvs_i++]= number;
					}
					else {
						for( jt= buff.begin(); jt!= buff.end() && jvs_i< jvs_size- 1; ++jt ) {
							jvs[jvs_i++]= (jchar)*jt;
						}
					}
				}
				buff.erase();
				inside_entity_b= false;
			}
		}
		else if( *it== '&' ) {
			buff= "&";
			inside_entity_b= true;
		}
		else {
			jvs[jvs_i++]= (jchar)*it;
		}
	}
	if( !buff.empty() ) {
		for( string::const_iterator jt= buff.begin();
				 jt!= buff.end() && jvs_i< jvs_size- 1; ++jt )
			{
				jvs[jvs_i++]= (jchar)*jt;
			}
	}
	jvs[jvs_i]= 0;
	*jvs_len_p= jvs_i;
}
int
RemoveBookmarks( itext::PdfReader* reader_p,
								 itext::PdfDictionary* bookmark_p )
{
	int ret_val= 0;
	if( bookmark_p->contains( itext::PdfName::FIRST ) ) {
		itext::PdfDictionary* first_p= (itext::PdfDictionary*)
			reader_p->getPdfObject( bookmark_p->get( itext::PdfName::FIRST ) );
		RemoveBookmarks( reader_p, first_p );
		bookmark_p->remove( itext::PdfName::FIRST );
	}
	if( bookmark_p->contains( itext::PdfName::NEXT ) ) {
		itext::PdfDictionary* next_p= (itext::PdfDictionary*)
			reader_p->getPdfObject( bookmark_p->get( itext::PdfName::NEXT ) );
		RemoveBookmarks( reader_p, next_p );
		bookmark_p->remove( itext::PdfName::NEXT );
	}
	bookmark_p->remove( itext::PdfName::PARENT );
	bookmark_p->remove( itext::PdfName::PREV );
	bookmark_p->remove( itext::PdfName::LAST );
	return ret_val;
}
int
BuildBookmarks( itext::PdfReader* reader_p,
								vector<PdfBookmark>::const_iterator& it,
								vector<PdfBookmark>::const_iterator it_end,
								itext::PdfDictionary* parent_p,
								itext::PRIndirectReference* parent_ref_p,
								int parent_level,
								int& num_bookmarks_total,
								bool utf8_b )
{
	int ret_val= 0;
	itext::PdfDictionary* bookmark_prev_p= 0;
	itext::PRIndirectReference* bookmark_first_ref_p= 0;
	itext::PRIndirectReference* bookmark_prev_ref_p= 0;
	int num_bookmarks= 0;
	if( parent_level+ 1< it->m_level ) {
		++num_bookmarks; ++num_bookmarks_total;
		itext::PdfDictionary* bookmark_p= new itext::PdfDictionary();
		itext::PRIndirectReference* bookmark_ref_p= reader_p->getPRIndirectReference( bookmark_p );
		bookmark_first_ref_p= bookmark_ref_p;
		bookmark_p->put( itext::PdfName::PARENT, (itext::PdfObject*)parent_ref_p );
		itext::PdfString* title_p= new itext::PdfString( JvNewStringUTF("") );
		bookmark_p->put( itext::PdfName::TITLE, title_p );
		bookmark_prev_p= bookmark_p;
		bookmark_prev_ref_p= bookmark_ref_p;
	}
	for( ;it!= it_end; ++it ) {
		if( parent_level+ 1< it->m_level ) {
			ret_val= BuildBookmarks( reader_p,
															 it,
															 it_end,
															 bookmark_prev_p,
															 bookmark_prev_ref_p,
															 parent_level+ 1,
															 num_bookmarks_total,
															 utf8_b );
			--it;
			continue;
		}
		else if( it->m_level< parent_level+ 1 ) {
			break;
		}
		++num_bookmarks; ++num_bookmarks_total;
		itext::PdfDictionary* bookmark_p= new itext::PdfDictionary();
		itext::PRIndirectReference* bookmark_ref_p= reader_p->getPRIndirectReference( bookmark_p );
		if( !bookmark_first_ref_p )
			bookmark_first_ref_p= bookmark_ref_p;
		bookmark_p->put( itext::PdfName::PARENT, (itext::PdfObject*)parent_ref_p );
		if( bookmark_prev_ref_p ) {
			bookmark_p->put( itext::PdfName::PREV, (itext::PdfObject*)bookmark_prev_ref_p );
			bookmark_prev_p->put( itext::PdfName::NEXT, (itext::PdfObject*)bookmark_ref_p );
		}
		if( utf8_b ) {
			bookmark_p->put( itext::PdfName::TITLE,
											 new itext::PdfString( JvNewStringUTF(it->m_title.c_str())  ) );
		}
		else {
			const jsize jvs_size= 4096;
			jchar jvs[jvs_size];
			jsize jvs_len= 0;
			XmlStringToJcharArray( jvs, jvs_size, &jvs_len, it->m_title );
			bookmark_p->put( itext::PdfName::TITLE,
											 new itext::PdfString( JvNewString(jvs, jvs_len)  ) );
		}
		if( 0< it->m_page_num ) {
			itext::PdfDestination* dest_p= new itext::PdfDestination(itext::PdfDestination::FIT);
			itext::PRIndirectReference* page_ref_p= reader_p->getPageOrigRef( it->m_page_num );
			if( page_ref_p ) {
				dest_p->addPage( (itext::PdfIndirectReference*)page_ref_p );
			}
			bookmark_p->put( itext::PdfName::DEST, dest_p );
		}
		bookmark_prev_p= bookmark_p;
		bookmark_prev_ref_p= bookmark_ref_p;
	}
	if( bookmark_first_ref_p && bookmark_prev_ref_p ) {
		parent_p->put( itext::PdfName::FIRST, (itext::PdfObject*)bookmark_first_ref_p );
		parent_p->put( itext::PdfName::LAST, (itext::PdfObject*)bookmark_prev_ref_p );
		if( parent_level== 0 ) {
			parent_p->put( itext::PdfName::COUNT, new itext::PdfNumber( (jint)num_bookmarks_total ) );
		}
		else {
			parent_p->put( itext::PdfName::COUNT, new itext::PdfNumber( (jint)num_bookmarks ) );
		}
	}
	return ret_val;
}
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
								bool utf8_b )
{
	int ret_val= 0;
	itext::PdfDictionary* bookmark_prev_p= after_child_p;
	itext::PdfIndirectReference* bookmark_prev_ref_p= after_child_ref_p;
	itext::PdfIndirectReference* bookmark_first_ref_p= 0;
	int num_bookmarks= 0;
	if( parent_level+ 1< it->m_level ) {
		++num_bookmarks; ++num_bookmarks_total;
		itext::PdfDictionary* bookmark_p= new itext::PdfDictionary();
		itext::PdfIndirectReference* bookmark_ref_p= writer_p->getPdfIndirectReference();
		bookmark_first_ref_p= bookmark_ref_p;
		bookmark_p->put( itext::PdfName::PARENT, (itext::PdfObject*)parent_ref_p );
		itext::PdfString* title_p= new itext::PdfString( JvNewStringUTF("") );
		bookmark_p->put( itext::PdfName::TITLE, title_p );
		bookmark_prev_p= bookmark_p;
		bookmark_prev_ref_p= bookmark_ref_p;
	}
	for( ;it!= it_end; ++it ) {
		if( parent_level+ 1< it->m_level ) {
			ret_val= BuildBookmarks( writer_p,
															 it,
															 it_end,
															 bookmark_prev_p,
															 bookmark_prev_ref_p,
															 0, 0,
															 final_child_p, final_child_ref_p,
															 parent_level+ 1,
															 num_bookmarks_total,
															 page_num_offset,
															 level_offset,
															 utf8_b );
			--it;
			continue;
		}
		else if( it->m_level< parent_level+ 1 ) {
			break;
		}
		++num_bookmarks; ++num_bookmarks_total;
		itext::PdfDictionary* bookmark_p= new itext::PdfDictionary();
		itext::PdfIndirectReference* bookmark_ref_p= writer_p->getPdfIndirectReference();
		if( !bookmark_first_ref_p )
			bookmark_first_ref_p= bookmark_ref_p;
		bookmark_p->put( itext::PdfName::PARENT, (itext::PdfObject*)parent_ref_p );
		if( bookmark_prev_ref_p ) {
			bookmark_p->put( itext::PdfName::PREV, (itext::PdfObject*)bookmark_prev_ref_p );
			bookmark_prev_p->put( itext::PdfName::NEXT, (itext::PdfObject*)bookmark_ref_p );
		}
		if( utf8_b ) {
			bookmark_p->put( itext::PdfName::TITLE,
											 new itext::PdfString( JvNewStringUTF(it->m_title.c_str())  ) );
		}
		else {
			const jsize jvs_size= 4096;
			jchar jvs[jvs_size];
			jsize jvs_len= 0;
			XmlStringToJcharArray( jvs, jvs_size, &jvs_len, it->m_title );
			bookmark_p->put( itext::PdfName::TITLE,
											 new itext::PdfString( JvNewString(jvs, jvs_len)  ) );
		}
		if( 0< it->m_page_num ) {
			itext::PdfDestination* dest_p= new itext::PdfDestination(itext::PdfDestination::FIT);
			itext::PdfIndirectReference* page_ref_p=
				writer_p->getPageReference( it->m_page_num+ page_num_offset );
			if( page_ref_p ) {
				dest_p->addPage( (itext::PdfIndirectReference*)page_ref_p );
			}
			bookmark_p->put( itext::PdfName::DEST, dest_p );
		}
		if( bookmark_prev_p )
			writer_p->addToBody( bookmark_prev_p, bookmark_prev_ref_p );
		bookmark_prev_p= bookmark_p;
		bookmark_prev_ref_p= bookmark_ref_p;
	}
	if( bookmark_prev_p && !after_child_p )
		writer_p->addToBody( bookmark_prev_p, bookmark_prev_ref_p );
	if( bookmark_first_ref_p && bookmark_prev_ref_p ) {
		if( !parent_p->contains( itext::PdfName::FIRST ) )
			parent_p->put( itext::PdfName::FIRST, (itext::PdfObject*)bookmark_first_ref_p );
		parent_p->put( itext::PdfName::LAST, (itext::PdfObject*)bookmark_prev_ref_p );
		if( parent_level== 0 ) {
			parent_p->put( itext::PdfName::COUNT, new itext::PdfNumber( (jint)num_bookmarks_total ) );
		}
		else {
			parent_p->put( itext::PdfName::COUNT, new itext::PdfNumber( (jint)num_bookmarks ) );
		}
	}
	final_child_p= bookmark_prev_p;
	final_child_ref_p= bookmark_prev_ref_p;
	return ret_val;
}
bool
UpdateInfo( itext::PdfReader* reader_p,
						istream& ifs,
						bool utf8_b )
{
	bool ret_val_b= true;
	PdfData pdf_data;
	if( LoadDataFile( ifs, &pdf_data )== 0 ) {
		{
			itext::PdfDictionary* trailer_p= reader_p->getTrailer();
			if( trailer_p && trailer_p->isDictionary() ) {
				if( !pdf_data.m_bookmarks.empty() ) {
					itext::PdfDictionary* outlines_p= new itext::PdfDictionary( itext::PdfName::OUTLINES );
					if( outlines_p ) {
						itext::PRIndirectReference* outlines_ref_p= reader_p->getPRIndirectReference( outlines_p );
						int num_bookmarks_total= 0;
						vector<PdfBookmark>::const_iterator vit= pdf_data.m_bookmarks.begin();
						BuildBookmarks( reader_p,
														vit,
														pdf_data.m_bookmarks.end(),
														outlines_p,
														outlines_ref_p,
														0,
														num_bookmarks_total,
														utf8_b );
						itext::PdfDictionary* root_p= (itext::PdfDictionary*)
							reader_p->getPdfObject( trailer_p->get( itext::PdfName::ROOT ) );
						if( root_p->contains( itext::PdfName::OUTLINES ) ) {
							itext::PdfDictionary* old_outlines_p= (itext::PdfDictionary*)
								reader_p->getPdfObject( root_p->get( itext::PdfName::OUTLINES ) );
							RemoveBookmarks( reader_p, old_outlines_p );
						}
						root_p->put( itext::PdfName::OUTLINES, (itext::PdfObject*)outlines_ref_p );
					}
				}
				if( !pdf_data.m_info.empty() ) {
					itext::PdfDictionary* info_p= (itext::PdfDictionary*)
						reader_p->getPdfObject( trailer_p->get( itext::PdfName::INFO ) );
					if( info_p && info_p->isDictionary() ) {
						for( vector<PdfInfo>::const_iterator it= pdf_data.m_info.begin(); it!= pdf_data.m_info.end(); ++it ) {
							if( it->m_value.empty() ) {
								info_p->remove( new itext::PdfName( JvNewStringUTF(it->m_key.c_str()) ) );
							}
							else {
								if( utf8_b ) {
									info_p->put( new itext::PdfName( JvNewStringUTF(it->m_key.c_str()) ),
															 new itext::PdfString( JvNewStringUTF((char* )it->m_value.c_str()) ) );
								}
								else {
									const jsize jvs_size= 4096;
									jchar jvs[jvs_size];
									jsize jvs_len= 0;
									XmlStringToJcharArray( jvs, jvs_size, &jvs_len, it->m_value );
									info_p->put( new itext::PdfName( JvNewStringUTF(it->m_key.c_str()) ),
															 new itext::PdfString( JvNewString(jvs, jvs_len) ) );
								}
							}
						}
					}
					else {
						cerr << "pdftk Error in UpdateInfo(): no Info dictionary found;" << endl;
						ret_val_b= false;
					}
				}
			}
			else {
				cerr << "pdftk Error in UpdateInfo(): no document trailer found;" << endl;
				ret_val_b= false;
			}
		}
	}
	else {
		cerr << "pdftk Error in UpdateInfo(): LoadDataFile() failure;" << endl;
	}
	return ret_val_b;
}
void
ReportAction( ostream& ofs,
							itext::PdfReader* reader_p,
							itext::PdfDictionary* action_p,
							bool utf8_b, string prefix )
{
	if( action_p->contains( itext::PdfName::S ) ) {
		itext::PdfName* s_p= (itext::PdfName*)
			reader_p->getPdfObject( action_p->get( itext::PdfName::S ) );
		if( s_p->equals( itext::PdfName::URI ) ) {
			ofs << prefix << "ActionSubtype: URI" << endl;
			if( action_p->contains( itext::PdfName::URI ) ) {
				itext::PdfString* uri_p= (itext::PdfString*)
					reader_p->getPdfObject( action_p->get( itext::PdfName::URI ) );
				if( uri_p && uri_p->isString() ) {
					ofs << prefix << "ActionURI: ";
					OutputPdfString( ofs, uri_p, utf8_b );
					ofs << endl;
				}
			}
			if( action_p->contains( itext::PdfName::ISMAP ) ) {
				itext::PdfBoolean* ismap_p= (itext::PdfBoolean*)
					reader_p->getPdfObject( action_p->get( itext::PdfName::ISMAP ) );
				if( ismap_p && ismap_p->isBoolean() )
					if( ismap_p->booleanValue() )
						ofs << prefix << "ActionIsMap: true" << endl;
					else
						ofs << prefix << "ActionIsMap: false" << endl;
			}
			else
				ofs << prefix << "ActionIsMap: false" << endl;
		}
	}
	if( action_p->contains( itext::PdfName::NEXT ) ) {
		itext::PdfObject* next_p= reader_p->getPdfObject( action_p->get( itext::PdfName::NEXT ) );
		if( next_p->isDictionary() ) {
			ReportAction( ofs, reader_p, (itext::PdfDictionary*)next_p, utf8_b, prefix );
		}
		else if( next_p->isArray() ) {
			java::ArrayList* actions_p= ((itext::PdfArray*)next_p)->getArrayList();
			for( jint ii= 0; ii< actions_p->size(); ++ii ) {
				itext::PdfDictionary* action_p= (itext::PdfDictionary*)
					reader_p->getPdfObject( (itext::PdfObject*)actions_p->get(ii) );
				if( action_p && action_p->isDictionary() )
					ReportAction( ofs, reader_p, action_p, utf8_b, prefix );
			}
		}
	}
}
static const int LLx= 0;
static const int LLy= 1;
static const int URx= 2;
static const int URy= 3;
void
ReportAnnot( ostream& ofs,
						 itext::PdfReader* reader_p,
						 int page_num,
						 itext::PdfDictionary* page_p,
						 itext::PdfDictionary* annot_p,
						 bool utf8_b )
{
	itext::PdfName* subtype_p= (itext::PdfName*)
		reader_p->getPdfObject( annot_p->get( itext::PdfName::SUBTYPE ) );
	if( subtype_p && subtype_p->isName() ) {
		ofs << "AnnotSubtype: ";
		OutputPdfName( ofs, subtype_p );
		ofs << endl;
	}
	float rect[4]= { 0.0, 0.0, 0.0, 0.0 };
	itext::PdfArray* rect_p= (itext::PdfArray*)
		reader_p->getPdfObject( annot_p->get( itext::PdfName::RECT ) );
	if( rect_p && rect_p->isArray() ) {
		java::ArrayList* rect_al_p= rect_p->getArrayList();
		if( rect_al_p && rect_al_p->size()== 4 ) {
			for( jint ii= 0; ii< 4; ++ii ) {
				itext::PdfNumber* coord_p= (itext::PdfNumber*)
					reader_p->getPdfObject( (itext::PdfObject*)(rect_al_p->get( ii ) ) );
				if( coord_p && coord_p->isNumber() )
					rect[ ii ]= (float)coord_p->floatValue();
				else
					rect[ ii ]= -1;
			}
		}
	}
	float page_crop_width= 0;
	float page_crop_height= 0;
	{
		itext::Rectangle* page_crop_p= reader_p->getCropBox( page_num );
		rect[0]= rect[0]- page_crop_p->left();
		rect[1]= rect[1]- page_crop_p->bottom();
		rect[2]= rect[2]- page_crop_p->left();
		rect[3]= rect[3]- page_crop_p->bottom();
		page_crop_width= (float)(page_crop_p->right()- page_crop_p->left());
		page_crop_height= (float)(page_crop_p->top()- page_crop_p->bottom());
	}
	int page_rot= (int)(reader_p->getPageRotation( page_num )) % 360;
	float rot_rect[4]= { 0.0, 0.0, 0.0, 0.0 };
	switch( page_rot ) {
	case 90:
		rot_rect[0]= rect[LLy];
		rot_rect[1]= page_crop_width- rect[URx];
		rot_rect[2]= rect[URy];
		rot_rect[3]= page_crop_width- rect[LLx];
		break;
	case 180:
		rot_rect[0]= page_crop_width- rect[URx];
		rot_rect[1]= page_crop_height- rect[URy];
		rot_rect[2]= page_crop_width- rect[LLx];
		rot_rect[3]= page_crop_height- rect[LLy];
		break;
	case 270:
		rot_rect[0]= page_crop_height- rect[URy];
		rot_rect[1]= rect[LLx];
		rot_rect[2]= page_crop_height- rect[LLy];
		rot_rect[3]= rect[URx];
		break;
	default:
		rot_rect[0]= rect[0];
		rot_rect[1]= rect[1];
		rot_rect[2]= rect[2];
		rot_rect[3]= rect[3];
		break;
	}
	ofs << "AnnotRect: " << rot_rect[0] << " " << rot_rect[1];
	ofs << " " << rot_rect[2] << " " << rot_rect[3] << endl;
}
void
ReportAnnots( ostream& ofs,
							itext::PdfReader* reader_p,
							bool utf8_b )
{
	reader_p->resetReleasePage();
	ofs << "NumberOfPages: " << (int)reader_p->getNumberOfPages() << endl;
	itext::PdfDictionary* uri_p= (itext::PdfDictionary*)
		reader_p->getPdfObject( reader_p->catalog->get( itext::PdfName::URI ) );
	if( uri_p && uri_p->isDictionary() ) {
		itext::PdfString* base_p= (itext::PdfString*)
			reader_p->getPdfObject( uri_p->get( itext::PdfName::BASE ) );
		if( base_p && base_p->isString() ) {
			ofs << "PdfUriBase: ";
			OutputPdfString( ofs, base_p, utf8_b );
			ofs << endl;
		}
	}
	for( jint ii= 1; ii<= reader_p->getNumberOfPages(); ++ii ) {
		itext::PdfDictionary* page_p= reader_p->getPageN( ii );
		itext::PdfArray* annots_p= (itext::PdfArray*)
			reader_p->getPdfObject( page_p->get( itext::PdfName::ANNOTS ) );
		if( annots_p && annots_p->isArray() ) {
			java::ArrayList* annots_al_p= annots_p->getArrayList();
			if( annots_al_p ) {
				for( jint jj= 0; jj< annots_al_p->size(); ++jj ) {
					itext::PdfDictionary* annot_p= (itext::PdfDictionary*)
						reader_p->getPdfObject( (itext::PdfObject*)annots_al_p->get( jj ) );
					if( annot_p && annot_p->isDictionary() ) {
						itext::PdfName* type_p= (itext::PdfName*)
							reader_p->getPdfObject( annot_p->get( itext::PdfName::TYPE ) );
						if( type_p->equals( itext::PdfName::ANNOT ) ) {
							itext::PdfName* subtype_p= (itext::PdfName*)
								reader_p->getPdfObject( annot_p->get( itext::PdfName::SUBTYPE ) );
							if( subtype_p->equals( itext::PdfName::LINK ) ) {
								ofs << "---" << endl;
								ReportAnnot( ofs, reader_p, (int)ii, page_p, annot_p, utf8_b );
								ofs << "AnnotPageNumber: " << (int)ii << endl;
								if( annot_p->contains( itext::PdfName::A ) ) {
									itext::PdfDictionary* action_p= (itext::PdfDictionary*)
										reader_p->getPdfObject( annot_p->get( itext::PdfName::A ) );
									if( action_p && action_p->isDictionary() ) {
										ReportAction( ofs, reader_p, action_p, utf8_b, "Annot" );
									}
								}
							}
						}
					}
				}
			}
		}
		reader_p->releasePage( ii );
	}
	reader_p->resetReleasePage();
}