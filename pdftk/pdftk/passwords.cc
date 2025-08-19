#pragma GCC java_exceptions
#include <gcj/cni.h>
#include "passwords.h"
static unsigned long unicode_latin_extended_windows_map[]=
	{
		'A',
		'a',
		0xC3,
		0xC4,
		0xA5,
		0xB9,
		0xC6,
		0xE6,
		'.',
		'.',
		'.',
		'.',
		0xC8,
		0xE8,
		0xCF,
		0xEF,
		0xD0,
		0xF0,
		'E',
		'e',
		'.',
		'.',
		'E',
		'e',
		0xCA,
		0xEA,
		0xCC,
		0xEC,
		'.',
		'.',
		'G',
		'g',
		'.',
		'.',
		'G',
		'g',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'I',
		'i',
		'.',
		'.',
		'I',
		'i',
		'I',
		'i',
		'.',
		'.',
		'.',
		'.',
		'K',
		'k',
		'.',
		0xC5,
		0xE5,
		'L',
		'l',
		0xBC,
		0xBE,
		'.',
		'.',
		0xA3,
		0xB3,
		0xD1,
		0xF1,
		'N',
		'n',
		0xD2,
		0xF2,
		'.',
		'.',
		'.',
		'O',
		'o',
		'.',
		'.',
		0xD5,
		0xF5,
		0226,
		0234,
		0xC0,
		0xE0,
		'R',
		'r',
		0xD8,
		0xF8,
		0x8C,
		0x9C,
		'.',
		'.',
		0xAA,
		0xBA,
		0x8A,
		0x9A,
		0xDE,
		0xFE,
		0x8D,
		0x9D,
		'T',
		't',
		'.',
		'.',
		'U',
		'u',
		'.',
		'.',
		0xD9,
		0xF9,
		0xDB,
		0xFB,
		'U',
		'u',
		'.',
		'.',
		'.',
		'.',
		0230,
		0x8F,
		0x9F,
		0xAF,
		0xBF,
		0231,
		0236,
		'.',
		'b',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		0xD0,
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		0x83,
		0x83,
		'.',
		'.',
		'.',
		'.',
		'I',
		'.',
		'.',
		'l',
		'.',
		'.',
		'.',
		'.',
		'O',
		'O',
		'o',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		't',
		'.',
		'.',
		'T',
		'U',
		'u',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'|',
		'.',
		'.',
		'!',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'A',
		'a',
		'.',
		'.',
		'.',
		'.',
		'G',
		'g',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'O',
		'o',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.',
		'.'
	};
int utf8_password_to_pdfdoc( jbyte* bb, const char* ss, int ss_size, bool encrypt_b ) {
	int ret_val= 0;
	for( int ii= 0; ii< ss_size && ss[ii] && ret_val!= -1; ) {
		unsigned long data= 0;
		if( (ss[ii] & 0x80) == 0 )
			{
				data= ss[ii];
				ii++;
			}
		else if( (ss[ii] & 0xE0) == 0xC0 &&
						 ii+ 1< ss_size && (ss[ii+ 1] & 0xC0)== 0x80 )
			{
				data= (ss[ii] & 0x1f);
				data= data<< 6;
				data= data+ (ss[ii+ 1] & 0x3f);
				ii+= 2;
			}
		else if( (ss[ii] & 0xF0) == 0xE0 &&
						 ii+ 1< ss_size && (ss[ii+ 1] & 0xC0)== 0x80 &&
						 ii+ 2< ss_size && (ss[ii+ 2] & 0xC0)== 0x80 )
			{
				data= (ss[ii] & 0x0f);
				data= data<< 6;
				data= data+ (ss[ii+ 1] & 0x3f);
				data= data<< 6;
				data= data+ (ss[ii+ 2] & 0x3f);
				ii+= 3;
			}
		else {
			ret_val= -1;
			break;
		}
		if( 0x20<= data && data< 0x7f || 0xa0<= data && data<= 0xff ) {
		}
		else {
			switch( data ) {
			case 0x0152:
				data= 0226;
				break;
			case 0x0153:
				data= 0234;
				break;
			case 0x0160:
				data= 0227;
				break;
			case 0x017E:
				data= 0236;
				break;
			case 0x0178:
				data= 0230;
				break;
			case 0x017D:
				data= 0231;
				break;
			case 0x0192:
				data= 0206;
				break;
			case 0x0161:
				data= 0235;
				break;
			default:
				if( encrypt_b ) {
					ret_val= -1;
					break;
				}
				else {
					if( 0x100<= data && data<= 0x1FF ) {
						data= unicode_latin_extended_windows_map[ data- 0x100 ];
					}
					else {
						switch( data ) {
						case 0x20AC:
							data= 0240;
							break;
						case 0x2022:
							data= 0200;
							break;
						case 0x2020:
							data= 0201;
							break;
						case 0x2021:
							data= 0202;
							break;
						case 0x2026:
							data= 0203;
							break;
						case 0x02C6:
							data= 0032;
							break;
						case 0x2014 :
							data= 0204;
							break;
						case 0x2013:
							data= 0205;
							break;
						case 0x2039:
							data= 0210;
							break;
						case 0x203A:
							data= 0211;
							break;
						case 0x2030:
							data= 0213;
							break;
						case 0x201E:
							data= 0214;
							break;
						case 0x201C:
							data= 0215;
							break;
						case 0x201D:
							data= 0216;
							break;
						case 0x2018:
							data= 0217;
							break;
						case 0x2019:
							data= 0220;
							break;
						case 0x201A:
							data= 0221;
							break;
						case 0x02DC:
							data= 0037;
							break;
						case 0x2122:
							data= 0222;
							break;
						default:
							ret_val= -1;
							break;
						}
					}
				}
			}
		}
		if( ret_val!= -1 ) {
			if( bb )
				bb[ ret_val ]= data;
			++ret_val;
		}
	}
	return ret_val;
}