package smsfilter.app;

import smsfilter.utils.Logger;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


public final class Preferences extends PreferenceActivity
{
// constants //
	private static final String	TAG = "Preferences";
	
	public static final String	CHECK_PRIORITY_KEY = "checkPriority";
	

// methods //
	
	// onCreate
	@Override
	protected final void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource( AppConfig.PRO ? D.layout.preferences_pro : D.layout.preferences );
	}
	
	
	// onStart
	@Override
	protected final void onStart () {
		super.onStart();
	}

	
	// onStop
	@Override
	protected final void onStop () {
		super.onStop();
	}
	
	
	// ClassPreferences
	public static final SharedPreferences ClassPreferences (String name)
	{
		return MyApplication.GetContext().getSharedPreferences( name, Context.MODE_PRIVATE );
	}

	
	//
	// Application Preferences
	//
	public final static class AppPrefs
	{
	// constants //
		public static final int	AUTO_CLEAR_NEVER			= 1;
		public static final int	AUTO_CLEAR_OLDER_2_DAYS		= 2;
		public static final int	AUTO_CLEAR_OLDER_1_WEEK		= 3;
		public static final int	AUTO_CLEAR_OLDER_2_WEEK		= 4;
		public static final int	AUTO_CLEAR_OLDER_1_MONTH	= 5;

		public static final int	MODE_TYPE_DEFAULT			= 0;
		public static final int	MODE_TYPE_MOST_TRIGGERED	= 1;
		public static final int	MODE_TYPE_STRICT_TIME		= 2;
		public static final int	MODE_TYPE_STRONG			= 3;
		public static final int	MODE_TYPE_VERY_STRONG		= 4;
		
		public static final int	SUSPECT_SMS_ALLOW		= 0;
		public static final int	SUSPECT_SMS_BLOCK		= 1;
		public static final int	SUSPECT_SMS_SHOW_POPUP	= 2;
		
		
	// variables //
		public boolean	showNotif		= true;
		public boolean	enabled			= true;
		public boolean	ledLight		= AppConfig.PRO;
		public int		suspectMode		= SUSPECT_SMS_BLOCK;
		public int		autoClear		= AUTO_CLEAR_NEVER;
		public int		analyzeMode		= MODE_TYPE_DEFAULT;
	}
	
	
	// ReadPrefs
	public final static
	AppPrefs ReadPrefs ()
	{
		try {
			Context				ctx 	= MyApplication.GetContext();
			SharedPreferences 	prefs 	= PreferenceManager.getDefaultSharedPreferences( ctx );
			AppPrefs			app		= new AppPrefs();
			
			app.showNotif 		= prefs.getBoolean( "showNotifications", true );
			app.enabled 		= prefs.getBoolean( "programEnabled", true );
			app.ledLight		= prefs.getBoolean( "lightIndicator", true );
			app.suspectMode		= Integer.parseInt( prefs.getString( "suspectMode", 
								  Integer.toString( AppPrefs.SUSPECT_SMS_SHOW_POPUP ) ) );
			app.autoClear 		= Integer.parseInt( prefs.getString( "autoClearList",
								  Integer.toString( AppPrefs.AUTO_CLEAR_NEVER ) ) );
			app.analyzeMode		= Integer.parseInt( prefs.getString( "analyzeMode",
								  Integer.toString( AppPrefs.MODE_TYPE_DEFAULT ) ) );
			
			return app;
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
			return new AppPrefs();
		}
	}
}
