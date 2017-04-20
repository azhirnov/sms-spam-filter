package smsfilter.app;

import smsfilter.utils.Logger;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Secure;
import android.widget.Toast;


//
// Application
//
public final class MyApplication extends Application
{
// constants //
	private static final String	TAG = "MyApplication";

	
// variables //
    private static Context 	context;
    private static String	devId;
    
    
// methods //
    
    // onCreate
    public final void onCreate () {
        super.onCreate();
        MyApplication.context = getApplicationContext();
        
        MyApplication.devId = Secure.getString( MyApplication.context.getContentResolver(), Secure.ANDROID_ID );
    }

    
    // GetContext
    public final static Context GetContext () {
        return MyApplication.context;
    }
    
    
    // ShowToast
    public final static void ShowToast (String text, int duration)
    {
    	if ( GetContext() == null )
    		return;
    	try {
    		Toast.makeText( GetContext(), text, duration ).show();
    	}
    	catch (Exception e) {
    		Logger.OnCatchException( TAG, e );
    	}
    }
    
    
    // ShowToast
    public final static void ShowToast (int text, int duration)
    {
    	if ( GetContext() == null )
    		return;
    	try {
    		Toast.makeText( GetContext(), text, duration ).show();
    	}
    	catch (Exception e) {
    		Logger.OnCatchException( TAG, e );
    	}
    }
    
    
    // GetString
    public final static String GetString (int id)
    {
    	if ( GetContext() == null )
    		return "";
    	
    	return GetContext().getString( id );
    }
    
    
    // GetDeviceId
    public final static String GetDeviceId () {
    	return MyApplication.devId;
    }

    
	// GetPackageName
	public static final String GetPackageName () {
		return GetContext().getPackageName();
	}
	
    
    // RunOnUIThread
    public final static void RunOnUIThread (Runnable r) {
    	(new Handler( Looper.getMainLooper() )).post( r );
    }
    
    
    // IsUIThread
    public final static boolean IsUIThread () {
    	return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}
