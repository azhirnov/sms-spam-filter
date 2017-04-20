package smsfilter.app;

import java.net.URLDecoder;

import smsfilter.utils.Logger;
import smsfilter.classes.SmsFilters;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;


//
// Open Filters Activity
//
public final class OpenFiltersActivity extends Activity
{
// constants //
	public static final String	EXT	= "smsfilters";
	
	private static final String	TAG = "OpenFiltersActivity";

	
// variables //
	private String		filtersFilePath = "";
	
	
// methods //
	
	// onCreate
	@Override
	protected final void onCreate (Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView( D.layout.open_filters );

		try {
			filtersFilePath = UriToPath( getIntent().getData() );
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
		
		Logger.I( TAG, "open file: " + filtersFilePath );
	}
	
	
	// UriToPath
	private static final String UriToPath (Uri uri)
	{
		String uriPath  = uri.toString();
	
		if ( uriPath.startsWith( "file://" ) )
		{
			String filepath = uriPath.replace( "file://", "" );
			try {
				return URLDecoder.decode( filepath, "UTF-8" );
			} 
			catch (Exception e) {
				Logger.OnCatchException( TAG, e );
			}
		}
		return uriPath;
	}
	
	
	// GetSMSFilter
	private final SmsFilters GetSMSFilter ()
	{
		final FilterActivity	act = FilterActivity.GetInstance();
		
		if ( act != null )
			return act.GetSMSFilters();
		else
			return new SmsFilters( null );
	}
	
	
	// ReplaceFilters
	public final void ReplaceFilters (View view)
	{
		final SmsFilters	smsFilter = GetSMSFilter();

		_BackupCurrentFilters( smsFilter );
		
		smsFilter.Load( filtersFilePath );
		smsFilter.Save( null, SmsFilters.SAVE_FLAG_NONE );
		
		_StartProgram();
	}
	
	
	// AppendFilters
	public final void AppendFilters (View view)
	{
		final SmsFilters	smsFilter = GetSMSFilter();

		_BackupCurrentFilters( smsFilter );
		
		smsFilter.AppendFilters( filtersFilePath );
		smsFilter.Save( null, SmsFilters.SAVE_FLAG_NONE );
		
		_StartProgram();
	}
	
	
	// MergeFilters
	public final void MergeFilters (View view)
	{
		final SmsFilters	smsFilter = GetSMSFilter();

		_BackupCurrentFilters( smsFilter );
		
		smsFilter.MergeFilters( filtersFilePath );
		smsFilter.Save( null, SmsFilters.SAVE_FLAG_NONE );
		
		_StartProgram();
	}
	
	
	// _StartProgram
	private final void _StartProgram ()
	{
		Intent	intent = new Intent( this, FilterActivity.class );
		intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
		intent.putExtra( FilterActivity.EXTRA_REFRESH_FILTERS, true );
		
		startActivity( intent );
		finish();
	}
	
	
	// _BackupCurrentFilters
	private static final void _BackupCurrentFilters (SmsFilters smsFilter)
	{
		smsFilter.Backup();
	}
}
