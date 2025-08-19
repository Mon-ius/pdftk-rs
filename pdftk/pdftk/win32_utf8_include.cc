#ifndef __MSVCRT__
#error Unicode main function requires linking to MSVCRT
#endif
#ifndef _UNICODE
#error Unicode main function requires -D_UNICODE
#endif
#include <wchar.h>
#include <stdlib.h>
typedef struct {
  int newmode;
} _startupinfo;
int win32_utf8_main( int argc, char *argv[] );
typedef int (*WGETMAINARGS_TYPE)(int*, wchar_t***, wchar_t***, int, _startupinfo*);
int main() {
  int ret_val= 100;
  HMODULE hmod= GetModuleHandleA( "msvcrt.dll" );
  if( hmod ) {
    WGETMAINARGS_TYPE wgetmainargs= (WGETMAINARGS_TYPE)GetProcAddress( hmod, "__wgetmainargs" );
    if( wgetmainargs ) {
      int argc;
      wchar_t** wargv;
      wchar_t** wenvp;
      _startupinfo si; si.newmode= 0;
      int rr= wgetmainargs(&argc, &wargv, &wenvp, 1 , &si);
      if( rr== 0 ) {
	char** argv= (char**)malloc( (argc+ 1)* sizeof( char* ) );
	if( argv ) {
	  memset( argv, 0, (argc+ 1)* sizeof( char* ) );
	  bool success_b= true;
	  for( int ii= 0; ii< argc; ++ii ) {
	    int len= WideCharToMultiByte( CP_UTF8, 0, (wargv)[ii], -1, NULL, 0, NULL, NULL );
	    argv[ii]= (char*)malloc( (len+ 1)* sizeof( char ) );
	    if( !argv[ii] ) {
	      success_b= false;
	      break;
	    }
	    memset( argv[ii], 0, (len+ 1)* sizeof( char ) );
	    WideCharToMultiByte( CP_UTF8, 0, (wargv)[ii], -1, argv[ii], len, NULL, NULL );
	    argv[ii][len]= 0;
	  }
	  if( success_b ) {
	    ret_val= win32_utf8_main( argc, argv );
	  }
	  else {
	    cerr << "PDFtk Error trying to malloc space for argv elements" << endl;
	  }
	  for( int ii= 0; ii< argc; ++ii ) {
	    free( argv[ii] );
	    argv[ii]= 0;
	  }
	  free( argv );
	  argv= 0;
	}
	else {
	  cerr << "PDFtk Error trying to malloc space for argv" << endl;
	}
      }
      else {
	cerr << "PDFtk Error trying to call wgetmainargs" << endl;
      }
    }
    else {
      cerr << "PDFtk Error trying to get proc address for _wgetmainargs" << endl;
    }
  }
  else {
    cerr << "PDFtk Error trying to get a module handle of msvcrt.dll" << endl;
  }
  return ret_val;
}