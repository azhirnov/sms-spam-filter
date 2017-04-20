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
			case 'A' :	p = 'À';	break;
			case 'a' :	p = 'à';	break;
			case 'B' :	p = 'Â';	break;
			case 'C' :	p = 'Ñ';	break;
			case 'c' :	p = 'ñ';	break;
			case 'E' :	p = 'Å';	break;
			case 'e' :	p = 'å';	break;
			case 'H' :	p = 'Í';	break;
			case 'K' :	p = 'Ê';	break;
			case 'M' :	p = 'Ì';	break;
			case 'O' :	p = 'Î';	break;
			case 'o' :	p = 'î';	break;
			case 'P' :	p = 'Ð';	break;
			case 'p' :	p = 'ð';	break;
			case 'T' :	p = 'Ò';	break;
			case 'y' :	p = 'ó';	break;
			case 'X' :	p = 'Õ';	break;
			case 'x' :	p = 'õ';	break;
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
		return (c == 'Î') | (c == 'î');
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
			case 'À' :	p = 'A';	break;
			case 'à' :	p = 'a';	break;
			case 'Â' :	p = 'B';	break;
			case 'Ñ' :	p = 'C';	break;
			case 'ñ' :	p = 'c';	break;
			case 'Å' :	p = 'E';	break;
			case 'å' :	p = 'e';	break;
			case 'Í' :	p = 'H';	break;
			case 'Ê' :	p = 'K';	break;
			case 'Ì' :	p = 'M';	break;
			case 'Î' :	p = 'O';	break;
			case 'î' :	p = 'o';	break;
			case 'Ð' :	p = 'P';	break;
			case 'ð' :	p = 'p';	break;
			case 'Ò' :	p = 'T';	break;
			case 'ó' :	p = 'y';	break;
			case 'Õ' :	p = 'X';	break;
			case 'õ' :	p = 'x';	break;
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
