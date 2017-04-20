package smsfilter.ads;

import android.app.Activity;


public abstract class IBanner
{
	public abstract void OnDestroy ();
	public abstract void OnStart ();
	public abstract void OnStop ();
	public abstract void InitBanner (Activity root, int adViewId);
	public abstract void SetBannerRefreshRate (int seconds);
	public abstract void RefreshBanner ();
	public abstract boolean IsBannerInitialized ();
	public abstract boolean IsBannerShown ();
}
