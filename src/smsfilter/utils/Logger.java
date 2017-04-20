package smsfilter.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import smsfilter.app.AppConfig;
import smsfilter.app.MyApplication;


public final class Logger
{
	private static final boolean	DEBUG 		= AppConfig.DEBUG;
	private static final boolean	SHOW_TOAST	= AppConfig.SHOW_TOAST;
	private static final boolean	TO_FILE		= AppConfig.LOG_FILE;
	private static final String		DEF_TAG		= "SMS ";
	private static final String		FILE_PATH	= "/SMS Filter";
	private static final String		FILE_NAME	= FILE_PATH + "/log.txt";
	
	private static OutputStreamWriter	filestream 	= null;
	private static final Object			mutex		= new Object();
	
	
	// OnCatchException
	public static final void OnCatchException (final String tag, final Exception e)
	{
		if ( !DEBUG )
			return;
		
		StringBuilder s = new StringBuilder("Catched exception: ");
		s.append( e.toString() );
		s.append( android.util.Log.getStackTraceString( e ) );
		
		Log.e( DEF_TAG + tag, s.toString() );
		_LogToToast( tag, "Catched exception: " + e.toString() );
		_LoadToFile( tag, s.toString() );
	}
	
	public static final void OnCatchException (final Exception e)		{ OnCatchException( "", e ); }

	
	// E
	public static final void E (final String tag, final String msg)
	{
		if ( !DEBUG )
			return;
		
		Log.e( DEF_TAG + tag, msg );
		_LogToToast( tag, msg );
		_LoadToFile( tag, msg );
	}
	
	public static final void E (final String msg)		{ E( "", msg ); }
	
	
	// W
	public static final void W (final String tag, final String msg)
	{
		if ( !DEBUG )
			return;
		
		Log.w( DEF_TAG + tag, msg );
		_LogToToast( tag, msg );
		_LoadToFile( tag, msg );
	}
	
	public static final void W (final String msg)		{ W( "", msg ); }
	
	
	// I
	public static final void I (final String tag, final String msg)
	{
		if ( !DEBUG )
			return;
		
		Log.i( DEF_TAG + tag, msg );
		_LoadToFile( tag, msg );
	}
	
	public static final void I (final String msg)		{ I( "", msg ); }

	
	// ShowToast
	public final static void ShowToast (final String str)
	{
		MyApplication.RunOnUIThread( new Runnable() {
			public void run () {
				Toast.makeText( MyApplication.GetContext(), str, Toast.LENGTH_LONG ).show();
			}
		});
	}
	
	
	// _LogToToast
	private final static void _LogToToast (final String tag, final String msg)
	{
		if ( !SHOW_TOAST ) return;
		ShowToast( tag + ": " + msg );
	}

	
	// _LoadToFile
	private final static void _LoadToFile (final String tag, final String msg)
	{
		if ( !TO_FILE )	return;
		
		_OpenFile();
		
		synchronized(mutex)
		{
			if ( filestream != null )
			{
				try {
					filestream.write( tag + " - " + msg + "\n" );
					filestream.flush();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	// _OpenFile
	private final static void _OpenFile ()
	{
		if ( filestream == null )
		{
			synchronized(mutex)
			{
				if ( filestream == null )
				{
					File dir = Environment.getExternalStorageDirectory();
					
					if ( dir == null )
						return;
					
					File	file = new File( dir.getAbsolutePath() + FILE_PATH );
					
					if ( !( file.isDirectory() && file.exists() ) ) {
						file.mkdirs();
					}
					
					file = new File( dir.getAbsolutePath() + FILE_NAME );
					
					OutputStream	os = null;
					
					try {
						os = new FileOutputStream( file, true );
						filestream = new OutputStreamWriter( os );
					}
					catch (Exception e) {
						e.printStackTrace();
						return;
					}
				}
			}
		}
	}
}
