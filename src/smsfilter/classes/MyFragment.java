package smsfilter.classes;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import smsfilter.app.FilterActivity;


//
// Fragment
//

public final class MyFragment extends Fragment
{
	public static final String 	EXTRA_SCREEN 	= "EXTRA_SCREEN";
	public static final String	EXTRA_TAB_ID	= "EXTRA_TAB_ID";
	
	
	public MyFragment ()
	{}
	
	
	@Override
    public final void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        setUserVisibleHint(true);
    }
	
	
	public static final MyFragment newInstance (int screenId, int tabId)
	{
		MyFragment 	f 	= new MyFragment();
		Bundle 		bdl	= new Bundle(1);

		bdl.putInt( EXTRA_SCREEN, 	screenId );
		bdl.putInt( EXTRA_TAB_ID,	tabId );

		f.setArguments( bdl );
		return f;
	}

	
	@Override
	public final View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		int 	screenId 	= getArguments().getInt( EXTRA_SCREEN );
		int		tabId		= getArguments().getInt( EXTRA_TAB_ID );
		View	v 			= inflater.inflate( screenId, container, false );
		
		FilterActivity act = FilterActivity.GetInstance();
		
		if ( act != null ) {
			act.UpdateTab( v, tabId );
		}
		return v;
	}
}
