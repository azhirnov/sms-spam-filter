package smsfilter.classes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import smsfilter.app.FilterActivity;
import smsfilter.app.MyApplication;
import smsfilter.app.Preferences;
import smsfilter.utils.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;


//
// SMS Journal Manager
//

public final class SmsJournalMngr
{
// types //
	public static
	class JournalItem
	{
		public String	contact = null;
		public String	filename;
		public String	sender;
		public String	body;
		public long		date;
		public int		id;
		
		public JournalItem () {
			contact 	= null;
			filename	= null;
			sender		= null;
			body		= null;
			date		= 0;
			id			= 0;
		}
		
		public JournalItem (JournalItem ji) {
			this.contact	= ji.contact;
			this.filename	= ji.filename;
			this.sender		= ji.sender;
			this.body		= ji.body;
			this.date		= ji.date;
			this.id			= ji.id;
		}
	}
	
	
// constants //
	private static final String		TAG 		 = "SmsJournalMngr";

	private static final String		PREFS_NAME 	 = "SmsJournalPrefs";
	private static final String		JOURNAL_SIZE = "JOURNAL_SIZE";
	
	private static final int		VERSION 	 = 3;
	
	private static final int		MAX_LOG_ITEMS	= 500;
	
	
	
// methods //
	
	// SaveSms
	public static final boolean SaveSms (MySmsMessage msg, boolean showNotif)
	{
		final File dir = _GetJournalDir();
		
		Logger.I( TAG, "write to journal dir: " + dir.getAbsolutePath() );
		
		if ( !dir.exists() || !dir.isDirectory() )
		{
			dir.mkdirs();
		}
		
		if ( dir.exists() && dir.isDirectory() )
		{
			final int	num = _IncJournalSize();
			final File 	ff  = _GetUniqueFile( dir, num );
				
			if ( ff == null ) {
				Logger.E( TAG, "can't get unique file name!" );
				return false;
			}
				
			_SaveToFile( ff, msg, num );
			
			if ( showNotif ) {
				NotificationMngr.OnSave( msg, num );
			}
			FilterActivity.OnSmsReceived();
			return true;
		}
		else
		{
			Logger.E( TAG, "can't found or create directory " + dir.getAbsolutePath() );
			return false;
		}
	}
	
	/*
	// _LoadList
	private static final ArrayList< JournalItem > _LoadList ()
	{
		final File dir = _GetJournalDir();

		Logger.I( TAG, "read from journal dir: " + dir.getAbsolutePath() );
		
		if ( dir.exists() && dir.isDirectory() )
		{
			final File[]	files = dir.listFiles();
			
			final ArrayList< JournalItem >	items = new ArrayList< JournalItem >();

			Collections.sort( Arrays.asList(files), new Comparator< File > () {
				@Override
				public int compare (File lhs, File rhs) {
					return (int)( (rhs.lastModified() - lhs.lastModified()) / 60000 );
				}
			});
			
			for (int i = 0; i < files.length; ++i)
			{
				final JournalItem ji = _LoadFile( files[i] );
				
				if ( ji != null ) {
					items.add( ji );
				}
			}
			
			return items;
		}
		return null;
	}*/
	
	
	//
	// Async Load List Callback
	//
	public static interface AsyncLoadListCallback
	{
		public void OnLoad (ArrayList<JournalItem> part);
		public void OnStart ();
		public void OnFinish ();
	}
	
	
	//
	// Async Load List
	//
	private static final class _AsyncLoadList extends AsyncTask< Void, ArrayList<JournalItem>, Void >
	{
		private static final int	MAX_ITEMS = 6;
		
		private final AsyncLoadListCallback		_cb;
		
