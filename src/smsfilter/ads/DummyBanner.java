package smsfilter.ads;

import android.app.Activity;


public final class DummyBanner extends IBanner
{
	public void OnDestroy () 								{}
	public void OnStart ()	 								{}
	public void OnStop () 									{}
	public void InitBanner (Activity root, int adViewId) 	{}
	public void SetBannerRefreshRate (int seconds) 			{}
	public void RefreshBanner () 							{}
	public boolean IsBannerInitialized ()					{ return false; }
	public boolean IsBannerShown ()							{ return false; }
}
