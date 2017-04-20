package smsfilter.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import smsfilter.utils.Logger;


//
// Custom List Adapter
//
public abstract class CustomListAdapter< ItemType > extends BaseAdapter
{
// variables
	protected ArrayList< ItemType >	items;
	protected final LayoutInflater	inflater;
	protected final int				viewTemplateId;
	
	private static final String		TAG = "CustomListAdapter";
	
	
// methods //
	public CustomListAdapter (Context ctx, int viewId) {
		this.inflater 		= (LayoutInflater) ctx.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		this.viewTemplateId = viewId;
		this.items			= new ArrayList< ItemType >();
	}
	
	
	public CustomListAdapter (Context ctx, int viewId, List<ItemType> data) {
		this.inflater 		= (LayoutInflater) ctx.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		this.viewTemplateId = viewId;
		this.items			= new ArrayList< ItemType >( data );
	}
	

	public CustomListAdapter (LayoutInflater inf, int viewId) {
		this.inflater 		= inf;
		this.viewTemplateId = viewId;
		this.items			= new ArrayList< ItemType >();
	}
	
	
	// Add
	public final void Add (ItemType newItem) {
		AddNoChange( newItem );
		Update();
	}
	
	
	// AddNoChange
	public final void AddNoChange (ItemType newItem) {
		this.items.add( newItem );
	}
	
	
	// Append
	public final void Append (ArrayList< ItemType > newItems) {
		this.items.addAll( newItems );
		Update();
	}
	
	
	// EraseFromIndex
	public final void EraseFromIndex (int index) {
		this.items.remove( index );
		Update();
	}
	
	
	// GetList
	public final List< ItemType > GetList () {
		return Collections.unmodifiableList( this.items );
	}
	
	
	// GetItemSafe
	public final ItemType GetItemSafe (int index) {
		try {
			return this.items.get( index );
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
		return null;
	}
	
	
	// Clear
	public final void Clear () {
		this.items.clear();
		Update();
	}
	
	
	// Update
	public final void Update () {
		this.notifyDataSetChanged();
	}
	

	@Override
	public final int getCount () {
		return this.items != null ? this.items.size() : 0;
	}

	
	@Override
	public final Object getItem (int position) {
		return this.items.get( position );
	}

	
	@Override
	public final long getItemId (int position) {
		return 0;
	}

	
	@Override
	public final View getView (int position, View convertView, ViewGroup parent) {
		if ( convertView == null ) {
			convertView = this.inflater.inflate( this.viewTemplateId, null );
		}
		
		InitView( this.items.get( position ), convertView, position );
		return convertView;
	}
	
	
	// InitView
	protected void InitView (ItemType item, View view, int pos)	// abstract
	{
		// do nothing
	}
}
