package smsfilter.app;

import smsfilter.classes.MySmsMessage;
import smsfilter.classes.NotificationMngr;
import smsfilter.classes.SmsFilters;
import smsfilter.classes.SmsJournalMngr;
import smsfilter.dialogs.SmsViewer;
import smsfilter.utils.Logger;
import smsfilter.utils.MyUtils;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;


//
// Popup Activity
//
public final class PopupActivity extends Activity
{
// constants //
	private static final String	TAG = "PopupActivity";
	
	public static final String	EXT_SENDER	= "sms.sender";		// String
	public static final String	EXT_MESSAGE	= "sms.message";	// String
	public static final String	EXT_DATE	= "sms.date";		// long
	
	private static final int	NITIF_ID	= -1;
	
	
	
// variables //
	private View	_rootView;
	private int		_dialogCounter;
	
	
// methods //

	// Run
	public static final void Run (String sender, String message, long date)
	{
		Context ctx = MyApplication.GetContext();
		Intent intent = new Intent( ctx, PopupActivity.class );
		intent.putExtra( EXT_SENDER, sender );
		intent.putExtra( EXT_MESSAGE, message );
		intent.putExtra( EXT_DATE, date );
		intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
		ctx.startActivity( intent );
		Logger.I( TAG, "Run" );
	}
	

	// Run
	public static final void Run (MySmsMessage sms) {
		Run( sms.sender, sms.body, sms.timeMillis );
	}
	
	
	// onCreate
	@Override
	public final void onCreate (Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		LinearLayout	ll = new LinearLayout( this );
		ll.setLayoutParams( new LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT ) );
		setContentView( ll );
		
		_rootView = ll;
		_dialogCounter = 0;

    	if ( Preferences.ReadPrefs().ledLight ) {
    		NotificationMngr.ShowDefaultLedLight( this, NITIF_ID );
    	}
        
		HandleCommand( getIntent() );
	    Logger.I( TAG, "onCreate" );
	}
	
	
	// onDestroy
	@Override
	public final void onDestroy ()
	{
		super.onDestroy();
        
		if ( AppConfig.PRO ) {
			NotificationMngr.RemoveNotification( NITIF_ID );
        }
		
		Logger.I( TAG, "onDestroy" );
		
		// TODO: save all sms
	}

	
	// onNewIntent
	@Override
	protected final void onNewIntent (Intent intent)
	{
	    super.onNewIntent(intent);

		HandleCommand( intent );
	    Logger.I( TAG, "onNewIntent" );
	}
	
	
	// HandleCommand
	private final void HandleCommand (Intent intent)
	{
		String	sender		= "";
		String	text		= "";
		long	date		= 0;

		try {
			Bundle ext = intent.getExtras(); 
			sender 	= ext.getString( EXT_SENDER );
			text	= ext.getString( EXT_MESSAGE );
			date	= ext.getLong( EXT_DATE );
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
			return;
		}
		
		String	senderName	= MyUtils.GetContactNameFromNumber( sender );
		
		final SmsViewer		viewer 	= new SmsViewer( (Activity)this );
		final MySmsMessage	sms		= new MySmsMessage();
		final String		name	= senderName != null ? senderName : sender;
		sms.sender 		= sender;
		sms.body		= text;
		sms.timeMillis	= date;
		
		++_dialogCounter;
		Logger.I( TAG, "show dialog, id: " + _dialogCounter );
		
		_rootView.post( new Runnable()
		{
			public void run ()
			{
				viewer.ShowDialog(
						name, sms.body, sms.timeMillis,
						new View.OnClickListener() {
							@Override
							public void onClick (View view) {
								_AddToInbox( sms.sender, sms.body, sms.timeMillis );
								_AddSender( sms.sender, true );
								viewer.CloseDialog();
								_Close();
							}
						},
						new View.OnClickListener() {
							@Override
							public void onClick (View view) {
								SmsJournalMngr.SaveSms( sms, false );
								_AddSender( sms.sender, false );
								viewer.CloseDialog();
								_Close();
							}
						},
						new DialogInterface.OnCancelListener() {
							@Override
							public void onCancel (DialogInterface dialog) {
								_AddToInbox( sms.sender, sms.body, sms.timeMillis );
								viewer.CloseDialog();
								_Close();
							}
						},
						D.string.btn_allow_sms,
						D.string.btn_block_sms,
						D.string.txt_received_suspect_sms );
				Logger.I( TAG, "popup shown" );
			}
		});
	}
	
	
	// _Close
	private final void _Close ()
	{
		Logger.I( TAG, "dialog closed, id: " + _dialogCounter );
		
		if ( --_dialogCounter <= 0 )
		{
			Logger.I( TAG, "popup shown" );

	        if ( AppConfig.PRO ) {
	        	NotificationMngr.RemoveNotification( NITIF_ID );
	        }
			finish();
		}
	}
	
	
	// GetSMSFilter
	private final SmsFilters _GetSMSFilter ()
	{
		final FilterActivity	act = FilterActivity.GetInstance();
		
		if ( act != null )
			return act.GetSMSFilters();
		else
			return new SmsFilters( null );
	}
	
	
	// _AddSender
	private final void _AddSender (String sender, boolean allow)
	{
		Logger.I( TAG, "_AddSender " + (allow ? "allow" : "block") );
		
		final SmsFilters	smsFilter = _GetSMSFilter();
		
		smsFilter.AddSender( sender, allow );
		smsFilter.Save( null, SmsFilters.SAVE_FLAG_NONE );
		smsFilter.Refresh();
	}
	
	
	// _AddToInbox
	private final void _AddToInbox (String sender, String text, long time)
	{
		Logger.I( TAG, "_AddToInbox" );
		MyUtils.MoveSmsToInbox( sender, text, time );
	}
}
