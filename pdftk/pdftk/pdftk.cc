#pragma GCC java_exceptions
#include <gcj/cni.h>
#ifdef UNBLOCK_SIGNALS
#include <signal.h>
#endif
#include <iostream>
#include <fstream>
#include <string>
#include <map>
#include <vector>
#include <set>
#include <algorithm>
#include <unistd.h>
#include <gnu/gcj/convert/Input_UTF8.h>
#include <gnu/gcj/convert/Input_8859_1.h>
#include <gnu/gcj/convert/Input_ASCII.h>
#include <java/lang/System.h>
#include <java/lang/ClassCastException.h>
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
#include <java/util/Locale.h>
#include <java/util/TimeZone.h>
#include <java/util/Calendar.h>
#include <java/util/GregorianCalendar.h>
#ifdef WIN32
#include <gnu/java/locale/Calendar.h>
#include <gnu/java/locale/LocaleInformation.h>
#include <gnu/java/locale/Calendar_de.h>
#include <gnu/java/locale/Calendar_en.h>
#include <gnu/java/locale/Calendar_nl.h>
#include <gnu/java/locale/LocaleInformation_de.h>
#include <gnu/java/locale/LocaleInformation_en.h>
#include <gnu/java/locale/LocaleInformation_nl.h>
#endif
#include "pdftk/com/lowagie/text/Document.h"
#include "pdftk/com/lowagie/text/Rectangle.h"
#include "pdftk/com/lowagie/text/pdf/PdfName.h"
#include "pdftk/com/lowagie/text/pdf/PdfString.h"
#include "pdftk/com/lowagie/text/pdf/PdfNumber.h"
#include "pdftk/com/lowagie/text/pdf/PdfArray.h"
#include "pdftk/com/lowagie/text/pdf/PdfDictionary.h"
#include "pdftk/com/lowagie/text/pdf/PdfStream.h"
#include "pdftk/com/lowagie/text/pdf/PdfOutline.h"
#include "pdftk/com/lowagie/text/pdf/PdfCopy.h"
#include "pdftk/com/lowagie/text/pdf/PdfReader.h"
#include "pdftk/com/lowagie/text/pdf/PdfImportedPage.h"
#include "pdftk/com/lowagie/text/pdf/PdfWriter.h"
#include "pdftk/com/lowagie/text/pdf/PdfStamperImp.h"
#include "pdftk/com/lowagie/text/pdf/PdfNameTree.h"
#include "pdftk/com/lowagie/text/pdf/PdfAcroForm.h"
#include "pdftk/com/lowagie/text/pdf/FdfReader.h"
#include "pdftk/com/lowagie/text/pdf/FdfWriter.h"
#include "pdftk/com/lowagie/text/pdf/XfdfReader.h"
#include "pdftk/com/lowagie/text/pdf/AcroFields.h"
#include "pdftk/com/lowagie/text/pdf/PdfIndirectReference.h"
#include "pdftk/com/lowagie/text/pdf/PdfIndirectObject.h"
#include "pdftk/com/lowagie/text/pdf/PdfFileSpecification.h"
#include "pdftk/com/lowagie/text/pdf/PdfBoolean.h"
#include "pdftk/com/lowagie/text/pdf/PdfDestination.h"
#include "pdftk/com/lowagie/text/pdf/PdfAnnotation.h"
#include "pdftk/com/lowagie/text/pdf/RandomAccessFileOrArray.h"
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
#ifdef WIN32
#include <windows.h>
#endif
#include "pdftk.h"
#include "attachments.h"
#include "report.h"
#include "passwords.h"
static java::Vector* g_dont_collect_p= 0;
static void
describe_header();
static void
describe_synopsis();
static void
describe_full();
static void
prompt_for_password( const string pass_name,
										 const string pass_app,
										 string& password )
{
	cout << "Please enter the " << pass_name << " password to use on " << pass_app << "." << endl;
	cout << "   It can be empty, or have a maximum of 32 characters:" << endl;
	char buff[64];
	cin.getline( buff, 64 );
	password= buff;
	if( 32< password.size() ) {
		cout << "The password you entered was over 32 characters long," << endl;
		cout << "   so I am dropping: \"" << string(password, 32 ) << "\"" << endl;
		password= string( password, 0, 32 );
	}
}
void
prompt_for_filename( const string message,
										 string& fn )
{
	char cc= '\n';
	fn= "";
	cout << message << endl;
  while( cin.get( cc ) && cc!= '\n' ) { fn+= cc; }
}
void
copy_argv_as_utf8( string& ss, char** argv, int ii )
{
#ifdef WIN32
	ss= argv[ii];
#else
	ss= argv[ii];
#endif
}
bool
TK_Session::add_reader( InputPdf* input_pdf_p,
												bool keep_artifacts_b= false )
{
	bool open_success_b= true;
	try {
		itext::PdfReader* reader= 0;
		if( input_pdf_p->m_filename== "PROMPT" ) {
			prompt_for_filename( "Please enter a filename for an input PDF:",
													 input_pdf_p->m_filename );
		}
		if( input_pdf_p->m_password.empty() ) {
			reader= new itext::PdfReader( JvNewStringUTF( input_pdf_p->m_filename.c_str() ) );
		}
		else {
			if( input_pdf_p->m_password== "PROMPT" ) {
				prompt_for_password( "open", "the input PDF:\n   "+ input_pdf_p->m_filename, input_pdf_p->m_password );
			}
			int size= utf8_password_to_pdfdoc( 0, input_pdf_p->m_password.c_str(), input_pdf_p->m_password.size(),
																				 false );
			if( 0<= size ) {
				jbyteArray password= JvNewByteArray( size );
				utf8_password_to_pdfdoc( elements(password), input_pdf_p->m_password.c_str(), input_pdf_p->m_password.size(),
																 false );
				reader= new itext::PdfReader( JvNewStringUTF( input_pdf_p->m_filename.c_str() ), password );
				if( reader== 0 ) {
					cerr << "Error: Unexpected null from open_reader()" << endl;
					return false;
				}
			}
			else {
				cerr << "Error: Password used to decrypt input PDF:" << endl;
				cerr << "   " << input_pdf_p->m_filename << endl;
				cerr << "   includes invalid characters." << endl;
				return false;
			}
		}
		if( !keep_artifacts_b ) {
			reader->consolidateNamedDestinations();
			reader->removeUnusedObjects();
		}
		input_pdf_p->m_num_pages= reader->getNumberOfPages();
		input_pdf_p->m_readers.push_back( pair< set<jint>, itext::PdfReader* >( set<jint>(), reader ) );
		g_dont_collect_p->addElement( reader );
		input_pdf_p->m_authorized_b= ( !reader->encrypted || reader->ownerPasswordUsed );
		if( !input_pdf_p->m_authorized_b ) {
			open_success_b= false;
		}
	}
	catch( java::io::IOException* ioe_p ) {
		if( ioe_p->getMessage()->equals( JvNewStringUTF( "Bad password" ) ) ) {
			input_pdf_p->m_authorized_b= false;
		}
		else if( ioe_p->getMessage()->indexOf( JvNewStringUTF( "not found" ) )!= -1 ) {
			cerr << "Error: Unable to find file." << endl;
		}
		else {
			cerr << "Error: Unexpected Exception in open_reader()" << endl;
			ioe_p->printStackTrace();
		}
		open_success_b= false;
	}
	catch( java::lang::Throwable* t_p ) {
		cerr << "Error: Unexpected Exception in open_reader()" << endl;
		t_p->printStackTrace();
		open_success_b= false;
	}
	if( !input_pdf_p->m_authorized_b && m_ask_about_warnings_b ) {
		cerr << "The password you supplied for the input PDF:" << endl;
		cerr << "   " << input_pdf_p->m_filename << endl;
		cerr << "   did not work.  This PDF is encrypted, and you must supply the" << endl;
		cerr << "   owner password to open it.  If it has no owner password, then" << endl;
		cerr << "   enter the user password, instead.  To quit, enter a blank password" << endl;
		cerr << "   at the next prompt." << endl;
		prompt_for_password( "open", "the input PDF:\n   "+ input_pdf_p->m_filename, input_pdf_p->m_password );
		if( !input_pdf_p->m_password.empty() ) {
			input_pdf_p->m_authorized_b= true;
			return( add_reader(input_pdf_p) );
		}
	}
	if( !open_success_b ) {
		cerr << "Error: Failed to open PDF file: " << endl;
		cerr << "   " << input_pdf_p->m_filename << endl;
		if( !input_pdf_p->m_authorized_b ) {
			cerr << "   OWNER PASSWORD REQUIRED, but not given (or incorrect)" << endl;
		}
	}
	m_authorized_b= m_authorized_b && input_pdf_p->m_authorized_b;
	return open_success_b;
}
bool
TK_Session::open_input_pdf_readers()
{
	bool open_success_b= true;
	if( !m_input_pdf_readers_opened_b ) {
		if( m_operation== filter_k && m_input_pdf.size()== 1 ) {
			open_success_b= add_reader( &(*(m_input_pdf.begin())), true );
		}
		else {
			for( vector< InputPdf >::iterator it= m_input_pdf.begin(); it!= m_input_pdf.end(); ++it ) {
				open_success_b= add_reader( &(*it) ) && open_success_b;
			}
		}
		m_input_pdf_readers_opened_b= open_success_b;
	}
	return open_success_b;
}
static int
copy_downcase( char* ll, int ll_len,
							 char* rr )
{
  int ii= 0;
  for( ; rr[ii] && ii< ll_len- 1; ++ii ) {
    ll[ii]=
      ( 'A'<= rr[ii] && rr[ii]<= 'Z' ) ?
      rr[ii]- ('A'- 'a') :
      rr[ii];
  }
  ll[ii]= 0;
	return ii;
}
TK_Session::keyword
TK_Session::is_keyword( char* ss, int* keyword_len_p )
{
  *keyword_len_p= 0;
  const int ss_copy_max= 256;
  char ss_copy[ss_copy_max]= "";
  int ss_copy_size= copy_downcase( ss_copy, ss_copy_max, ss );
	*keyword_len_p= ss_copy_size;
  if( strcmp( ss_copy, "cat" )== 0 ) {
    return cat_k;
  }
	else if( strcmp( ss_copy, "shuffle" )== 0 ) {
		return shuffle_k;
	}
	else if( strcmp( ss_copy, "burst" )== 0 ) {
		return burst_k;
	}
#ifdef BARCODE_BURST
	else if( strcmp( ss_copy, "barcode_burst" )== 0 ) {
		return barcode_burst_k;
	}
#endif
	else if( strcmp( ss_copy, "filter" )== 0 ) {
		return filter_k;
	}
	else if( strcmp( ss_copy, "dump_data" )== 0 ||
					 strcmp( ss_copy, "dumpdata" )== 0 ||
					 strcmp( ss_copy, "data_dump" )== 0 ||
					 strcmp( ss_copy, "datadump" )== 0 ) {
		return dump_data_k;
	}
	else if( strcmp( ss_copy, "dump_data_utf8" )== 0 ) {
		return dump_data_utf8_k;
	}
	else if( strcmp( ss_copy, "dump_data_fields" )== 0 ) {
		return dump_data_fields_k;
	}
	else if( strcmp( ss_copy, "dump_data_fields_utf8" )== 0 ) {
		return dump_data_fields_utf8_k;
	}
	else if( strcmp( ss_copy, "dump_data_annots" )== 0 ) {
		return dump_data_annots_k;
	}
	else if( strcmp( ss_copy, "generate_fdf" )== 0 ||
					 strcmp( ss_copy, "fdfgen" )== 0 ||
					 strcmp( ss_copy, "fdfdump" )== 0 ||
					 strcmp( ss_copy, "dump_data_fields_fdf" )== 0 ) {
		return generate_fdf_k;
	}
	else if( strcmp( ss_copy, "fill_form" )== 0 ||
					 strcmp( ss_copy, "fillform" )== 0 ) {
		return fill_form_k;
	}
	else if( strcmp( ss_copy, "attach_file" )== 0 ||
					 strcmp( ss_copy, "attach_files" )== 0 ||
					 strcmp( ss_copy, "attachfile" )== 0 ) {
		return attach_file_k;
	}
	else if( strcmp( ss_copy, "unpack_file" )== 0 ||
					 strcmp( ss_copy, "unpack_files" )== 0 ||
					 strcmp( ss_copy, "unpackfiles" )== 0 ) {
		return unpack_files_k;
	}
	else if( strcmp( ss_copy, "update_info" )== 0 ||
					 strcmp( ss_copy, "undateinfo" )== 0 ) {
		return update_info_k;
	}
	else if( strcmp( ss_copy, "update_info_utf8" )== 0 ||
					 strcmp( ss_copy, "undateinfoutf8" )== 0 ) {
		return update_info_utf8_k;
	}
	else if( strcmp( ss_copy, "background" )== 0 ) {
		return background_k;
	}
	else if( strcmp( ss_copy, "multibackground" )== 0 ) {
		return multibackground_k;
	}
	else if( strcmp( ss_copy, "multistamp" )== 0 ) {
		return multistamp_k;
	}
	else if( strcmp( ss_copy, "stamp" )== 0 ) {
		return stamp_k;
	}
	else if( strcmp( ss_copy, "rotate" )== 0 ) {
		return rotate_k;
	}
  else if( strncmp( ss_copy, "end", 3 )== 0 ) {
    *keyword_len_p= 3;
    return end_k;
  }
  else if( strncmp( ss_copy, "even", 4 )== 0 ) {
    *keyword_len_p= 4;
    return even_k;
  }
  else if( strncmp( ss_copy, "odd", 3 )== 0 ) {
    *keyword_len_p= 3;
    return odd_k;
  }
	else if( strcmp( ss_copy, "to_page" )== 0 ||
					 strcmp( ss_copy, "topage" )== 0 ) {
		return attach_file_to_page_k;
	}
  else if( strcmp( ss_copy, "output" )== 0 ) {
    return output_k;
  }
	else if( strcmp( ss_copy, "owner_pw" )== 0 ||
					 strcmp( ss_copy, "ownerpw" )== 0 ) {
		return owner_pw_k;
	}
	else if( strcmp( ss_copy, "user_pw" )== 0 ||
					 strcmp( ss_copy, "userpw" )== 0 ) {
		return user_pw_k;
	}
	else if( strcmp( ss_copy, "input_pw" )== 0 ||
					 strcmp( ss_copy, "inputpw" )== 0 ) {
		return input_pw_k;
	}
	else if( strcmp( ss_copy, "allow" )== 0 ) {
		return user_perms_k;
	}
	else if( strcmp( ss_copy, "encrypt_40bit" )== 0 ||
					 strcmp( ss_copy, "encrypt_40bits" )== 0 ||
					 strcmp( ss_copy, "encrypt40bit" )== 0 ||
					 strcmp( ss_copy, "encrypt40bits" )== 0 ||
					 strcmp( ss_copy, "encrypt40_bit" )== 0 ||
					 strcmp( ss_copy, "encrypt40_bits" )== 0 ||
					 strcmp( ss_copy, "encrypt_40_bit" )== 0 ||
					 strcmp( ss_copy, "encrypt_40_bits" )== 0 ) {
		return encrypt_40bit_k;
	}
	else if( strcmp( ss_copy, "encrypt_128bit" )== 0 ||
					 strcmp( ss_copy, "encrypt_128bits" )== 0 ||
					 strcmp( ss_copy, "encrypt128bit" )== 0 ||
					 strcmp( ss_copy, "encrypt128bits" )== 0 ||
					 strcmp( ss_copy, "encrypt128_bit" )== 0 ||
					 strcmp( ss_copy, "encrypt128_bits" )== 0 ||
					 strcmp( ss_copy, "encrypt_128_bit" )== 0 ||
					 strcmp( ss_copy, "encrypt_128_bits" )== 0 ) {
		return encrypt_128bit_k;
	}
	else if( strcmp( ss_copy, "printing" )== 0 ) {
		return perm_printing_k;
	}
	else if( strcmp( ss_copy, "modifycontents" )== 0 ) {
		return perm_modify_contents_k;
	}
	else if( strcmp( ss_copy, "copycontents" )== 0 ) {
		return perm_copy_contents_k;
	}
	else if( strcmp( ss_copy, "modifyannotations" )== 0 ) {
		return perm_modify_annotations_k;
	}
	else if( strcmp( ss_copy, "fillin" )== 0 ) {
		return perm_fillin_k;
	}
	else if( strcmp( ss_copy, "screenreaders" )== 0 ) {
		return perm_screen_readers_k;
	}
	else if( strcmp( ss_copy, "assembly" )== 0 ) {
		return perm_assembly_k;
	}
	else if( strcmp( ss_copy, "degradedprinting" )== 0 ) {
		return perm_degraded_printing_k;
	}
	else if( strcmp( ss_copy, "allfeatures" )== 0 ) {
		return perm_all_k;
	}
	else if( strcmp( ss_copy, "uncompress" )== 0 ) {
		return filt_uncompress_k;
	}
	else if( strcmp( ss_copy, "compress" )== 0 ) {
		return filt_compress_k;
	}
	else if( strcmp( ss_copy, "flatten" )== 0 ) {
		return flatten_k;
	}
	else if( strcmp( ss_copy, "need_appearances" )== 0 ) {
		return need_appearances_k;
	}
	else if( strcmp( ss_copy, "drop_xfa" )== 0 ) {
		return drop_xfa_k;
	}
	else if( strcmp( ss_copy, "drop_xmp" )== 0 ) {
		return drop_xmp_k;
	}
	else if( strcmp( ss_copy, "keep_first_id" )== 0 ) {
		return keep_first_id_k;
	}
	else if( strcmp( ss_copy, "keep_final_id" )== 0 ) {
		return keep_final_id_k;
	}
	else if( strcmp( ss_copy, "verbose" )== 0 ) {
		return verbose_k;
	}
	else if( strcmp( ss_copy, "dont_ask" )== 0 ||
					 strcmp( ss_copy, "dontask" )== 0 ) {
		return dont_ask_k;
	}
	else if( strcmp( ss_copy, "do_ask" )== 0 ) {
		return do_ask_k;
	}
	else if( strcmp( ss_copy, "north" )== 0 ) {
    *keyword_len_p= 5;
		return rot_north_k;
	}
	else if( strcmp( ss_copy, "south" )== 0 ) {
    *keyword_len_p= 5;
		return rot_south_k;
	}
	else if( strcmp( ss_copy, "east" )== 0 ) {
    *keyword_len_p= 4;
		return rot_east_k;
	}
	else if( strcmp( ss_copy, "west" )== 0 ) {
    *keyword_len_p= 4;
		return rot_west_k;
	}
	else if( strcmp( ss_copy, "left" )== 0 ) {
    *keyword_len_p= 4;
		return rot_left_k;
	}
	else if( strcmp( ss_copy, "right" )== 0 ) {
    *keyword_len_p= 5;
		return rot_right_k;
	}
	else if( strcmp( ss_copy, "down" )== 0 ) {
    *keyword_len_p= 4;
		return rot_upside_down_k;
	}
  return none_k;
}
bool
TK_Session::is_valid() const
{
	return( m_valid_b &&
					( m_operation== dump_data_k ||
						m_operation== dump_data_fields_k ||
						m_operation== dump_data_annots_k ||
						m_operation== generate_fdf_k ||
						m_authorized_b ) &&
					!m_input_pdf.empty() &&
					m_input_pdf_readers_opened_b &&
					first_operation_k<= m_operation &&
					m_operation<= final_operation_k &&
					( !( m_operation== burst_k ||
#ifdef BARCODE_BURST
							 m_operation== barcode_burst_k ||
#endif
							 m_operation== filter_k ) ||
						( m_input_pdf.size()== 1 ) ) &&
					( m_operation== burst_k ||
#ifdef BARCODE_BURST
					  m_operation== barcode_burst_k ||
#endif
					  m_operation== dump_data_k ||
						m_operation== dump_data_fields_k ||
						m_operation== dump_data_annots_k ||
						m_operation== generate_fdf_k ||
						m_operation== unpack_files_k ||
					  !m_output_filename.empty() ) );
}
void
TK_Session::dump_session_data() const
{
	if( !m_verbose_reporting_b )
		return;
	if( !m_input_pdf_readers_opened_b ) {
		cout << "Input PDF Open Errors" << endl;
		return;
	}
	if( is_valid() ) {
		cout << "Command Line Data is valid." << endl;
	}
	else {
		cout << "Command Line Data is NOT valid." << endl;
	}
	cout << endl;
	cout << "Input PDF Filenames & Passwords in Order\n( <filename>[, <password>] ) " << endl;
	if( m_input_pdf.empty() ) {
		cout << "   No input PDF filenames have been given." << endl;
	}
	else {
		for( vector< InputPdf >::const_iterator it= m_input_pdf.begin();
				 it!= m_input_pdf.end(); ++it )
			{
				cout << "   " << it->m_filename;
				if( !it->m_password.empty() ) {
					cout << ", " << it->m_password;
				}
				if( !it->m_authorized_b ) {
					cout << ", OWNER PASSWORD REQUIRED, but not given (or incorrect)";
				}
				cout << endl;
			}
	}
	cout << endl;
	cout << "The operation to be performed: " << endl;
	switch( m_operation ) {
	case cat_k:
		cout << "   cat - Catenate given page ranges into a new PDF." << endl;
		break;
	case shuffle_k:
		cout << "   shuffle - Interleave given page ranges into a new PDF." << endl;
		break;
	case burst_k:
		cout << "   burst - Split a single, input PDF into individual pages." << endl;
		break;
#ifdef BARCODE_BURST
	case barcode_burst_k:
		cout << "   barcode_burst - Split a single, input PDF into individual pages" << endl;
		cout << "      based on the presence of barcode." << endl;
		break;
#endif
	case filter_k:
		cout << "   filter - Apply 'filters' to a single, input PDF based on output args." << endl;
		cout << "      (When the operation is omitted, this is the default.)" << endl;
		break;
	case dump_data_k:
		cout << "   dump_data - Report statistics on a single, input PDF." << endl;
		break;
	case dump_data_fields_k:
		cout << "   dump_data_fields - Report form field data on a single, input PDF." << endl;
		break;
	case dump_data_annots_k:
		cout << "   dump_data_annots - Report annotation data on a single, input PDF." << endl;
		break;
	case generate_fdf_k:
		cout << "   generate_fdf - Generate a dummy FDF file from a PDF." << endl;
		break;
	case unpack_files_k:
		cout << "   unpack_files - Copy PDF file attachments into given directory." << endl;
		break;
	case none_k:
		cout << "   NONE - No operation has been given.  See usage instructions." << endl;
		break;
	default:
		cout << "   INTERNAL ERROR - An unexpected operation has been given." << endl;
		break;
	}
	cout << endl;
	cout << "The output file will be named:" << endl;
	if( m_output_filename.empty() ) {
		cout << "   No output filename has been given." << endl;
	}
	else {
		cout << "   " << m_output_filename << endl;
	}
	cout << endl;
	bool output_encrypted_b=
		m_output_encryption_strength!= none_enc ||
		!m_output_user_pw.empty() ||
		!m_output_owner_pw.empty();
	cout << "Output PDF encryption settings:" << endl;
	if( output_encrypted_b ) {
		cout << "   Output PDF will be encrypted." << endl;
		switch( m_output_encryption_strength ) {
		case none_enc:
			cout << "   Encryption strength not given. Defaulting to: 128 bits." << endl;
			break;
		case bits40_enc:
			cout << "   Given output encryption strength: 40 bits" << endl;
			break;
		case bits128_enc:
			cout << "   Given output encryption strength: 128 bits" << endl;
			break;
		}
		cout << endl;
		{
			using itext::PdfWriter;
			if( m_output_user_pw.empty() )
				cout << "   No user password given." << endl;
			else
				cout << "   Given user password: " << m_output_user_pw << endl;
			if( m_output_owner_pw.empty() )
				cout << "   No owner password given." << endl;
			else
				cout << "   Given owner password: " << m_output_owner_pw << endl;
			if( (m_output_user_perms & PdfWriter::AllowPrinting)== PdfWriter::AllowPrinting )
				cout << "   ALLOW Top Quality Printing" << endl;
			else if( (m_output_user_perms & PdfWriter::AllowPrinting)== PdfWriter::AllowDegradedPrinting )
				cout << "   ALLOW Degraded Printing (Top-Quality Printing NOT Allowed)" << endl;
			else
				cout << "   Printing NOT Allowed" << endl;
			if( (m_output_user_perms & PdfWriter::AllowModifyContents)== PdfWriter::AllowModifyContents )
				cout << "   ALLOW Modifying of Contents" << endl;
			else
				cout << "   Modifying of Contents NOT Allowed" << endl;
			if( (m_output_user_perms & PdfWriter::AllowCopy)== PdfWriter::AllowCopy )
				cout << "   ALLOW Copying of Contents" << endl;
			else
				cout << "   Copying of Contents NOT Allowed" << endl;
			if( (m_output_user_perms & PdfWriter::AllowModifyAnnotations)== PdfWriter::AllowModifyAnnotations )
				cout << "   ALLOW Modifying of Annotations" << endl;
			else
				cout << "   Modifying of Annotations NOT Allowed" << endl;
			if( (m_output_user_perms & PdfWriter::AllowFillIn)== PdfWriter::AllowFillIn )
				cout << "   ALLOW Fill-In" << endl;
			else
				cout << "   Fill-In NOT Allowed" << endl;
			if( (m_output_user_perms & PdfWriter::AllowScreenReaders)== PdfWriter::AllowScreenReaders )
				cout << "   ALLOW Screen Readers" << endl;
			else
				cout << "   Screen Readers NOT Allowed" << endl;
			if( (m_output_user_perms & PdfWriter::AllowAssembly)== PdfWriter::AllowAssembly )
				cout << "   ALLOW Assembly" << endl;
			else
				cout << "   Assembly NOT Allowed" << endl;
		}
	}
	else {
		cout << "   Output PDF will not be encrypted." << endl;
	}
	cout << endl;
	if( m_operation!= filter_k ||
			output_encrypted_b ||
			!( m_output_compress_b ||
				 m_output_uncompress_b ) )
		{
			cout << "No compression or uncompression being performed on output." << endl;
		}
	else {
		if( m_output_compress_b ) {
			cout << "Compression will be applied to some PDF streams." << endl;
		}
		else {
			cout << "Some PDF streams will be uncompressed." << endl;
		}
	}
}
bool
TK_Session::handle_some_output_options( TK_Session::keyword kw, ArgState* arg_state_p )
{
	switch( kw ) {
	case output_k:
		*arg_state_p= output_filename_e;
		break;
	case owner_pw_k:
		*arg_state_p= output_owner_pw_e;
		break;
	case user_pw_k:
		*arg_state_p= output_user_pw_e;
		break;
	case user_perms_k:
		*arg_state_p= output_user_perms_e;
		break;
	case encrypt_40bit_k:
		m_output_encryption_strength= bits40_enc;
		break;
	case encrypt_128bit_k:
		m_output_encryption_strength= bits128_enc;
		break;
	case filt_uncompress_k:
		m_output_uncompress_b= true;
		break;
	case filt_compress_k:
		m_output_compress_b= true;
		break;
	case flatten_k:
		m_output_flatten_b= true;
		break;
	case need_appearances_k:
		m_output_need_appearances_b= true;
		break;
	case drop_xfa_k:
		m_output_drop_xfa_b= true;
		break;
	case drop_xmp_k:
		m_output_drop_xmp_b= true;
		break;
	case keep_first_id_k:
		m_output_keep_first_id_b= true;
		break;
	case keep_final_id_k:
		m_output_keep_final_id_b= true;
		break;
	case verbose_k:
		m_verbose_reporting_b= true;
		break;
	case dont_ask_k:
		m_ask_about_warnings_b= false;
		break;
	case do_ask_k:
		m_ask_about_warnings_b= true;
		break;
	case background_k:
		if( m_operation!= filter_k ) {
			cerr << "Warning: the \"background\" output option works only in filter mode." << endl;
			cerr << "  This means it won't work in combination with \"cat\", \"burst\"," << endl;
			cerr << "  \"attach_file\", etc.  To run pdftk in filter mode, simply omit" << endl;
			cerr << "  the operation, e.g.: pdftk in.pdf output out.pdf background back.pdf" << endl;
			cerr << "  Or, use background as an operation; this is the preferred technique:" << endl;
			cerr << "    pdftk in.pdf background back.pdf output out.pdf" << endl;
		}
		*arg_state_p= background_filename_e;
		break;
	default:
		return false;
	}
	return true;
}
TK_Session::TK_Session( int argc, char** argv ) :
 	m_valid_b( false ),
 	m_authorized_b( true ),
 	m_input_pdf_readers_opened_b( false ),
 	m_verbose_reporting_b( false ),
 	m_ask_about_warnings_b( ASK_ABOUT_WARNINGS ),
 	m_input_pdf(),
 	m_input_pdf_index(),
 	m_input_attach_file_filename(),
 	m_input_attach_file_pagenum( 0 ),
 	m_update_info_filename(),
	m_update_info_utf8_b( false ),
 	m_update_xmp_filename(),
 	m_operation( none_k ),
 	m_page_seq(),
 	m_form_data_filename(),
 	m_background_filename(),
 	m_stamp_filename(),
 	m_output_filename(),
	m_output_utf8_b( false ),
 	m_output_owner_pw(),
 	m_output_user_pw(),
 	m_output_user_perms( 0 ),
 	m_multistamp_b ( false ),
 	m_multibackground_b ( false ),
 	m_output_uncompress_b( false ),
 	m_output_compress_b( false ),
 	m_output_flatten_b( false ),
 	m_output_need_appearances_b( false ),
 	m_output_drop_xfa_b( false ),
 	m_output_drop_xmp_b( false ),
 	m_output_keep_first_id_b( false ),
 	m_output_keep_final_id_b( false ),
	m_cat_full_pdfs_b( true ),
	m_output_encryption_strength( none_enc )
{
	TK_Session::ArgState arg_state = input_files_e;
	g_dont_collect_p= new java::Vector();
  bool password_using_handles_not_b= false;
  bool password_using_handles_b= false;
	InputPdfIndex password_input_pdf_index= 0;
  bool fail_b= false;
	for( int ii= 1; ii< argc; ++ii ) {
    int keyword_len= 0;
		keyword kw= is_keyword( argv[ii], &keyword_len );
		if( kw== dont_ask_k ) {
			m_ask_about_warnings_b= false;
		}
		else if( kw== do_ask_k ) {
			m_ask_about_warnings_b= true;
		}
	}
  for( int ii= 1; ii< argc && !fail_b && arg_state!= done_e; ++ii ) {
    int keyword_len= 0;
    keyword arg_keyword= is_keyword( argv[ii], &keyword_len );
		if( arg_keyword== end_k ||
				arg_keyword== even_k ||
				arg_keyword== odd_k )
			{
				arg_keyword= none_k;
			}
    switch( arg_state ) {
    case input_files_e:
		case input_pw_e: {
			if( arg_keyword== input_pw_k ) {
				arg_state= input_pw_e;
			}
      else if( arg_keyword== cat_k ) {
				m_operation= cat_k;
				arg_state= page_seq_e;
      }
      else if( arg_keyword== shuffle_k ) {
				m_operation= shuffle_k;
				arg_state= page_seq_e;
      }
      else if( arg_keyword== burst_k ) {
				m_operation= burst_k;
				arg_state= output_args_e;
      }
#ifdef BARCODE_BURST
      else if( arg_keyword== barcode_burst_k ) {
				m_operation= barcode_burst_k;
				arg_state= output_args_e;
      }
#endif
			else if( arg_keyword== filter_k ) {
				m_operation= filter_k;
				arg_state= output_e;
			}
			else if( arg_keyword== dump_data_k ) {
				m_operation= dump_data_k;
				arg_state= output_e;
			}
			else if( arg_keyword== dump_data_utf8_k ) {
				m_operation= dump_data_k;
				m_output_utf8_b= true;
				arg_state= output_e;
			}
			else if( arg_keyword== dump_data_fields_k ) {
				m_operation= dump_data_fields_k;
				arg_state= output_e;
			}
			else if( arg_keyword== dump_data_fields_utf8_k ) {
				m_operation= dump_data_fields_k;
				m_output_utf8_b= true;
				arg_state= output_e;
			}
			else if( arg_keyword== dump_data_k ) {
				m_operation= dump_data_k;
				arg_state= output_e;
			}
			else if( arg_keyword== dump_data_annots_k ) {
				m_operation= dump_data_annots_k;
				arg_state= output_e;
			}
			else if( arg_keyword== generate_fdf_k ) {
				m_operation= generate_fdf_k;
				m_output_utf8_b= true;
				arg_state= output_e;
			}
			else if( arg_keyword== fill_form_k ) {
				m_operation= filter_k;
				arg_state= form_data_filename_e;
			}
			else if( arg_keyword== attach_file_k ) {
				m_operation= filter_k;
				arg_state= attach_file_filename_e;
			}
			else if( arg_keyword== attach_file_to_page_k ) {
				arg_state= attach_file_pagenum_e;
			}
			else if( arg_keyword== unpack_files_k ) {
				m_operation= unpack_files_k;
				arg_state= output_e;
			}
			else if( arg_keyword== update_info_k ) {
				m_operation= filter_k;
				m_update_info_utf8_b= false;
				arg_state= update_info_filename_e;
			}
			else if( arg_keyword== update_info_utf8_k ) {
				m_operation= filter_k;
				m_update_info_utf8_b= true;
				arg_state= update_info_filename_e;
			}
			else if( arg_keyword== background_k ) {
				m_operation= filter_k;
				arg_state= background_filename_e;
			}
			else if( arg_keyword== multibackground_k ) {
				m_operation= filter_k;
				m_multibackground_b= true;
				arg_state= background_filename_e;
			}
			else if( arg_keyword== stamp_k ) {
				m_operation= filter_k;
				arg_state= stamp_filename_e;
			}
			else if( arg_keyword== multistamp_k ) {
				m_operation= filter_k;
				m_multistamp_b= true;
				arg_state= stamp_filename_e;
			}
			else if( arg_keyword== rotate_k ) {
				m_operation= filter_k;
				arg_state= page_seq_e;
			}
			else if( arg_keyword== output_k ) {
				arg_state= output_filename_e;
			}
      else if( arg_keyword== none_k ) {
				string handle; {
					char* eq_loc= strchr( argv[ii], '=' );
					if( eq_loc!= 0 ) {
						for( char* pp= argv[ii]; pp< eq_loc; ++pp ) {
							if( 'A'<= *pp && *pp<= 'Z' )
								handle+= *pp;
							else {
								eq_loc= 0;
								handle= "";
								break;
							}
						}
					}
				}
				string handle_and_data;
				copy_argv_as_utf8( handle_and_data, argv, ii );
				string data= handle_and_data;
				if( !handle.empty() ) {
					if( data.find( '=' )+ 1< data.length() )
						data= data.substr( data.find( '=' )+ 1 );
					else
						data= "";
				}
				if( arg_state== input_files_e ) {
					InputPdf input_pdf;
					input_pdf.m_filename= data;
					if( handle.empty() ) {
						m_input_pdf.push_back( input_pdf );
					}
					else {
						map< string, InputPdfIndex >::const_iterator it=
							m_input_pdf_index.find( handle );
						if( it!= m_input_pdf_index.end() ) {
							cerr << "Error: Handle given here: " << endl;
							cerr << "      " << handle_and_data << endl;
							cerr << "   is already associated with: " << endl;
							cerr << "      " << m_input_pdf[it->second].m_filename << endl;
							cerr << "   Exiting." << endl;
							fail_b= true;
						}
						else {
							m_input_pdf.push_back( input_pdf );
							m_input_pdf_index[ handle ]= m_input_pdf.size()- 1;
						}
					}
				}
				else if( arg_state== input_pw_e ) {
					if( m_input_pdf_index.size()== 0 ) {
						handle= "";
						data= handle_and_data;
					}
					if( handle.empty() ) {
						if( password_using_handles_b ) {
							cerr << "Error: Expected a user-supplied handle for this input" << endl;
							cerr << "   PDF password: " << handle_and_data << endl << endl;
							cerr << "   Handles must be supplied with ~all~ input" << endl;
							cerr << "   PDF passwords, or with ~no~ input PDF passwords." << endl;
							cerr << "   If no handles are supplied, then passwords are applied" << endl;
							cerr << "   according to input PDF order." << endl << endl;
							cerr << "   Handles are given like this: <handle>=<password>, and" << endl;
							cerr << "   they must be single, upper case letters, like: A, B, etc." << endl;
							fail_b= true;
						}
						else {
							password_using_handles_not_b= true;
							if( password_input_pdf_index< m_input_pdf.size() ) {
								m_input_pdf[ password_input_pdf_index ].m_password= handle_and_data;
								++password_input_pdf_index;
							}
							else {
								cerr << "Error: more input passwords than input PDF documents." << endl;
								cerr << "   Exiting." << endl;
								fail_b= true;
							}
						}
					}
					else {
						if( password_using_handles_not_b ) {
							cerr << "Error: Expected ~no~ user-supplied handle for this input" << endl;
							cerr << "   PDF password: " << handle_and_data << endl << endl;
							cerr << "   Handles must be supplied with ~all~ input" << endl;
							cerr << "   PDF passwords, or with ~no~ input PDF passwords." << endl;
							cerr << "   If no handles are supplied, then passwords are applied" << endl;
							cerr << "   according to input PDF order." << endl << endl;
							cerr << "   Handles are given like this: <handle>=<password>, and" << endl;
							cerr << "   they must be single, upper case letters, like: A, B, etc." << endl;
							fail_b= true;
						}
						else {
							password_using_handles_b= true;
							map< string, InputPdfIndex >::const_iterator it=
								m_input_pdf_index.find( handle );
							if( it!= m_input_pdf_index.end() ) {
								if( m_input_pdf[it->second].m_password.empty() ) {
									m_input_pdf[it->second].m_password= data;
								}
								else {
									cerr << "Error: Handle given here: " << endl;
									cerr << "      " << handle_and_data << endl;
									cerr << "   is already associated with this password: " << endl;
									cerr << "      " << m_input_pdf[it->second].m_password << endl;
									cerr << "   Exiting." << endl;
									fail_b= true;
								}
							}
							else {
								cerr << "Error: Password handle: " << handle_and_data << endl;
								cerr << "   is not associated with an input PDF file." << endl;
								cerr << "   Exiting." << endl;
								fail_b= true;
							}
						}
					}
				}
				else {
					cerr << "Error: Internal error: unexpected arg_state.  Exiting." << endl;
					fail_b= true;
				}
			}
			else {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				cerr << "Error: Unexpected command-line data: " << endl;
				cerr << "      " << argv_ss << endl;
				if( arg_state== input_files_e ) {
					cerr << "   where we were expecting an input PDF filename," << endl;
					cerr << "   operation (e.g. \"cat\") or \"input_pw\".  Exiting." << endl;
				}
				else {
					cerr << "   where we were expecting an input PDF password" << endl;
					cerr << "   or operation (e.g. \"cat\").  Exiting." << endl;
				}
				fail_b= true;
			}
    }
    break;
    case page_seq_e: {
      if( m_page_seq.empty() ) {
				if( m_input_pdf.empty() ) {
					cerr << "Error: No input files.  Exiting." << endl;
					fail_b= true;
					break;
				}
				if( !open_input_pdf_readers() ) {
					fail_b= true;
					break;
				}
      }
      if( arg_keyword== output_k ) {
				arg_state= output_filename_e;
      }
			else if( arg_keyword== none_k ||
							 arg_keyword== end_k )
				{
					bool even_pages_b= false;
					bool odd_pages_b= false;
					int jj= 0;
					InputPdfIndex range_pdf_index= 0; {
						string handle;
						for( ; argv[ii][jj] && isupper(argv[ii][jj]); ++jj ) {
							handle.push_back( argv[ii][jj] );
						}
						if( !handle.empty() ) {
							map< string, InputPdfIndex >::const_iterator it= m_input_pdf_index.find( handle );
							if( it== m_input_pdf_index.end() ) {
								string argv_ss;
								copy_argv_as_utf8( argv_ss, argv, ii );
								cerr << "Error: Given handle has no associated file: " << endl;
								cerr << "   " << handle << ", used here: " << argv_ss << endl;
								cerr << "   Exiting." << endl;
								fail_b= true;
								break;
							}
							else {
								range_pdf_index= it->second;
							}
						}
					}
					char* hyphen_loc= strchr( argv[ii]+ jj, '-' );
					if( hyphen_loc )
						*hyphen_loc= 0;
					PageRotate page_rotate= NORTH;
					PageRotateAbsolute page_rotate_absolute= false;
					bool reverse_b= ( argv[ii][jj]== 'r' );
					if( reverse_b )
						++jj;
					PageNumber page_num_beg= 0;
					bool page_num_beg_out_of_range_b= false;
					for( ; argv[ii][jj] && isdigit( argv[ii][jj] ); ++jj ) {
						page_num_beg= page_num_beg* 10+ argv[ii][jj]- '0';
					}
					if( !page_num_beg && argv[ii][jj] ) {
						int keyword_len= 0;
						keyword arg_keyword= is_keyword( argv[ii]+ jj, &keyword_len );
						if( arg_keyword== end_k ) {
							page_num_beg= m_input_pdf[range_pdf_index].m_num_pages;
							jj+= keyword_len;
						}
					}
					if( argv[ii][jj] && hyphen_loc ) {
						string argv_ss;
						copy_argv_as_utf8( argv_ss, argv, ii );
						cerr << "Error: Unexpected combination of digits and text in" << endl;
						cerr << "   page range start, here: " << argv_ss << endl;
						cerr << "   Exiting." << endl;
						fail_b= true;
						break;
					}
					if( m_input_pdf[range_pdf_index].m_num_pages< page_num_beg ) {
						cerr << "Error: Range start page number exceeds size of PDF" << endl;
						cerr << "   here: " << argv[ii] << endl;
						cerr << "   input PDF has: " << m_input_pdf[range_pdf_index].m_num_pages << " pages." << endl;
						cerr << "   Exiting." << endl;
						fail_b= true;
						break;
					}
					if( reverse_b )
						page_num_beg= m_input_pdf[range_pdf_index].m_num_pages- page_num_beg+ 1;
					PageNumber page_num_end= page_num_beg;
					if( hyphen_loc ) {
						while( argv[ii][jj] )
							++jj;
						++jj;
						bool reverse_b= ( argv[ii][jj]== 'r' );
						if( reverse_b )
							++jj;
						page_num_end= 0;
						for( ; argv[ii][jj] && isdigit( argv[ii][jj] ); ++jj ) {
							page_num_end= page_num_end* 10+ argv[ii][jj]- '0';
						}
						if( !page_num_end && argv[ii][jj] ) {
							int keyword_len= 0;
							keyword arg_keyword= is_keyword( argv[ii]+ jj, &keyword_len );
							if( arg_keyword== end_k ) {
								page_num_end= m_input_pdf[range_pdf_index].m_num_pages;
								jj+= keyword_len;
							}
						}
						if( !page_num_end ) {
							string argv_ss;
							copy_argv_as_utf8( argv_ss, argv, ii );
							cerr << "Error: Unexpected range end; expected a page" << endl;
							cerr << "   number or legal keyword, here: " << argv_ss << endl;
							cerr << "   Exiting." << endl;
							fail_b= true;
							break;
						}
						if( m_input_pdf[range_pdf_index].m_num_pages< page_num_end ) {
							cerr << "Error: Range end page number exceeds size of PDF" << endl;
							cerr << "   input PDF has: " << m_input_pdf[range_pdf_index].m_num_pages << " pages." << endl;
							cerr << "   Exiting." << endl;
							fail_b= true;
							break;
						}
						if( reverse_b )
							page_num_end= m_input_pdf[range_pdf_index].m_num_pages- page_num_end+ 1;
					}
					while( argv[ii][jj] ) {
						int keyword_len= 0;
						keyword arg_keyword= is_keyword( argv[ii]+ jj, &keyword_len );
						if( arg_keyword== even_k ) {
							even_pages_b= true;
						}
						else if( arg_keyword== odd_k ) {
							odd_pages_b= true;
						}
						else if( arg_keyword== rot_north_k ) {
							page_rotate= TK_Session::NORTH;
							page_rotate_absolute= true;
						}
						else if( arg_keyword== rot_east_k ) {
							page_rotate= TK_Session::EAST;
							page_rotate_absolute= true;
						}
						else if( arg_keyword== rot_south_k ) {
							page_rotate= TK_Session::SOUTH;
							page_rotate_absolute= true;
						}
						else if( arg_keyword== rot_west_k ) {
							page_rotate= TK_Session::WEST;
							page_rotate_absolute= true;
						}
						else if( arg_keyword== rot_left_k ) {
							page_rotate= TK_Session::WEST;
							page_rotate_absolute= false;
						}
						else if( arg_keyword== rot_right_k ) {
							page_rotate= TK_Session::EAST;
							page_rotate_absolute= false;
						}
						else if( arg_keyword== rot_upside_down_k ) {
							page_rotate= TK_Session::SOUTH;
							page_rotate_absolute= false;
						}
						else {
							string argv_ss;
							copy_argv_as_utf8( argv_ss, argv, ii );
							cerr << "Error: Unexpected text in page range end, here: " << endl;
							cerr << "   " << argv_ss  << endl;
							cerr << "   Exiting." << endl;
							cerr << "   Acceptable keywords, for example: \"even\" or \"odd\"." << endl;
							cerr << "   To rotate pages, use: \"north\" \"south\" \"east\"" << endl;
							cerr << "       \"west\" \"left\" \"right\" or \"down\"" << endl;
							fail_b= true;
							break;
						}
						jj+= keyword_len;
					}
					if( page_num_beg== 0 && page_num_end== 0 ) {
						page_num_beg= 1;
						page_num_end= m_input_pdf[range_pdf_index].m_num_pages;
						m_cat_full_pdfs_b= m_cat_full_pdfs_b && ( !even_pages_b && !odd_pages_b );
					}
					else if( page_num_beg== 0 || page_num_end== 0 ) {
						cerr << "Error: Input page numbers include 0 (zero)" << endl;
						cerr << "   The first PDF page is 1 (one)" << endl;
						cerr << "   Exiting." << endl;
						fail_b= true;
						break;
					}
					else
						m_cat_full_pdfs_b= false;
					vector< PageRef > temp_page_seq;
					bool reverse_sequence_b= ( page_num_end< page_num_beg );
					if( reverse_sequence_b ) {
						PageNumber temp= page_num_end;
						page_num_end= page_num_beg;
						page_num_beg= temp;
					}
					for( PageNumber kk= page_num_beg; kk<= page_num_end; ++kk ) {
						if( (!even_pages_b || !(kk % 2)) &&
								(!odd_pages_b || (kk % 2)) )
							{
								if( kk<= m_input_pdf[range_pdf_index].m_num_pages ) {
									vector< pair< set<jint>, itext::PdfReader* > >::iterator it=
										m_input_pdf[range_pdf_index].m_readers.begin();
									for( ; it!= m_input_pdf[range_pdf_index].m_readers.end(); ++it ) {
										set<jint>::iterator jt= it->first.find( kk );
										if( jt== it->first.end() ) {
											it->first.insert( kk );
											break;
										}
									}
									if( it== m_input_pdf[range_pdf_index].m_readers.end() ) {
										if( add_reader( &(m_input_pdf[range_pdf_index]) ) ) {
											m_input_pdf[range_pdf_index].m_readers.back().first.insert( kk );
										}
										else {
											cerr << "Internal Error: unable to add reader" << endl;
											fail_b= true;
											break;
										}
									}
									temp_page_seq.push_back( PageRef( range_pdf_index, kk, page_rotate, page_rotate_absolute ) );
								}
								else {
									cerr << "Error: Page number: " << kk << endl;
									cerr << "   does not exist in file: " << m_input_pdf[range_pdf_index].m_filename << endl;
									fail_b= true;
								}
							}
					}
					if( fail_b )
						break;
					if( reverse_sequence_b ) {
						reverse( temp_page_seq.begin(), temp_page_seq.end() );
					}
					m_page_seq.push_back( temp_page_seq );
				}
			else {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				cerr << "Error: expecting page ranges.  Instead, I got:" << endl;
				cerr << "   " << argv_ss << endl;
				fail_b= true;
				break;
			}
		}
    break;
		case form_data_filename_e: {
      if( arg_keyword== none_k )
				{
					if( m_form_data_filename.empty() ) {
						copy_argv_as_utf8( m_form_data_filename, argv, ii );
					}
					else {
						string argv_ss;
						copy_argv_as_utf8( argv_ss, argv, ii );
						cerr << "Error: Multiple fill_form filenames given: " << endl;
						cerr << "   " << m_form_data_filename << " and " << argv_ss << endl;
						cerr << "Exiting." << endl;
						fail_b= true;
						break;
					}
					arg_state= output_e;
				}
			else {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				cerr << "Error: expecting a form data filename," << endl;
				cerr << "   instead I got this keyword: " << argv_ss << endl;
				cerr << "Exiting." << endl;
				fail_b= true;
				break;
			}
		}
		break;
		case attach_file_filename_e: {
			if( arg_keyword== attach_file_to_page_k ) {
				arg_state= attach_file_pagenum_e;
			}
			else if( arg_keyword== output_k ) {
				arg_state= output_filename_e;
			}
			else if( arg_keyword== none_k ) {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				m_input_attach_file_filename.push_back( argv_ss );
			}
			else {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				cerr << "Error: expecting an attachment filename," << endl;
				cerr << "   instead I got this keyword: " << argv_ss << endl;
				cerr << "Exiting." << endl;
				fail_b= true;
				break;
			}
		}
		break;
		case attach_file_pagenum_e: {
			if( strcmp(argv[ii], "PROMPT")== 0 ) {
				m_input_attach_file_pagenum= -1;
			}
			else if( strcmp(argv[ii], "end")== 0 ) {
				m_input_attach_file_pagenum= -2;
			}
			else {
				m_input_attach_file_pagenum= 0;
				for( int jj= 0; argv[ii][jj]; ++jj ) {
					if( !isdigit(argv[ii][jj]) ) {
						string argv_ss;
						copy_argv_as_utf8( argv_ss, argv, ii );
						cerr << "Error: expecting a (1-based) page number.  Instead, I got:" << endl;
						cerr << "   " << argv_ss << endl;
						cerr << "Exiting." << endl;
						fail_b= true;
						break;
					}
					m_input_attach_file_pagenum=
						m_input_attach_file_pagenum* 10+ argv[ii][jj]- '0';
				}
			}
			arg_state= output_e;
		}
		break;
		case update_info_filename_e : {
			if( arg_keyword== none_k ) {
					if( m_update_info_filename.empty() ) {
						copy_argv_as_utf8( m_update_info_filename, argv, ii );
					}
					else {
						string argv_ss;
						copy_argv_as_utf8( argv_ss, argv, ii );
						cerr << "Error: Multiple update_info filenames given: " << endl;
						cerr << "   " << m_update_info_filename << " and " << argv_ss << endl;
						cerr << "Exiting." << endl;
						fail_b= true;
						break;
					}
				}
			else {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				cerr << "Error: expecting an INFO file filename," << endl;
				cerr << "   instead I got this keyword: " << argv_ss << endl;
				cerr << "Exiting." << endl;
				fail_b= true;
				break;
			}
			arg_state= output_e;
		}
		break;
		case output_e: {
			if( m_input_pdf.empty() ) {
				cerr << "Error: No input files.  Exiting." << endl;
				fail_b= true;
				break;
			}
      if( arg_keyword== output_k ) {
				arg_state= output_filename_e;
      }
			else {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				cerr << "Error: expecting \"output\" keyword.  Instead, I got:" << endl;
				cerr << "   " << argv_ss << endl;
				fail_b= true;
				break;
			}
		}
		break;
    case output_filename_e: {
			if( m_operation== none_k ) {
				if( 1< m_input_pdf.size() ) {
					m_operation= cat_k;
				}
				else {
					m_operation= filter_k;
				}
			}
			if( !open_input_pdf_readers() ) {
				fail_b= true;
				break;
			}
			if( ( m_operation== cat_k ||
						m_operation== shuffle_k ) )
				{
					if( m_page_seq.empty() ) {
						for( InputPdfIndex ii= 0; ii< m_input_pdf.size(); ++ii ) {
							InputPdf& input_pdf= m_input_pdf[ii];
							vector< PageRef > temp_page_seq;
							for( PageNumber jj= 1; jj<= input_pdf.m_num_pages; ++jj ) {
								temp_page_seq.push_back( PageRef( ii, jj ) );
								m_input_pdf[ii].m_readers.back().first.insert( jj );
							}
							m_page_seq.push_back( temp_page_seq );
						}
					}
				}
			if( m_output_filename.empty() ) {
				copy_argv_as_utf8( m_output_filename, argv, ii );
				if( m_output_filename!= "-" ) {
					for( vector< InputPdf >::const_iterator it= m_input_pdf.begin();
							 it!= m_input_pdf.end(); ++it )
						{
							if( it->m_filename== m_output_filename ) {
								cerr << "Error: The given output filename: " << m_output_filename << endl;
								cerr << "   matches an input filename.  Exiting." << endl;
								fail_b= true;
								break;
							}
						}
				}
			}
			else {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				cerr << "Error: Multiple output filenames given: " << endl;
				cerr << "   " << m_output_filename << " and " << argv_ss << endl;
				cerr << "Exiting." << endl;
				fail_b= true;
				break;
			}
			arg_state= output_args_e;
		}
		break;
		case output_args_e: {
			if( handle_some_output_options( arg_keyword, &arg_state ) ) {
				break;
			}
			else {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				cerr << "Error: Unexpected data in output section: " << endl;
				cerr << "      " << argv_ss << endl;
				cerr << "Exiting." << endl;
				fail_b= true;
				break;
			}
		}
		break;
		case output_owner_pw_e: {
			if( m_output_owner_pw.empty() ) {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				if( argv_ss== "PROMPT" || argv_ss!= m_output_user_pw ) {
					m_output_owner_pw= argv_ss;
				}
				else {
					cerr << "Error: The user and owner passwords are the same." << endl;
					cerr << "   PDF Viewers interpret this to mean your PDF has" << endl;
					cerr << "   no owner password, so they must be different." << endl;
					cerr << "   Or, supply no owner password to pdftk if this is" << endl;
					cerr << "   what you desire." << endl;
					cerr << "Exiting." << endl;
					fail_b= true;
					break;
				}
			}
			else {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				cerr << "Error: Multiple output owner passwords given: " << endl;
				cerr << "   " << m_output_owner_pw << " and " << argv_ss << endl;
				cerr << "Exiting." << endl;
				fail_b= true;
				break;
			}
			arg_state= output_args_e;
		}
		break;
		case output_user_pw_e: {
			if( m_output_user_pw.empty() ) {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				if( argv_ss== "PROMPT" || m_output_owner_pw!= argv_ss ) {
					m_output_user_pw= argv_ss;
				}
				else {
					cerr << "Error: The user and owner passwords are the same." << endl;
					cerr << "   PDF Viewers interpret this to mean your PDF has" << endl;
					cerr << "   no owner password, so they must be different." << endl;
					cerr << "   Or, supply no owner password to pdftk if this is" << endl;
					cerr << "   what you desire." << endl;
					cerr << "Exiting." << endl;
					fail_b= true;
					break;
				}
			}
			else {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				cerr << "Error: Multiple output user passwords given: " << endl;
				cerr << "   " << m_output_user_pw << " and " << argv_ss << endl;
				cerr << "Exiting." << endl;
				fail_b= true;
				break;
			}
			arg_state= output_args_e;
		}
		break;
		case output_user_perms_e: {
			using itext::PdfWriter;
			if( handle_some_output_options( arg_keyword, &arg_state ) ) {
				break;
			}
			switch( arg_keyword ) {
			case perm_printing_k:
				m_output_user_perms|=
					PdfWriter::AllowPrinting;
				break;
			case perm_modify_contents_k:
				m_output_user_perms|=
					( PdfWriter::AllowModifyContents | PdfWriter::AllowAssembly );
				break;
			case perm_copy_contents_k:
				m_output_user_perms|=
					( PdfWriter::AllowCopy | PdfWriter::AllowScreenReaders );
				break;
			case perm_modify_annotations_k:
				m_output_user_perms|=
					( PdfWriter::AllowModifyAnnotations | PdfWriter::AllowFillIn );
				break;
			case perm_fillin_k:
				m_output_user_perms|=
					PdfWriter::AllowFillIn;
				break;
			case perm_screen_readers_k:
				m_output_user_perms|=
					PdfWriter::AllowScreenReaders;
				break;
			case perm_assembly_k:
				m_output_user_perms|=
					PdfWriter::AllowAssembly;
				break;
			case perm_degraded_printing_k:
				m_output_user_perms|=
					PdfWriter::AllowDegradedPrinting;
				break;
			case perm_all_k:
				m_output_user_perms=
					( PdfWriter::AllowPrinting |
						PdfWriter::AllowModifyContents |
						PdfWriter::AllowCopy |
						PdfWriter::AllowModifyAnnotations |
						PdfWriter::AllowFillIn |
						PdfWriter::AllowScreenReaders |
						PdfWriter::AllowAssembly );
				break;
			default:
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				cerr << "Error: Unexpected data in output section: " << endl;
				cerr << "      " << argv_ss << endl;
				cerr << "Exiting." << endl;
				fail_b= true;
				break;
			}
		}
		break;
		case background_filename_e : {
			if( arg_keyword== none_k ) {
				if( m_background_filename.empty() ) {
					copy_argv_as_utf8( m_background_filename, argv, ii );
				}
				else {
					string argv_ss;
					copy_argv_as_utf8( argv_ss, argv, ii );
					cerr << "Error: Multiple background filenames given: " << endl;
					cerr << "   " << m_background_filename << " and " << argv_ss << endl;
					cerr << "Exiting." << endl;
					fail_b= true;
					break;
				}
			}
			else {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				cerr << "Error: expecting a PDF filename for background operation," << endl;
				cerr << "   instead I got this keyword: " << argv_ss << endl;
				cerr << "Exiting." << endl;
				fail_b= true;
				break;
			}
			arg_state= output_args_e;
		}
		break;
		case stamp_filename_e : {
			if( arg_keyword== none_k ) {
				if( m_stamp_filename.empty() ) {
					copy_argv_as_utf8( m_stamp_filename, argv, ii );
				}
				else {
					string argv_ss;
					copy_argv_as_utf8( argv_ss, argv, ii );
					cerr << "Error: Multiple stamp filenames given: " << endl;
					cerr << "   " << m_stamp_filename << " and " << argv_ss << endl;
					cerr << "Exiting." << endl;
					fail_b= true;
					break;
				}
			}
			else {
				string argv_ss;
				copy_argv_as_utf8( argv_ss, argv, ii );
				cerr << "Error: expecting a PDF filename for stamp operation," << endl;
				cerr << "   instead I got this keyword: " << argv_ss << endl;
				cerr << "Exiting." << endl;
				fail_b= true;
				break;
			}
			arg_state= output_e;
		}
		break;
    default: {
			cerr << "Internal Error: Unexpected arg_state.  Exiting." << endl;
			fail_b= true;
			break;
		}
		break;
    }
  }
	if( fail_b ) {
		cerr << "Errors encountered.  No output created." << endl;
		m_valid_b= false;
		g_dont_collect_p->clear();
		m_input_pdf.erase( m_input_pdf.begin(), m_input_pdf.end() );
	}
	else {
		m_valid_b= true;
		if(!m_input_pdf_readers_opened_b ) {
			open_input_pdf_readers();
		}
	}
}
TK_Session::~TK_Session()
{
	g_dont_collect_p->clear();
}
static java::OutputStream*
get_output_stream( string output_filename,
									 bool ask_about_warnings_b )
{
	java::OutputStream* os_p= 0;
	if( output_filename.empty() || output_filename== "PROMPT" ) {
		prompt_for_filename( "Please enter a name for the output:",
												 output_filename );
		return get_output_stream( output_filename,
															ask_about_warnings_b );
	}
	if( output_filename== "-" ) {
		os_p= java::System::out;
	}
	else {
		if( ask_about_warnings_b ) {
			bool output_exists_b= false;
			{
				FILE* fp= fopen( output_filename.c_str(), "rb" );
				if( fp ) {
					output_exists_b= true;
					fclose( fp );
				}
			}
			if( output_exists_b ) {
				cout << "Warning: the output file: " << output_filename << " already exists.  Overwrite? (y/n)" << endl;
				char buff[64];
				cin.getline( buff, 64 );
				if( buff[0]!= 'y' && buff[0]!= 'Y' ) {
					return get_output_stream( "PROMPT",
																		ask_about_warnings_b );
				}
			}
		}
		java::String* jv_output_filename_p=
			JvNewStringUTF( output_filename.c_str() );
		try {
			os_p= new java::FileOutputStream( jv_output_filename_p );
		}
		catch( java::io::IOException* ioe_p ) {
			cerr << "Error: Failed to open output file: " << endl;
			cerr << "   " << output_filename << endl;
			cerr << "   No output created." << endl;
			os_p= 0;
		}
	}
	return os_p;
}
static char g_page_marker[]= "pdftk_PageNum";
static void
add_mark_to_page( itext::PdfReader* reader_p,
									jint page_index,
									jint page_num )
{
	itext::PdfName* page_marker_p=
		new itext::PdfName( JvNewStringUTF( g_page_marker ) );
	itext::PdfDictionary* page_p= reader_p->getPageN( page_index );
	if( page_p && page_p->isDictionary() ) {
		page_p->put( page_marker_p, new itext::PdfNumber( page_num ) );
	}
}
static void
add_marks_to_pages( itext::PdfReader* reader_p )
{
	jint num_pages= reader_p->getNumberOfPages();
	for( jint ii= 1; ii<= num_pages; ++ii ) {
		add_mark_to_page( reader_p, ii, ii );
	}
}
static void
remove_mark_from_page( itext::PdfReader* reader_p,
											 jint page_num )
{
	itext::PdfName* page_marker_p=
		new itext::PdfName( JvNewStringUTF( g_page_marker ) );
	itext::PdfDictionary* page_p= reader_p->getPageN( page_num );
	if( page_p && page_p->isDictionary() ) {
		page_p->remove( page_marker_p );
	}
}
static void
remove_marks_from_pages( itext::PdfReader* reader_p )
{
	jint num_pages= reader_p->getNumberOfPages();
	for( jint ii= 1; ii<= num_pages; ++ii ) {
		remove_mark_from_page( reader_p, ii );
	}
}
static void
apply_rotation_to_page( itext::PdfReader* reader_p, TK_Session::PageNumber page_num, int rotation, bool absolute ) {
	itext::PdfDictionary* page_p= reader_p->getPageN( page_num );
	if( !absolute )	{
		rotation= reader_p->getPageRotation( page_num )+ rotation;
	}
	rotation= rotation % 360;
	page_p->remove( itext::PdfName::ROTATE );
	if( rotation!= TK_Session::NORTH ) {
		page_p->put( itext::PdfName::ROTATE,
								 new itext::PdfNumber( (jint)rotation ) );
	}
}
int
TK_Session::create_output_page( itext::PdfCopy* writer_p, PageRef page_ref, int output_page_count )
{
	int ret_val= 0;
	if( page_ref.m_input_pdf_index< m_input_pdf.size() ) {
		InputPdf& page_pdf= m_input_pdf[ page_ref.m_input_pdf_index ];
		if( m_verbose_reporting_b ) {
			cout << "   Adding page " << page_ref.m_page_num << " X" << page_ref.m_page_rot << "X ";
			cout << " from " << page_pdf.m_filename << endl;
		}
		itext::PdfReader* input_reader_p= 0;
		vector< pair< set<jint>, itext::PdfReader* > >::iterator mt= page_pdf.m_readers.begin();
		for( ; mt!= page_pdf.m_readers.end(); ++mt ) {
			set<jint>::iterator nt= mt->first.find( page_ref.m_page_num );
			if( nt!= mt->first.end() ) {
				input_reader_p= mt->second;
				mt->first.erase( nt );
				break;
			}
		}
		if( input_reader_p ) {
			if( m_output_uncompress_b ) {
				add_mark_to_page( input_reader_p, page_ref.m_page_num, output_page_count+ 1 );
			}
			else if( m_output_compress_b ) {
				remove_mark_from_page( input_reader_p, page_ref.m_page_num );
			}
			apply_rotation_to_page( input_reader_p, page_ref.m_page_num, page_ref.m_page_rot, page_ref.m_page_abs );
			itext::PdfImportedPage* page_p=
				writer_p->getImportedPage( input_reader_p, page_ref.m_page_num );
			if( page_p ) {
				writer_p->addPage( page_p );
			}
			else {
				cerr << "Internal Error: getImportedPage() failed for: ";
				cerr << page_ref.m_page_num << " in file: " << page_pdf.m_filename << endl;
				ret_val= 2;
			}
		}
		else {
			cerr << "Internal Error: no reader found for page: ";
			cerr << page_ref.m_page_num << " in file: " << page_pdf.m_filename << endl;
			ret_val= 2;
		}
	}
	else {
		cerr << "Internal Error: Unable to find handle in m_input_pdf." << endl;
		ret_val= 2;
	}
	return ret_val;
}
static jchar GetPdfVersionChar( itext::PdfName* version_p ) {
	jchar version_cc= itext::PdfWriter::VERSION_1_4;
	if( version_p )
		if( version_p->equals( itext::PdfName::VERSION_1_4 ) )
			version_cc= itext::PdfWriter::VERSION_1_4;
		else if( version_p->equals( itext::PdfName::VERSION_1_5 ) )
			version_cc= itext::PdfWriter::VERSION_1_5;
		else if( version_p->equals( itext::PdfName::VERSION_1_6 ) )
			version_cc= itext::PdfWriter::VERSION_1_6;
		else if( version_p->equals( itext::PdfName::VERSION_1_7 ) )
			version_cc= itext::PdfWriter::VERSION_1_7;
		else if( version_p->equals( itext::PdfName::VERSION_1_3 ) )
			version_cc= itext::PdfWriter::VERSION_1_3;
		else if( version_p->equals( itext::PdfName::VERSION_1_2 ) )
			version_cc= itext::PdfWriter::VERSION_1_2;
		else if( version_p->equals( itext::PdfName::VERSION_1_1 ) )
			version_cc= itext::PdfWriter::VERSION_1_1;
		else if( version_p->equals( itext::PdfName::VERSION_1_0 ) )
			version_cc= itext::PdfWriter::VERSION_1_0;
	return version_cc;
}
int
TK_Session::create_output()
{
	int ret_val= 0;
	if( is_valid() ) {
		if( m_verbose_reporting_b ) {
			cout << endl << "Creating Output ..." << endl;
		}
		string creator= "pdftk "+ string(PDFTK_VER)+ " - www.pdftk.com";
		java::String* jv_creator_p= JvNewStringUTF( creator.c_str() );
		if( m_output_owner_pw== "PROMPT" ) {
			prompt_for_password( "owner", "the output PDF", m_output_owner_pw );
		}
		if( m_output_user_pw== "PROMPT" ) {
			prompt_for_password( "user", "the output PDF", m_output_user_pw );
		}
		jbyteArray output_owner_pw_p= JvNewByteArray( 0 );
		if( m_output_owner_pw.size() ) {
			int size= utf8_password_to_pdfdoc( 0, m_output_owner_pw.c_str(), m_output_owner_pw.size(), true );
			if( 0<= size ) {
				output_owner_pw_p= JvNewByteArray( size );
				utf8_password_to_pdfdoc( elements(output_owner_pw_p), m_output_owner_pw.c_str(), m_output_owner_pw.size(), true );
			}
			else {
				cerr << "Error: Owner password used to encrypt output PDF includes" << endl;
				cerr << "   invalid characters." << endl;
				cerr << "   No output created." << endl;
				ret_val= 1;
			}
		}
		jbyteArray output_user_pw_p= JvNewByteArray( 0 );
		if( m_output_user_pw.size() ) {
			int size= utf8_password_to_pdfdoc( 0, m_output_user_pw.c_str(), m_output_user_pw.size(), true );
			if( 0<= size ) {
				output_user_pw_p= JvNewByteArray( size );
				utf8_password_to_pdfdoc( elements(output_user_pw_p), m_output_user_pw.c_str(), m_output_user_pw.size(), true );
			}
			else {
				cerr << "Error: User password used to encrypt output PDF includes" << endl;
				cerr << "   invalid characters." << endl;
				cerr << "   No output created." << endl;
				ret_val= 1;
			}
		}
		if( ret_val )
			return ret_val;
		try {
			switch( m_operation ) {
			case cat_k :
			case shuffle_k : {
				itext::Document* output_doc_p= new itext::Document();
				java::OutputStream* ofs_p=
					get_output_stream( m_output_filename,
														 m_ask_about_warnings_b );
				if( !ofs_p ) {
					ret_val= 1;
					break;
				}
				itext::PdfCopy* writer_p= new itext::PdfCopy( output_doc_p, ofs_p );
				jchar max_version_cc= itext::PdfWriter::VERSION_1_2;
				output_doc_p->addCreator( jv_creator_p );
				if( m_output_uncompress_b ) {
					writer_p->filterStreams= true;
					writer_p->compressStreams= false;
				}
				else if( m_output_compress_b ) {
					writer_p->filterStreams= false;
					writer_p->compressStreams= true;
				}
				if( m_output_encryption_strength!= none_enc ||
						!m_output_owner_pw.empty() ||
						!m_output_user_pw.empty() )
					{
						jboolean bit128_b=
							( m_output_encryption_strength!= bits40_enc );
						writer_p->setEncryption( output_user_pw_p,
																		 output_owner_pw_p,
																		 m_output_user_perms,
																		 bit128_b );
						if( bit128_b )
							max_version_cc= itext::PdfWriter::VERSION_1_4;
						else
							max_version_cc= itext::PdfWriter::VERSION_1_3;
					}
				if( m_output_keep_first_id_b ||
						m_output_keep_final_id_b )
					{
						itext::PdfReader* input_reader_p=
							m_output_keep_first_id_b ?
							m_input_pdf[0].m_readers.begin()->second :
							m_input_pdf[m_input_pdf.size()- 1].m_readers.begin()->second;
						itext::PdfDictionary* trailer_p= input_reader_p->getTrailer();
						itext::PdfArray* file_id_p= (itext::PdfArray*)
							input_reader_p->getPdfObject( trailer_p->get( itext::PdfName::ID ) );
						if( file_id_p && file_id_p->isArray() ) {
							writer_p->setFileID( file_id_p );
						}
					}
				map< jint, itext::PdfName* > ext_developers;
				map< jint, itext::PdfName* > ext_base_versions;
				map< jint, jint > ext_levels;
				for( vector< InputPdf >::const_iterator it= m_input_pdf.begin();
						 it!= m_input_pdf.end(); ++it )
					{
						itext::PdfReader* reader_p= it->m_readers.begin()->second;
						if( max_version_cc< reader_p->getPdfVersion() )
							max_version_cc= reader_p->getPdfVersion();
						itext::PdfDictionary* catalog_p= reader_p->getCatalog();
						if( catalog_p->contains( itext::PdfName::VERSION ) ) {
							itext::PdfName* version_p= (itext::PdfName*)
								reader_p->getPdfObject( catalog_p->get( itext::PdfName::VERSION ) );
							jchar version_cc= GetPdfVersionChar( version_p );
							if( max_version_cc< version_cc )
								max_version_cc= version_cc;
						}
						if( catalog_p->contains( itext::PdfName::EXTENSIONS ) ) {
							itext::PdfDictionary* extensions_p= (itext::PdfDictionary*)
								reader_p->getPdfObject( catalog_p->get( itext::PdfName::EXTENSIONS ) );
							if( extensions_p && extensions_p->isDictionary() ) {
								java::Set* keys_p= extensions_p->getKeys();
								java::Iterator* kit= keys_p->iterator();
								while( kit->hasNext() ) {
									itext::PdfName* developer_p= (itext::PdfName*)
										reader_p->getPdfObject( (itext::PdfObject*)kit->next() );
									ext_developers[ developer_p->hashCode() ]= developer_p;
									itext::PdfDictionary* dev_exts_p= (itext::PdfDictionary*)
										reader_p->getPdfObject( extensions_p->get( developer_p ) );
									if( dev_exts_p && dev_exts_p->isDictionary() ) {
										if( dev_exts_p->contains( itext::PdfName::BASEVERSION ) &&
												dev_exts_p->contains( itext::PdfName::EXTENSIONLEVEL ) )
											{
												itext::PdfName* base_version_p= (itext::PdfName*)
													reader_p->getPdfObject( dev_exts_p->get( itext::PdfName::BASEVERSION ) );
												itext::PdfNumber* ext_level_p= (itext::PdfNumber*)
													reader_p->getPdfObject( dev_exts_p->get( itext::PdfName::EXTENSIONLEVEL ) );
												if( ext_base_versions.find( developer_p->hashCode() )== ext_base_versions.end() ||
														GetPdfVersionChar( ext_base_versions[ developer_p->hashCode() ] )<
														GetPdfVersionChar( base_version_p ) )
													{
														ext_base_versions[ developer_p->hashCode() ]= base_version_p;
														ext_levels[ developer_p->hashCode() ]= ext_level_p->intValue();
													}
												else if( GetPdfVersionChar( ext_base_versions[ developer_p->hashCode() ] )==
																 GetPdfVersionChar( base_version_p ) &&
																 ext_levels[ developer_p->hashCode() ]< ext_level_p->intValue() )
													{
														ext_levels[ developer_p->hashCode() ]= ext_level_p->intValue();
													}
											}
									}
								}
							}
						}
					}
				writer_p->setPdfVersion( max_version_cc );
				output_doc_p->open();
				if( !ext_base_versions.empty() ) {
					itext::PdfDictionary* extensions_dict_p= new itext::PdfDictionary();
					itext::PdfIndirectReference* extensions_ref_p= writer_p->getPdfIndirectReference();
					for( map< jint, itext::PdfName* >::
								 const_iterator it= ext_base_versions.begin(); it!= ext_base_versions.end(); ++it )
						{
							itext::PdfDictionary* ext_dict_p= new itext::PdfDictionary();
							ext_dict_p->put( itext::PdfName::BASEVERSION, it->second );
							ext_dict_p->put( itext::PdfName::EXTENSIONLEVEL,
															 new itext::PdfNumber( ext_levels[ it->first ] ) );
							extensions_dict_p->put( ext_developers[ it->first ], ext_dict_p );
						}
					writer_p->addToBody( extensions_dict_p, extensions_ref_p );
					writer_p->setExtensions( extensions_ref_p );
				}
				if( m_operation== shuffle_k ) {
					unsigned int max_seq_length= 0;
					for( vector< vector< PageRef > >::const_iterator jt= m_page_seq.begin();
							 jt!= m_page_seq.end(); ++jt )
						{
							max_seq_length= ( max_seq_length< jt->size() ) ? jt->size() : max_seq_length;
						}
					int output_page_count= 0;
					for( unsigned int ii= 0; ( ii< max_seq_length && ret_val== 0 ); ++ii ) {
						for( vector< vector< PageRef > >::const_iterator jt= m_page_seq.begin();
								 ( jt!= m_page_seq.end() && ret_val== 0 ); ++jt )
							{
								if( ii< jt->size() ) {
									ret_val= create_output_page( writer_p, (*jt)[ii], output_page_count );
									++output_page_count;
								}
							}
					}
				}
				else {
					int output_page_count= 0;
					for( vector< vector< PageRef > >::const_iterator jt= m_page_seq.begin();
							 ( jt!= m_page_seq.end() && ret_val== 0 ); ++jt )
						{
							for( vector< PageRef >::const_iterator it= jt->begin();
									 ( it!= jt->end() && ret_val== 0 ); ++it, ++output_page_count )
								{
									ret_val= create_output_page( writer_p, *it, output_page_count );
								}
						}
					if( m_cat_full_pdfs_b ) {
						itext::PdfDictionary* output_outlines_p=
							new itext::PdfDictionary( itext::PdfName::OUTLINES );
						itext::PdfIndirectReference* output_outlines_ref_p=
							writer_p->getPdfIndirectReference();
						itext::PdfDictionary* after_child_p= 0;
						itext::PdfIndirectReference* after_child_ref_p= 0;
						int page_count= 1;
						int num_bookmarks_total= 0;
						for( vector< vector< PageRef > >::const_iterator jt= m_page_seq.begin();
								 jt!= m_page_seq.end(); ++jt )
							{
								itext::PdfReader* reader_p=
									m_input_pdf[ jt->begin()->m_input_pdf_index ].m_readers.begin()->second;
								int reader_page_count=
									m_input_pdf[ jt->begin()->m_input_pdf_index ].m_num_pages;
								{
									itext::PdfDictionary* catalog_p= reader_p->getCatalog();
									itext::PdfDictionary* outlines_p= (itext::PdfDictionary*)
										reader_p->getPdfObject( catalog_p->get( itext::PdfName::OUTLINES ) );
									if( outlines_p && outlines_p->isDictionary() ) {
										itext::PdfDictionary* top_outline_p= (itext::PdfDictionary*)
											reader_p->getPdfObject( outlines_p->get( itext::PdfName::FIRST ) );
										if( top_outline_p && top_outline_p->isDictionary() ) {
											vector<PdfBookmark> bookmark_data;
											int rr= ReadOutlines( bookmark_data, top_outline_p, 0, reader_p, true );
											if( rr== 0 && !bookmark_data.empty() ) {
												vector<PdfBookmark>::const_iterator vit= bookmark_data.begin();
												BuildBookmarks( writer_p,
																				vit, bookmark_data.end(),
																				output_outlines_p, output_outlines_ref_p,
																				after_child_p, after_child_ref_p,
																				after_child_p, after_child_ref_p,
																				0, num_bookmarks_total,
																				page_count- 1,
																				0,
																				true );
											}
										}
									}
								}
								page_count+= reader_page_count;
							}
						if( num_bookmarks_total ) {
							if( after_child_p && after_child_ref_p )
								writer_p->addToBody( after_child_p, after_child_ref_p );
							writer_p->addToBody( output_outlines_p, output_outlines_ref_p );
							writer_p->setOutlines( output_outlines_ref_p );
						}
					}
				}
				output_doc_p->close();
				writer_p->close();
			}
			break;
			case burst_k : {
				if( 1< m_input_pdf.size() ) {
					cerr << "Error: Only one input PDF file may be given for \"burst\" op." << endl;
					cerr << "   No output created." << endl;
					break;
				}
				itext::PdfReader* input_reader_p=
					m_input_pdf.begin()->m_readers.front().second;
				jint input_num_pages=
					m_input_pdf.begin()->m_num_pages;
				if( m_output_filename== "PROMPT" ) {
					prompt_for_filename( "Please enter a filename pattern for the PDF pages (e.g. pg_%04d.pdf):",
															 m_output_filename );
				}
				if( m_output_filename.empty() ) {
					m_output_filename= "pg_%04d.pdf";
				}
				itext::PdfDictionary* input_info_p= 0; {
					itext::PdfDictionary* input_trailer_p= input_reader_p->getTrailer();
					if( input_trailer_p && input_trailer_p->isDictionary() ) {
						input_info_p= (itext::PdfDictionary*)
							input_reader_p->getPdfObject( input_trailer_p->get( itext::PdfName::INFO ) );
						if( input_info_p && input_info_p->isDictionary() ) {
						}
						else {
							input_info_p= 0;
						}
					}
				}
				for( jint ii= 0; ii< input_num_pages; ++ii ) {
					char buff[4096]= "";
					sprintf( buff, m_output_filename.c_str(), ii+ 1 );
					java::String* jv_output_filename_p= JvNewStringUTF( buff );
					itext::Document* output_doc_p= new itext::Document();
					java::FileOutputStream* ofs_p= new java::FileOutputStream( jv_output_filename_p );
					itext::PdfCopy* writer_p= new itext::PdfCopy( output_doc_p, ofs_p );
					writer_p->setFromReader( input_reader_p );
					output_doc_p->addCreator( jv_creator_p );
					if( m_output_uncompress_b ) {
						writer_p->filterStreams= true;
						writer_p->compressStreams= false;
					}
					else if( m_output_compress_b ) {
						writer_p->filterStreams= false;
						writer_p->compressStreams= true;
					}
					if( m_output_encryption_strength!= none_enc ||
							!m_output_owner_pw.empty() ||
							!m_output_user_pw.empty() )
						{
							jboolean bit128_b=
								( m_output_encryption_strength!= bits40_enc );
							writer_p->setEncryption( output_user_pw_p,
																			 output_owner_pw_p,
																			 m_output_user_perms,
																			 bit128_b );
						}
					output_doc_p->open();
					{
						if( input_info_p ) {
							itext::PdfDictionary* writer_info_p= writer_p->getInfo();
							if( writer_info_p ) {
								itext::PdfDictionary* info_copy_p= writer_p->copyDictionary( input_info_p );
								if( info_copy_p ) {
									writer_info_p->putAll( info_copy_p );
								}
							}
						}
						jbyteArray input_reader_xmp_p= input_reader_p->getMetadata();
						if( input_reader_xmp_p ) {
							writer_p->setXmpMetadata( input_reader_xmp_p );
						}
					}
					itext::PdfImportedPage* page_p=
						writer_p->getImportedPage( input_reader_p, ii+ 1 );
					writer_p->addPage( page_p );
					output_doc_p->close();
					writer_p->close();
				}
				string doc_data_fn= "doc_data.txt";
				if( !m_output_filename.empty() ) {
					const char path_delim= PATH_DELIM;
					string::size_type loc= 0;
					if( (loc=m_output_filename.rfind( path_delim ))!= string::npos ) {
						doc_data_fn= m_output_filename.substr( 0, loc )+
							((char)PATH_DELIM)+ doc_data_fn;
					}
				}
				ofstream ofs( doc_data_fn.c_str() );
				if( ofs ) {
					ReportOnPdf( ofs, input_reader_p, m_output_utf8_b );
				}
				else {
					cerr << "Error: unable to open file for output: doc_data.txt" << endl;
					ret_val= 1;
				}
			}
			break;
#ifdef BARCODE_BURST
			case barcode_burst_k : {
				if( 1< m_input_pdf.size() ) {
					cerr << "Error: Only one input PDF file may be given for \"barcode_burst\" op." << endl;
					cerr << "   No output created." << endl;
					break;
				}
				itext::PdfReader* input_reader_p=
					m_input_pdf.begin()->m_readers.front().second;
				jint input_num_pages=
					m_input_pdf.begin()->m_num_pages;
				if( m_output_filename== "PROMPT" ) {
					prompt_for_filename( "Please enter a filename pattern for the PDF pages (e.g. pg_%04d.pdf):",
															 m_output_filename );
				}
				if( m_output_filename.empty() ) {
					m_output_filename= "sec_%04d.pdf";
				}
				vector< int > stream_lengths;
				int stream_length_min= 0;
				int stream_length_mid= 0;
				int stream_length_max= 0;
				jint ii= 0;
				for( ; ii< input_num_pages; ++ii ) {
					int stream_length= -1;
					itext::PdfDictionary* page_p= input_reader_p->getPageN( ii+ 1 );
					if( page_p && page_p->isDictionary() ) {
						itext::PdfDictionary* page_res_p= (itext::PdfDictionary*)
							input_reader_p->getPdfObject( page_p->get( itext::PdfName::RESOURCES ) );
						if( page_res_p && page_res_p->isDictionary() ) {
							itext::PdfDictionary* res_xobj_p= (itext::PdfDictionary*)
								input_reader_p->getPdfObject( page_res_p->get( itext::PdfName::XOBJECT ) );
							if( res_xobj_p && res_xobj_p->isDictionary() ) {
								java::Set* xobj_keys_p= res_xobj_p->getKeys();
								if( xobj_keys_p->size()== 1 ) {
									itext::PdfName* image_key_p= (itext::PdfName*)
										input_reader_p->getPdfObject( (itext::PdfObject*)(xobj_keys_p->iterator()->next()) );
									if( image_key_p && image_key_p->isName() ) {
										itext::PdfStream* image_p= (itext::PdfStream*)
											input_reader_p->getPdfObject( res_xobj_p->get( image_key_p ) );
										if( image_p && image_p->isStream() ) {
											itext::PdfNumber* image_len_p= (itext::PdfNumber*)
												input_reader_p->getPdfObject( image_p->get( itext::PdfName::LENGTH ) );
											if( image_len_p && image_len_p->isNumber() ) {
												stream_length= (int)image_len_p->intValue();
											}}}}}}
						if( stream_length== -1 ) {
							itext::PdfStream* page_cont_p= (itext::PdfStream*)
								input_reader_p->getPdfObject( page_p->get( itext::PdfName::CONTENTS ) );
							if( page_cont_p && page_cont_p->isStream() ) {
								itext::PdfNumber* cont_len_p= (itext::PdfNumber*)
									input_reader_p->getPdfObject( page_cont_p->get( itext::PdfName::LENGTH ) );
								if( cont_len_p && cont_len_p->isNumber() ) {
									stream_length= (int)cont_len_p->intValue();
								}
							}
						}
					}
					else {
						cerr << "Error: barcode_burst - didn't find page." << endl;
						break;
					}
					if( stream_length!= -1 ) {
						stream_lengths.push_back( stream_length );
						if( stream_length_min== 0 && stream_length_max== 0 ) {
							stream_length_min= stream_length;
							stream_length_max= stream_length;
						}
						else {
							stream_length_max= (stream_length_max< stream_length) ? stream_length : stream_length_max;
						}
					}
					else {
						cerr << "Error: barcode_burst - didn't find page image/content." << endl;
						break;
					}
				}
				if( ii< input_num_pages ) {
					cerr << "Error: Unable to find document information needed for" << endl;
					cerr << "    the barcode_burst.  Maybe the input PDF isn't from a scan?" << endl;
					ret_val= 1;
					break;
				}
				stream_length_mid= (int)(stream_length_min* 1.15);
				itext::PdfDictionary* input_info_p= 0; {
					itext::PdfDictionary* input_trailer_p= input_reader_p->getTrailer();
					if( input_trailer_p && input_trailer_p->isDictionary() ) {
						input_info_p= (itext::PdfDictionary*)
							input_reader_p->getPdfObject( input_trailer_p->get( itext::PdfName::INFO ) );
						if( input_info_p && input_info_p->isDictionary() ) {
						}
						else {
							input_info_p= 0;
						}
					}
				}
				int jj= 0;
				for( jint ii= 0; ii< input_num_pages; ++jj ) {
					jint num_sec_pages= 0;
					char buff[4096]= "";
					sprintf( buff, m_output_filename.c_str(), jj+ 1 );
					java::String* jv_output_filename_p= JvNewStringUTF( buff );
					itext::Document* output_doc_p= new itext::Document();
					java::FileOutputStream* ofs_p= new java::FileOutputStream( jv_output_filename_p );
					itext::PdfCopy* writer_p= new itext::PdfCopy( output_doc_p, ofs_p );
					output_doc_p->addCreator( jv_creator_p );
					if( m_output_uncompress_b ) {
						writer_p->filterStreams= true;
						writer_p->compressStreams= false;
					}
					else if( m_output_compress_b ) {
						writer_p->filterStreams= false;
						writer_p->compressStreams= true;
					}
					if( m_output_encryption_strength!= none_enc ||
							!m_output_owner_pw.empty() ||
							!m_output_user_pw.empty() )
						{
							bool bit128_b=
								( m_output_encryption_strength!= bits40_enc );
							writer_p->setEncryption( output_user_pw_p,
																			 output_owner_pw_p,
																			 m_output_user_perms,
																			 bit128_b );
						}
					{
						if( input_info_p ) {
							itext::PdfDictionary* writer_info_p= writer_p->getInfo();
							itext::PdfDictionary* info_copy_p= writer_p->copyDictionary( input_info_p );
							if( writer_info_p && info_copy_p ) {
								writer_info_p->putAll( info_copy_p );
							}
						}
						jbyteArray input_reader_xmp_p= input_reader_p->getMetadata();
						if( input_reader_xmp_p ) {
							writer_p->setXmpMetadata( input_reader_xmp_p );
						}
					}
					output_doc_p->open();
					while( num_sec_pages== 0 ||
								 ( ii< input_num_pages && stream_length_mid< stream_lengths[ii] ) )
						{
							itext::PdfImportedPage* page_p=
								writer_p->getImportedPage( input_reader_p, ii+ 1 );
							writer_p->addPage( page_p );
							++ii; ++num_sec_pages;
						}
					output_doc_p->close();
					writer_p->close();
				}
			}
			break;
#endif
			case filter_k: {
				if( 1< m_input_pdf.size() ) {
					cerr << "Error: Only one input PDF file may be given for this" << endl;
					cerr << "   operation.  Maybe you meant to use the \"cat\" operator?" << endl;
					cerr << "   No output created." << endl;
					ret_val= 1;
					break;
				}
				itext::FdfReader* fdf_reader_p= 0;
				itext::XfdfReader* xfdf_reader_p= 0;
				if( m_form_data_filename== "PROMPT" ) {
					prompt_for_filename( "Please enter a filename for the form data:",
															 m_form_data_filename );
				}
				if( !m_form_data_filename.empty() ) {
					if( m_form_data_filename== "-" ) {
						try {
							fdf_reader_p= new itext::FdfReader( java::System::in );
						}
						catch( java::io::IOException* ioe_p ) {
							try {
								xfdf_reader_p= new itext::XfdfReader( java::System::in );
							}
							catch( java::io::IOException* ioe_p ) {
								cerr << "Error: Failed read form data on stdin." << endl;
								cerr << "   No output created." << endl;
								ret_val= 1;
								break;
							}
						}
					}
					else {
						try {
							fdf_reader_p=
								new itext::FdfReader( JvNewStringUTF( m_form_data_filename.c_str() ) );
						}
						catch( java::io::IOException* ioe_p ) {
							try {
								xfdf_reader_p=
									new itext::XfdfReader( JvNewStringUTF( m_form_data_filename.c_str() ) );
							}
							catch( java::io::IOException* ioe_p ) {
								cerr << "Error: Failed to open form data file: " << endl;
								cerr << "   " << m_form_data_filename << endl;
								cerr << "   No output created." << endl;
								ret_val= 1;
								break;
							}
						}
					}
				}
				itext::PdfReader* mark_p= 0;
				bool background_b= true;
				if( m_background_filename== "PROMPT" ) {
					prompt_for_filename( "Please enter a filename for the background PDF:",
															 m_background_filename );
				}
				if( !m_background_filename.empty() ) {
					try {
						mark_p= new itext::PdfReader( JvNewStringUTF( m_background_filename.c_str() ) );
						mark_p->removeUnusedObjects();
					}
					catch( java::io::IOException* ioe_p ) {
						cerr << "Error: Failed to open background PDF file: " << endl;
						cerr << "   " << m_background_filename << endl;
						cerr << "   No output created." << endl;
						ret_val= 1;
						break;
					}
				}
				if( !mark_p ) {
					if( m_stamp_filename== "PROMPT" ) {
						prompt_for_filename( "Please enter a filename for the stamp PDF:",
																 m_stamp_filename );
					}
					if( !m_stamp_filename.empty() ) {
						background_b= false;
						try {
							mark_p= new itext::PdfReader( JvNewStringUTF( m_stamp_filename.c_str() ) );
							mark_p->removeUnusedObjects();
						}
						catch( java::io::IOException* ioe_p ) {
							cerr << "Error: Failed to open stamp PDF file: " << endl;
							cerr << "   " << m_stamp_filename << endl;
							cerr << "   No output created." << endl;
							ret_val= 1;
							break;
						}
					}
				}
				java::OutputStream* ofs_p= get_output_stream( m_output_filename, m_ask_about_warnings_b );
				if( !ofs_p ) {
					cerr << "Error: unable to open file for output: " << m_output_filename << endl;
					ret_val= 1;
					break;
				}
				itext::PdfReader* input_reader_p= m_input_pdf.begin()->m_readers.front().second;
				if( m_output_drop_xfa_b ) {
					itext::PdfDictionary* catalog_p= input_reader_p->catalog;
					if( catalog_p && catalog_p->isDictionary() ) {
						itext::PdfDictionary* acro_form_p= (itext::PdfDictionary*)
							input_reader_p->getPdfObject( catalog_p->get( itext::PdfName::ACROFORM ) );
						if( acro_form_p && acro_form_p->isDictionary() ) {
							acro_form_p->remove( itext::PdfName::XFA );
						}
					}
				}
				if( m_output_drop_xmp_b ) {
					itext::PdfDictionary* catalog_p= input_reader_p->catalog;
					if( catalog_p && catalog_p->isDictionary() ) {
						catalog_p->remove( itext::PdfName::METADATA );
					}
				}
				itext::PdfStamperImp* writer_p=
					new itext::PdfStamperImp( input_reader_p, ofs_p, 0, false  );
				if( m_update_info_filename== "PROMPT" ) {
					prompt_for_filename( "Please enter an Info file filename:",
															 m_update_info_filename );
				}
				if( !m_update_info_filename.empty() ) {
					if( m_update_info_filename== "-" ) {
						if( !UpdateInfo( input_reader_p, cin, m_update_info_utf8_b ) ) {
							cerr << "Warning: no Info added to output PDF." << endl;
							ret_val= 3;
						}
					}
					else {
						ifstream ifs( m_update_info_filename.c_str() );
						if( ifs ) {
							if( !UpdateInfo( input_reader_p, ifs, m_update_info_utf8_b ) ) {
								cerr << "Warning: no Info added to output PDF." << endl;
								ret_val= 3;
							}
						}
						else {
							cerr << "Error: unable to open FDF file for input: " << m_update_info_filename << endl;
							ret_val= 1;
							break;
						}
					}
				}
				if( !m_page_seq.empty() ) {
					for( vector< vector< PageRef > >::const_iterator jt= m_page_seq.begin();
							 jt!= m_page_seq.end(); ++jt ) {
						for( vector< PageRef >::const_iterator kt= jt->begin(); kt!= jt->end(); ++kt ) {
							apply_rotation_to_page( input_reader_p, (*kt).m_page_num,
																			(*kt).m_page_rot, (*kt).m_page_abs );
						}
					}
				}
				if( m_output_uncompress_b ) {
					add_marks_to_pages( input_reader_p );
					writer_p->filterStreams= true;
					writer_p->compressStreams= false;
				}
				else if( m_output_compress_b ) {
					remove_marks_from_pages( input_reader_p );
					writer_p->filterStreams= false;
					writer_p->compressStreams= true;
				}
				if( m_output_encryption_strength!= none_enc ||
						!m_output_owner_pw.empty() ||
						!m_output_user_pw.empty() )
					{
						jboolean bit128_b=
							( m_output_encryption_strength!= bits40_enc );
						writer_p->setEncryption( output_user_pw_p,
																			output_owner_pw_p,
																			m_output_user_perms,
																			bit128_b );
					}
				if( fdf_reader_p || xfdf_reader_p ) {
					if( input_reader_p->getAcroForm() ) {
						itext::AcroFields* fields_p= writer_p->getAcroFields();
						fields_p->setGenerateAppearances( true );
						if( ( fdf_reader_p && fields_p->setFields( fdf_reader_p ) ) ||
								( xfdf_reader_p && fields_p->setFields( xfdf_reader_p ) ) )
							{
								m_output_need_appearances_b= true;
							}
					}
					else {
						cerr << "Warning: input PDF is not an acroform, so its fields were not filled." << endl;
						ret_val= 3;
					}
				}
				writer_p->setFormFlattening( m_output_flatten_b );
				if( m_output_need_appearances_b ) {
					itext::PdfDictionary* catalog_p= input_reader_p->catalog;
					if( catalog_p && catalog_p->isDictionary() ) {
						itext::PdfDictionary* acro_form_p= (itext::PdfDictionary*)
							input_reader_p->getPdfObject( catalog_p->get( itext::PdfName::ACROFORM ) );
						if( acro_form_p && acro_form_p->isDictionary() ) {
							acro_form_p->put( itext::PdfName::NEEDAPPEARANCES,
																itext::PdfBoolean::PDFTRUE );
						}
					}
				}
				if( mark_p ) {
					jint mark_num_pages= 1;
					if( m_multistamp_b || m_multibackground_b ) {
						mark_num_pages= mark_p->getNumberOfPages();
					}
					itext::PdfImportedPage* mark_page_p= 0;
					itext::Rectangle* mark_page_size_p= 0;
					jint mark_page_rotation= 0;
					jint num_pages= input_reader_p->getNumberOfPages();
					for( jint ii= 0; ii< num_pages; ) {
						++ii;
						if( ii<= mark_num_pages ) {
							mark_page_size_p= mark_p->getCropBox( ii );
							mark_page_rotation= mark_p->getPageRotation( ii );
							for( jint mm= 0; mm< mark_page_rotation; mm+=90 ) {
								mark_page_size_p= mark_page_size_p->rotate();
							}
							mark_page_p= writer_p->getImportedPage( mark_p, ii );
						}
						itext::Rectangle* doc_page_size_p=
							input_reader_p->getCropBox( ii );
						jint doc_page_rotation= input_reader_p->getPageRotation( ii );
						for( jint mm= 0; mm< doc_page_rotation; mm+=90 ) {
							doc_page_size_p= doc_page_size_p->rotate();
						}
						jfloat h_scale= doc_page_size_p->width() / mark_page_size_p->width();
						jfloat v_scale= doc_page_size_p->height() / mark_page_size_p->height();
						jfloat mark_scale= (h_scale< v_scale) ? h_scale : v_scale;
						jfloat h_trans= (jfloat)(doc_page_size_p->left()- mark_page_size_p->left()* mark_scale +
																		 (doc_page_size_p->width()-
																			mark_page_size_p->width()* mark_scale) / 2.0);
						jfloat v_trans= (jfloat)(doc_page_size_p->bottom()- mark_page_size_p->bottom()* mark_scale +
																		 (doc_page_size_p->height()-
																			mark_page_size_p->height()* mark_scale) / 2.0);
						itext::PdfContentByte* content_byte_p=
							( background_b ) ? writer_p->getUnderContent( ii ) : writer_p->getOverContent( ii );
						if( mark_page_rotation== 0 ) {
							content_byte_p->addTemplate( mark_page_p,
																					 mark_scale, 0,
																					 0, mark_scale,
																					 h_trans,
																					 v_trans );
						}
						else if( mark_page_rotation== 90 ) {
							content_byte_p->addTemplate( mark_page_p,
																					 0, -1* mark_scale,
																					 mark_scale, 0,
																					 h_trans,
																					 v_trans+ mark_page_size_p->height()* mark_scale );
						}
						else if( mark_page_rotation== 180 ) {
							content_byte_p->addTemplate( mark_page_p,
																					 -1* mark_scale, 0,
																					 0, -1* mark_scale,
																					 h_trans+ mark_page_size_p->width()* mark_scale,
																					 v_trans+ mark_page_size_p->height()* mark_scale );
						}
						else if( mark_page_rotation== 270 ) {
							content_byte_p->addTemplate( mark_page_p,
																					 0, mark_scale,
																					 -1* mark_scale, 0,
																					 h_trans+ mark_page_size_p->width()* mark_scale, v_trans );
						}
					}
				}
				if( !m_input_attach_file_filename.empty() ) {
					this->attach_files( input_reader_p,
															writer_p );
				}
				input_reader_p->removeUnusedObjects();
				writer_p->close();
			}
			break;
			case dump_data_fields_k :
			case dump_data_annots_k :
			case dump_data_k: {
				if( 1< m_input_pdf.size() ) {
					cerr << "Error: Only one input PDF file may be used for the dump_data operation" << endl;
					cerr << "   No output created." << endl;
					ret_val= 1;
					break;
				}
				itext::PdfReader* input_reader_p=
					m_input_pdf.begin()->m_readers.front().second;
				if( m_output_filename.empty() || m_output_filename== "-" ) {
					if( m_operation== dump_data_k ) {
						ReportOnPdf( cout, input_reader_p, m_output_utf8_b );
					}
					else if( m_operation== dump_data_fields_k ) {
						ReportAcroFormFields( cout, input_reader_p, m_output_utf8_b );
					}
					else if( m_operation== dump_data_annots_k ) {
						ReportAnnots( cout, input_reader_p, m_output_utf8_b );
					}
				}
				else {
					ofstream ofs( m_output_filename.c_str() );
					if( ofs ) {
						if( m_operation== dump_data_k ) {
							ReportOnPdf( ofs, input_reader_p, m_output_utf8_b );
						}
						else if( m_operation== dump_data_fields_k ) {
							ReportAcroFormFields( ofs, input_reader_p, m_output_utf8_b );
						}
						else if( m_operation== dump_data_annots_k ) {
							ReportAnnots( ofs, input_reader_p, m_output_utf8_b );
						}
					}
					else {
						cerr << "Error: unable to open file for output: " << m_output_filename << endl;
					}
				}
			}
			break;
			case generate_fdf_k : {
				if( 1< m_input_pdf.size() ) {
					cerr << "Error: Only one input PDF file may be used for the generate_fdf operation" << endl;
					cerr << "   No output created." << endl;
					break;
				}
				itext::PdfReader* input_reader_p=
					m_input_pdf.begin()->m_readers.front().second;
				java::OutputStream* ofs_p=
					get_output_stream( m_output_filename,
														 m_ask_about_warnings_b );
				if( ofs_p ) {
					itext::FdfWriter* writer_p= new itext::FdfWriter();
					input_reader_p->getAcroFields()->exportAsFdf( writer_p );
					writer_p->writeTo( ofs_p );
				}
				else {
					ret_val= 1;
					break;
				}
			}
				break;
			case unpack_files_k: {
				if( 1< m_input_pdf.size() ) {
					cerr << "Error: Only one input PDF file may be given for \"unpack_files\" op." << endl;
					cerr << "   No output created." << endl;
					ret_val= 1;
					break;
				}
				itext::PdfReader* input_reader_p=
					m_input_pdf.begin()->m_readers.front().second;
				this->unpack_files( input_reader_p );
			}
				break;
			default:
				cerr << "Unexpected pdftk Error in create_output()" << endl;
				ret_val= 2;
				break;
			}
		}
		catch( java::lang::Throwable* t_p )
			{
				cerr << "Unhandled Java Exception in create_output():" << endl;
				t_p->printStackTrace();
				ret_val= 2;
			}
	}
	else {
		ret_val= 1;
	}
	return ret_val;
}
#ifdef WIN32
#include "win32_utf8_include.cc"
int win32_utf8_main( int argc, char *argv[] )
#else
int main( int argc, char *argv[] )
#endif
{
	bool help_b= false;
	bool version_b= false;
	bool synopsis_b= ( argc== 1 );
	int ret_val= 0;
#ifdef UNBLOCK_SIGNALS
	sigset_t sigmask;
	sigemptyset( &sigmask );
	sigprocmask( SIG_SETMASK, &sigmask, 0 );
#endif
#ifdef WIN32
	static char my_lang[]= "LANG=C";
	_putenv( my_lang );
#else
	static char my_lang[]= "LANG=C";
	putenv( my_lang );
#endif
	for( int ii= 1; ii< argc; ++ii ) {
		version_b=
			(version_b ||
			 strcmp( argv[ii], "--version" )== 0  ||
			 strcmp( argv[ii], "-version" )== 0 );
		help_b=
			(strcmp( argv[ii], "--help" )== 0 ||
			 strcmp( argv[ii], "-help" )== 0 ||
			 strcmp( argv[ii], "-h" )== 0 );
	}
	if( help_b ) {
		describe_full();
	}
	else if( version_b ) {
		describe_header();
	}
	else if( synopsis_b ) {
		describe_synopsis();
	}
	else {
		try {
			JvCreateJavaVM(NULL);
			JvAttachCurrentThread(NULL, NULL);
			JvInitClass(&java::System::class$);
			JvInitClass(&java::io::IOException::class$);
			JvInitClass(&java::lang::Throwable::class$);
			JvInitClass(&java::String::class$);
			JvInitClass(&java::HashMap::class$);
			JvInitClass(&java::Vector::class$);
			JvInitClass(&java::OutputStream::class$);
			JvInitClass(&java::FileOutputStream::class$);
			JvInitClass(&java::Set::class$);
			JvInitClass(&java::Iterator::class$);
			JvInitClass(&java::util::ArrayList::class$);
			JvInitClass(&java::util::Iterator::class$);
			JvInitClass(&java::util::Locale::class$);
			JvInitClass(&java::util::TimeZone::class$);
			JvInitClass(&java::util::Calendar::class$);
			JvInitClass(&java::util::GregorianCalendar::class$);
#ifdef WIN32
			JvInitClass(&gnu::java::locale::Calendar::class$);
			JvInitClass(&gnu::java::locale::LocaleInformation::class$);
			JvInitClass(&gnu::java::locale::Calendar_de::class$);
			JvInitClass(&gnu::java::locale::Calendar_en::class$);
			JvInitClass(&gnu::java::locale::Calendar_nl::class$);
			JvInitClass(&gnu::java::locale::LocaleInformation_de::class$);
			JvInitClass(&gnu::java::locale::LocaleInformation_en::class$);
			JvInitClass(&gnu::java::locale::LocaleInformation_nl::class$);
#endif
			JvInitClass(&gnu::gcj::convert::Input_UTF8::class$);
			JvInitClass(&gnu::gcj::convert::Input_8859_1::class$);
			JvInitClass(&gnu::gcj::convert::Input_ASCII::class$);
			JvInitClass(&itext::Document::class$);
			JvInitClass(&itext::Rectangle::class$);
			JvInitClass(&itext::PdfObject::class$);
			JvInitClass(&itext::PdfIndirectReference::class$);
			JvInitClass(&itext::PdfName::class$);
			JvInitClass(&itext::PdfBoolean::class$);
			JvInitClass(&itext::PdfNumber::class$);
			JvInitClass(&itext::PdfArray::class$);
			JvInitClass(&itext::PdfString::class$);
			JvInitClass(&itext::PdfDictionary::class$);
			JvInitClass(&itext::PdfStream::class$);
			JvInitClass(&itext::RandomAccessFileOrArray::class$);
			JvInitClass(&itext::PdfContentByte::class$);
			JvInitClass(&itext::PdfNameTree::class$);
			JvInitClass(&itext::PdfOutline::class$);
			JvInitClass(&itext::PdfDestination::class$);
			JvInitClass(&itext::PdfAnnotation::class$);
			JvInitClass(&itext::PdfFileSpecification::class$);
			JvInitClass(&itext::AcroFields::class$);
			JvInitClass(&itext::PdfReader::class$);
			JvInitClass(&itext::PdfWriter::class$);
			JvInitClass(&itext::PdfCopy::class$);
			JvInitClass(&itext::FdfReader::class$);
			JvInitClass(&itext::FdfWriter::class$);
			JvInitClass(&itext::XfdfReader::class$);
			JvInitClass(&itext::PdfStamperImp::class$);
			JvInitClass(&itext::PdfImportedPage::class$);
			TK_Session tk_session( argc, argv );
			tk_session.dump_session_data();
			if( tk_session.is_valid() ) {
				ret_val= tk_session.create_output();
			}
			else {
				cerr << "Done.  Input errors, so no output created." << endl;
				ret_val= 1;
			}
		}
		catch(java::lang::ClassCastException* c_p ) {
			jstring message= c_p->getMessage();
			if( message->indexOf( JvNewStringUTF( "com.lowagie.text.pdf.PdfDictionary" ) )>= 0 &&
					message->indexOf( JvNewStringUTF( "com.lowagie.text.pdf.PRIndirectReference" ) )>= 0 )
			{
				cerr << "Error: One input PDF seems to not conform to the PDF standard." << endl;
				cerr << "Perhaps the document information dictionary is a direct object" << endl;
				cerr << "   instead of an indirect reference." << endl;
				cerr << "Please report this bug to the program which produced the PDF." << endl;
				cerr << endl;
			}
			cerr << "Java Exception:" << endl;
			c_p->printStackTrace();
			ret_val= 1;
		}
		catch( java::lang::Throwable* t_p ) {
				cerr << "Unhandled Java Exception in main():" << endl;
				t_p->printStackTrace();
				ret_val= 2;
		}
		try {
			JvDetachCurrentThread();
		}
		catch( java::lang::Throwable* t_p ) {
				cerr << "Unhandled Java Exception running JvDetachCurrentThread():" << endl;
				t_p->printStackTrace();
				ret_val= 2;
		}
	}
	return ret_val;
}
static void
describe_header() {
	cout << endl;
	cout << "pdftk " << PDFTK_VER << " a Handy Tool for Manipulating PDF Documents" << endl;
	cout << "Copyright (c) 2003-13 Steward and Lee, LLC - Please Visit: www.pdftk.com" << endl;
	cout << "This is free software; see the source code for copying conditions. There is" << endl;
	cout << "NO warranty, not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE." << endl;
}
static void
describe_synopsis() {
	cout <<
"SYNOPSIS\n\
       pdftk <input PDF files | - | PROMPT>\n\
	    [ input_pw <input PDF owner passwords | PROMPT> ]\n\
	    [ <operation> <operation arguments> ]\n\
	    [ output <output filename | - | PROMPT> ]\n\
	    [ encrypt_40bit | encrypt_128bit ]\n\
	    [ allow <permissions> ]\n\
	    [ owner_pw <owner password | PROMPT> ]\n\
	    [ user_pw <user password | PROMPT> ]\n\
	    [ flatten ] [ need_appearances ]\n\
	    [ compress | uncompress ]\n\
	    [ keep_first_id | keep_final_id ] [ drop_xfa ] [ drop_xmp ]\n\
	    [ verbose ] [ dont_ask | do_ask ]\n\
       Where:\n\
	    <operation> may be empty, or:\n\
	    [ cat | shuffle | burst | rotate |\n\
	      generate_fdf | fill_form |\n\
	      background | multibackground |\n\
	      stamp | multistamp |\n\
	      dump_data | dump_data_utf8 |\n\
	      dump_data_fields | dump_data_fields_utf8 |\n\
	      dump_data_annots |\n\
	      update_info | update_info_utf8 |\n\
	      attach_files | unpack_files ]\n\
\n\
       For Complete Help: pdftk --help\n";
}
static void
describe_full() {
	describe_header();
	cout << endl;
	describe_synopsis();
	cout << endl;
	cout <<
"DESCRIPTION\n\
       If PDF is electronic paper, then pdftk is an electronic staple-remover,\n\
       hole-punch, binder, secret-decoder-ring, and X-Ray-glasses.  Pdftk is a\n\
       simple tool for doing everyday things with PDF documents.  Use it to:\n\
\n\
       * Merge PDF Documents or Collate PDF Page Scans\n\
       * Split PDF Pages into a New Document\n\
       * Rotate PDF Documents or Pages\n\
       * Decrypt Input as Necessary (Password Required)\n\
       * Encrypt Output as Desired\n\
       * Fill PDF Forms with X/FDF Data and/or Flatten Forms\n\
       * Generate FDF Data Stencils from PDF Forms\n\
       * Apply a Background Watermark or a Foreground Stamp\n\
       * Report PDF Metrics, Bookmarks and Metadata\n\
       * Add/Update PDF Bookmarks or Metadata\n\
       * Attach Files to PDF Pages or the PDF Document\n\
       * Unpack PDF Attachments\n\
       * Burst a PDF Document into Single Pages\n\
       * Uncompress and Re-Compress Page Streams\n\
       * Repair Corrupted PDF (Where Possible)\n\
\n\
OPTIONS\n\
       A summary of options is included below.\n\
\n\
       --help, -h\n\
	      Show this summary of options.\n\
\n\
       <input PDF files | - | PROMPT>\n\
	      A list of the input PDF files. If you plan to combine these PDFs\n\
	      (without using handles) then list files in the order you want\n\
	      them combined.  Use - to pass a single PDF into pdftk via stdin.\n\
	      Input files can be associated with handles, where a handle is\n\
	      one or more upper-case letters:\n\
\n\
	      <input PDF handle>=<input PDF filename>\n\
\n\
	      Handles are often omitted.  They are useful when specifying PDF\n\
	      passwords or page ranges, later.\n\
\n\
	      For example: A=input1.pdf QT=input2.pdf M=input3.pdf\n\
\n\
       [input_pw <input PDF owner passwords | PROMPT>]\n\
	      Input PDF owner passwords, if necessary, are associated with\n\
	      files by using their handles:\n\
\n\
	      <input PDF handle>=<input PDF file owner password>\n\
\n\
	      If handles are not given, then passwords are associated with\n\
	      input files by order.\n\
\n\
	      Most pdftk features require that encrypted input PDF are accom-\n\
	      panied by the ~owner~ password. If the input PDF has no owner\n\
	      password, then the user password must be given, instead.	If the\n\
	      input PDF has no passwords, then no password should be given.\n\
\n\
	      When running in do_ask mode, pdftk will prompt you for a pass-\n\
	      word if the supplied password is incorrect or none was given.\n\
\n\
       [<operation> <operation arguments>]\n\
	      Available operations are: cat, shuffle, burst, rotate, gener-\n\
	      ate_fdf, fill_form, background, multibackground, stamp, multi-\n\
	      stamp, dump_data, dump_data_utf8, dump_data_fields,\n\
	      dump_data_fields_utf8, dump_data_annots, update_info,\n\
	      update_info_utf8, attach_files, unpack_files. Some operations\n\
	      takes additional arguments, described below.\n\
\n\
	      If this optional argument is omitted, then pdftk runs in 'fil-\n\
	      ter' mode.  Filter mode takes only one PDF input and creates a\n\
	      new PDF after applying all of the output options, like encryp-\n\
	      tion and compression.\n\
\n\
	  cat [<page ranges>]\n\
		 Assembles (catenates) pages from input PDFs to create a new\n\
		 PDF. Use cat to merge PDF pages or to split PDF pages from\n\
		 documents. You can also use it to rotate PDF pages. Page\n\
		 order in the new PDF is specified by the order of the given\n\
		 page ranges. Page ranges are described like this:\n\
\n\
		 <input PDF handle>[<begin page number>[-<end page num-\n\
		 ber>[<qualifier>]]][<page rotation>]\n\
\n\
		 Where the handle identifies one of the input PDF files, and\n\
		 the beginning and ending page numbers are one-based refer-\n\
		 ences to pages in the PDF file.  The qualifier can be even or\n\
		 odd, and the page rotation can be north, south, east, west,\n\
		 left, right, or down.\n\
\n\
		 If a PDF handle is given but no pages are specified, then the\n\
		 entire PDF is used. If no pages are specified for any of the\n\
		 input PDFs, then the input PDFs' bookmarks are also merged\n\
		 and included in the output.\n\
\n\
		 If the handle is omitted from the page range, then the pages\n\
		 are taken from the first input PDF.\n\
\n\
		 The even qualifier causes pdftk to use only the even-numbered\n\
		 PDF pages, so 1-6even yields pages 2, 4 and 6 in that order.\n\
		 6-1even yields pages 6, 4 and 2 in that order.\n\
\n\
		 The odd qualifier works similarly to the even.\n\
\n\
		 The page rotation setting can cause pdftk to rotate pages and\n\
		 documents.  Each option sets the page rotation as follows (in\n\
		 degrees): north: 0, east: 90, south: 180, west: 270, left:\n\
		 -90, right: +90, down: +180. left, right, and down make rela-\n\
		 tive adjustments to a page's rotation.\n\
\n\
		 If no arguments are passed to cat, then pdftk combines all\n\
		 input PDFs in the order they were given to create the output.\n\
\n\
		 NOTES:\n\
		 * <end page number> may be less than <begin page number>.\n\
		 * The keyword end may be used to reference the final page of\n\
		 a document instead of a page number.\n\
		 * Reference a single page by omitting the ending page number.\n\
		 * The handle may be used alone to represent the entire PDF\n\
		 document, e.g., B1-end is the same as B.\n\
		 * You can reference page numbers in reverse order by prefix-\n\
		 ing them with the letter r. For example, page r1 is the last\n\
		 page of the document, r2 is the next-to-last page of the doc-\n\
		 ument, and rend is the first page of the document. You can\n\
		 use this prefix in ranges, too, for example r3-r1 is the last\n\
		 three pages of a PDF.\n\
\n\
		 Page Range Examples without Handles:\n\
		 1-endeast - rotate entire document 90 degrees\n\
		 5 11 20 - take single pages from input PDF\n\
		 5-25oddwest - take odd pages in range, rotate 90 degrees\n\
		 6-1 - reverse pages in range from input PDF\n\
\n\
		 Page Range Examples Using Handles:\n\
		 Say A=in1.pdf B=in2.pdf, then:\n\
		 A1-21 - take range from in1.pdf\n\
		 Bend-1odd - take all odd pages from in2.pdf in reverse order\n\
		 A72 - take a single page from in1.pdf\n\
		 A1-21 Beven A72 - assemble pages from both in1.pdf and\n\
		 in2.pdf\n\
		 Awest - rotate entire in1.pdf document 90 degrees\n\
		 B - use all of in2.pdf\n\
		 A2-30evenleft - take the even pages from the range, remove 90\n\
		 degrees from each page's rotation\n\
		 A A - catenate in1.pdf with in1.pdf\n\
		 Aevenwest Aoddeast - apply rotations to even pages, odd pages\n\
		 from in1.pdf\n\
		 Awest Bwest Bdown - catenate rotated documents\n\
\n\
	  shuffle [<page ranges>]\n\
		 Collates pages from input PDFs to create a new PDF.  Works\n\
		 like the cat operation except that it takes one page at a\n\
		 time from each page range to assemble the output PDF.	If one\n\
		 range runs out of pages, it continues with the remaining\n\
		 ranges.  Ranges can use all of the features described above\n\
		 for cat, like reverse page ranges, multiple ranges from a\n\
		 single PDF, and page rotation.  This feature was designed to\n\
		 help collate PDF pages after scanning paper documents.\n\
\n\
	  burst  Splits a single input PDF document into individual pages.\n\
		 Also creates a report named doc_data.txt which is the same as\n\
		 the output from dump_data.  If the output section is omitted,\n\
		 then PDF pages are named: pg_%04d.pdf, e.g.: pg_0001.pdf,\n\
		 pg_0002.pdf, etc.  To name these pages yourself, supply a\n\
		 printf-styled format string via the output section.  For\n\
		 example, if you want pages named: page_01.pdf, page_02.pdf,\n\
		 etc., pass output page_%02d.pdf to pdftk.  Encryption can be\n\
		 applied to the output by appending output options such as\n\
		 owner_pw, e.g.:\n\
\n\
		 pdftk in.pdf burst owner_pw foopass\n\
\n\
	  rotate [<page ranges>]\n\
		 Takes a single input PDF and rotates just the specified\n\
		 pages.  All other pages remain unchanged.  The page order\n\
		 remains unchaged.  Specify the pages to rotate using the same\n\
		 notation as you would with cat, except you omit the pages\n\
		 that you aren't rotating:\n\
\n\
		 [<begin page number>[-<end page number>[<qualifier>]]][<page\n\
		 rotation>]\n\
\n\
		 The qualifier can be even or odd, and the page rotation can\n\
		 be north, south, east, west, left, right, or down.\n\
\n\
		 Each option sets the page rotation as follows (in degrees):\n\
		 north: 0, east: 90, south: 180, west: 270, left: -90, right:\n\
		 +90, down: +180. left, right, and down make relative adjust-\n\
		 ments to a page's rotation.\n\
\n\
		 The given order of the pages doesn't change the page order in\n\
		 the output.\n\
\n\
	  generate_fdf\n\
		 Reads a single input PDF file and generates an FDF file suit-\n\
		 able for fill_form out of it to the given output filename or\n\
		 (if no output is given) to stdout.  Does not create a new\n\
		 PDF.\n\
\n\
	  fill_form <FDF data filename | XFDF data filename | - | PROMPT>\n\
		 Fills the single input PDF's form fields with the data from\n\
		 an FDF file, XFDF file or stdin. Enter the data filename\n\
		 after fill_form, or use - to pass the data via stdin, like\n\
		 so:\n\
\n\
		 pdftk form.pdf fill_form data.fdf output form.filled.pdf\n\
\n\
		 If the input FDF file includes Rich Text formatted data in\n\
		 addition to plain text, then the Rich Text data is packed\n\
		 into the form fields as well as the plain text.  Pdftk also\n\
		 sets a flag that cues Reader/Acrobat to generate new field\n\
		 appearances based on the Rich Text data.  So when the user\n\
		 opens the PDF, the viewer will create the Rich Text appear-\n\
		 ance on the spot.  If the user's PDF viewer does not support\n\
		 Rich Text, then the user will see the plain text data\n\
		 instead.  If you flatten this form before Acrobat has a\n\
		 chance to create (and save) new field appearances, then the\n\
		 plain text field data is what you'll see.\n\
\n\
		 Also see the flatten and need_appearances options.\n\
\n\
	  background <background PDF filename | - | PROMPT>\n\
		 Applies a PDF watermark to the background of a single input\n\
		 PDF.  Pass the background PDF's filename after background\n\
		 like so:\n\
\n\
		 pdftk in.pdf background back.pdf output out.pdf\n\
\n\
		 Pdftk uses only the first page from the background PDF and\n\
		 applies it to every page of the input PDF.  This page is\n\
		 scaled and rotated as needed to fit the input page.  You can\n\
		 use - to pass a background PDF into pdftk via stdin.\n\
\n\
		 If the input PDF does not have a transparent background (such\n\
		 as a PDF created from page scans) then the resulting back-\n\
		 ground won't be visible -- use the stamp operation instead.\n\
\n\
	  multibackground <background PDF filename | - | PROMPT>\n\
		 Same as the background operation, but applies each page of\n\
		 the background PDF to the corresponding page of the input\n\
		 PDF.  If the input PDF has more pages than the stamp PDF,\n\
		 then the final stamp page is repeated across these remaining\n\
		 pages in the input PDF.\n\
\n\
	  stamp <stamp PDF filename | - | PROMPT>\n\
		 This behaves just like the background operation except it\n\
		 overlays the stamp PDF page on top of the input PDF docu-\n\
		 ment's pages.	This works best if the stamp PDF page has a\n\
		 transparent background.\n\
\n\
	  multistamp <stamp PDF filename | - | PROMPT>\n\
		 Same as the stamp operation, but applies each page of the\n\
		 background PDF to the corresponding page of the input PDF.\n\
		 If the input PDF has more pages than the stamp PDF, then the\n\
		 final stamp page is repeated across these remaining pages in\n\
		 the input PDF.\n\
\n\
	  dump_data\n\
		 Reads a single input PDF file and reports its metadata, book-\n\
		 marks (a/k/a outlines), page metrics (media, rotation and\n\
		 labels), data embedded by STAMPtk (see STAMPtk's embed\n\
		 option) and other data to the given output filename or (if no\n\
		 output is given) to stdout.  Non-ASCII characters are encoded\n\
		 as XML numerical entities.  Does not create a new PDF.\n\
\n\
	  dump_data_utf8\n\
		 Same as dump_data excepct that the output is encoded as\n\
		 UTF-8.\n\
\n\
	  dump_data_fields\n\
		 Reads a single input PDF file and reports form field statis-\n\
		 tics to the given output filename or (if no output is given)\n\
		 to stdout. Non-ASCII characters are encoded as XML numerical\n\
		 entities. Does not create a new PDF.\n\
\n\
	  dump_data_fields_utf8\n\
		 Same as dump_data_fields excepct that the output is encoded\n\
		 as UTF-8.\n\
\n\
	  dump_data_annots\n\
		 This operation currently reports only link annotations.\n\
		 Reads a single input PDF file and reports annotation informa-\n\
		 tion to the given output filename or (if no output is given)\n\
		 to stdout. Non-ASCII characters are encoded as XML numerical\n\
		 entities. Does not create a new PDF.\n\
\n\
	  update_info <info data filename | - | PROMPT>\n\
		 Changes the bookmarks and metadata in a single PDF's Info\n\
		 dictionary to match the input data file. The input data file\n\
		 uses the same syntax as the output from dump_data. Non-ASCII\n\
		 characters should be encoded as XML numerical entities.\n\
\n\
		 This operation does not change the metadata stored in the\n\
		 PDF's XMP stream, if it has one. (For this reason you should\n\
		 include a ModDate entry in your updated info with a current\n\
		 date/timestamp, format: D:YYYYMMDDHHmmSS, e.g. D:201307241346\n\
		 -- omitted data after YYYY revert to default values.)\n\
\n\
		 For example:\n\
\n\
		 pdftk in.pdf update_info in.info output out.pdf\n\
\n\
	  update_info_utf8 <info data filename | - | PROMPT>\n\
		 Same as update_info except that the input is encoded as\n\
		 UTF-8.\n\
\n\
	  attach_files <attachment filenames | PROMPT> [to_page <page number |\n\
	  PROMPT>]\n\
		 Packs arbitrary files into a PDF using PDF's file attachment\n\
		 features. More than one attachment may be listed after\n\
		 attach_files. Attachments are added at the document level\n\
		 unless the optional to_page option is given, in which case\n\
		 the files are attached to the given page number (the first\n\
		 page is 1, the final page is end). For example:\n\
\n\
		 pdftk in.pdf attach_files table1.html table2.html to_page 6\n\
		 output out.pdf\n\
\n\
	  unpack_files\n\
		 Copies all of the attachments from the input PDF into the\n\
		 current folder or to an output directory given after output.\n\
		 For example:\n\
\n\
		 pdftk report.pdf unpack_files output ~/atts/\n\
\n\
		 or, interactively:\n\
\n\
		 pdftk report.pdf unpack_files output PROMPT\n\
\n\
       [output <output filename | - | PROMPT>]\n\
	      The output PDF filename may not be set to the name of an input\n\
	      filename. Use - to output to stdout.  When using the dump_data\n\
	      operation, use output to set the name of the output data file.\n\
	      When using the unpack_files operation, use output to set the\n\
	      name of an output directory.  When using the burst operation,\n\
	      you can use output to control the resulting PDF page filenames\n\
	      (described above).\n\
\n\
       [encrypt_40bit | encrypt_128bit]\n\
	      If an output PDF user or owner password is given, output PDF\n\
	      encryption strength defaults to 128 bits.  This can be overrid-\n\
	      den by specifying encrypt_40bit.\n\
\n\
       [allow <permissions>]\n\
	      Permissions are applied to the output PDF only if an encryption\n\
	      strength is specified or an owner or user password is given.  If\n\
	      permissions are not specified, they default to 'none,' which\n\
	      means all of the following features are disabled.\n\
\n\
	      The permissions section may include one or more of the following\n\
	      features:\n\
\n\
	      Printing\n\
		     Top Quality Printing\n\
\n\
	      DegradedPrinting\n\
		     Lower Quality Printing\n\
\n\
	      ModifyContents\n\
		     Also allows Assembly\n\
\n\
	      Assembly\n\
\n\
	      CopyContents\n\
		     Also allows ScreenReaders\n\
\n\
	      ScreenReaders\n\
\n\
	      ModifyAnnotations\n\
		     Also allows FillIn\n\
\n\
	      FillIn\n\
\n\
	      AllFeatures\n\
		     Allows the user to perform all of the above, and top\n\
		     quality printing.\n\
\n\
       [owner_pw <owner password | PROMPT>]\n\
\n\
       [user_pw <user password | PROMPT>]\n\
	      If an encryption strength is given but no passwords are sup-\n\
	      plied, then the owner and user passwords remain empty, which\n\
	      means that the resulting PDF may be opened and its security\n\
	      parameters altered by anybody.\n\
\n\
       [compress | uncompress]\n\
	      These are only useful when you want to edit PDF code in a text\n\
	      editor like vim or emacs.  Remove PDF page stream compression by\n\
	      applying the uncompress filter. Use the compress filter to\n\
	      restore compression.\n\
\n\
       [flatten]\n\
	      Use this option to merge an input PDF's interactive form fields\n\
	      (and their data) with the PDF's pages. Only one input PDF may be\n\
	      given. Sometimes used with the fill_form operation.\n\
\n\
       [need_appearances]\n\
	      Sets a flag that cues Reader/Acrobat to generate new field\n\
	      appearances based on the form field values.  Use this when fill-\n\
	      ing a form with non-ASCII text to ensure the best presentation\n\
	      in Adobe Reader or Acrobat.  It won't work when combined with\n\
	      the flatten option.\n\
\n\
       [keep_first_id | keep_final_id]\n\
	      When combining pages from multiple PDFs, use one of these\n\
	      options to copy the document ID from either the first or final\n\
	      input document into the new output PDF. Otherwise pdftk creates\n\
	      a new document ID for the output PDF. When no operation is\n\
	      given, pdftk always uses the ID from the (single) input PDF.\n\
\n\
       [drop_xfa]\n\
	      If your input PDF is a form created using Acrobat 7 or Adobe\n\
	      Designer, then it probably has XFA data.	Filling such a form\n\
	      using pdftk yields a PDF with data that fails to display in\n\
	      Acrobat 7 (and 6?).  The workaround solution is to remove the\n\
	      form's XFA data, either before you fill the form using pdftk or\n\
	      at the time you fill the form. Using this option causes pdftk to\n\
	      omit the XFA data from the output PDF form.\n\
\n\
	      This option is only useful when running pdftk on a single input\n\
	      PDF.  When assembling a PDF from multiple inputs using pdftk,\n\
	      any XFA data in the input is automatically omitted.\n\
\n\
       [drop_xmp]\n\
	      Many PDFs store document metadata using both an Info dictionary\n\
	      (old school) and an XMP stream (new school).  Pdftk's\n\
	      update_info operation can update the Info dictionary, but not\n\
	      the XMP stream.  The proper remedy for this is to include a\n\
	      ModDate entry in your updated info with a current date/time-\n\
	      stamp. The date/timestamp format is: D:YYYYMMDDHHmmSS, e.g.\n\
	      D:201307241346 -- omitted data after YYYY revert to default val-\n\
	      ues. This newer ModDate should cue PDF viewers that the Info\n\
	      metadata is more current than the XMP data.\n\
\n\
	      Alternatively, you might prefer to remove the XMP stream from\n\
	      the PDF altogether -- that's what this option does.  Note that\n\
	      objects inside the PDF might have their own, separate XMP meta-\n\
	      data streams, and that drop_xmp does not remove those.  It only\n\
	      removes the PDF's document-level XMP stream.\n\
\n\
       [verbose]\n\
	      By default, pdftk runs quietly. Append verbose to the end and it\n\
	      will speak up.\n\
\n\
       [dont_ask | do_ask]\n\
	      Depending on the compile-time settings (see ASK_ABOUT_WARNINGS),\n\
	      pdftk might prompt you for further input when it encounters a\n\
	      problem, such as a bad password. Override this default behavior\n\
	      by adding dont_ask (so pdftk won't ask you what to do) or do_ask\n\
	      (so pdftk will ask you what to do).\n\
\n\
	      When running in dont_ask mode, pdftk will over-write files with\n\
	      its output without notice.\n\
\n\
EXAMPLES\n\
       Collate scanned pages\n\
	 pdftk A=even.pdf B=odd.pdf shuffle A B output collated.pdf\n\
	 or if odd.pdf is in reverse order:\n\
	 pdftk A=even.pdf B=odd.pdf shuffle A Bend-1 output collated.pdf\n\
\n\
       Decrypt a PDF\n\
	 pdftk secured.pdf input_pw foopass output unsecured.pdf\n\
\n\
       Encrypt a PDF using 128-bit strength (the default), withhold all per-\n\
       missions (the default)\n\
	 pdftk 1.pdf output 1.128.pdf owner_pw foopass\n\
\n\
       Same as above, except password 'baz' must also be used to open output\n\
       PDF\n\
	 pdftk 1.pdf output 1.128.pdf owner_pw foo user_pw baz\n\
\n\
       Same as above, except printing is allowed (once the PDF is open)\n\
	 pdftk 1.pdf output 1.128.pdf owner_pw foo user_pw baz allow printing\n\
\n\
       Join in1.pdf and in2.pdf into a new PDF, out1.pdf\n\
	 pdftk in1.pdf in2.pdf cat output out1.pdf\n\
	 or (using handles):\n\
	 pdftk A=in1.pdf B=in2.pdf cat A B output out1.pdf\n\
	 or (using wildcards):\n\
	 pdftk *.pdf cat output combined.pdf\n\
\n\
       Remove page 13 from in1.pdf to create out1.pdf\n\
	 pdftk in.pdf cat 1-12 14-end output out1.pdf\n\
	 or:\n\
	 pdftk A=in1.pdf cat A1-12 A14-end output out1.pdf\n\
\n\
       Apply 40-bit encryption to output, revoking all permissions (the\n\
       default). Set the owner PW to 'foopass'.\n\
	 pdftk 1.pdf 2.pdf cat output 3.pdf encrypt_40bit owner_pw foopass\n\
\n\
       Join two files, one of which requires the password 'foopass'. The out-\n\
       put is not encrypted.\n\
	 pdftk A=secured.pdf 2.pdf input_pw A=foopass cat output 3.pdf\n\
\n\
       Uncompress PDF page streams for editing the PDF in a text editor (e.g.,\n\
       vim, emacs)\n\
	 pdftk doc.pdf output doc.unc.pdf uncompress\n\
\n\
       Repair a PDF's corrupted XREF table and stream lengths, if possible\n\
	 pdftk broken.pdf output fixed.pdf\n\
\n\
       Burst a single PDF document into pages and dump its data to\n\
       doc_data.txt\n\
	 pdftk in.pdf burst\n\
\n\
       Burst a single PDF document into encrypted pages. Allow low-quality\n\
       printing\n\
	 pdftk in.pdf burst owner_pw foopass allow DegradedPrinting\n\
\n\
       Write a report on PDF document metadata and bookmarks to report.txt\n\
	 pdftk in.pdf dump_data output report.txt\n\
\n\
       Rotate the first PDF page to 90 degrees clockwise\n\
	 pdftk in.pdf cat 1east 2-end output out.pdf\n\
\n\
       Rotate an entire PDF document to 180 degrees\n\
	 pdftk in.pdf cat 1-endsouth output out.pdf\n\
\n\
NOTES\n\
       The pdftk home page permalink is:\n\
       http:
       The easy-to-remember shortcut is: www.pdftk.com\n\
\n\
AUTHOR\n\
       Sid Steward (sid.steward at pdflabs dot com) maintains pdftk.  Please\n\
       email him with questions or bug reports.  Include pdftk in the subject\n\
       line to ensure successful delivery.  Thank you.";
}