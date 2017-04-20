package smsfilter.dialogs;

import java.util.ArrayList;

import smsfilter.app.D;
import smsfilter.app.FilterActivity;
import smsfilter.app.MyApplication;
import smsfilter.classes.CustomListAdapter;
import smsfilter.classes.SmsJournalMngr;
import smsfilter.utils.Logger;
import smsfilter.utils.MyUtils;
import smsfilter.utils.TextTransform;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public final class InboxViewer
{
// constants //
	private static final String	TAG = "InboxViewer";
	
	private static final int	LIST_PART_SIZE	= 4;
		
	
// types //
	public final static class InboxSms extends SmsJournalMngr.JournalItem
	{
		public String	contactId 	= null;
		public boolean	checked		= false;
		
		public InboxSms () { super(); }
	}
	
	public interface OnItemChoosen
	{
		public abstract void OnResult (InboxSms sms);
	}
	
		
// variables //
	private Dialog					_dialog = null;
	private ListAdapter				_adapter;
	private final FilterActivity	_activity;
	private ProgressBar				_progress;
	
	
// methods //
	public InboxViewer (FilterActivity act) {
		_activity = act;
	}

	
	// Show
	public final void Show (final OnItemChoosen onResult, final OnCancelListener onDialogCancel)
	{
		_dialog = new Dialog( _activity );
		_dialog.requestWindowFeature( Window.FEATURE_NO_TITLE );
		_dialog.setContentView( D.layout.inbox_view );
		_dialog.setCancelable( true );
		_dialog.setCanceledOnTouchOutside( false );

		if ( _adapter == null )
			_adapter = new ListAdapter( _activity, D.layout.journal_item );
		else
			_adapter.Clear();

		(new SmsReader()).execute();
		
		_progress = (ProgressBar) _dialog.findViewById( D.id.progressBar1 );
		
		final ListView	list = (ListView) _dialog.findViewById( D.id.listView1 );

		list.setAdapter( _adapter );

		list.setOnItemClickListener( new OnItemClickListener() {
			@Override
			public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
				_dialog.dismiss();
				onResult.OnResult( _adapter.GetList().get(position) );
			}
		});
		
		if ( onDialogCancel != null ) {
			_dialog.setOnCancelListener( onDialogCancel );
		}
		
		_dialog.show();
	}
	
	
	// _AddToList
	private final void _AddToList (ArrayList< InboxSms > arr)
	{
		if ( _progress != null )
		{
			_progress.setVisibility( View.INVISIBLE );
			_progress = null;
		}
		_adapter.Append( arr );
	}
	
	
	// _OnEndReading
	private final void _OnEndReading ()
	{
		final TextView	text = (TextView) _dialog.findViewById( D.id.noItemsInInbox );
		final ListView	list = (ListView) _dialog.findViewById( D.id.listView1 );
		
		if ( _adapter.getCount() == 0 )
		{
			text.setVisibility( View.VISIBLE );
			list.setVisibility( View.GONE );
		}
		if ( _progress != null )
		{
			_progress.setVisibility( View.INVISIBLE );
			_progress = null;
		}
	}

	
	
	//
	// List Adapter
	//
	private final class ListAdapter extends CustomListAdapter< InboxSms >
	{
		public ListAdapter (Context ctx, int viewId) {
			super(ctx, viewId);
		}

		protected final void InitView (InboxSms item, View view, int pos)
		{
			TextView	number 	= (TextView) view.findViewById( D.id.phoneNumber );
			TextView	time	= (TextView) view.findViewById( D.id.smsTime );
			TextView	text 	= (TextView) view.findViewById( D.id.smsText );

			if ( item.contact != null )	number.setText( item.contact );
			else						number.setText( item.sender );

			time.setText( TextTransform.FormatDate2Lines( item.date ) );
			text.setText( item.body );
		}
	}
	
	
	
	//
	// SMS Reader
	//
	private final class SmsReader extends AsyncTask< Void, ArrayList<InboxSms>, Void >
	{
		private Cursor	_cursor;
		
		
	    @Override
	    protected final void onPreExecute ()
	    {
	        super.onPreExecute();

			Context			ctx	= MyApplication.GetContext();
			ContentResolver	cr 	= ctx.getContentResolver();
			
			_cursor = cr.query( Uri.parse("content://sms/inbox"), null, null, null, null );

			if ( _cursor != null && _cursor.moveToFirst() )
			{}
			else
				_CloseCursor();
	    }
	    
	    
		@SuppressWarnings("unchecked")
		@Override
		protected final Void doInBackground (Void... params)
		{
			if ( _cursor == null )
				return null;

			ArrayList< InboxSms >	list = new ArrayList< InboxSms >();
			
			do {
				final InboxSms	sms = new InboxSms();
				
				try
				{
					String type = _cursor.getString( _cursor.getColumnIndexOrThrow( "type" ) );
					
					if ( !type.contains( "1" ) )
						continue;
					
					sms.sender 	= _cursor.getString( _cursor.getColumnIndexOrThrow( "address" ) );
					sms.body	= _cursor.getString( _cursor.getColumnIndexOrThrow( "body" ) );
					sms.date	= Long.parseLong( _cursor.getString( _cursor.getColumnIndexOrThrow( "date" ) ) );
				}
				catch (Exception e) {
					Logger.OnCatchException( TAG, e );
				}
				
				final String[]	result = new String[2];
				
				if ( MyUtils.GetContactIdAndNameFromNumber( sms.sender, result ) )
				{
					sms.contact   = result[1];
					sms.contactId = result[0];
				}
				
				list.add( sms );
				
				if ( list.size() > LIST_PART_SIZE )
				{
					publishProgress( list );
					list = new ArrayList< InboxSms >();
				}
			}
			while ( _cursor.moveToNext() );

			if ( ! list.isEmpty() )
			{
				publishProgress( list );
				list = null;
			}
			
			return null;
		}

		
	    @Override
	    protected final void onProgressUpdate (ArrayList<InboxSms>... values)
	    {
	    	if ( values == null )
	    		return;
	    	
	    	for (ArrayList<InboxSms> arr : values) {
	    		InboxViewer.this._AddToList( arr );
	    	}
	    }

	    
	    @Override
	    protected final void onPostExecute (Void result)
	    {
	        super.onPostExecute(result);
	        _CloseCursor();
	        InboxViewer.this._OnEndReading();
	    }
	    
	    
	    private final void _CloseCursor ()
	    {
		    if ( _cursor != null && !_cursor.isClosed() ) {
		    	_cursor.close();
		    	_cursor = null;
		    }
	    }
	}
	
}
