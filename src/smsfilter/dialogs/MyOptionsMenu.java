package smsfilter.dialogs;

import smsfilter.app.D;
import smsfilter.app.FilterActivity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;
import smsfilter.dialogs.Dialogs;



//
// Options Menu
//
public final class MyOptionsMenu
{
// variables //
	private final PopupWindow		_popup;
	private final FilterActivity	_activity;
	
	
// methods //
	public MyOptionsMenu (FilterActivity act, View view)
	{
		_activity = act;
		
		LayoutInflater layoutInflater = (LayoutInflater) _activity.getSystemService( Context.LAYOUT_INFLATER_SERVICE );

		final View v = layoutInflater.inflate( D.layout.options_menu, null );
		
		View	btnTestSMS 		= v.findViewById( D.id.btnTestSMS );
		View	btnPreferences 	= v.findViewById( D.id.btnPreferences );
		View	btnClearLogs 	= v.findViewById( D.id.btnClearLogs );
		View	btnClearFilters	= v.findViewById( D.id.btnClearFilters );
		View	btnInfo			= v.findViewById( D.id.btnInfo );
		View	btnExport		= v.findViewById( D.id.btnExport );
		
		btnTestSMS.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick (View v) {
				_popup.dismiss();
				_activity.WriteTestSms();
			}
		});
		btnPreferences.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick (View v) {
				_popup.dismiss();
				_activity.ShowPreferences();
			}
		});
		btnClearLogs.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick (View v) {
				_popup.dismiss();
				_activity.DeleteAllLogs();
			}
		});
		btnClearFilters.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick (View v) {
				_popup.dismiss();
				_activity.DeleteAllFilters();
			}
		});
		btnInfo.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick (View v) {
				_popup.dismiss();
				ShowInfo();
			}
		});
		btnExport.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick (View v) {
				_popup.dismiss();
				_activity.ShareFilters();
			}
		});
		
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

		final int	marginTop	= res.getDimensionPixelOffset( D.dimen.opt_menu_margin_top );
		Point		popupOffset	= new Point( 0, marginTop );
		
		_popup.showAsDropDown( view, popupOffset.x, popupOffset.y );
	}
	
	
	// ShowInfo
	private final void ShowInfo ()
	{
		final Dialogs.OkDialog	dlg = new Dialogs.OkDialog( _activity );
		dlg.GetTitleView().setText( D.string.info_title );
		dlg.GetQuestionView().setText( D.string.info_text );
		dlg.GetQuestionView().setMovementMethod( new ScrollingMovementMethod() );
		dlg.Show();
	}
	
	private final boolean IsViewContains (View view, int rx, int ry) {
	    int[] l = new int[2];
	    view.getLocationOnScreen(l);
	    int x = l[0];
	    int y = l[1];
	    int w = view.getWidth();
	    int h = view.getHeight();

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
	    int[] l = new int[2];
	    view.getLocationOnScreen(l);
	    int w = view.getWidth();
	    int h = view.getHeight();
		
	    r.left  	= l[0];
	    r.right 	= l[0] + w;
	    r.top    	= l[1];
	    r.bottom	= l[1] + h;
	}
}
