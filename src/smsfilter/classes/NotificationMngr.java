package smsfilter.classes;

import smsfilter.app.D;
import smsfilter.app.FilterActivity;
import smsfilter.app.MyApplication;
import smsfilter.app.Preferences;
import android.app.Activity;
import android.support.v4.app.*;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import smsfilter.utils.Logger;
import smsfilter.utils.MyUtils;



//
// Notification Receiver
//
public final class NotificationMngr
{
// constants //
	private static final String	TAG = "NotificationReceiver";
	
	private static final int	MAX_SUBJ_LEN = 30;
	
	// default
	private static final int	LED_LIGHT_COLOR		= 0xFFFF6C00;
	private static final int	LED_LIGHT_ON_MS		= 500;
	private static final int	LED_LIGHT_OFF_MS	= 2000;
	

	
// methods //

	// ShowLedLight
	public static final
	void ShowLedLight (Context ctx, int index, int color, int onMs, int offMs)
	{
		try {
			NotificationManager nm = (NotificationManager) ctx.getSystemService( Activity.NOTIFICATION_SERVICE );
			
			NotificationCompat.Builder	nb = new NotificationCompat.Builder(ctx);

	        nb.setOnlyAlertOnce( true );
	        nb.setAutoCancel( true );
	        nb.setLights( color, onMs, offMs );
			
			nm.notify( index, nb.build() );
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
	}
	
	
	// ShowDefaultLedLight
	public static final
	void ShowDefaultLedLight (Context ctx, int index)
	{
		ShowLedLight( ctx, index, LED_LIGHT_COLOR, LED_LIGHT_ON_MS, LED_LIGHT_OFF_MS );
	}
	
	
	// RemoveNotification
	public static final
	void RemoveNotification (int index)
	{
		try {
			final NotificationManager nm = (NotificationManager) MyApplication.GetContext().
											getSystemService( Activity.NOTIFICATION_SERVICE );
			nm.cancel( index );
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
	}
	
	
	// OnSave
	public static final
	void OnSave (MySmsMessage sms, int num)
	{
		Preferences.AppPrefs p = Preferences.ReadPrefs();
		
		if ( !p.showNotif )
			return;
		
		try {
			Context	ctx 	= MyApplication.GetContext();
			String	ticker 	= ctx.getString( D.string.notif_block_incoming_sms );
			String	subj 	= sms.body.substring( 0, Math.min( MAX_SUBJ_LEN, sms.body.length() ) ) + "...";
			String	sender	= MyUtils.GetContactNameFromNumber( sms.sender );
			
			if ( sender == null )
				sender = sms.sender;

			NotificationManager nm = (NotificationManager) ctx.getSystemService( Activity.NOTIFICATION_SERVICE );
			
			Intent 			intent  = new Intent( ctx, FilterActivity.class );
			intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
			intent.putExtra( FilterActivity.NOTIF_SMS_ID, num );
			
			PendingIntent 	pIntent = PendingIntent.getActivity( ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );
	
			NotificationCompat.Builder	nb = new NotificationCompat.Builder(ctx);

			nb.setTicker( ticker );
	        nb.setContentTitle( sender );
	        nb.setContentText( subj );
	        nb.setSmallIcon( D.drawable.ic_launcher );
	        nb.setContentIntent( pIntent );
	        nb.setOnlyAlertOnce( true );
	        nb.setAutoCancel( true );
	        
	        if ( p.ledLight ) {
	        	nb.setLights( LED_LIGHT_COLOR, LED_LIGHT_ON_MS, LED_LIGHT_OFF_MS );
	        }
			nm.notify( num, nb.build() );
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
	}
	
	
	// RemoveNotifications
	public static final
	void RemoveNotifications (int min, int max)
	{
		try {
			final NotificationManager nm = (NotificationManager) MyApplication.GetContext().
											getSystemService( Activity.NOTIFICATION_SERVICE );
			
			for (int i = max; i >= min; --i) {
				nm.cancel( i );
			}
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
	}
}