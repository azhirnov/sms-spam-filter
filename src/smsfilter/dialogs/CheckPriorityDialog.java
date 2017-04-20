package smsfilter.dialogs;

import java.util.List;

import smsfilter.app.D;
import smsfilter.app.FilterActivity;
import smsfilter.app.MyApplication;
import smsfilter.app.Preferences;
import smsfilter.utils.Logger;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;



public final class CheckPriorityDialog
{
// constants //
	private final static String	TAG 		= "CheckPriorityDialog";
	

// methods //
	
	public CheckPriorityDialog (FilterActivity act)
	{
		SharedPreferences 	prefs = PreferenceManager.getDefaultSharedPreferences( act );
		
		if ( ! prefs.getBoolean( Preferences.CHECK_PRIORITY_KEY, true ) )
			return;
		
		(new CheckingTask( act, prefs )).execute();
	}
	
	
	
	private final static class CheckingTask extends AsyncTask< Void, Void, String >
	{
	// variables //
		private final SharedPreferences		_prefs;
		private final FilterActivity		_activity;
		
		
	// methods //
		public CheckingTask (FilterActivity act, SharedPreferences prefs)
		{
			_prefs = prefs;
			_activity = act;
		}

		
		@Override
		protected String doInBackground (Void... arg0)
		{
			try {
				final PackageManager	pm	 = _activity.getPackageManager();
				final List<ResolveInfo>	list = pm.queryBroadcastReceivers(
						new Intent("android.provider.Telephony.SMS_RECEIVED"),
						PackageManager.GET_INTENT_FILTERS | PackageManager.GET_RESOLVED_FILTER );
				 
				final String		selfPackage	= MyApplication.GetPackageName();
				final StringBuilder	builder		= new StringBuilder();
				int					counter		= 0;
				 
				for (ResolveInfo ri : list)
				{
					final String	pack = ri.activityInfo.packageName;
					 
					if ( pack.equals( selfPackage ) )
						break;
		
					++counter;
					
					final CharSequence	appName = pm.getApplicationLabel( ri.activityInfo.applicationInfo );
					
					builder.append( " ● " );
					
					if ( appName != null && appName.length() > 0 )
						builder.append( appName );
					else
						builder.append( pack );
					
					builder.append("\n");
				}
				 
				if ( counter == 0 || builder.length() == 0 )
					return null;
				
				return builder.toString();
			}
			catch (Exception e) {
				Logger.OnCatchException( TAG, e );
			}
			return null;
		}
		
		
		@Override
		protected void onPostExecute (final String result)
		{
			if ( result == null || result.isEmpty() )
				return;
			
			_activity.GetRootView().post( new Runnable() {
				public void run () {
					_ShowDialog( result );
				}
			});
		}
		
		
		private final void _ShowDialog (String result)
		{
			try {
				final Dialogs.OkCancelDialog	dlg = new Dialogs.OkCancelDialog( _activity );
				 
				dlg.GetTitleView().setText( D.string.dlg_reinstall_title );
				 
				TextView quest = dlg.GetQuestionView();
				quest.setText( String.format( _activity.getString( D.string.dlg_reinstall_txt ),
						result, _activity.getString( D.string.title_text ) ) );
				
				quest.setMovementMethod( new ScrollingMovementMethod() );
				quest.setLineSpacing( 0.0f, 1.25f );
				
				dlg.GetCancelButton().setText( D.string.dlg_reinstall_dont_check );
				dlg.GetCancelButton().setOnClickListener( new View.OnClickListener() {
					@Override
					public void onClick (View v) {
						dlg.Close();
						SharedPreferences.Editor ed = _prefs.edit();
						ed.putBoolean( Preferences.CHECK_PRIORITY_KEY, false );
						ed.commit();
					}
				});
				 
				dlg.Show();
			}
			catch (Exception e) {
				Logger.OnCatchException( TAG, e );
			}
		}
	}
}
