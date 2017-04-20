package smsfilter.classes;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import smsfilter.app.D;
import smsfilter.app.FilterActivity;
import smsfilter.classes.SmsJournalMngr.JournalItem;
import smsfilter.dialogs.SmsViewer;
import smsfilter.utils.*;


//
// Sms Journal Tab
//
public final class SmsJournal implements TabViewItem, SmsJournalMngr.AsyncLoadListCallback
{
// constants //
	private static final String	TAG = "SmsJournal";

	
// variables //
	private final FilterActivity	_activity;
	private ListAdapter				_adapter;

	private ProgressBar				_progress;
	
	private int						_viewSmsAfterUpdate = -1;
	
	private boolean					_visible		 = false;
	private boolean					_updating		 = false;
	private boolean					_progressVisible = true;
	
	
// methods //
	public SmsJournal (FilterActivity act) {
		_activity = act;
	}
	

	// Update
	@Override
	public final void Update (View v)
	{
		_UpdateList();
		
		if ( v == null )  return;
		
		final ListView	list = (ListView) v.findViewById( D.id.listView1 );

		if ( _progress == null )
			_progress = (ProgressBar) v.findViewById( D.id.progressBar1 );
		
		if ( _progress != null )
			_progress.setVisibility( _progressVisible ? View.VISIBLE : View.GONE );
		
		list.setAdapter( _adapter );
		
		list.setOnItemClickListener( new OnItemClickListener()
		{
			@Override
			public void onItemClick (AdapterView<?> parent, View view, int position, long id)
			{
				_ShowSmsViewer( _adapter.GetList().get( position ), position );
			}
		});
		
		if ( _visible )
		{
			final int max = SmsJournalMngr.GetJournalSize()-1;
			NotificationMngr.RemoveNotifications( 0, max );
		}
		
		Logger.I( TAG, "Update" );
	}
	
	
	// OnShow
	@Override
	public final void OnShow ()
	{
		_visible = true;
		
		final int max = SmsJournalMngr.GetJournalSize()-1;
		
		boolean	needUpdate = true;
		
		if ( _adapter != null && _adapter.getCount() > 0 )
		{
			final int last = _adapter.GetList().get(0).id;
			needUpdate = ( last != max );
		}

		if ( ! needUpdate && _progress != null )
			needUpdate = ( _progress.getVisibility() == View.VISIBLE );
		
		if ( needUpdate )
			_UpdateList();
		
		NotificationMngr.RemoveNotifications( 0, max );
		
		Logger.I( TAG, "OnShow" );
	}
	
	
	// OnHide
	@Override
	public final void OnHide ()
	{
		_visible = false;
	}
	
	
	// ViewSMS
	public final void ViewSMS (int id)
	{
		List< SmsJournalMngr.JournalItem >  list = _adapter.GetList();
		
		for (int i = list.size()-1; i >= 0; --i)
		{
			SmsJournalMngr.JournalItem item = list.get(i);
			
			if ( item.id == id ) {
				_viewSmsAfterUpdate = -1;
				_ShowSmsViewer( item, i );
				return;
			}
		}
		
		_viewSmsAfterUpdate = id;
	}
	
	
	// _ShowSmsViewer
	private final void _ShowSmsViewer (final SmsJournalMngr.JournalItem item, final int pos)
	{
		final SmsViewer	viewer = new SmsViewer( _activity );

		String		sender = null;
		
		if ( item.contact != null )
			sender = item.contact + "\n(" + item.sender + ")";
		else
			sender = item.sender;
		
		View.OnClickListener delete = new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				viewer.CloseDialog();
				_adapter.EraseFromIndex( pos );
				SmsJournalMngr.DeleteSms( item.filename );
			}
		};
		View.OnClickListener toInbox = new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				viewer.CloseDialog();
				_MoveToInbox( item );
				_adapter.EraseFromIndex( pos );
				SmsJournalMngr.DeleteSms( item.filename );
			}
		};
		
		viewer.ShowDialog( sender, item.body, item.date, toInbox, delete, null,
					D.string.btn_to_inbox, D.string.btn_delete, D.string.view_sms );
	}
	
	
	// _MoveToInbox
	private final void _MoveToInbox (SmsJournalMngr.JournalItem item)
	{
		MyUtils.MoveSmsToInbox( item.sender, item.body, item.date );
	}

	
	// _UpdateList
	private final void _UpdateList ()
	{
		if ( _adapter == null ) {
			_adapter = new ListAdapter( _activity, D.layout.journal_item );
		}
		
		if ( _updating )
			return;
		
		SmsJournalMngr.AsyncLoadList( this );
	}


	// OnLoad
	@Override
	public final void OnLoad (ArrayList<JournalItem> part)
	{
		if ( _progress != null )
			_progress.setVisibility( View.GONE );
		
		_progressVisible = false;
		
		_adapter.Append( part );

		if ( _viewSmsAfterUpdate >= 0 ) {
			ViewSMS( _viewSmsAfterUpdate );
		}
	}


	// OnStart
	@Override
	public void OnStart ()
	{
		if ( _progress != null )
			_progress.setVisibility( View.VISIBLE );
		
		_progressVisible = true;
		_updating = true;
		
		_adapter.Clear();
	}


	// OnFinish
	@Override
	public final void OnFinish ()
	{
		if ( _progress != null )
			_progress.setVisibility( View.GONE );
		
		_progressVisible = false;
		_updating = false;
		
		if ( _viewSmsAfterUpdate >= 0 ) {
			ViewSMS( _viewSmsAfterUpdate );
		}
	}
	
	
	//
	// List Adapter
	//
	private final class ListAdapter extends CustomListAdapter< SmsJournalMngr.JournalItem >
	{
		public ListAdapter (Context ctx, int viewId) {
			super(ctx, viewId);
		}

		protected final void InitView (SmsJournalMngr.JournalItem item, View view, int pos)
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
}
