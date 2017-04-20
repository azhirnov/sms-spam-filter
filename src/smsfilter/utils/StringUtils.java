package smsfilter.utils;


//
// String Utils
//

public final class StringUtils
{
	// FindUrlsInString
	public static final
	boolean FindUrlsInString (final String str)
	{
		final String	pattern1 = "://";
		final String	pattern2 = "www.";
		final char		pattern3 = '/';
		final char[]	cs = str.toCharArray();

		// ://[].[]
		int	pos = 0;
		
		while ( true )
		{
			pos = str.indexOf( pattern1, pos );
			if ( pos == -1 ) break;
			++pos;
			
			if ( _CheckUrl( cs, pos + pattern1.length() ) )
				return true;
		}

		// www.[].[]
		pos = 0;
		
		while ( true )
		{
			pos = str.indexOf( pattern2, pos );
			if ( pos == -1 ) break;
			++pos;
			
			if ( _CheckUrl( cs, pos + pattern2.length()+1 ) )
				return true;
		}

		// [].[]/[]
		pos = 0;
		
		while ( true )
		{
			pos = str.indexOf( pattern3, pos );
			if ( pos == -1 ) break;
			
			if ( _CheckUrlBackFront( cs, pos ) )
				return true;
			
			++pos;
		}
		
		// [].com, [].ru, [].net, [].org
		final String	WEB_ZONE[] = new String[]{ ".COM", ".RU", ".NET", ".ORG" };
		
		for (String s : WEB_ZONE)
		{
			pos = 0;
			
			while ( true )
			{
				pos = str.indexOf( s, pos );
				if ( pos == -1 ) break;

				final int	start 	= pos-1;
				final int	MIN_LEN	= 4;
				
				for (int i = start; i >= 0; --i)
				{
					if ( ! Character.isLetter( cs[i] ) )
					{
						if ( (start-i) >= MIN_LEN )
							return true;
						
						break;
					}
				}
				
				++pos;
			}
		}
		
		return false;
	}
	
	
	// _CheckUrl
	private static final
	boolean _CheckUrl (final char[] cs, final int start)
	{
		// [].[]
		// ^
		final int	MIN_LEN	= 2;
		
		int i = start;
				
		// []
		for (; i < cs.length; ++i)
		{
			final char	c = cs[i];
			
			if ( ! Character.isLetter( c ) )
			{
				if ( ((i-start) >= MIN_LEN) & (c == '.') )  break;
				return false;
			}
		}
		
		if ( i >= cs.length )
			return false;
		
		++i;
		
		// []
		for (; i < cs.length; ++i)
		{
			final char	c = cs[i];
			
			if ( ! Character.isLetter( c ) )
			{
				if ( (i-start) >= MIN_LEN )  break;
				return false;
			}
		}
		return true;
	}
	
	
	// _CheckUrlBackFront
	private static final
	boolean _CheckUrlBackFront (final char[] cs, final int start)
	{
		// [].[]/[]
		//      ^
		final int	MIN_LEN	= 2;
		
		int i 	  = start;
		
		if ( cs[i] == '/' )
			--i;
		
		final int part1 = i;
		
		// backward []
		for (; i > 0; --i)
		{
			final char c = cs[i];
			
			if ( ! Character.isLetter( c ) )
			{
				if ( ((part1-i) >= MIN_LEN) & (c == '.') )  break;
				return false;
			}
		}
		
		if ( i <= 1 )
			return false;
		
		final int part2 = --i;
		
		// backward []
		for (; i > 0; --i)
		{
			final char c = cs[i];
			
			if ( ! Character.isLetter( c ) )
			{
				if ( (part2-i) >= MIN_LEN )  break;
				return false;
			}
		}
		
		// forward []
		i = start;
		
		if ( cs[i] == '/' )
			++i;
		
		final int part3 = i;

		for (; i < cs.length; ++i)
		{
			final char	c = cs[i];
			
			if ( ! Character.isLetter( c ) )
			{
				if ( (i-part3) >= MIN_LEN )  break;
				return false;
			}
		}
		return true;
	}
	
	
	// FindSubStringSpecial
	public static final
	boolean FindSubStringSpecial (final char[] str, final char[] search)
	{
		try {
			final char[]	s = str;
			final char[]	p = search;
			
			int	j = 0;
			
			for (int i = 0; i < s.length; ++i)
			{
				while ( i+j < s.length && j < p.length )
				{
					final int	k = i+j;
					final char	c = s[k];
					final char	d = p[j];
	
					boolean		b = (c == d);
					
					if ( !b )
						b = LocaleUtils.CompareCharacters( c, d );
					
					if ( !b )	break;

					if ( ++j >= p.length )
						return true;
				}
				j = 0;
			}
		}
		catch (Exception e) {
			Logger.OnCatchException( e );
		}
		return false;
	}

	
	
