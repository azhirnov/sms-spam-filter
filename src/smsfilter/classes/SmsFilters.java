package smsfilter.classes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TimePicker;
import smsfilter.app.D;
import smsfilter.app.FilterActivity;
import smsfilter.app.MyApplication;
import smsfilter.app.OpenFiltersActivity;
import smsfilter.app.Preferences;
import smsfilter.utils.Logger;
import smsfilter.utils.MyUtils;
import smsfilter.dialogs.AddFilterDialog;
import smsfilter.dialogs.CheckPriorityDialog;
import smsfilter.dialogs.Dialogs;
import smsfilter.dialogs.ExportDialog;
import smsfilter.dialogs.ImportFilters;



//
// Sms Filters
//

public class SmsFilters extends SmsFilterTypes implements TabViewItem
{
// constants //
	public static final int			RES_MARK_AS_SUSPECT			= 0;
	public static final int			RES_PASSED					= 1;
	public static final int			RES_ALLOWED_AS_SUSPECT		= 2;
	public static final int			RES_SHOW_POPUP_FOR_SUSPECT	= 3;
	public static final int			RES_BLOCKED_AS_SPAM			= -1;
	public static final int			RES_BLOCKED_AS_SUSPECT		= -2;
	
	private static final int		FLAG_ALLOW				= 1 << 0;
	private static final int		FLAG_ENABLED			= 1 << 1;
	
	private static final String		PREFS_FILE_NAME			= "filters." + OpenFiltersActivity.EXT;
	
	private static final String		PREFS_NAME				= "sms-filters-class";
	private static final String		PREF_LAST_BACKUP		= "last-backup";
	
	private static final long		AUTO_BACKUP_RATE		= 7 * 24 * 60 * 60 * 1000;
	
	private static final int		VERSION					= 2;
	

// types //
	
	//
	// Popup List
	//
	
	private final class SmsFiltersPopupList extends AddFilterDialog.PopupList
	{
		public SmsFiltersPopupList ()
		{
			Init( D.layout.add_filter, D.layout.add_filter_item, D.id.addFilterItemText, D.id.dlgTitle );
			SetTitleStringId( D.string.txt_add_filter );
		}

		@Override
		public final void OnItemSelected (View v, int index)
		{
			Logger.I( TAG, "item selected: " + index );
			
			int		type 	= (index >> 1) + SMS_FILTER_FOR_NAME;
			boolean	allow 	= (index & 1) == 0;

			BaseFilter	filter = null;
			
			switch ( type )
			{
				case SMS_FILTER_FOR_NAME :	filter = new NameFilter( allow );	break;
				case SMS_FILTER_FOR_DATE :	filter = new DateFilter( allow );	break;
				case SMS_FILTER_FOR_TEXT :	filter = new TextFilter( allow );	break;
				case SMS_FILTER_CUSTOM   :	filter = new CustomFilter();		break;
			}
			
			if ( filter == null )
				return;
			
			_filters.add( filter );
			Refresh();
		}
	}
	
	
	//
	// Popup Menu
	//
	
