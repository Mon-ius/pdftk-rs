#pragma GCC java_exceptions
#include <gcj/cni.h>
#include <iostream>
#include <fstream>
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
#include <java/util/ArrayList.h>
#include "pdftk/com/lowagie/text/Document.h"
#include "pdftk/com/lowagie/text/Rectangle.h"
#include "pdftk/com/lowagie/text/pdf/PdfName.h"
#include "pdftk/com/lowagie/text/pdf/PdfString.h"
#include "pdftk/com/lowagie/text/pdf/PdfNumber.h"
#include "pdftk/com/lowagie/text/pdf/PdfArray.h"
#include "pdftk/com/lowagie/text/pdf/PdfDictionary.h"
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
#include "pdftk/com/lowagie/text/pdf/PdfAnnotation.h"
#include "pdftk/com/lowagie/text/pdf/PRStream.h"
#include "pdftk/com/lowagie/text/pdf/BaseFont.h"
#include "pdftk/com/lowagie/text/pdf/PdfEncodings.h"
#include <gcj/array.h>
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
#include "attachments.h"
static string
	drop_path( string ss )
{
	const char path_delim= PATH_DELIM;
	string::size_type loc= 0;
	if( (loc=ss.rfind( path_delim ))!= string::npos && loc!= ss.length()- 1 ) {
		return string( ss, loc+ 1 );
	}
	return ss;
}
void
TK_Session::attach_files
( itext::PdfReader* input_reader_p,
	itext::PdfWriter* writer_p )
{
	if( !m_input_attach_file_filename.empty() ) {
		if( m_input_attach_file_pagenum== -1 ) {
			cout << "Please enter the page number you want to attach these files to." << endl;
			cout << "   The first page is 1.  The final page is \"end\"." << endl;
			cout << "   To attach files at the document level, just press Enter." << endl;
			char buff[64];
			cin.getline( buff, 64 );
			if( buff[0]== 0 ) {
				m_input_attach_file_pagenum= 0;
			}
			if( strcmp(buff, "end")== 0 ) {
				m_input_attach_file_pagenum= input_reader_p->getNumberOfPages();
			}
			else {
				m_input_attach_file_pagenum= 0;
				for( int ii= 0; buff[ii] && isdigit(buff[ii]); ++ii ) {
					m_input_attach_file_pagenum= m_input_attach_file_pagenum* 10+ buff[ii]- '0';
				}
			}
		}
		else if( m_input_attach_file_pagenum== -2 ) {
			m_input_attach_file_pagenum= input_reader_p->getNumberOfPages();
		}
		if( m_input_attach_file_pagenum ) {
			static int trans= 27;
			static int margin= 18;
			if( 0< m_input_attach_file_pagenum &&
					m_input_attach_file_pagenum<= input_reader_p->getNumberOfPages() ) {
				itext::PdfDictionary* page_p= input_reader_p->getPageN( m_input_attach_file_pagenum );
				if( page_p && page_p->isDictionary() ) {
					itext::Rectangle* crop_box_p=
						input_reader_p->getCropBox( m_input_attach_file_pagenum );
					float corner_top= crop_box_p->top()- margin;
					float corner_left= crop_box_p->left()+ margin;
					itext::PdfArray* annots_p= (itext::PdfArray*)
						input_reader_p->getPdfObject( page_p->get( itext::PdfName::ANNOTS ) );
					bool annots_new_b= false;
					if( !annots_p ) {
						annots_p= new itext::PdfArray();
						annots_new_b= true;
					}
					else {
						java::ArrayList* annots_array_p= annots_p->getArrayList();
						for( jint ii= 0; ii< annots_array_p->size(); ++ii ) {
							itext::PdfDictionary* annot_p= (itext::PdfDictionary*)
								input_reader_p->getPdfObject( (itext::PdfObject*)annots_array_p->get( ii ) );
							if( annot_p && annot_p->isDictionary() ) {
								itext::PdfArray* annot_bbox_p= (itext::PdfArray*)
									input_reader_p->getPdfObject( annot_p->get( itext::PdfName::RECT ) );
								if( annot_bbox_p && annot_bbox_p->isArray() ) {
									java::ArrayList* bbox_array_p= annot_bbox_p->getArrayList();
									if( bbox_array_p->size()== 4 ) {
										corner_top= ((itext::PdfNumber*)bbox_array_p->get( 1 ))->floatValue();
										corner_left= ((itext::PdfNumber*)bbox_array_p->get( 2 ))->floatValue();
									}
								}
							}
						}
					}
					if( annots_p && annots_p->isArray() ) {
						for( vector< string >::iterator vit= m_input_attach_file_filename.begin();
								 vit!= m_input_attach_file_filename.end(); ++vit )
							{
								if( *vit== "PROMPT" ) {
									prompt_for_filename( "Please enter a filename for attachment:", *vit );
								}
								string filename= drop_path(*vit);
								if( crop_box_p->right() < corner_left+ trans ) {
									corner_left= crop_box_p->left()+ margin;
								}
								if( corner_top- trans< crop_box_p->bottom() ) {
									corner_top= crop_box_p->top()- margin;
								}
								itext::Rectangle* annot_bbox_p=
									new itext::Rectangle( corner_left,
																				corner_top- trans,
																				corner_left+ trans,
																				corner_top );
								itext::PdfAnnotation* annot_p=
									itext::PdfAnnotation::createFileAttachment
									( writer_p,
										annot_bbox_p,
										JvNewStringUTF( filename.c_str() ),
										0,
										JvNewStringUTF( vit->c_str() ),
										JvNewStringUTF( filename.c_str() ) );
								itext::PdfIndirectReference* ref_p=
									writer_p->addToBody( annot_p )->getIndirectReference();
								annots_p->add( ref_p );
								corner_left+= trans;
								corner_top-= trans;
							}
						if( annots_new_b ) {
							itext::PdfIndirectReference* ref_p=
								writer_p->addToBody( annots_p )->getIndirectReference();
							page_p->put( itext::PdfName::ANNOTS, ref_p );
						}
					}
				}
				else {
					cerr << "Internal Error: unable to get page dictionary" << endl;
				}
			}
			else {
				cerr << "Error: page number " << (int)m_input_attach_file_pagenum;
				cerr << " is not present in the input PDF." << endl;
			}
		}
		else {
			itext::PdfDictionary* catalog_p= input_reader_p->catalog;
			if( catalog_p && catalog_p->isDictionary() ) {
				itext::PdfDictionary* names_p= (itext::PdfDictionary*)
					input_reader_p->getPdfObject( catalog_p->get( itext::PdfName::NAMES ) );
				bool names_new_b= false;
				if( !names_p ) {
					names_p= new itext::PdfDictionary();
					names_new_b= true;
				}
				if( names_p && names_p->isDictionary() ) {
					itext::PdfDictionary* emb_files_tree_p= (itext::PdfDictionary*)
						input_reader_p->getPdfObject( names_p->get( itext::PdfName::EMBEDDEDFILES ) );
					java::HashMap* emb_files_map_p= 0;
					bool emb_files_tree_new_b= false;
					if( emb_files_tree_p ) {
						emb_files_map_p= itext::PdfNameTree::readTree( emb_files_tree_p );
					}
					else {
						emb_files_map_p= new java::HashMap();
						emb_files_tree_new_b= true;
					}
					for( vector< string >::iterator vit= m_input_attach_file_filename.begin();
							 vit!= m_input_attach_file_filename.end(); ++vit )
						{
							if( *vit== "PROMPT" ) {
								prompt_for_filename( "Please enter a filename for attachment:", *vit );
							}
							string filename= drop_path(*vit);
							itext::PdfFileSpecification* filespec_p= 0;
							try {
								filespec_p=
									itext::PdfFileSpecification::fileEmbedded
									( writer_p,
										JvNewStringUTF( vit->c_str() ),
										JvNewStringUTF( filename.c_str() ),
										0 );
							}
							catch( java::io::IOException* ioe_p ) {
								cerr << "Error: Failed to open attachment file: " << endl;
								cerr << "   " << *vit << endl;
								cerr << "   Skipping this file." << endl;
								continue;
							}
							itext::PdfIndirectReference* ref_p=
								writer_p->addToBody( filespec_p )->getIndirectReference();
							java::String* key_p=
								JvNewStringUTF( vit->c_str() );
							{
								int counter= 1;
								while( emb_files_map_p->containsKey( key_p ) ) {
									char buff[256];
									sprintf( buff, "-%d", counter++ );
									key_p=
										JvNewStringUTF( ( *vit + buff ).c_str() );
								}
							}
							emb_files_map_p->put( key_p, ref_p );
						}
					if( !emb_files_map_p->isEmpty() ) {
						itext::PdfDictionary* emb_files_tree_new_p=
							itext::PdfNameTree::writeTree( emb_files_map_p, writer_p );
						if( emb_files_tree_new_b && emb_files_tree_new_p ) {
							itext::PdfIndirectReference* ref_p=
								writer_p->addToBody( emb_files_tree_new_p )->getIndirectReference();
							names_p->put( itext::PdfName::EMBEDDEDFILES, ref_p );
						}
						else if( emb_files_tree_p && emb_files_tree_new_p ) {
							emb_files_tree_p->merge( emb_files_tree_new_p );
						}
						else {
							cerr << "Internal Error: no valid EmbeddedFiles tree to add to PDF." << endl;
						}
						if( names_new_b ) {
							itext::PdfIndirectReference* ref_p=
								writer_p->addToBody( names_p )->getIndirectReference();
							catalog_p->put( itext::PdfName::NAMES, ref_p );
						}
					}
				}
				else {
					cerr << "Internal Error: couldn't read or create PDF Names dictionary." << endl;
				}
			}
			else {
				cerr << "Internal Error: couldn't read input PDF Root dictionary." << endl;
				cerr << "   File attachment failed; no new files attached to output." << endl;
			}
		}
	}
}
static string
normalize_pathname( string output_pathname )
{
	const char path_delim= PATH_DELIM;
	if( output_pathname== "PROMPT" ) {
		prompt_for_filename( "Please enter the directory where you want attachments unpacked:",
												 output_pathname );
	}
	if( output_pathname.rfind( path_delim )== output_pathname.length()- 1 ) {
		return output_pathname;
	}
	else{
		return output_pathname+ (char)PATH_DELIM;
	}
}
static void
unpack_file( itext::PdfReader* input_reader_p,
						 itext::PdfDictionary* filespec_p,
						 string output_pathname,
						 bool ask_about_warnings_b )
{
	if( filespec_p && filespec_p->isDictionary() ) {
		itext::PdfName* type_p= (itext::PdfName*)
			input_reader_p->getPdfObject( filespec_p->get( itext::PdfName::TYPE ) );
		if( type_p && type_p->isName() &&
				( type_p->compareTo( itext::PdfName::FILESPEC )== 0 ||
					type_p->compareTo( itext::PdfName::F )== 0 ) )
			{
				itext::PdfDictionary* ef_p= (itext::PdfDictionary*)
					input_reader_p->getPdfObject( filespec_p->get( itext::PdfName::EF ) );
				if( ef_p && ef_p->isDictionary() ) {
					itext::PdfString* fn_p= (itext::PdfString*)
						input_reader_p->getPdfObject( filespec_p->get( itext::PdfName::UF ) );
					if( !fn_p ) {
						fn_p= (itext::PdfString*)
						input_reader_p->getPdfObject( filespec_p->get( itext::PdfName::F ) );
					}
					if( fn_p && fn_p->isString() ) {
						jstring fn_str = fn_p->toUnicodeString();
						int fn_buff_len = JvGetStringUTFLength( fn_str );
						char* fn_buff= (char*)malloc( fn_buff_len* sizeof(char) );
						JvGetStringUTFRegion( fn_str, 0, fn_str->length(), fn_buff );
						string fn= drop_path( string( fn_buff, fn_buff_len ) );
						free( fn_buff );
						if( !output_pathname.empty() ) {
							fn= output_pathname+ fn;
						}
						itext::PdfStream* f_p= (itext::PdfStream*)
							input_reader_p->getPdfObject( ef_p->get( itext::PdfName::F ) );
						if( f_p && f_p->isStream() ) {
							jbyteArray byte_arr_p= input_reader_p->getStreamBytes( (itext::PRStream*)f_p );
							const jbyte* bytes_p= elements(byte_arr_p);
							jsize num_bytes= byte_arr_p->length;
							if( ask_about_warnings_b ) {
								bool output_exists_b= false;
								{
									FILE* fp= fopen( fn.c_str(), "rb" );
									if( fp ) {
										output_exists_b= true;
										fclose( fp );
									}
								}
								if( output_exists_b ) {
									cout << "Warning: the file: " << fn << " already exists.  Overwrite? (y/n)" << endl;
									char buff[64];
									cin.getline( buff, 64 );
									if( buff[0]!= 'y' && buff[0]!= 'Y' ) {
										cout << "   Skipping: " << fn << endl;
										return;
									}
								}
							}
							ofstream ofs( fn.c_str(), ios_base::binary | ios_base::out );
							if( ofs ) {
								ofs.write( (const char*)bytes_p, num_bytes );
								ofs.close();
							}
							else {
								cerr << "Error: unable to create the file:" << endl;
								cerr << "   " << fn << endl;
								cerr << "   Skipping." << endl;
							}
						}
					}
				}
			}
	}
}
void
TK_Session::unpack_files
( itext::PdfReader* input_reader_p )
{
	string output_pathname= normalize_pathname( m_output_filename );
	{
		itext::PdfDictionary* catalog_p= input_reader_p->catalog;
		if( catalog_p && catalog_p->isDictionary() ) {
			itext::PdfDictionary* names_p= (itext::PdfDictionary*)
				input_reader_p->getPdfObject( catalog_p->get( itext::PdfName::NAMES ) );
			if( names_p && names_p->isDictionary() ) {
				itext::PdfDictionary* emb_files_tree_p= (itext::PdfDictionary*)
					input_reader_p->getPdfObject( names_p->get( itext::PdfName::EMBEDDEDFILES ) );
				java::HashMap* emb_files_map_p= 0;
				if( emb_files_tree_p && emb_files_tree_p->isDictionary() ) {
					emb_files_map_p= itext::PdfNameTree::readTree( emb_files_tree_p );
					for( java::Iterator* jit= emb_files_map_p->keySet()->iterator(); jit->hasNext(); ) {
						java::String* key_p= (java::String*)jit->next();
						itext::PdfDictionary* filespec_p= (itext::PdfDictionary*)
							input_reader_p->getPdfObject( (itext::PdfObject*)(emb_files_map_p->get(key_p)) );
						if( filespec_p && filespec_p->isDictionary() ) {
							unpack_file( input_reader_p,
													 filespec_p,
													 output_pathname,
													 m_ask_about_warnings_b );
						}
					}
				}
			}
		}
	}
	{
		jint num_pages= input_reader_p->getNumberOfPages();
		for( jint ii= 1; ii<= num_pages; ++ii ) {
				itext::PdfDictionary* page_p= input_reader_p->getPageN( ii );
				if( page_p && page_p->isDictionary() ) {
					itext::PdfArray* annots_p= (itext::PdfArray*)
						input_reader_p->getPdfObject( page_p->get( itext::PdfName::ANNOTS ) );
					if( annots_p && annots_p->isArray() ) {
						java::ArrayList* annots_array_p= annots_p->getArrayList();
						for( jint jj= 0; jj< annots_array_p->size(); ++jj ) {
							itext::PdfDictionary* annot_p= (itext::PdfDictionary*)
								input_reader_p->getPdfObject( (itext::PdfObject*)annots_array_p->get( jj ) );
							if( annot_p && annot_p->isDictionary() ) {
								itext::PdfName* subtype_p= (itext::PdfName*)
									input_reader_p->getPdfObject( annot_p->get( itext::PdfName::SUBTYPE ) );
								if( subtype_p && subtype_p->isName() &&
										subtype_p->equals(itext::PdfName::FILEATTACHMENT) ) {
									itext::PdfDictionary* filespec_p= (itext::PdfDictionary*)
										input_reader_p->getPdfObject( annot_p->get( itext::PdfName::FS ) );
									if( filespec_p && filespec_p->isDictionary() ) {
										unpack_file( input_reader_p,
																 filespec_p,
																 output_pathname,
																 m_ask_about_warnings_b );
									}
								}
							}
						}
					}
				}
		}
	}
}