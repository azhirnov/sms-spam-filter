/*
*/

package smsfilter.classes;

import java.util.List;


//
// Filter
//
public abstract class Filter
{
	public abstract String			GetName ();
	public abstract List<String>	GetItems ();
}
