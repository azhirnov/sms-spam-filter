package smsfilter.app;


public final class AppConfig
{
	public	static final boolean	RELEASE		= true;
	public 	static final boolean	PRO 		= true;
	
	
	public 	static final boolean	DEBUG 		= ! RELEASE;
	public	static final boolean	SHOW_TOAST	= ! RELEASE;
	public	static final boolean	LOG_FILE	= ! RELEASE;
	public 	static final boolean	TESTS 		= ! RELEASE;
}
