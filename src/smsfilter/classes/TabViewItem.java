package smsfilter.classes;

import android.view.View;


//
// Tab View Items interface
//
public interface TabViewItem
{
	public abstract void Update (View v);
	
	public abstract void OnShow ();
	public abstract void OnHide ();
}