	private final class GroupItemPopupMenu
	{
		private final PopupWindow		_popup;
		
		
		public GroupItemPopupMenu (final int groupId, View view)
		{
			final LayoutInflater layoutInflater = (LayoutInflater) _activity.getSystemService( Context.LAYOUT_INFLATER_SERVICE );

			final View v = layoutInflater.inflate( D.layout.group_popup, null );
			
			TextView	btnMoveUp 	= (TextView) v.findViewById( D.id.btnMoveUp );
			TextView	btnMoveDown = (TextView) v.findViewById( D.id.btnMoveDown );
			TextView	btnDelete 	= (TextView) v.findViewById( D.id.btnDelete );
			//TextView	btnDisable	= (TextView) v.findViewById( R.id.btnDisable );
			
			if ( groupId == 0 ) {
				btnMoveUp.setEnabled( false );
			}
			btnMoveUp.setOnClickListener( new OnClickListener()
			{
				@Override
				public void onClick (View v) {
					_GroupMoveUp( groupId );
					_popup.dismiss();
				}
			});
			
			if ( groupId == _filters.size()-1 ) {
				btnMoveDown.setEnabled( false );
			}
			btnMoveDown.setOnClickListener( new OnClickListener()
			{
				@Override
				public void onClick (View v) {
					_GroupMoveDown( groupId );
					_popup.dismiss();
				}
			});
			
			btnDelete.setOnClickListener( new OnClickListener()
			{
				@Override
				public void onClick (View v) {
					_DeleteGroup( groupId );
					_popup.dismiss();
				}
			});
			/*
			BaseFilter bf = _filters.get( groupId );
			
			if ( bf.IsEnabled() ) {
				btnDisable.setText( R.string.btn_disable );
			}
			else {
				btnDisable.setText( R.string.btn_enable );
			}
			btnDisable.setOnClickListener( new OnClickListener()
			{
				@Override
				public void onClick (View v) {
					_DisableGroup( groupId );
					_popup.dismiss();
				}
			});*/
			
			_popup = new PopupWindow( v, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT );
			
			_popup.setOutsideTouchable(true);
			_popup.setTouchable(true);
			_popup.setBackgroundDrawable( new BitmapDrawable() );
			
			_popup.setTouchInterceptor( new OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if ( !IsViewContains( v, (int)event.getRawX(), (int)event.getRawY() ) ) {
						_popup.dismiss();
					}
					return false;
				}
			}); 

			// compute position
			Resources res = _activity.getResources();
			
			
			Point	screenSize = new Point();
			Rect	viewRegion = new Rect();
			
			GetScreenSize( screenSize );
			GetViewRegion( view, viewRegion );

			final int	marginY = 10;
			
			Point	screenCenter = new Point( screenSize.x/2, screenSize.y/2 );
			Point	viewCenter 	 = new Point( viewRegion.centerX(), viewRegion.centerY() );
			Point	popupSize	 = new Point( res.getDimensionPixelOffset( D.dimen.group_popup_width ),
											  res.getDimensionPixelOffset( D.dimen.group_popup_height ) );
			Point	popupOffset	 = new Point();
			
			popupOffset.x = screenCenter.x - popupSize.x/2;
			
			// top
			if ( viewCenter.y > screenCenter.y ) {
				popupOffset.y = -popupSize.y - viewRegion.height() - marginY;
			}
			// bottom
			else {
				popupOffset.y = marginY;
			}
			
			_popup.showAsDropDown( view, popupOffset.x, popupOffset.y );
		}
		
		private final boolean IsViewContains (View view, int rx, int ry) {
			final int[] l = new int[2];
		    view.getLocationOnScreen(l);
		    final int x = l[0];
		    final int y = l[1];
		    final int w = view.getWidth();
		    final int h = view.getHeight();

		    if (rx < x || rx > x + w || ry < y || ry > y + h) {
		        return false;
		    }
		    return true;
		}

		private final void GetScreenSize (Point size) {
			size.x = _activity.GetRootView().getWidth();
			size.y = _activity.GetRootView().getHeight();
		}

		private final void GetViewRegion (View view, Rect r) {
			final int[] l = new int[2];
		    view.getLocationOnScreen(l);
		    final int w = view.getWidth();
		    final int h = view.getHeight();
			
		    r.left  	= l[0];
		    r.right 	= l[0] + w;
		    r.top    	= l[1];
		    r.bottom	= l[1] + h;
		}
	}
	
	
	
// variables //
	private final ArrayList< BaseFilter >		_filters		= new ArrayList< BaseFilter >();
	private final ArrayList< String >			_filterTypes	= new ArrayList<String>();
	private FilterActivity						_activity 		= null;
	private ImportFilters						_importDialog	= null;
	
	
