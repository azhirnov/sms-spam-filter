package smsfilter.classes;


import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;


//
// Page Adapter
//

public final class MyPageAdapter extends FragmentPagerAdapter
{
// variables
	private List<Fragment> _fragments;

	
// methods
	public MyPageAdapter (FragmentManager fm, List<Fragment> fragments) {
		super(fm);
		_fragments = fragments;
	}

	@Override
	public final Fragment getItem (int position) {
		return _fragments.get(position);
	}
	
	@Override
	public final int getCount () {
		return _fragments.size();
	}
}
