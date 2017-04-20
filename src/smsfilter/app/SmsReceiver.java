package smsfilter.app;

import java.util.ArrayList;

import smsfilter.utils.Logger;
import smsfilter.utils.MyUtils;
import smsfilter.classes.MySmsMessage;
import smsfilter.classes.SmsFilters;
import smsfilter.classes.SmsJournalMngr;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

 
//
// SMS Receiver
//

public final class SmsReceiver extends BroadcastReceiver
{
// constants //
	private static final String	TAG 		= "SmsReceiver";
	private static final String	PDUS		= "pdus";
	private static final String	RECEIVED	= "android.provider.Telephony.SMS_RECEIVED";

	
// types //
	private final static class MySmsMessageBuilder extends MySmsMessage
	{
		private StringBuilder	builder = null;

		
		public MySmsMessageBuilder (SmsMessage msg)
		{
			sender 		= msg.getOriginatingAddress();
			builder		= new StringBuilder( msg.getMessageBody().toString() );
			timeMillis	= msg.getTimestampMillis();
		}
		
		
		public final boolean AddMessage (SmsMessage msg)
		{
			String	address = msg.getOriginatingAddress();
		
			if ( address == null || address.isEmpty() )
				address = EMPTY_SENDER;
			
			if ( sender.equals( address ) )
			{
				builder.append( msg.getMessageBody().toString() );
				timeMillis	 = Math.max( timeMillis, msg.getTimestampMillis() );
				return true;
			}
			return false;
		}


		public final void Build ()
		{
			body	= builder.toString();
		}
	}
	
	
// methods //
	
	// onReceive
	@Override
	public final void onReceive (Context context, Intent intent) 
	{
        if ( ! intent.getAction().equals( RECEIVED ) )
        	return;
        
		Logger.I( TAG, "onReceive" );
		
		final Bundle 	bundle 	= intent.getExtras();
		
		Preferences.AppPrefs prefs = Preferences.ReadPrefs();
		
		if ( ! prefs.enabled ) {
			Logger.I( TAG, "filtering disabled" );
			return;
		}
		
		if ( MyUtils.CheckAPIVersion( 11 ) )
		{
			final BroadcastReceiver.PendingResult result = this.goAsync();
			
			new Thread()
			{
	            public void run()
	            {
	            	try {
						Thread.sleep( 8000 );
					} catch (InterruptedException e) {
						Logger.OnCatchException( TAG, e );
					}
	            	
	    			if ( CheckMessage( bundle ) )
	    				result.clearAbortBroadcast();
	    			else
	    				result.abortBroadcast();
	    			
	    			result.finish();
	            }
	        }.start();
		}
		else
		{
			if ( ! CheckMessage( bundle ) )
				this.abortBroadcast();
		}
	}
	
	
	// CheckMessage
	public static final
	boolean CheckMessage (Bundle bundle)
	{
		try
		{
			SmsFilters		filter 	= new SmsFilters();
			
			Preferences.AppPrefs prefs = Preferences.ReadPrefs();
			
			if ( bundle != null )
			{
				final Object[]	pdus 	= (Object[]) bundle.get( PDUS );
				int				allow 	= 1;

				final ArrayList< MySmsMessageBuilder >	msgs = new ArrayList< MySmsMessageBuilder >();
				
				
				// build messages from unique senders
				for (int i = 0; i < pdus.length; ++i)
				{
					SmsMessage	tmp = SmsMessage.createFromPdu( (byte[]) pdus[i] );
					
					boolean	found = false;
					
					for (MySmsMessageBuilder m : msgs)
					{
						if ( m.AddMessage( tmp ) ) {
							found = true;
							break;
						}
					}
					
					if ( !found )
					{
						msgs.add( new MySmsMessageBuilder( tmp ) );
					}
				}
				
				Logger.I( TAG, "unique senders: " + msgs.size() );
				
				
				// check messages
				for (MySmsMessageBuilder m : msgs)
				{
					m.Build();
					
					Logger.I( TAG, "check sms from: " + m.sender + ", text: " + m.body );
					
					final int res = filter.Apply( prefs, m );
					
					if ( res == SmsFilters.RES_SHOW_POPUP_FOR_SUSPECT )
					{
						Logger.I( TAG, "show suspicious sms popup" );
						
						PopupActivity.Run( m );
						allow = 0;
						continue;
					}
					
					final boolean	passed = res >= SmsFilters.RES_PASSED;
					
					if ( !passed )
					{
						Logger.I( TAG, "sms blocked" );
						SmsJournalMngr.SaveSms( m, true );
					}

					allow &= passed ? 1 : 0;
				}
				
				return allow == 1;
			}
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
		return true;
	}
}
