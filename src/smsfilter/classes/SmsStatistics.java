package smsfilter.classes;

import android.content.SharedPreferences;
import smsfilter.app.Preferences;

public final class SmsStatistics
{
// constants //
	private static final String		PREFS_NAME 		= "sms-stat-class";
	
	private static final String		TOTAL_INCOMING	= "total-incoming";
	private static final String		TOTAL_BLOCKED	= "total-blocked";
	private static final String		TOTAL_PASSED	= "total-passed";
	
	
// variables //
	private long	_totalIncoming;
	private long	_totalBlocked;
	private long	_totalPassed;
	

// methods //
	protected SmsStatistics ()
	{
		_totalIncoming	= 0;
		_totalBlocked	= 0;
		_totalPassed	= 0;
	}
	
	
	public static final SmsStatistics Load ()
	{
		final SharedPreferences	prefs = Preferences.ClassPreferences( PREFS_NAME );
		final SmsStatistics		stat  = new SmsStatistics();
		
		stat._totalBlocked	= prefs.getLong( TOTAL_BLOCKED,  0 );
		stat._totalIncoming	= prefs.getLong( TOTAL_INCOMING, 0 );
		stat._totalPassed	= prefs.getLong( TOTAL_PASSED,   0 );
		
		return stat;
	}
	
	
	public final void	Save ()
	{
		final SharedPreferences			prefs = Preferences.ClassPreferences( PREFS_NAME );
		final SharedPreferences.Editor 	ed 	  = prefs.edit();
		
		ed.putLong( TOTAL_INCOMING,	_totalIncoming );
		ed.putLong( TOTAL_BLOCKED,	_totalBlocked );
		ed.putLong( TOTAL_PASSED, 	_totalPassed );
		ed.commit();
	}
	
	
	public final void 	IncIncoming ()		{ ++_totalIncoming; }
	public final void 	IncBlocked ()		{ ++_totalBlocked; }
	public final void 	IncPassed ()		{ ++_totalPassed; }
	
	public final long 	GetIncoming ()		{ return _totalIncoming; }
	public final long	GetBlocked ()		{ return _totalBlocked; }
	public final long	GetPassed ()		{ return _totalPassed; }
}
