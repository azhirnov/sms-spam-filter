package smsfilter.classes;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.PopupWindow;


//
// Popup Window
//

public final class MyPopupWindow
{
	private PopupWindow		_popupWindow;
	
	
	public MyPopupWindow ()
	{
	}
	

	public final void Create (Activity act, int viewId, int parentViewId)
	{
		LayoutInflater layoutInflater = (LayoutInflater) act.getSystemService( Context.LAYOUT_INFLATER_SERVICE );

		View	popupView 	= layoutInflater.inflate( viewId, null );
		View	parentView 	= layoutInflater.inflate( parentViewId, null );
		
		Create( act, popupView, parentView );
	}

	
	public final void Create (Activity act, int viewId, View parentView)
	{
		LayoutInflater layoutInflater = (LayoutInflater) act.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		
		View	popupView 	= layoutInflater.inflate( viewId, null );
		
		Create( act, popupView, parentView );
	}
	
	
	public final void Create (Activity act, View view, View parentView)
	{
		_popupWindow = new PopupWindow( view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT );

		_popupWindow.showAtLocation( parentView, 0, 0, 0 );
	}
	
	
	public final void Close ()
	{
		if ( _popupWindow != null ) {
			_popupWindow.dismiss();
			_popupWindow = null;
		}
	}
	
	
	public final View FindView (int id)
	{
		return _popupWindow.getContentView().findViewById( id );
	}
}
