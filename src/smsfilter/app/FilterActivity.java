/*
 * TODO:
 *
 *	- statistics
 *	- - counters for blocked sms in one day, week, etc
 *	- - list with week statistic
 *	
 *	- filters:
 *	- - check translit words for russian key words
 *	- - manual set min/max length of number for number search
 *
 *	- Bugs:
 *	- - log loading???
*/

package smsfilter.app;


import java.util.ArrayList;
import java.util.List;

import smsfilter.dialogs.Dialogs;
import smsfilter.dialogs.ExportDialog;
import smsfilter.utils.Logger;
import smsfilter.ads.DummyBanner;
import smsfilter.ads.IBanner;
import smsfilter.classes.MyFragment;
import smsfilter.dialogs.MyOptionsMenu;
import smsfilter.classes.MyPageAdapter;
import smsfilter.classes.SmsFilters;
import smsfilter.classes.SmsJournal;
import smsfilter.classes.SmsJournalMngr;
import smsfilter.classes.TabViewItem;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


//
// Activity
//

public final class FilterActivity extends FragmentActivity
{
// constants //
	private static final String		TAG = "FilterActivity";
	
	public static final String		NOTIF_SMS_ID = "SMS_ID";

	public static final String		EXTRA_REFRESH_FILTERS	= "refresh-filters";
	
	public static final int			FILTERS_ITEM_INDEX	= 0;
	public static final int			LOGS_ITEM_INDEX		= 1;
	
	
// types //
	public interface ActivityResultCallback
	{
		public abstract boolean	OnResult (int resultCode, Intent data);
		public abstract int		GetResultId ();
	}
	
	
// variables //
	private static FilterActivity	_instance = null;
	
	private ActivityResultCallback	_resultCallback;
	private TabViewItem[]			_tabItems;
	
	private SmsFilters				_smsFilter;
	private SmsJournal				_smsJournal;

	private MyPageAdapter 			_pageAdapter;
	private ViewPager 				_pager;
	private int						_currentTab			= 0;
	
	private boolean					_backBtnPressed		= false;
	private boolean					_shownAtFirstStart	= false;

	private ViewGroup				_rootView = null;
	
	private View[] 					_tabLines = null;
	private View[]					_tabBtns  = null;
	
	private final IBanner			_banner = new DummyBanner();
	
	
// Activity //