		public _AsyncLoadList (AsyncLoadListCallback cb) {
			this._cb = cb;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected final Void doInBackground(Void... params)
		{
			try {
				final File dir = _GetJournalDir();
	
				Logger.I( TAG, "read from journal dir: " + dir.getAbsolutePath() );
				
				if ( !( dir.exists() && dir.isDirectory() ) )
					return null;

				final File[]	files = dir.listFiles();

				if ( files == null || files.length <= 0 )
					return null;
					
				Collections.sort( Arrays.asList(files), new Comparator< File > () {
					@Override
					public int compare (File lhs, File rhs) {
						return (int)( (rhs.lastModified() - lhs.lastModified()) / 60000 );
					}
				});
				
				ArrayList< JournalItem >	items = new ArrayList< JournalItem >();
				
				final int	maxFiles = Math.min( files.length, MAX_LOG_ITEMS );
				
				for (int i = 0; i < maxFiles; ++i)
				{
					final File ff = files[i];
					
					final JournalItem ji = _LoadFile( ff );
					
					if ( ji != null )
					{
						ji.contact = MyUtils.GetContactNameFromNumber( ji.sender );
						items.add( ji );
					}
					
					if ( items.size() > MAX_ITEMS )
					{
						publishProgress( items );
						items = new ArrayList< JournalItem >();
					}
				}

				if ( ! items.isEmpty() )
				{
					publishProgress( items );
					items = null;
				}
			}
			catch (Exception e) {
				Logger.OnCatchException( TAG, e );
			}
			return null;
		}

	    @Override
	    protected final void onProgressUpdate (ArrayList<JournalItem>... values)
	    {
	    	if ( values == null )
	    		return;
	    	
	    	for (ArrayList<JournalItem> item : values) {
	    		_cb.OnLoad( item );
	    	}
	    }

	    @Override
	    protected final void onPostExecute (Void result)
	    {
	        super.onPostExecute(result);
	        _cb.OnFinish();
	    }
	    
	    @Override
	    protected final void onPreExecute ()
	    {
	    	super.onPreExecute();
	    	_cb.OnStart();
	    }
	}
	
	
	// AsyncLoadList
	public static final void AsyncLoadList (AsyncLoadListCallback cb)
	{
		(new _AsyncLoadList( cb )).execute();
	}
	
	
	// DeleteSms
	public static final void DeleteSms (String filename)
	{
		try {
			final File f = new File( filename );
			f.delete();
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
	}
	
	
	// DeleteAll
	public static final void DeleteAll ()
	{
		try
		{
			final File 		dir  = _GetJournalDir();
			final File[] 	list = dir.listFiles();
			
			for (File f : list)
			{
				f.delete();
			}
	
			final SharedPreferences prefs = MyApplication.GetContext().
					getSharedPreferences( PREFS_NAME, Context.MODE_PRIVATE );
			
			final SharedPreferences.Editor ed = prefs.edit();
			
			ed.putInt( JOURNAL_SIZE, 0 );
	
			if ( !ed.commit() ) {
				Logger.E( TAG, "can't save journal preferences" );
			}
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
	}
	
	
	// ClearOlder
	public static final void ClearOlder ()
	{
		final Preferences.AppPrefs p = Preferences.ReadPrefs();
		
		if ( p.autoClear == Preferences.AppPrefs.AUTO_CLEAR_NEVER )
			return;
		
		final Date	 	d = new Date();
		final Calendar 	c = Calendar.getInstance();
	    c.setTime( d );
	    
		switch ( p.autoClear )
		{
			case Preferences.AppPrefs.AUTO_CLEAR_OLDER_2_DAYS :
			    c.add( Calendar.DATE, -2 );
				break;
				
			case Preferences.AppPrefs.AUTO_CLEAR_OLDER_1_WEEK :
			    c.add( Calendar.DATE, -7 );
				break;
				
			case Preferences.AppPrefs.AUTO_CLEAR_OLDER_2_WEEK :
			    c.add( Calendar.DATE, -2*7 );
				break;
				
			case Preferences.AppPrefs.AUTO_CLEAR_OLDER_1_MONTH :
			    c.add( Calendar.MONTH, -1 );
				break;
		}

		final long	time = c.getTime().getTime();

		(new _AsyncClearOlder( time )).execute();
	}
	
	
	//
	// Async Clear Older
	//
	private static final class _AsyncClearOlder extends AsyncTask< Void, Void, Void >
	{
		private final long	_cmpTime;
		
		public _AsyncClearOlder (long time)
		{
			_cmpTime	= time;
		}
		
		@Override
		protected final Void doInBackground (Void... params)
		{
			try
			{
				final File dir = _GetJournalDir();
				
				if ( !( dir.exists() && dir.isDirectory() ) )
					return null;
				
				final File[]	files = dir.listFiles();
				
				for (File f : files)
				{
					if ( f.lastModified() < _cmpTime )
						f.delete();
				}
				
			}
			catch (Exception e) {
				Logger.OnCatchException( TAG, e );
			}
			return null;
		}
	}
	
	
	// _GetJournalDir
	private static final File _GetJournalDir ()
	{
		final Context	ctx = MyApplication.GetContext();
		
		final File f = new File( ctx.getFilesDir().getAbsolutePath() + "/journal" );
		
		if ( f.exists() && f.isDirectory() )
			return f;
		
		f.mkdirs();
		return f;
	}
	
	
	// _GetUniqueFile
	private static final File _GetUniqueFile (File f, int num)
	{
		int		c		= 0;
		String 	fname 	= f.getAbsolutePath() + "/sms_" + num + ".bin";
		File	ff 		= new File( fname );
		
		while ( ff.exists() && c < 10 )
		{
			++c;
			fname = "sms_" + num + "_(" + c + ").bin";
			ff	  = new File( fname );
		}
		
		if ( ff.exists() ) {
			Logger.E( TAG, "file is exist!" );
			return null;
		}
		return ff;
	}
	
	
	// GetJournalSize
	public static final int GetJournalSize ()
	{
		final SharedPreferences prefs = MyApplication.GetContext().
				getSharedPreferences( PREFS_NAME, Context.MODE_PRIVATE );
			
		return prefs.getInt( JOURNAL_SIZE, 0 );
	}
	
	
	// _IncJournalSize
	private static final int _IncJournalSize ()
	{
		int	result = 0;
		
		// load
		final SharedPreferences prefs = MyApplication.GetContext().
				getSharedPreferences( PREFS_NAME, Context.MODE_PRIVATE );
			
		result = prefs.getInt( JOURNAL_SIZE, 0 );
		
		// save
		final SharedPreferences.Editor ed = prefs.edit();
		
		ed.putInt( JOURNAL_SIZE, result+1 );

		if ( !ed.commit() ) {
			Logger.E( TAG, "can't save journal preferences" );
		}
		
		return result;
	}
	
	
	// _SaveToFile
	private static final void _SaveToFile (File filename, MySmsMessage sms, int id)
	{
		/*
		 * Format:
		 *	- int		version
		 *	- int		id
		 * 	- long		time
		 * 	- byte[]	sender
		 * 	- byte[]	message
		 */
		
		FileOutputStream	fos = null;
		ObjectOutputStream	oos = null;
		
		try
		{
			fos = new FileOutputStream( filename );
			oos = new ObjectOutputStream( fos );
			
			oos.writeInt( VERSION );
			oos.writeInt( id );
			oos.writeLong( sms.timeMillis );
			oos.writeObject( sms.sender );
			oos.writeObject( sms.body );
			
			// TODO: add info of blocked words
			
			oos.close();
			fos.close();
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
			return;
		}
		finally {
			MyUtils.CloseStream( oos );
			MyUtils.CloseStream( fos );
		}
		
		Logger.I( TAG, "journal items saved: " + filename.toString() );
	}
	
	
	// _LoadFile
	private static final JournalItem _LoadFile (File filename)
	{
		FileInputStream		fis = null;
		ObjectInputStream	ois = null;
		JournalItem			ji  = null;
		
		try
		{
			fis = new FileInputStream( filename );
			ois = new ObjectInputStream( fis );
    		
			final int	ver = ois.readInt();

			switch ( ver )
			{
				case VERSION :	ji = _LoadFile_Current( ois );	break;
				case 2 :		ji = _LoadFile_v2( ois );		break;
				case 1 :		ji = _LoadFile_v1( ois );		break;
				default :		Logger.E( TAG, "unsupported version of SMS file, ver: " + ver );
			}
			
			if ( ji != null ) {
				ji.filename = filename.getAbsolutePath();
			}
    		
			ois.close();
			fis.close();
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
		finally {
			MyUtils.CloseStream( ois );
			MyUtils.CloseStream( fis );
		}
		return ji;
	}
	

	// _LoadFile_Current
	private static final JournalItem _LoadFile_Current (ObjectInputStream ois)
							throws IOException, ClassNotFoundException
	{
		final JournalItem	ji = new JournalItem();
    		
		ji.id		= ois.readInt();
	    ji.date		= ois.readLong();
	    ji.sender	= (String) ois.readObject();
	    ji.body		= (String) ois.readObject();
	    
	    return ji;
	}
	

	// _LoadFile_v2
	private static final JournalItem _LoadFile_v2 (ObjectInputStream ois)
							throws IOException, ClassNotFoundException
	{
		final JournalItem	ji = new JournalItem();
    		
		ji.id		= ois.readInt();
	    ji.date		= ois.readLong();
	    ji.sender	= (String) ois.readObject();
	    ji.body		= (String) ois.readObject();
	    
	    return ji;
	}
	
	
	// _LoadFile_v1
	private static final JournalItem _LoadFile_v1 (ObjectInputStream ois)
							throws IOException, ClassNotFoundException
	{
		final JournalItem	ji = new JournalItem();
    		
	    ji.date		= ois.readLong();
	    ji.sender	= (String) ois.readObject();
	    ji.body		= (String) ois.readObject();
	    
	    return ji;
	}
}
