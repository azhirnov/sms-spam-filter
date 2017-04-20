package smsfilter.utils;

public final class LocaleUtils
{
// constants //
	private static final char	UNKNOWN_CHAR = ' ';
	
	
// Main //
	
	// CompareCharacters
	public static final
	boolean CompareCharacters (final char left, final char right)
	{
		if ( Character.toUpperCase( left ) == Character.toUpperCase( right ) )
			return true;
		
		if ( left == '0' ) {
			return 	CmpZeroLatin( right ) |
					CmpZeroCyrillic( right );
		}

		Character.UnicodeBlock	ubLeft  = Character.UnicodeBlock.of( left );
		Character.UnicodeBlock	ubRight = Character.UnicodeBlock.of( right );
		
		if ( ubLeft == ubRight )
			return false;	// TODO
		
		// Cyrillic - Latin
		if ( IsCyrillic( ubLeft ) & IsLatin( ubRight ) )
			return CmpCyrillicLatin( left, right );
		
		// Latin - Cyrillic
		if ( IsLatin( ubLeft ) & IsCyrillic( ubRight ) )
			return CmpLatinCyrillic( left, right );
		
		return false;
	}
	
	
	// IsReplacedToSameSymbol
	public static final
	boolean IsReplacedToSameSymbol (final Character.UnicodeBlock prevCharLocale,
									final Character.UnicodeBlock currCharLocale,
									final char symbol)
	{
		if ( prevCharLocale == currCharLocale )
			return false;	// TODO
		
		if ( IsCyrillic( currCharLocale ) & IsLatin( prevCharLocale ) )
			return CyrillicHasSameLatin( symbol );
		
		if ( IsLatin( currCharLocale ) & IsCyrillic( prevCharLocale ) )
			return LatinHasSameCyrillic( symbol );
		
		return false;
	}
	
	
// Latin Symbols //
	
	// IsLatin
	public static final
	boolean IsLatin (Character.UnicodeBlock ub)
	{
		return  (ub == Character.UnicodeBlock.BASIC_LATIN) |
				(ub == Character.UnicodeBlock.LATIN_1_SUPPLEMENT) |
				(ub == Character.UnicodeBlock.LATIN_EXTENDED_A) |
				(ub == Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL) |
				(ub == Character.UnicodeBlock.LATIN_EXTENDED_B);
	}
	
	
	// CmpZeroLatin
	public static final
	boolean CmpZeroLatin (char l) {
		return (l == 'O') | (l == 'o');
	}
	
	
	// LatinToSameCyrillic
	public static final
	char LatinToSameCyrillic (final char l)
	{
		char	p = UNKNOWN_CHAR;
		
		switch ( l )
		{
			// latin - cyrillic
			case 'A' :	p = '�';	break;
			case 'a' :	p = '�';	break;
			case 'B' :	p = '�';	break;
			case 'C' :	p = '�';	break;
			case 'c' :	p = '�';	break;
			case 'E' :	p = '�';	break;
			case 'e' :	p = '�';	break;
			case 'H' :	p = '�';	break;
			case 'K' :	p = '�';	break;
			case 'M' :	p = '�';	break;
			case 'O' :	p = '�';	break;
			case 'o' :	p = '�';	break;
			case 'P' :	p = '�';	break;
			case 'p' :	p = '�';	break;
			case 'T' :	p = '�';	break;
			case 'y' :	p = '�';	break;
			case 'X' :	p = '�';	break;
			case 'x' :	p = '�';	break;
		}
		return p;
	}
	
	
	// LatinHasSameCyrillic
	public static final
	boolean LatinHasSameCyrillic (final char l)
	{
		return LatinToSameCyrillic( l ) != UNKNOWN_CHAR;
	}
	
	
	// CmpLatinCyrillic
	public static final
	boolean CmpLatinCyrillic (final char l, final char c)
	{
		return LatinToSameCyrillic( l ) == c;
	}
	
	
	
// Cyrillic Symbols //
	
	// CmpZeroCyrillic
	public static final
	boolean CmpZeroCyrillic (char c) {
		return (c == '�') | (c == '�');
	}
	
	
	// IsCyrillic
	public static final
	boolean IsCyrillic (Character.UnicodeBlock ub)
	{
		return 	(ub == Character.UnicodeBlock.CYRILLIC) |
				(ub == Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY);
	}
	
	
	// CmpCyrillicLatin
	public static final
	char CyrillicToSameLatin (final char c)
	{
		char	p = UNKNOWN_CHAR;
		
		switch ( c )
		{
			// cyrillic - latin
			case '�' :	p = 'A';	break;
			case '�' :	p = 'a';	break;
			case '�' :	p = 'B';	break;
			case '�' :	p = 'C';	break;
			case '�' :	p = 'c';	break;
			case '�' :	p = 'E';	break;
			case '�' :	p = 'e';	break;
			case '�' :	p = 'H';	break;
			case '�' :	p = 'K';	break;
			case '�' :	p = 'M';	break;
			case '�' :	p = 'O';	break;
			case '�' :	p = 'o';	break;
			case '�' :	p = 'P';	break;
			case '�' :	p = 'p';	break;
			case '�' :	p = 'T';	break;
			case '�' :	p = 'y';	break;
			case '�' :	p = 'X';	break;
			case '�' :	p = 'x';	break;
		}
		return p;
	}


	// CyrillicHasSameLatin
	public static final
	boolean CyrillicHasSameLatin (final char c)
	{
		return CyrillicToSameLatin( c ) != UNKNOWN_CHAR;
	}
	
	
	// CmpCyrillicLatin
	public static final
	boolean CmpCyrillicLatin (final char c, final char l)
	{
		return CyrillicToSameLatin( c ) == l;
	}
	
}