	// onCreate
	@Override
	protected final void onCreate (Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	
		// tests
		if ( AppConfig.TESTS ) {
			smsfilter.test.TestSmsFilters.RunTests();
		}
		
		LayoutInflater inflater = (LayoutInflater) getSystemService( LAYOUT_INFLATER_SERVICE );
		_rootView = (ViewGroup) inflater.inflate( D.layout.main, null );
		
        setContentView( _rootView );
   
        _smsFilter = new SmsFilters();
        _smsFilter.SetActivity( this );
        _smsFilter.AutoBackup();
        _smsFilter.CheckPriority();
        
        _smsJournal = new SmsJournal( this );
		SmsJournalMngr.ClearOlder();
		

		// tabs
		List<Fragment> fragments = _GetFragments();
        
		_tabItems = new TabViewItem[ fragments.size() ];
		_tabItems[0] = _smsFilter;
		_tabItems[1] = _smsJournal;
		
		_pageAdapter = new MyPageAdapter( getSupportFragmentManager(), fragments );
		_pager = (ViewPager) findViewById( D.id.viewpager );
		_pager.setAdapter( _pageAdapter );
		
		_tabLines 	= new View[] { findViewById( D.id.line1 ), findViewById( D.id.line2 ) };
		_tabBtns	= new View[] { findViewById( D.id.tab1 ),  findViewById( D.id.tab2 ) };

		_pager.setOnPageChangeListener( new ViewPager.OnPageChangeListener()
		{
			@Override
			public void onPageSelected (int position)
			{
				if ( _currentTab == position )
					return;
				
				int lastPageId = _currentTab;
				
				_currentTab = position;
				
				_OnPageChanged( lastPageId, position );
			}
			
			@Override
			public void onPageScrolled (int arg0, float arg1, int arg2) {
			}
			
			@Override
			public void onPageScrollStateChanged (int state) {
			}
		});

		
		_ProcessIntent( getIntent() );
		
		
		// Show Notification At First Start
		final String	SHOWN_AT_FIRST_START = "SHOWN_AT_FIRST_START";
		final SharedPreferences 	prefs 	= PreferenceManager.getDefaultSharedPreferences( this );
		_shownAtFirstStart = prefs.getBoolean( SHOWN_AT_FIRST_START, false );

		if ( !_shownAtFirstStart )
		{
			SharedPreferences.Editor ed = prefs.edit();
			ed.putBoolean( SHOWN_AT_FIRST_START, true );
			ed.commit();
		}
	}
	
	
	// onDestroy
	@Override
	protected final void onDestroy ()
	{
		super.onDestroy();
		_banner.OnDestroy();
		_instance = null;
	}

	
	// onPause
	@Override
	protected final void onPause ()
	{
		super.onPause();
	}

	
	// onResume
	@Override
	protected final void onResume ()
	{
		super.onResume();
		_backBtnPressed = false;
	}
	
	
	// onStart
	@Override
	protected final void onStart ()
	{
		super.onStart();
		_banner.OnStart();
		_instance = this;
		_OnFirstStart();
	}

	
	// onStop
	@Override
	protected final void onStop ()
	{
		super.onStop();
		//_smsFilter.Save( null, SmsFilters.SAVE_FLAG_NONE );
		_banner.OnStop();
		_instance = null;
	}

	
	// onNewIntent
	@Override
	protected final void onNewIntent (Intent intent)
	{
	    super.onNewIntent(intent);
	    _ProcessIntent( intent );
	}
	
	
	// _ProcessIntent
	private final void _ProcessIntent (Intent intent)
	{
		try
		{
		    if ( intent.hasExtra( NOTIF_SMS_ID ) )
		    {
		    	_OnOpenFromNotification( intent );
		    	return;
		    }
		    if ( intent.hasExtra( EXTRA_REFRESH_FILTERS ) )
		    {
		    	_smsFilter.Reload();
		    	return;
		    }
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
	}


    // onKeyDown
    @Override
    public final boolean onKeyDown (int keyCode, KeyEvent event)
    {
        if ( keyCode == KeyEvent.KEYCODE_BACK )
        {
        	if ( !_backBtnPressed ) {
        		Toast.makeText( this, D.string.press_back_again, Toast.LENGTH_SHORT ).show();
        		_backBtnPressed = true;
        	}
        	else {
        		super.onBackPressed();
        	}
        	return true;
        }
        
        if ( keyCode == KeyEvent.KEYCODE_MENU )
        {
        	OpenOptions( findViewById( D.id.contextMenu ) );
        	return true;
        }
    	return false;
    }
    
    
    // UpdateTab
	public final void UpdateTab (View v, int tabId)
	{
		try {
			_tabItems[ tabId ].Update( v );
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
	}
	
	
	// OnSmsReceived
	public static final void OnSmsReceived ()
	{
		if ( _instance != null ) {
			_instance._smsJournal.Update( null );	
		}
	}
	
	
	// GetInstance
	public final static FilterActivity GetInstance () {
		return _instance;
	}
	
	
	// GetSMSFilters
	public final SmsFilters GetSMSFilters () {
		return _smsFilter;
	}
	
 
// Public Utils //
	
	// GetRootView
	public final View GetRootView () {
		return _rootView;
	}
	
	
	// ShowPreferences
	public final void ShowPreferences ()
	{
		Intent prefIntent = new Intent( this, Preferences.class );
		startActivity( prefIntent );
	}
	
	
	// WriteTestSms
	public final void WriteTestSms () {
		_smsFilter.WriteTestSms();
	}
	
	
	// DeleteAllFilters
	public final void DeleteAllFilters () {

		final Dialogs.OkCancelDialog	dlg = new Dialogs.OkCancelDialog( this );
		dlg.GetTitleView().setText( D.string.dlg_delete_all_filter );
		dlg.GetOKButton().setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick (View v) {
				_smsFilter.DeleteAll();
				dlg.Close();
			}
		});
		dlg.Show();
	}
	
	
	// DeleteAllLogs
	public final void DeleteAllLogs () {
		final Dialogs.OkCancelDialog	dlg = new Dialogs.OkCancelDialog( this );
		dlg.GetTitleView().setText( D.string.dlg_delete_all_logs );
		dlg.GetOKButton().setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick (View v) {
				SmsJournalMngr.DeleteAll();
				_smsJournal.Update( null );
				dlg.Close();
			}
		});
		dlg.Show();
	}

	
	// ShareFilters
	public final void ShareFilters ()
	{
		final ExportDialog	dlg = new ExportDialog( this );
		dlg.ShowDialog( null );
	}
	
	
