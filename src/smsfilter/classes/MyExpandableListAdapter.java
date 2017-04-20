/*
*/

package smsfilter.classes;

import smsfilter.app.D;
import smsfilter.app.MyApplication;
import smsfilter.drawable.ListRowGroupDrawable;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;



//
// Expandable List Adapter
//
public final class MyExpandableListAdapter 	extends BaseExpandableListAdapter
{
// variables //
	private final FiltersGroup		groups;
	private LayoutInflater 			inflater;
	private ExpandableListView		listView;

	
	// constructor
	public MyExpandableListAdapter (FiltersGroup group)
	{
		this.groups 	= group;
		this.inflater	= (LayoutInflater) MyApplication.GetContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		
		groups.SetAdapter( this );
	}
	
	
	// Attached
	public final void Attached (ExpandableListView list) {
		listView = list;
	}
	
	
	// GetListView
	public final ExpandableListView GetListView () {
		return listView;
	}

	
	// getChild
	@Override
	public final Object getChild (int groupPosition, int childPosition)
	{
		return groups.GetFilter( groupPosition ).GetItemText( childPosition );
	}

	
	// getChildId
	@Override
	public final long getChildId (int groupPosition, int childPosition)
	{
		return 0;
	}

	
	// getChildView
	@Override
	public final View getChildView (final int groupPosition, final int childPosition,
									boolean isLastChild, View convertView, ViewGroup parent)
	{
		FiltersGroup.Filter	fltr = groups.GetFilter( groupPosition );
		
		convertView = inflater.inflate( D.layout.listrow_details, null );

		TextView	text = (TextView) convertView.findViewById( D.id.filterItem );
		text.setText( fltr.GetItemText( childPosition ) );
		
		View		details = convertView.findViewById( D.id.listrowDetails );
		details.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick (View v) {
				groups.OnItemClick( groupPosition, childPosition );
			}
		});
		
		ImageView	icon = (ImageView) convertView.findViewById( D.id.contactImg );
		int			imgRes = fltr.GetItemIcon( childPosition );
		
		if ( imgRes == 0 ) {
			icon.setVisibility( View.INVISIBLE );
		}
		else {
			icon.setVisibility( View.VISIBLE );
			icon.setImageResource( imgRes );
		}
		
		
		ImageView	img = (ImageView) convertView.findViewById( D.id.deleteItem );
		img.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick (View v) {
				groups.OnItemDeleteClick( groupPosition, childPosition );
			}
		});
		
		return convertView;
	}

	
	// getChildrenCount
	@Override
	public final int getChildrenCount (int groupPosition)
	{
		return groups.GetFilter( groupPosition ).GetItemsCount();
	}

	
	// getGroup
	@Override
	public final Object getGroup (int groupPosition)
	{
		return groups.GetFilter( groupPosition );
	}

	
	// getGroupCount
	@Override
	public final int getGroupCount ()
	{
		return groups.GetFiltersCount();
	}

	
	// onGroupCollapsed
	@Override
	public final void onGroupCollapsed (int groupPosition)
	{
		super.onGroupCollapsed( groupPosition );
	}

	
	// onGroupExpanded
	@Override
	public final void onGroupExpanded (int groupPosition)
	{
		super.onGroupExpanded( groupPosition );
	}

	
	// getGroupId
	@Override
	public final long getGroupId (int groupPosition)
	{
		return 0;
	}

	
	// getGroupView
	@Override
	public final View getGroupView (final int groupPosition, boolean isExpanded,
									View convertView, ViewGroup parent)
	{
		convertView = inflater.inflate( D.layout.listrow_group, null );

		FiltersGroup.Filter	group = (FiltersGroup.Filter) getGroup( groupPosition );
		
		
		// text
		TextView chText = (TextView) convertView.findViewById( D.id.checkedTextView1 );
		chText.setText( group.GetName() );
		
		
		// background
		Drawable listDr = ListRowGroupDrawable.Create( 
				((RelativeLayout.LayoutParams) chText.getLayoutParams()).leftMargin, isExpanded );
		
		View	layout = convertView.findViewById( D.id.listrowGroup );
		
		layout.setBackgroundDrawable( listDr );
		
		layout.setOnLongClickListener( new View.OnLongClickListener()
		{
			@Override
			public boolean onLongClick (View v) {
				groups.OnGroupShowInfoClick( groupPosition, v );
				return true;
			}
		});
		
		layout.setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick (View v) {
				final boolean exp = ! listView.isGroupExpanded( groupPosition );
				if ( exp )	listView.expandGroup( groupPosition );
				else		listView.collapseGroup( groupPosition );
			}
		});
		
		
		// image
		View	img = convertView.findViewById( D.id.addFilterItem );
		
		img.setOnClickListener( new OnClickListener()
		{
			@Override
			public void onClick (View v) {
				groups.OnGroupAddItemClick( groupPosition );
			}
		});
		
		return convertView;
	}

	
	// hasStableIds
	@Override
	public final boolean hasStableIds ()
	{
		return false;
	}

	
	// isChildSelectable
	@Override
	public final boolean isChildSelectable (int groupPosition, int childPosition)
	{
		return false;
	}
}
