package smsfilter.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import smsfilter.app.MyApplication;



public final class TextTransform
{
	
	// FormatDate
	public final static String FormatDate (long date, int formatStrId, int maxDateLen)
	{
		return FormatDate( date, MyApplication.GetContext().getString( formatStrId ), maxDateLen );
	}
	
	
	// FormatDate
	public final static String FormatDate (long date, String formatStr, int maxDateLen)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis( date );
		
		String	dateStr = ( new SimpleDateFormat( formatStr )).format( calendar.getTime() );
		
		if ( dateStr.length() > maxDateLen )
			dateStr = dateStr.substring( 0, maxDateLen );
		
		return dateStr;
	}
	
	
	// FormatDate2Lines
	public final static String FormatDate2Lines (long date)
	{
		final int	maxDateLen = 2+1+2 + 1 + 2+1+3;	// hh:mm\ndd MMM
		return FormatDate( date, "HH:mm\ndd MMM", maxDateLen );
	}

}