// methods //
	
	// constructor
	public SmsFilters ()
	{
		Reload();
		
		UpdateContacts( true );
		
		try {
			Resources r = MyApplication.GetContext().getResources();
			
			_filterTypes.add( r.getString( D.string.allow_sender 	) );
			_filterTypes.add( r.getString( D.string.block_sender 	) );
			_filterTypes.add( r.getString( D.string.allow_content 	) );
			_filterTypes.add( r.getString( D.string.block_content 	) );
			_filterTypes.add( r.getString( D.string.allow_time 		) );
			_filterTypes.add( r.getString( D.string.block_time 		) );
			_filterTypes.add( r.getString( D.string.custom_filters 	) );
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
	}
	
	// constructor
	public SmsFilters (String fromFile)
	{
		Load( fromFile );
	}
	
	
	// Reload
	public final void Reload ()
	{
		_Load( _GetDefaultFiltersFileName(), _filters );
		Refresh();
	}
	
	
	// Load
	public final boolean Load (String filename) {
		return _Load( filename == null ? _GetDefaultFiltersFileName() : filename, _filters );
	}
	
	
	// Save
	@Override
	public final boolean Save (String filename, int flags) {
		return _Save( filename == null ? _GetDefaultFiltersFileName() : filename, _filters, flags );
	}
	
	
	// Backup
	public final void Backup ()
	{
		final String	fname = ExportDialog.BuildExternalBackupFilename();
		
		if ( !fname.isEmpty() && Save( fname, SmsFilters.SAVE_FLAG_NONE ) )
		{
			final SharedPreferences	prefs = Preferences.ClassPreferences( PREFS_NAME );
			final SharedPreferences.Editor ed = prefs.edit();
			
			ed.putLong( PREF_LAST_BACKUP, System.currentTimeMillis() );
			ed.commit();
		}
	}
	
	
	// AutoBackup
	public final void AutoBackup ()
	{
		final SharedPreferences	prefs = Preferences.ClassPreferences( PREFS_NAME );
		
		final long	now 		= System.currentTimeMillis();
		final long	lastBackup 	= prefs.getLong( PREF_LAST_BACKUP, now );
		
		if ( lastBackup + AUTO_BACKUP_RATE > now )
		{
			Backup();
		}
	}
	
	
	// AppendFilters
	public final void AppendFilters (String filename)
	{
		final ArrayList< BaseFilter >	filters = new ArrayList< BaseFilter >();
		
		_Load( filename, filters );
		_filters.addAll( filters );
		Refresh();
	}
	
	
	// MergeFilters
	public final void MergeFilters (String filename)
	{
		// TODO: check, test, ...
		
		final ArrayList< BaseFilter >	filters = new ArrayList< BaseFilter >();
		
		_Load( filename, filters );

		for (BaseFilter bf : _filters)
		{
			for (int i = 0; i < filters.size(); ++i)
			{
				BaseFilter f = filters.get(i);
				
				if ( bf.GetFilterTypeId() == f.GetFilterTypeId() )
				{
					filters.remove(i);
					--i;
				}
			}
		}
		_filters.addAll( filters );
		Refresh();
	}
	
	
	// AddSender
	public final void AddSender (String sender, boolean allow)
	{
		for (BaseFilter bf : _filters)
		{
			if ( bf.GetFilterTypeId() == SMS_FILTER_FOR_NAME && bf.IsAllowed() == allow )
			{
				((NameFilter) bf).AddSender( sender );
				return;
			}
		}
		
		NameFilter	nf = new NameFilter( allow );
		nf.AddSender( sender );
		
		_filters.add( nf );
		Refresh();
	}


	// AddFilter
	public final void AddFilter ()
	{
		if ( _importDialog != null ) {
			_importDialog.Hide();
		}
		
		new AddFilterDialog( _activity, new SmsFiltersPopupList(), _filterTypes );
	}
	
	
	// Apply
	public final int Apply (Preferences.AppPrefs prefs, MySmsMessage smsMessage)
	{
		if ( _filters.isEmpty() )
			return RES_PASSED;
		
		final FilterTrace 		trace = new FilterTrace();
		final SmsMessageData	sms   = new SmsMessageData( smsMessage,
				MyUtils.GetContactNamesFromPhoneNumber( smsMessage.sender ) );
		
		
		for (int i = 0; i < _filters.size(); ++i)
		{
			BaseFilter bf = _filters.get(i);
			
			trace.currGroup = i;
			
			try {
				bf.Apply( sms, trace );
			}
			catch (Exception e) {
				Logger.OnCatchException( TAG, e );
			}
		}
		
		int result = _TraceAnalyzer( prefs.analyzeMode, trace );
		
		if ( result == RES_MARK_AS_SUSPECT )
		{
			if ( prefs.suspectMode == Preferences.AppPrefs.SUSPECT_SMS_ALLOW )
				return RES_ALLOWED_AS_SUSPECT;
			
			if ( prefs.suspectMode == Preferences.AppPrefs.SUSPECT_SMS_BLOCK )
				return RES_BLOCKED_AS_SUSPECT;
			
			return RES_SHOW_POPUP_FOR_SUSPECT;
		}
			
		return result;
	}


	// GetFilter
	@Override
	public final Filter GetFilter (int index) {
		return _filters.get( index );
	}


	// GetFiltersCount
	@Override
	public final int GetFiltersCount () {
		return _filters.size();
	}

	
	// OnGroupAddItemClick
	@Override
	public final void OnGroupAddItemClick (int group)
	{
		final BaseFilter	filter 	= _filters.get( group );
		filter.AddItem( _activity, this );
	}

	
	// OnGroupShowInfoClick
	@Override
	public final void OnGroupShowInfoClick (int group, View v) {
		new GroupItemPopupMenu( group, v );
	}
	
	
	// OnItemDeleteClick
	@Override
	public final void OnItemDeleteClick (int group, final int item)
	{
		final BaseFilter				filter 	= _filters.get( group );
		final Dialogs.OkCancelDialog	dlg 	= new Dialogs.OkCancelDialog( _activity );
		
		dlg.GetTitleView().setText( D.string.confirm_deleting );
		dlg.GetQuestionView().setText( filter.GetItemText( item ) );
		dlg.GetOKButton().setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick (View v)
			{
				dlg.Close();
				filter.DeleteItem( item );
				_OnFilterChanged();
			}
		});
		
		dlg.Show();
	}

	
	// OnItemClick
	@Override
	public final void OnItemClick (int group, int item)
	{
		final BaseFilter	filter 	= _filters.get( group );
		filter.OnItemEdit( item, _activity, this );
	}
	

	// Update
	@Override
	public final void Update (View v)
	{
		final ExpandableListView 		listView = (ExpandableListView) v.findViewById( D.id.listView1 );
		final MyExpandableListAdapter	adap = new MyExpandableListAdapter( this );
        adap.Attached( listView );
		listView.setAdapter( _adapter );
		_adapter = adap;
	}
	
	
	// OnShow
	@Override
	public final void OnShow ()
	{}
	
	
	// OnHide
	@Override
	public final void OnHide ()
	{}
	
	
	// SetActivity
	public final void SetActivity (FilterActivity act)
	{
		this._activity = act;
		this._importDialog = new ImportFilters( _activity );
		
		if ( _filters.isEmpty() ) {
			_importDialog.Load();
		}
	}
	
	
	// UpdateContacts
	public final void UpdateContacts (boolean everyDay)
	{
		final String	LAST_UPDATE = "last-update";
		
		final SharedPreferences	prefs = Preferences.ClassPreferences( PREFS_NAME );
		
		final long	lastUpdate 	= prefs.getLong( LAST_UPDATE, 0 );
		final long	currTime   	= System.currentTimeMillis() / 1000;
		final long	DAY_SEC		= 24 * 60 * 60;
		
		if ( !everyDay || lastUpdate + DAY_SEC < currTime )
		{
			final SharedPreferences.Editor ed = prefs.edit();
			
			ed.putLong( LAST_UPDATE, currTime );
			ed.commit();
			
			for (BaseFilter bf : _filters)
			{
				if ( bf instanceof NameFilter )
				{
					((NameFilter) bf).UpdateContactsInfo();
				}
			}
		}
	}


	// DeleteAll
	public final void DeleteAll ()
	{
		Backup();
		
		_filters.clear();
		_OnFilterChanged();
		
		if ( _importDialog != null ) {
			_importDialog.Load();
		}
	}
	
	
	// WriteTestSms
	public final void WriteTestSms ()
	{
		final Dialog	dlg = new Dialog( _activity );
		dlg.requestWindowFeature( Window.FEATURE_NO_TITLE );
		dlg.setContentView( D.layout.write_test_sms );
		dlg.setCancelable( true );
		dlg.setCanceledOnTouchOutside( false );
		
		final EditText	sender	= (EditText) dlg.findViewById( D.id.senderName );
		final EditText	smsText	= (EditText) dlg.findViewById( D.id.smsText );
		final TextView	result	= (TextView) dlg.findViewById( D.id.result );
		
		final TimePicker	time = (TimePicker) dlg.findViewById( D.id.timePicker1 );
		time.setIs24HourView( true );
		
		TextView	tvTitle		= (TextView) dlg.findViewById( D.id.dlgTitle );
		TextView	btnSend 	= (TextView) dlg.findViewById( D.id.btnSend );
		TextView	btnCancel	= (TextView) dlg.findViewById( D.id.btnClose );
		
		tvTitle.setText( D.string.txt_test_sms_filters );
		
		btnSend.setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick (View v)
			{
				final long	MILLIS_IN_MIN	= 60 * 1000;
				
				final MySmsMessage	sms = new MySmsMessage( sender.getText().toString(), smsText.getText().toString(), 
											( time.getCurrentHour() * 60 + time.getCurrentMinute() ) * MILLIS_IN_MIN );

				Preferences.AppPrefs prefs = Preferences.ReadPrefs();
				
				int		res	= Apply( prefs, sms );
				String	str = "";
				int		colorId = 0;

				if ( res == RES_MARK_AS_SUSPECT )
				{
					if ( prefs.suspectMode == Preferences.AppPrefs.SUSPECT_SMS_ALLOW )
						res = RES_ALLOWED_AS_SUSPECT;
					else
					if ( prefs.suspectMode == Preferences.AppPrefs.SUSPECT_SMS_BLOCK )
						res = RES_BLOCKED_AS_SUSPECT;
					else
						res = RES_SHOW_POPUP_FOR_SUSPECT;
				}

				switch ( res )
				{
					case RES_PASSED :
						colorId = D.color.test_sms_success;
						str		= _activity.getString( D.string.test_sms_success );
						break;
				
					case RES_ALLOWED_AS_SUSPECT :
						colorId = D.color.test_sms_success;
						str		= _activity.getString( D.string.test_sms_pass_as_suspect );
						break;
						
					case RES_BLOCKED_AS_SUSPECT :
						colorId = D.color.test_sms_failed;
						str		= _activity.getString( D.string.test_sms_block_as_suspect );
						break;
						
					case RES_SHOW_POPUP_FOR_SUSPECT :
						colorId = D.color.test_sms_shown_popup;
						str		= _activity.getString( D.string.test_sms_shown_suspectious_popup );
						break;
						
					case RES_BLOCKED_AS_SPAM :
						colorId = D.color.test_sms_failed;
						str		= _activity.getString( D.string.test_sms_block_as_spam );
						break;
				}
				
				result.setVisibility( View.VISIBLE );
				result.setText( str );
				result.setBackgroundResource( colorId );
			}
		});
		
		btnCancel.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				dlg.cancel();
			}
		});
		
		dlg.findViewById( D.id.addFromContact ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
	            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
	            intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
	            
	            _activity.StartActivityForResult( intent, new FilterActivity.ActivityResultCallback() {
					@Override
					public boolean OnResult (int resultCode, Intent data) {
						if ( resultCode == Activity.RESULT_OK ) {
							final String[]	str = new String[1];
							
							if ( MyUtils.GetPhoneNumber( data, str ) ) {
								sender.setText( str[0] );
							}
						}
						return true;
					}
					
					@Override
					public int GetResultId () {
						return 0x110;
					}
				});
			}
		});
		
		dlg.show();
	}
	
	
	// AskToUseDefaultFilters
	public final void AskToUseDefaultFilters ()
	{
		if ( !_filters.isEmpty() || _activity == null )
			return;
		
		final Dialogs.OkCancelDialog	dlg = new Dialogs.OkCancelDialog( _activity );
		
		dlg.GetOKButton().setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				dlg.Close();
				_UseDefaultFilters();
			}
		});
		
		dlg.GetTitleView().setText( D.string.import_filters_title );
		dlg.GetQuestionView().setText( D.string.use_def_filter_text );
		
		dlg.Show();
	}
	
	
	// CheckPriority
	public final void CheckPriority ()
	{
		new CheckPriorityDialog( _activity );
	}
	
	