// Private Utils //
	
	// _GetFragments
	private final List<Fragment> _GetFragments ()
	{
		List<Fragment> fList = new ArrayList<Fragment>();
		
		fList.add( MyFragment.newInstance( D.layout.screen1, FILTERS_ITEM_INDEX ) );
		fList.add( MyFragment.newInstance( D.layout.screen2, LOGS_ITEM_INDEX ) );

		return fList;
	}
	
	
	// _OnFirstStart
	private final void _OnFirstStart ()
	{
		if ( _shownAtFirstStart )
			return;

		_shownAtFirstStart = true;
		_smsFilter.AskToUseDefaultFilters();
		/*
		final Dialogs.OkDialog	dlg = new Dialogs.OkDialog( this );
		dlg.GetTitleView().setText( D.string.first_notification_title );
		dlg.GetQuestionView().setText( D.string.first_notification );
		dlg.GetQuestionView().setMovementMethod( new ScrollingMovementMethod() );
		dlg.GetOKButton().setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				dlg.Close();
				_smsFilter.AskToUseDefaultFilters();
			}
		});
		dlg.GetDialog().setOnCancelListener( new OnCancelListener() {
			@Override
			public void onCancel (DialogInterface arg0) {
			}
		});
		dlg.Show();*/
	}
	
	
	// _OnPageChanged
	private final void _OnPageChanged (int lastPageId, int nextPageId)
	{
		TabViewItem 	lastPage = null;
		TabViewItem		nextPage = null;

		try { lastPage = _tabItems[ lastPageId ]; } catch (Exception e) {}
		try { nextPage = _tabItems[ nextPageId ]; } catch (Exception e) {}
		
		if ( lastPage != null ) {
			lastPage.OnHide();
		}
		if ( nextPage != null ) {
			nextPage.OnShow();
		}

		for (int i = 0; i < _tabLines.length; ++i)
		{
			_tabLines[i].setVisibility( i == _currentTab ? View.VISIBLE : View.INVISIBLE  );
			_tabBtns[i].setEnabled( _currentTab != i );
		}
	}
	
	
	// _OnOpenFromNotification
	private final void _OnOpenFromNotification (Intent intent)
	{
		final Bundle	extras	= intent.getExtras();
		final int 		smsId 	= extras.getInt( NOTIF_SMS_ID );
		
		_pager.setCurrentItem( 1, true );
		
		GetRootView().post( new Runnable() {
			public void run () {
				_smsJournal.ViewSMS( smsId );
			}
		});
	}
	
	
// Callbacks //
	
	// OnAddSmsFilterClick
	public final void OnAddSmsFilterClick (View view) {
		_smsFilter.AddFilter();
	}
	
	
	// OnSmsFilterClick
	public final void OnSmsFilterClick (View view) {
		_pager.setCurrentItem( FILTERS_ITEM_INDEX, true );
	}
	
	
	// OnSmsLogClick
	public final void OnSmsLogClick (View view) {
		_pager.setCurrentItem( LOGS_ITEM_INDEX, true );
	}
	
	
	// OpenOptions
	public final void OpenOptions (View view) {
		new MyOptionsMenu( this, view );
	}
	
	
	// StartActivityForResult
	public final void StartActivityForResult (Intent intent, ActivityResultCallback callback)
	{
		if ( callback == null )
			return;
		
		try {
			_resultCallback = callback;
			startActivityForResult( intent, _resultCallback.GetResultId() );
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
	}

	
	// onActivityResult
	@Override
	protected final void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if ( _resultCallback != null &&
			 requestCode == _resultCallback.GetResultId() &&
			 _resultCallback.OnResult( resultCode, data ) )
		{
			_resultCallback = null;
			return;
		}
		super.onActivityResult( resultCode, resultCode, data );
	}
}
