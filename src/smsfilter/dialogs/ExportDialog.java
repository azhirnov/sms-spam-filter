package smsfilter.dialogs;

import java.io.File;

import smsfilter.app.D;
import smsfilter.app.FilterActivity;
import smsfilter.app.MyApplication;
import smsfilter.app.OpenFiltersActivity;
import smsfilter.classes.SmsFilters;
import smsfilter.utils.Logger;
import smsfilter.utils.MyUtils;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;


//
// Export Filters Dialog
//
public final class ExportDialog
{
// constants //
	private static final String	TAG = "ExportDialog";
	
	public static final String	BACKUP 		= "backup";
	private static final String	USER_BACKUP	= "user-backup";
	
	
// variables //
	private Dialog					_dialog = null;
	private final FilterActivity	_activity;

	
// methods //
	
	public ExportDialog (FilterActivity act) {
		_activity = act;
	}
	
	
	// BuildExternalDirName
	public static final String BuildExternalDirName ()
	{
		try {
			String	path = Environment.getExternalStorageDirectory().getAbsolutePath();
	
			path += "/";
			path += MyApplication.GetString( D.string.app_dir );
			
			return path;
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
			return "";
		}
	}
	
	
	// BuildExternalBackupFilename
	public static final String BuildExternalBackupFilename ()
	{
		final String dir = BuildExternalDirName();
		
		if ( dir.isEmpty() )
			return "";
		
		final File	fdir = new File( dir );
		
		if ( !( fdir.isDirectory() && fdir.exists() ) )
			fdir.mkdirs();
		
		StringBuilder	builder = new StringBuilder( dir );
		builder.append( "/" );
		builder.append( BACKUP );
		builder.append( "." );
		builder.append( OpenFiltersActivity.EXT );
		
		return builder.toString();
	}
	
	
	// ShowDialog
	public final void ShowDialog (final String defName)
	{
		_dialog = new Dialog( _activity );
		_dialog.requestWindowFeature( Window.FEATURE_NO_TITLE );
		_dialog.setContentView( D.layout.export_dialog );
		_dialog.setCancelable( true );
		_dialog.setCanceledOnTouchOutside( false );
		
		final EditText	etFileName  = (EditText) _dialog.findViewById( D.id.filenameEdit );
		
		if ( defName != null )
			etFileName.setText( defName );
		
		_dialog.findViewById( D.id.btnToSdCard ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				_dialog.dismiss();
				_ExportToSdCard( etFileName.getText().toString().replace( '/', '_' ) );
			}
		});
		
		_dialog.findViewById( D.id.btnSend ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick (View v) {
				_dialog.dismiss();
				_SendByEmail( etFileName.getText().toString().replace( '/', '_' ) );
			}
		});
		
		_dialog.show();
	}
	
	
	// _ExportToSdCard
	private final void _ExportToSdCard (String filename)
	{
		boolean	exported = false;
		
		try {
			if ( ! MyUtils.IsExternalStorageWritable() ) {
				Toast.makeText( _activity, D.string.dlg_export_cant_save_to_sd, Toast.LENGTH_LONG ).show();
				return;
			}
			
			if ( filename.equals( BACKUP ) )
				filename = USER_BACKUP;
			
			final StringBuilder	path = new StringBuilder( BuildExternalDirName() );
			
			final File	dir = new File( path.toString() );
			
			if ( ! dir.exists() ) {
				dir.mkdirs();
			}
			
			path.append( "/" ).append( filename ).append( "." )
				.append( OpenFiltersActivity.EXT );
			
			final String		fname		= path.toString();
			final SmsFilters	smsFilter 	= _activity.GetSMSFilters();
			smsFilter.UpdateContacts( false );
			
			final File	f = new File( fname );
			
			if ( f.isFile() && f.exists() ) {
				exported = true;
				_ReplaceOrRenameFile( smsFilter, filename );
			}
			else {
				exported = _SaveToFile( smsFilter, fname );
			}
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}

		if ( !exported ) {
			Toast.makeText( _activity, D.string.filters_not_exported, Toast.LENGTH_LONG ).show();
		}
	}
	
	
	// _SaveToFile
	private final boolean _SaveToFile (final SmsFilters smsFilter, final String fname)
	{
		if ( smsFilter.Save( fname, SmsFilters.SAVE_FLAG_NONE ) )
		{
			Toast.makeText( _activity, _activity.getString( D.string.filters_exported ) + " " +
							fname, Toast.LENGTH_LONG ).show();
			return true;
		}
		return false;
	}
	
	
	// _RenameAndSave
	private final void _RenameAndSave (final SmsFilters smsFilter, final String filename)
	{
		final int	MAX_INDICES = 100;

		final StringBuilder	path = new StringBuilder( BuildExternalDirName() );
		final int			len	 = path.length();
		
		boolean	exported = false;
		
		for (int i = 1; i < MAX_INDICES; ++i)
		{
			path.append( "/" ).append( filename )
				.append( " (" ).append( i ).append( ")" )
				.append( "." ).append( OpenFiltersActivity.EXT );
			
			final File	f = new File( path.toString() );
			
			if ( !( f.isFile() && f.exists() ) )
			{
				exported = _SaveToFile( smsFilter, path.toString() );
				break;
			}
			
			path.setLength( len );
		}

		if ( !exported ) {
			Toast.makeText( _activity, D.string.filters_not_exported, Toast.LENGTH_LONG ).show();
		}
	}
	
	
	// _ReplaceOrRenameFile
	private final void _ReplaceOrRenameFile (final SmsFilters smsFilter, final String filename)
	{
		final Dialogs.OkCancelDialog	dlg = new Dialogs.OkCancelDialog( _activity );
		
		dlg.GetDialog().setOnCancelListener( new OnCancelListener() {
			@Override
			public void onCancel (DialogInterface dialog) {
				ShowDialog( filename );
			}
		});
		
		// replace
		dlg.GetCancelButton().setOnClickListener( new OnClickListener() {
			@Override
			public void onClick (View v) {
				dlg.Close();

				final StringBuilder	path = new StringBuilder( BuildExternalDirName() );
				
				path.append( "/" ).append( filename ).append( "." )
					.append( OpenFiltersActivity.EXT );
				
				if ( !_SaveToFile( smsFilter, path.toString() ) )
					Toast.makeText( _activity, D.string.filters_not_exported, Toast.LENGTH_LONG ).show();
			}
		});
		
		// rename
		dlg.GetOKButton().setOnClickListener( new OnClickListener() {
			@Override
			public void onClick (View v) {
				dlg.Close();
				_RenameAndSave( smsFilter, filename );
			}
		});
		
		dlg.GetTitleView().setText( D.string.replace_rename_title );
		dlg.GetQuestionView().setText( String.format(
				_activity.getString( D.string.replace_rename_text ), filename ) );
		
		dlg.GetCancelButton().setText( D.string.btn_replace );
		dlg.GetOKButton().setText( D.string.btn_rename );
		
		dlg.Show();
	}
	
	
	// _SendByEmail
	private final void _SendByEmail (String filename)
	{
		boolean	exported = false;
		
		try {
			if ( ! MyUtils.IsExternalStorageWritable() ) {
				Toast.makeText( _activity, D.string.dlg_export_cant_save_to_sd, Toast.LENGTH_LONG ).show();
				return;
			}
			
			final File	cache = _activity.getExternalCacheDir();
			_ClearCache( cache );
			
			final String path = cache.getAbsolutePath() + "/" + filename + "." + OpenFiltersActivity.EXT;

		    final Intent sharingIntent = new Intent( Intent.ACTION_SEND );
		    
		    sharingIntent.setType( "text/plain" );
		    sharingIntent.putExtra( android.content.Intent.EXTRA_TEXT, _activity.getString( D.string.share_text ) );
		    sharingIntent.putExtra( android.content.Intent.EXTRA_SUBJECT, _activity.getString( D.string.share_subject ) );
		    
		    final Uri  uri = Uri.fromFile( new File( path ) );
		    sharingIntent.putExtra( Intent.EXTRA_STREAM, uri );

			final SmsFilters	smsFilter = _activity.GetSMSFilters();
			smsFilter.UpdateContacts( false );
			
			if ( smsFilter.Save( path, SmsFilters.SAVE_FLAG_NONE ) )
			{
			    _activity.startActivity( Intent.createChooser( sharingIntent,
			    		_activity.getString( D.string.share_selector ) ) );
				exported = true;
			}
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}

		if ( !exported ) {
			Toast.makeText( _activity, D.string.filters_not_exported, Toast.LENGTH_LONG ).show();
		}
	}
	
	
	// _ClearCache
	private static final void _ClearCache (File cache)
	{
		File[] files = cache.listFiles();
		
		if ( files != null )
		{
			for (File f : files)
			{
				if ( f.isFile() ) {
					f.delete();
				}
			}
		}
	}
}
