package smsfilter.dialogs;

import smsfilter.app.D;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.TextView;


public final class Dialogs
{
	
	//
	// OK Cancel Dialog View
	//
	public static final class OkCancelDialogView
	{
		private final Dialog		_dialog;
		private final Activity		_activity;
		
		
		public OkCancelDialogView (Activity activity)
		{
			_activity = activity;
			_dialog = new Dialog( activity );
			_dialog.requestWindowFeature( Window.FEATURE_NO_TITLE );
			_dialog.setContentView( D.layout.ok_cancel_dialog_view );
			_dialog.setCancelable( true );
			_dialog.setCanceledOnTouchOutside( false );
			
			View.OnClickListener onClick = new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					Close();
				}
			};
			
			GetOKButton().setOnClickListener( onClick );
			GetCancelButton().setOnClickListener( onClick );
		}
		
		
		public final void AddView (View view, LayoutParams params) {
			ViewGroup v = (ViewGroup) _dialog.findViewById( D.id.dlgLayout );
			v.addView( view, params );
		}
		
		
		public final void AddView (int viewId, LayoutParams params) {
			LayoutInflater inflater = (LayoutInflater) _activity.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			AddView( inflater.inflate( viewId, null ), params );
		}
		
		
		public final TextView GetTitleView () {
			return (TextView) _dialog.findViewById( D.id.dlgTitle );
		}
		
		
		public final TextView GetCancelButton () {
			return (TextView) _dialog.findViewById( D.id.dlgCancel );
		}
		
		
		public final TextView GetOKButton () {
			return (TextView) _dialog.findViewById( D.id.dlgOK );
		}
		
		
		public final Dialog GetDialog () {
			return _dialog;
		}
		
		
		public final void Show () {
			_dialog.show();
		}
		
		
		public final void Close () {
			_dialog.dismiss();
		}
	}
	
	
	
	//
	// OK Cancel Dialog
	//
	public static final class OkCancelDialog
	{
		private final Dialog		_dialog;
		
		
		public OkCancelDialog (Activity activity)
		{
			_dialog = new Dialog( activity );
			_dialog.requestWindowFeature( Window.FEATURE_NO_TITLE );
			_dialog.setContentView( D.layout.ok_cancel_dialog );
			_dialog.setCancelable( true );
			_dialog.setCanceledOnTouchOutside( false );
			
			View.OnClickListener onClick = new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					Close();
				}
			};
			
			GetOKButton().setOnClickListener( onClick );
			GetCancelButton().setOnClickListener( onClick );
		}
		
		
		public final TextView GetTitleView () {
			return (TextView) _dialog.findViewById( D.id.dlgTitle );
		}
		
		
		public final TextView GetQuestionView () {
			return (TextView) _dialog.findViewById( D.id.dlgQuestion );
		}
		
		
		public final TextView GetCancelButton () {
			return (TextView) _dialog.findViewById( D.id.dlgCancel );
		}
		
		
		public final TextView GetOKButton () {
			return (TextView) _dialog.findViewById( D.id.dlgOK );
		}
		
		
		public final Dialog GetDialog () {
			return _dialog;
		}
		
		
		public final void Show () {
			_dialog.show();
		}
		
		
		public final void Close () {
			_dialog.dismiss();
		}
	}

	

	
	//
	// OK Dialog
	//
	public static final class OkDialog
	{
		private final Dialog		_dialog;
		
		
		public OkDialog (Activity activity)
		{
			_dialog = new Dialog( activity );
			_dialog.requestWindowFeature( Window.FEATURE_NO_TITLE );
			_dialog.setContentView( D.layout.ok_dialog );
			_dialog.setCancelable( true );
			_dialog.setCanceledOnTouchOutside( false );
			
			View.OnClickListener onClick = new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					Close();
				}
			};
			
			GetOKButton().setOnClickListener( onClick );
		}
		
		
		public final TextView GetTitleView () {
			return (TextView) _dialog.findViewById( D.id.dlgTitle );
		}
		
		
		public final TextView GetQuestionView () {
			return (TextView) _dialog.findViewById( D.id.dlgQuestion );
		}
		
		
		public final TextView GetOKButton () {
			return (TextView) _dialog.findViewById( D.id.dlgOK );
		}
		
		
		public final Dialog GetDialog () {
			return _dialog;
		}
		
		
		public final void Show () {
			_dialog.show();
		}
		
		
		public final void Close () {
			_dialog.dismiss();
		}
	}
}
