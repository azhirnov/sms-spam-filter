package smsfilter.dialogs;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import smsfilter.utils.MyUtils;


//
// Add Filter Dialog
//

public final class AddFilterDialog
{
// types //
	
	public static abstract class PopupList
	{
	// variables //
		// popup
		private int		_popupLayout 	= 0;

		// list
		private int		_itemLayout		= 0;
		private int		_textViewId		= 0;
		private int		_titleViewId 	= 0;
		
		private String	_title			= null;
		private int		_titleId		= 0;
		
		
	// methods //
		public final void Init (int popupLayout, int itemLayout, int textViewId, int titleViewId)
		{
			this._popupLayout 	= popupLayout;
			this._itemLayout 	= itemLayout;
			this._textViewId 	= textViewId;
			this._titleViewId	= titleViewId;
		}
		
		public final void SetTitle (String str) {
			this._title   = str;
			this._titleId = 0;
		}
		
		public final void SetTitleStringId (int id) {
			this._title   = null;
			this._titleId = id;
		}
		
		// get
		public final int	GetPopupLayout ()	{ return _popupLayout; }
		public final int	GetItemLayout ()	{ return _itemLayout; }
		public final int	GetTextViewId ()	{ return _textViewId; }
		public final int	GetTitleViewId ()	{ return _titleViewId; }
		
		public final String	GetTitle (Activity act)
		{
			if ( _title != null ) {
				return _title;
			}
			return act.getResources().getString( _titleId );
		}
		
		
		// interface
		public abstract void OnItemSelected (View v, int index);
	}
	
	
	
// variables //
	private Dialog 			_dialog;
	private Activity		_context;
	private PopupList		_popupList;
	

// methods //
	
	public AddFilterDialog (Activity ctx, PopupList popupList, ArrayList<String> items)
	{
		this._context = ctx;
		this._popupList = popupList;
		this._dialog = new Dialog( ctx );

		_dialog.requestWindowFeature( Window.FEATURE_NO_TITLE );
		_dialog.setContentView( _popupList.GetPopupLayout() );
		_dialog.setCancelable( true );
		_dialog.setCanceledOnTouchOutside( false );

		final StableArrayAdapter 	adapter = new StableArrayAdapter( _context, android.R.layout.simple_list_item_1, items );
		ListView 					listview = MyUtils.FindViewWithType( _GetRootView(), ListView.class );
		
		if ( listview != null ) {
			listview.setAdapter( adapter );
		}
		
		TextView	title = (TextView) _dialog.findViewById( popupList.GetTitleViewId() );
		
		title.setText( popupList.GetTitle( ctx ) );
		_dialog.show();
	}
	
	
	private final View _GetRootView () {
		return _dialog.getWindow().getDecorView().findViewById( android.R.id.content );
	}
	
	

	private final class StableArrayAdapter extends ArrayAdapter<String>
	{
		private List<String>	_items;
		

	    public StableArrayAdapter (Context context, int textViewResourceId, List<String> objects) {
	    	super( context, textViewResourceId, objects );
	    	_items = objects;
	    }
	    
	    
	    @Override
	    public final View getView (int position, View convertView, ViewGroup parent)  
	    { 
			LayoutInflater inflater = (LayoutInflater) _context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
	    	View v = inflater.inflate( _popupList.GetItemLayout(), null ); 

	    	if ( v != null )
	    	{
		    	TextView txt = (TextView) v.findViewById( _popupList.GetTextViewId() );
		    	
		    	final int	pos = position;
		    	
		    	txt.setOnClickListener( new View.OnClickListener()
		    	{
		    		@Override
		    		public void onClick (View arg0)
		    		{
		    			_popupList.OnItemSelected( arg0, pos );
		    			_dialog.dismiss();
		    			_dialog = null;
	
		    		}
		    	});
		    	txt.setText( _items.get( pos ) ); 
	    	}
	    	
	    	return v; 
	    } 
	}
	
}
