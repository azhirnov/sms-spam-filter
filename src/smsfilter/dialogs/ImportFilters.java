package smsfilter.dialogs;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import smsfilter.app.D;
import smsfilter.app.FilterActivity;
import smsfilter.app.OpenFiltersActivity;
import smsfilter.classes.CustomListAdapter;
import smsfilter.classes.SmsFilters;
import smsfilter.utils.Logger;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public final class ImportFilters
{
// constants //
	private static final String	TAG = "ImportFilters";
	
	private static final int	FIND_RATE	= 2000;
	
	
// types //
	private static final class SmsFilterFile
	{
		public final File		file;
		public final String		name;
		///
		public SmsFilterFile (File f)
		{
			file = f;
			
			String n = f.getName();
			name = n.substring( 0,n.lastIndexOf('.') );
		}
	}
	
	
	public interface OnChooseFileCallback
	{
		public void OnClick (File file);
	}
	
	
// variables //
	private ListAdapter				_adapter;
	private final FilterActivity	_activity;
	private View					_question;
	private boolean					_checked = false;
	

// methods //
	public ImportFilters (FilterActivity act) {
		_activity = act;
		_adapter = new ListAdapter( _activity, D.layout.import_item );
	}
	
	
	// Load
	public final void Load ()
	{
		if ( _checked ) {
			Show();
			return;
		}
		
		if ( _question == null )
		{
			final Handler	h = new Handler();
			final Runnable	r = new Runnable() {
				public void run ()
				{
					_question = _activity.GetRootView().findViewById( D.id.importQuestion );
					
					if ( _question != null )
						_StartLoading();
					else
						h.postDelayed( this, FIND_RATE );
				}
			};

			r.run();
		}
		else
			_StartLoading();
	}
	
	
	// Hide
	public final void Hide ()
	{
		if ( _question != null ) {
			_question.setVisibility( View.INVISIBLE );
		}
	}
	
	
	// Show
	public final void Show ()
	{
		if ( _question != null ) {
			_question.setVisibility( View.VISIBLE );
		}
	}
	
	
	// _StartLoading
	private final void _StartLoading ()
	{
		_checked = true;
		
		final String	path = ExportDialog.BuildExternalDirName();
		
		(new FiltersSearch()).execute( path );
	}
	
	
	// _OnLoaded
	private final void _OnLoaded (File[] files)
	{
		_adapter.Clear();
		
		int	count = 0;
		
		if ( files != null )
		{
			count = files.length;
			
			for (File f : files) {
				_adapter.AddNoChange( new SmsFilterFile( f ) );
			}
		}
		
		if ( count > 0 && _question != null )
		{
			_question.setVisibility( View.VISIBLE );
			
			((TextView) _question.findViewById( D.id.txtFoundFiles )).setText(
					String.format( _activity.getString( D.string.txt_found_filters ), count ) );
			
			_question.findViewById( D.id.btnImport ).setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					_ShowDialog();
				}
			});
		}
	}

	
	// _ShowDialog
	private final void _ShowDialog ()
	{
		final Dialog dlg = new Dialog( _activity );
		dlg.requestWindowFeature( Window.FEATURE_NO_TITLE );
		dlg.setContentView( D.layout.import_dialog );
		dlg.setCancelable( true );
		dlg.setCanceledOnTouchOutside( false );
		
		final ListView	list = (ListView) dlg.findViewById( D.id.listView1 );

		list.setAdapter( _adapter );

		list.setOnItemClickListener( new OnItemClickListener() {
			@Override
			public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
				dlg.dismiss();
				_OnClick( _adapter.GetList().get(position).file );
			}
		});
		
		dlg.show();
	}
	
	
	// _OnClick
	private final void _OnClick (File file)
	{
		Hide();
		
		SmsFilters smsFilter = _activity.GetSMSFilters();
		
		smsFilter.Load( file.getAbsolutePath() );
		smsFilter.Refresh();
		smsFilter.Save( null, SmsFilters.SAVE_FLAG_NONE );
	}


	
	
	//
	// List Adapter
	//
	private final class ListAdapter extends CustomListAdapter< SmsFilterFile >
	{
		public ListAdapter (Context ctx, int viewId) {
			super(ctx, viewId);
		}

		protected final void InitView (SmsFilterFile item, View view, int pos)
		{
			TextView	text = (TextView) view.findViewById( D.id.name );
			
			text.setText( item.name );
		}
	}
	
	
	
	//
	// Filters Search
	//
	private final class FiltersSearch extends AsyncTask< String, Void, File[] >
	{
		@Override
		protected final File[] doInBackground (String... params)
		{
			try
			{
				final File		dir = new File( params[0] );
				final File[]	fls = dir.listFiles( new FileFilter()
				{
					@Override
					public boolean accept (File pathname)
					{
						String n = pathname.getName();
						return n.substring( n.lastIndexOf('.')+1, n.length() )
								.equalsIgnoreCase( OpenFiltersActivity.EXT );
					}
				});

				if ( fls != null && fls.length > 0 )
				{
					Collections.sort( Arrays.asList(fls), new Comparator< File > () {
						@Override
						public int compare (File lhs, File rhs) {
							return (int)( (rhs.lastModified() - lhs.lastModified()) / 60000 );
						}
					});
				}
				return fls;
			}
			catch (Exception e) {
				Logger.OnCatchException( TAG, e );
			}
			return null;
		}

	    
	    @Override
	    protected final void onPostExecute (File[] result)
	    {
	        super.onPostExecute(result);
	        _OnLoaded( result );
	    }
	}
}