// private methods //
	
	// _UseDefaultFilters
	private final void _UseDefaultFilters ()
	{
		final String iso3 = _activity.getResources().getConfiguration().locale.getISO3Country();
		
		NameFilter	nf = new NameFilter( true );
		nf.AddAllContacts();
		
		CustomFilter	cf = new CustomFilter();
		cf.AddBlockUrls();
		cf.AddBlockPhoneNumber();
		cf.AddNotPhoneSender();
		
		if ( iso3.equalsIgnoreCase("rus") )
		{
			cf.AddReplacementWithSimilarChar();
		}
		
		_filters.clear();
		_filters.add( nf );
		_filters.add( cf );
		
		_importDialog.Hide();
		
		_OnFilterChanged();
	}
	
	
	// _GetDefaultFiltersFileName
	private static final String _GetDefaultFiltersFileName () {
		return MyApplication.GetContext().getFilesDir().getAbsolutePath() + "/" + PREFS_FILE_NAME;
	}
	
	
	// _TraceAnalyzer
	protected static final int _TraceAnalyzer (int mode, FilterTrace trace)
	{
		switch ( mode )
		{
			case Preferences.AppPrefs.MODE_TYPE_DEFAULT :
				return _TraceAnalyzer_Default( trace );
				
			case Preferences.AppPrefs.MODE_TYPE_MOST_TRIGGERED :
				return _TraceAnalyzer_MostTriggered( trace );
			
			case Preferences.AppPrefs.MODE_TYPE_STRICT_TIME :
				return _TraceAnalyzer_StrictTime( trace );
				
			case Preferences.AppPrefs.MODE_TYPE_STRONG :
				return _TraceAnalyzer_Strong( trace );

			case Preferences.AppPrefs.MODE_TYPE_VERY_STRONG :
				return _TraceAnalyzer_VeryStrong( trace );
		}
		
		Logger.E( TAG, "no trace analyzer defined for mode " + mode );
		return RES_MARK_AS_SUSPECT;
	}
	
	
	// _TraceAnalyzer_Default
	private static final int _TraceAnalyzer_Default (FilterTrace trace)
	{
		if ( trace.passed > 0 )
			return RES_PASSED;
		
		if ( trace.blocked > 0 )
			return RES_BLOCKED_AS_SPAM;
		
		return RES_MARK_AS_SUSPECT;
	}
	
	
	// _TraceAnalyzer_MostTriggered
	private static final int _TraceAnalyzer_MostTriggered (FilterTrace trace)
	{
		if ( ! trace.items.isEmpty() )
		{
			if ( trace.blocked >= trace.passed )
				return RES_BLOCKED_AS_SPAM;
			
			return RES_PASSED;
		}
		return RES_MARK_AS_SUSPECT;
	}
	
	
	// _TraceAnalyzer_StrictTime
	private static final int _TraceAnalyzer_StrictTime (FilterTrace trace)
	{
		for (FilterTraceItem i : trace.items) {
			if ( i.filterType == SMS_FILTER_FOR_DATE && !i.allowed )
				return RES_BLOCKED_AS_SPAM;
		}
		
		for (FilterTraceItem i : trace.groups) {
			if ( i.filterType == SMS_FILTER_FOR_DATE && !i.allowed )
				return RES_BLOCKED_AS_SPAM;
		}
		
		return _TraceAnalyzer_MostTriggered( trace );
	}
	
	
	// _TraceAnalyzer_Strong
	private static final int _TraceAnalyzer_Strong (FilterTrace trace)
	{
		if ( trace.blocked > 0 )
			return RES_BLOCKED_AS_SPAM;

		if ( trace.passed > 0 )
			return RES_PASSED;
		
		return RES_MARK_AS_SUSPECT;
	}
	
	
	// _TraceAnalyzer_VeryStrong
	private static final int _TraceAnalyzer_VeryStrong (FilterTrace trace)
	{
		if ( trace.blocked > 0 )
			return RES_BLOCKED_AS_SPAM;
		
		if ( trace.blockedGroups > 0 )
			return RES_BLOCKED_AS_SPAM;

		if ( trace.passed > 0 )
			return RES_PASSED;
		
		return RES_MARK_AS_SUSPECT;
	}
	
	
	// _CheckSuspect
	protected static final boolean _CheckSuspect (String sender, String text, int time)
	{
		// TODO
		return false;
	}
	
	
	// _GroupMoveUp
	private final void _GroupMoveUp (int groupId)
	{
		if ( groupId == 0 ) return;
		
		final BaseFilter	bf0 = _filters.get( groupId );
		final BaseFilter	bf1 = _filters.get( groupId-1 );
		
		_filters.set( groupId, bf1 );
		_filters.set( groupId-1, bf0 );
		Refresh();
	}
	
	
	// _GroupMoveDown
	private final void _GroupMoveDown (int groupId)
	{
		if ( groupId == _filters.size()-1 )	return;

		final BaseFilter	bf0 = _filters.get( groupId );
		final BaseFilter	bf1 = _filters.get( groupId+1 );
		
		_filters.set( groupId, bf1 );
		_filters.set( groupId+1, bf0 );
		Refresh();
	}
	
	
	// _DeleteGroup
	private final void _DeleteGroup (final int groupId)
	{
		final Dialogs.OkCancelDialog	dlg = new Dialogs.OkCancelDialog( _activity );
		
		dlg.GetTitleView().setText( D.string.confirm_deleting_group );
		dlg.GetQuestionView().setText( _filters.get( groupId ).GetName() );
		dlg.GetOKButton().setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick (View v)
			{
				dlg.Close();
				
				_filters.remove( groupId );
				_OnFilterChanged();
				
				final ExpandableListView  	list  = _adapter.GetListView();
				final int 					count = _adapter.getGroupCount();
						
				for (int i = 0; i < count; ++i) {
					list.collapseGroup(i);
					list.expandGroup(i);
					list.collapseGroup(i);
				}
				
				list.invalidate();

				if ( _filters.isEmpty() && _importDialog != null ) {
					_importDialog.Load();
				}
			}
		});
		
		dlg.Show();
	}
	
	/*
	// _DisableGroup
	private final void _DisableGroup (int groupId)
	{
		final BaseFilter bf = _filters.get( groupId );
		bf.SetEnabled( ! bf.IsEnabled() );
	}
	*/
	
	// _IsFileExist
	private static final boolean _IsFileExist (String filename) {
		return (new File( filename )).exists();
	}
	
	
	// _Load
	private final boolean _Load (String filename, ArrayList<BaseFilter> filters)
	{
		Logger.I( TAG, "Load" );

		if ( !_IsFileExist( filename ) ) {
			Logger.I( TAG, "file not found: " + filename );
			return false;
		}
		
		FileInputStream		fis = null;
		BufferedInputStream	bis = null;
		ObjectInputStream	ois = null;
		
		boolean	res = false;
		
		try
		{
			fis = new FileInputStream( filename );
			bis = new BufferedInputStream( fis );
			ois = new ObjectInputStream( bis );
		
			final int	version = ois.readInt();
			
			filters.clear();
			
			switch ( version )
			{
				case VERSION : 	res = _Load_Current( ois, filters );	break;
				case 1		 :	res = _Load_v1( ois, filters );			break;
				default :		Logger.E( TAG, "file version " + version + " not supported!" );
			}
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
		finally {
			MyUtils.CloseStream( ois );
			MyUtils.CloseStream( bis );
			MyUtils.CloseStream( fis );
		}
		
		return res;
	}

	
	/*
	 * File format:
	 * 	- version
	 * 	- contacts
	 * 	- count
	 * 	- filters
	 * 	- - type id
	 * 	- - flags
	 * 	- - name
	 * 	- -	object
	 */
	
	
	// _Save
	private final static boolean _Save (String filename, ArrayList<BaseFilter> filters, int saveFlags)
	{
		Logger.I( TAG, "Save" );
		
		FileOutputStream	fos  = null;
		ObjectOutputStream	oos  = null;
		boolean				res  = false;
		
		try
		{
			fos = new FileOutputStream( filename );
			oos = new ObjectOutputStream( fos );
			
			oos.writeInt( VERSION );
			
			{
				HashMap< String, Contact >  contacts = new HashMap< String, Contact >();
				
				for (BaseFilter bf : filters)
				{
					if ( bf instanceof NameFilter ) {
						contacts.putAll( ((NameFilter) bf).GetContacts() );
					}
				}
				
				oos.writeObject( contacts );
			}
			
			oos.writeInt( filters.size() );
			
			for (BaseFilter bf : filters)
			{
				int flags = 0;
				
				flags |= bf.IsAllowed() ? FLAG_ALLOW : 0;
				flags |= bf.IsEnabled() ? FLAG_ENABLED : 0;
				
				oos.writeInt( bf.GetFilterTypeId() );
				oos.writeInt( flags );
				oos.writeObject( bf.GetName() );
				oos.writeObject( bf.Save( saveFlags ) );
			}
			res = true;
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
		finally {
			MyUtils.CloseStream( oos );
			MyUtils.CloseStream( fos );
		}
		return res;
	}
	
	
	// _Load_Current
	private final static boolean _Load_Current (ObjectInputStream ois, ArrayList<BaseFilter> filters)
													throws IOException, ClassNotFoundException
	{
		@SuppressWarnings("unchecked")
		HashMap<String,Contact> contacts = (HashMap<String,Contact>) ois.readObject();
		
		final int	count	= ois.readInt();
		
		for (int i = 0; i < count; ++i)
		{
			final int 	typeId 	= ois.readInt();
			final int	flags	= ois.readInt();

			final boolean	allow 	= (flags & FLAG_ALLOW) != 0;
			final boolean	enabled	= (flags & FLAG_ENABLED) != 0;
			
			
			BaseFilter	filter 	= null;
			
			switch ( typeId )
			{
				case SMS_FILTER_FOR_NAME :	filter = new NameFilter( allow );	break;
				case SMS_FILTER_FOR_DATE :	filter = new DateFilter( allow );	break;
				case SMS_FILTER_FOR_TEXT :	filter = new TextFilter( allow );	break;
				case SMS_FILTER_CUSTOM   :	filter = new CustomFilter();		break;
			}

			ois.readObject();
			
			final Object	items = ois.readObject();
			
			filter.Load( items, contacts );
			filter.SetEnabled( enabled );
			
			filters.add( filter );
		}
		
		return true;
	}
	
	
	// _Load_v1
	private final static boolean _Load_v1 (ObjectInputStream ois, ArrayList<BaseFilter> filters)
												throws IOException, ClassNotFoundException
	{
		HashMap<String,Contact> contacts = new HashMap<String,Contact>();
		   
		final int	count	= ois.readInt();
		
		for (int i = 0; i < count; ++i)
		{
			final int 	typeId 	= ois.readInt();
			final int	flags	= ois.readInt();

			final boolean	allow 	= (flags & FLAG_ALLOW) != 0;
			final boolean	enabled	= (flags & FLAG_ENABLED) != 0;
			
			
			BaseFilter	filter 	= null;
			
			switch ( typeId )
			{
				case SMS_FILTER_FOR_NAME :	filter = new NameFilter( allow );	break;
				case SMS_FILTER_FOR_DATE :	filter = new DateFilter( allow );	break;
				case SMS_FILTER_FOR_TEXT :	filter = new TextFilter( allow );	break;
				case SMS_FILTER_CUSTOM   :	filter = new CustomFilter();		break;
			}

			ois.readObject();
			
			final Object	items = ois.readObject();
			
			filter.Load( items, contacts );
			filter.SetEnabled( enabled );
			
			filters.add( filter );
		}
		
		return true;
	}
}