	// FindWordSpecial
	public static final
	boolean FindWordSpecial (final char[] str, final char[] search)
	{
		try {
			final char[]	s = str;
			final char[]	p = search;
			
			int	j = 0;
			
			for (int i = 0; i < s.length; ++i)
			{
				while ( i+j < s.length && j < p.length )
				{
					final int	k = i+j;
					final char	c = s[k];
					final char	d = p[j];

					boolean		b = (c == d);
					
					if ( !b )
						b = LocaleUtils.CompareCharacters( c, d );
					
					if ( !b ) break;
					
					// is whole word
					if ( (j == 0) & (b) & (i > 0) )
					{
						if ( Character.isLetter( s[i-1] ) )
							break;
					}

					if ( ++j >= p.length )
					{
						// is whole word
						if ( i+j < s.length )
						{
							if ( Character.isLetter( s[i+j] ) )
								break;
						}
						
						return true;
					}
				}
				j = 0;
			}
		}
		catch (Exception e) {
			Logger.OnCatchException( e );
		}
		return false;
	}
	
	
	
	
	// HasLanguageSwitchingInWord
	public static final
	boolean HasLanguageSwitchingInWord (final char[] str)
	{
		int	counter = 0;
		
		try {
			Character.UnicodeBlock 	prev 	= null;
			boolean					newWord = false;
			
			for (char c : str)
			{
				if ( Character.isLetter( c ) )
				{
					if ( newWord )
					{
						Character.UnicodeBlock	u = Character.UnicodeBlock.of( c );
			
						counter += LocaleUtils.IsReplacedToSameSymbol( prev, u, c ) ? 1 : 0;
						prev = u;
					}
					else {
						newWord = true;
						prev = Character.UnicodeBlock.of( c );
					}
				}
				else {
					newWord = false;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return counter > 0;
	}
	
	
	// IsPhoneNumber
	public static final
	boolean IsPhoneNumber (final char[] cs)
	{
		if ( !( cs[0] >= '0' && cs[0] <= '9' || cs[0] == '+' ) )
			return false;
		
		for (char c : cs)
		{
			if ( !( c >= '0' && c <= '9' ) )
			{
				return false;
			}
		}
		return true;
	}
	
	
	// FindNumber
	public static final
	boolean FindNumber (final char[] cs, final int minNumbers)
	{
		// pattern:
		// +777-666 - 555 (444) [333] .9666.
		
		final int	FIND = 1;
		final int	READ = 2;
		final int	CHECK = 3;
		
		int				counter		= 0;
		int				mode		= FIND;
		int				i			= 0;
		
		for (; i < cs.length; ++i)
		{
			final char c = cs[i];

			switch ( mode )
			{
				case FIND :
					if ( c >= '0' && c <= '9' )
					{
						if ( i > 0 )
						{
							final char	p = cs[i-1];

							if ( (p != '+')  & (p != ' ') & (p != '\t') & (p != '\n') &
								 (p != '\r') & (p != '.') & (p != '(') )
							{
								break;
							}
						}
						
						counter = 1;
						mode = READ;
					}
					break;

				case READ :
					if ( ((c >= '0') & (c <= '9')) | (c == '-') | (c == ' ') |
						 (c == '(') | (c == ')') | (c == '[') | (c == ']') )
					{
						counter += ((c >= '0') & (c <= '9')) ? 1 : 0;
					}
					else {
						mode = CHECK;
						--i;
					}
					break;
				
				case CHECK :
				{
					mode = FIND;
					
					final char	n = i+1 < cs.length ? cs[i+1] : 0;

					// is money?
					if ( (c == '.') & (n >= '0') & (n <= '9') )
						break;
					else
					// is formula?
					if ( (c == '*') | (c == '/') | (c == '+') | (c == '=') )
						break;

					if ( counter >= minNumbers )
						return true;

					break;
				}
			}
		}

		if ( mode == READ && counter >= minNumbers )
			return true;
		
		return false;
	}
}
