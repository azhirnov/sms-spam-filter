package smsfilter.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import smsfilter.app.MyApplication;
import smsfilter.classes.MySmsMessage;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.view.View;
import android.view.ViewGroup;


//
// Utils
//

public final class MyUtils
{
// constants //
	private static final String	TAG = "MyUtils";
	
	
	
// methods //

	// GetAPIVersion
	public final static int GetAPIVersion () {
		return android.os.Build.VERSION.SDK_INT;
	}
	
	
	// CheckAPIVersion
	public final static boolean CheckAPIVersion (int api) {
		return GetAPIVersion() >= api;
	}
	
	
	// IsExternalStorageWritable
	public static final boolean IsExternalStorageWritable ()
	{
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();
	
		if ( Environment.MEDIA_MOUNTED.equals( state ) )
		    mExternalStorageWriteable = true;
		else
		if ( Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) )
		    mExternalStorageWriteable = false;
		else
		    mExternalStorageWriteable = false;
		
		return mExternalStorageWriteable;
	}
	
	
	// FindViewWithType
	@SuppressWarnings("unchecked")
	public final static <T>
	T FindViewWithType (View parentView, Class<T> classType)
	{
		if ( parentView instanceof ViewGroup )
		{
			ViewGroup	gr = (ViewGroup) parentView;
			
			for (int i = 0; i < gr.getChildCount(); ++i)
			{
				View nextChild = gr.getChildAt(i);
				
				if ( classType.isInstance( nextChild ) ) {
					return (T) nextChild;
				}
				
				T	tmp = FindViewWithType( nextChild, classType );
				
				if ( tmp != null )
					return tmp;
			}
		}
		
		return null;
	}
	
	
	// GetContactNameFromNumber
	public final static
	String GetContactNameFromNumber (String phoneNumber)
	{
	    String 	contactName = null;
	    Cursor 	c = null;
	    
	    try {
			Context			ctx		= MyApplication.GetContext();
			ContentResolver	cr 		= ctx.getContentResolver();
		    Uri 			uri 	= Uri.withAppendedPath( PhoneLookup.CONTENT_FILTER_URI, Uri.encode( phoneNumber ) );
		    
		    c = cr.query( uri, new String[]{ PhoneLookup.DISPLAY_NAME }, null, null, null );
	
		    if ( c != null && c.moveToFirst() ) {
		        contactName = c.getString( c.getColumnIndex( PhoneLookup.DISPLAY_NAME ) );
		    }
	    }
	    catch (Exception e) {
	    	Logger.OnCatchException( TAG, e );
	    }
	    finally {
		    if ( c != null && !c.isClosed() ) {
		        c.close();
		        c = null;
		    }
	    }
	    return contactName;
	}
	
	
	// GetContactIdFromNumber
	public final static
	boolean GetContactIdAndNameFromNumber (String phoneNumber, String[] result)
	{
		boolean	res	= false;
	    Cursor 	c 	= null;
    
	    try {
			Context			ctx		= MyApplication.GetContext();
			ContentResolver	cr 		= ctx.getContentResolver();
		    Uri 			uri 	= Uri.withAppendedPath( PhoneLookup.CONTENT_FILTER_URI, Uri.encode( phoneNumber ) );
		    
		    c = cr.query( uri, new String[]{ PhoneLookup._ID, PhoneLookup.DISPLAY_NAME }, null, null, null );
		    
		    if ( c != null && c.moveToFirst() ) {
		    	result[0] = c.getString( c.getColumnIndex( PhoneLookup._ID ) );
		    	result[1] = c.getString( c.getColumnIndex( PhoneLookup.DISPLAY_NAME ) );
		    	res = true;
		    }
	    }
	    catch (Exception e) {
	    	Logger.OnCatchException( TAG, e );
	    }
	    finally {
		    if ( c != null && !c.isClosed() ) {
		        c.close();
		        c = null;
		    }
	    }
	    
	    return res;
	}
	
	
	// GetContactPhoneNumbers
	public final static
	ArrayList<String> GetContactPhoneNumbers (String contactName)
	{
		Cursor				c 		= null;
		ArrayList<String> 	numbers = new ArrayList<String>();
		
		try {
			Context		ctx 		= MyApplication.GetContext();
			String 		selection 	= ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" like'%" + contactName +"%'";
			String[] 	projection 	= new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER };
			
			c = ctx.getContentResolver().query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
												projection, selection, null, null );
			if ( c != null && c.moveToFirst() )
			{
				do {
					numbers.add( c.getString(0) );
				}
				while ( c.moveToNext() );
			}
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
		finally {
			if ( c != null && !c.isClosed() ) {
				c.close();
				c = null;
			}
		}
		
		return numbers;
	}
	
	
	// GetContactName
	public final static
	String GetContactName (Uri contactData)
	{
		String 	name 	= null;
        Cursor 	c	 	= null;
		
		try {
			Context	ctx = MyApplication.GetContext();
			
			c = ctx.getContentResolver().query( contactData, null, null, null, null );
	
	        int nameId = c.getColumnIndex( ContactsContract.Contacts.DISPLAY_NAME );
	
	        if ( c.moveToFirst() )
	        {
	            name = c.getString( nameId );
	            Logger.I( TAG, "GetContactName: " + name );
	        }
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
        finally {
	        if ( c != null && !c.isClosed() ) {
	            c.close();
	        }
        }
		
		return name;
	}
	
	
	// GetContactNamesFromPhoneNumber
	public final static
	ArrayList<String> GetContactNamesFromPhoneNumber (String phoneNumber)
	{
	    ArrayList<String>	names 	= new ArrayList<String>();
		Cursor 				c 		= null;
		
		try {
			Context	ctx  = MyApplication.GetContext();
			
			Uri 	uri  = Uri.withAppendedPath( ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber) );
	
		    c = ctx.getContentResolver().query( uri, new String[] { BaseColumns._ID,
		            					ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null );
	
	        if ( c != null && c.getCount() > 0 )
	        {
	            while ( c.moveToNext() ) {
	            	names.add( c.getString( c.getColumnIndex( ContactsContract.Data.DISPLAY_NAME ) ) );
	            }
	        }
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
	    finally {
	        if ( c != null && !c.isClosed() ) {
	            c.close();
	            c = null;
	        }
	    }
		
	    return names;
	}

	
	
	// GetPhoneNumber
	public final static
	boolean GetPhoneNumber (Intent data, String[] str)
	{
		if ( data == null )
			return false;

        Uri uri = data.getData();

        if ( uri == null )
        	return false;

        Cursor c = null;
        
        try {
			Context	ctx = MyApplication.GetContext();
			
			ContentResolver cr = ctx.getContentResolver();
			
            c = cr.query(uri, new String[]{ 
                        ContactsContract.CommonDataKinds.Phone.NUMBER,  
                        ContactsContract.CommonDataKinds.Phone.TYPE },
                    null, null, null);

            if ( c != null && c.moveToFirst() )
            {
                String number = c.getString(0);
               // int type = c.getInt(1);	// mobie, word, etc
                
                str[0] = number;
            }
        }
        finally {
            if ( c != null ) {
            	c.close();
            	c = null;
            }
        }
        return true;
	}
	
	
	// CloseStream
	public final static
	void CloseStream (OutputStream os)
	{
		if ( os != null )
		{
			try {
				os.close();
			} catch (IOException e) {
				Logger.OnCatchException( TAG, e );
			}
		}
	}
	
	
	// CloseStream
	public final static
	void CloseStream (InputStream is)
	{
		if ( is != null )
		{
			try {
				is.close();
			} catch (IOException e) {
				Logger.OnCatchException( TAG, e );
			}
		}
	}
	
	
	// MoveSmsToInbox
	public final static
	void MoveSmsToInbox (MySmsMessage sms)
	{
		MoveSmsToInbox( sms.sender, sms.body, sms.timeMillis );
	}
	
	
	// MoveSmsToInbox
	public final static
	void MoveSmsToInbox (String sender, String msg, long time)
	{
		try {
			final Context 		ctx 	= MyApplication.GetContext();
			final ContentValues	values 	= new ContentValues();
			
			values.put( "address", 	sender );
			values.put( "body", 	msg );
			values.put( "date", 	time );
			values.put( "read",		0 );
			values.put( "type",		1 );
			values.put( "seen",		1 );
			
			ctx.getContentResolver().insert( Uri.parse("content://sms/inbox"), values );
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
	}
}
