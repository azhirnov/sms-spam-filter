package smsfilter.dialogs;

import smsfilter.app.D;
import smsfilter.utils.TextTransform;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;


//
// SMS Viewer
//
public final class SmsViewer
{
	
// variables //
	private Dialog			_dialog = null;
	private final Activity	_activity;
	
	
// methods //
	
	public SmsViewer (Activity act)
	{
		_activity = act;
	}
	
	
	// CreateView
	private final View CreateView (String sender, String message, long date,
								  View.OnClickListener onOK, View.OnClickListener onCancel,
								  int stringOK, int stringCancel, int stringTitle)
	{
		LayoutInflater inflater = (LayoutInflater) _activity.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		
		View v = inflater.inflate( D.layout.view_sms, null );

		TextView title		= (TextView) v.findViewById( D.id.dlgTitle );
		TextView btnCancel 	= (TextView) v.findViewById( D.id.btnCancel );
		TextView btnOK		= (TextView) v.findViewById( D.id.btnOK );
		
		btnCancel.setOnClickListener( onCancel );
		btnCancel.setText( stringCancel );
		
		btnOK.setOnClickListener( onOK );
		btnOK.setText( stringOK );
		
		TextView	txtSender = (TextView) v.findViewById( D.id.txtSender );
		TextView	txtDate	  = (TextView) v.findViewById( D.id.txtDate );
		TextView	txtSms	  = (TextView) v.findViewById( D.id.txtSms );
		
		txtSender.setText( sender );
		txtDate.setHorizontallyScrolling( true );

		txtDate.setText( TextTransform.FormatDate2Lines( date ) );
		
		txtSms.setText( message );
		txtSms.setMovementMethod( new ScrollingMovementMethod() );
		
		title.setText( stringTitle );
		
		return v;
	}
	
	
	// ShowDialog
	public final void ShowDialog (String sender, String message, long date,
								  View.OnClickListener onOK, View.OnClickListener onCancel,
								  OnCancelListener onDialogCancel,
								  int stringOK, int stringCancel, int stringTitle)
	{
		View	v = CreateView( sender, message, date, onOK, onCancel, stringOK, stringCancel, stringTitle );
		
		_dialog = new Dialog( _activity );
		_dialog.requestWindowFeature( Window.FEATURE_NO_TITLE );
		_dialog.setContentView( v );
		_dialog.setCancelable( true );
		_dialog.setCanceledOnTouchOutside( false );
		
		if ( onDialogCancel != null ) {
			_dialog.setOnCancelListener( onDialogCancel );
		}
		
		_dialog.show();
	}
	
	
	// CloseDialog
	public final void CloseDialog ()
	{
		if ( _dialog != null ) {
			_dialog.dismiss();
			_dialog = null;
		}
	}
}
