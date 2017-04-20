/*
*/

package smsfilter.classes;

import android.view.View;


//
// Filter
//
public abstract class FiltersGroup
{
// types //
	public static abstract class Filter
	{
		public abstract String	GetName ();
		public abstract String	GetItemText (int index);
		public abstract int		GetItemIcon (int index);
		public abstract int		GetItemsCount ();
	}
	
	
// methods //
	public abstract Filter	GetFilter (int index);
	public abstract int 	GetFiltersCount ();

	public abstract void 	OnGroupAddItemClick (int group);
	public abstract void	OnGroupShowInfoClick (int group, View v);
	
	public abstract void 	OnItemDeleteClick (int group, int item);
	public abstract void	OnItemClick (int group, int item);
	
	public abstract void	SetAdapter (MyExpandableListAdapter adapter);
}
