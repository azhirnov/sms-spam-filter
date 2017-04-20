package smsfilter.drawable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import smsfilter.app.D;
import smsfilter.app.MyApplication;



//
// List Row Group Drawable
//

public final class ListRowGroupDrawable extends Drawable
{
// constants //
	private static int		colorOrange;
	private static int		colorDarkRed;
	private static int		colorBodyIdle;
	private static int		colorBodyPrsd;
	private static int		colorBodyExp;
	private static boolean	colorsInitialized 	= false;
	
	private static float	unitWidth			= 1.0f;
	
	
// variables //
	private final Paint 	_paint	= new Paint();
    private final RectF 	_rect	= new RectF();
    private final int		_left;
    
	private final boolean	_pressed;
	private final boolean	_expanded;
	
	
// methods //
	
	// StateListDrawable
	public static final StateListDrawable Create (int marginLeft, boolean isExpanded)
	{
		Context			  ctx = MyApplication.GetContext();
		StateListDrawable st  = new StateListDrawable();
		
		st.addState( new int[] {android.R.attr.state_pressed}, new ListRowGroupDrawable( ctx, marginLeft, true, isExpanded ) );
		st.addState( new int[] { }, new ListRowGroupDrawable( ctx, marginLeft, false, isExpanded ) );
		
		return st;
	}
	
	
	// constructor
	public ListRowGroupDrawable (Context ctx, int marginLeft, boolean pressed, boolean isExpanded)
	{
		_left 	  = marginLeft;
		_pressed  = pressed;
		_expanded = isExpanded;
		
		if ( !colorsInitialized )
		{
			colorsInitialized = true;
			
			Resources	r = ctx.getResources();
			
			colorBodyIdle 	= r.getColor( D.color.filter_gr_idle );
			colorBodyPrsd	= r.getColor( D.color.filter_gr_prsd );
			colorBodyExp	= r.getColor( D.color.filter_gr_exp );
			
			colorOrange		= r.getColor( D.color.filter_gr_line );
			colorDarkRed	= r.getColor( D.color.filter_gr_line_exp );
			
			unitWidth		= (float) r.getDimensionPixelOffset( D.dimen.filter_unit_width );
		}
	}
	
	
	// draw
	@Override
	public final void draw (Canvas canvas)
	{
		//final float	step = 0.025f;
		final Rect 	r 	 = getBounds();
		
		_rect.left	 = (float) r.left;
		_rect.right	 = (float) r.right;
		_rect.top	 = (float) r.top;
		_rect.bottom = (float) r.bottom;
		
		final float	unit 	= unitWidth;	// w * step
		final float	p0		= Math.max( 4.0f, unit * (_expanded ? 1.4f : 1.0f) );
		final float	p1		= Math.max( p0, (float)_left - unit );
		
		_paint.setStyle( Style.FILL );
		
		_paint.setColor( _expanded ? colorDarkRed : colorOrange );
		_rect.right = (float) r.left + p0;
		canvas.drawRect( _rect, _paint );

		_paint.setColor( _pressed ? colorBodyPrsd : (_expanded ? colorBodyExp : colorBodyIdle) );
		_rect.left  = (float) r.left + p1;
		_rect.right = (float) r.right;
		canvas.drawRect( _rect, _paint );
	}

	
	// getOpacity
	@Override
	public final int getOpacity () {
		return PixelFormat.OPAQUE;
	}


	@Override
	public final void setAlpha (int arg0) {
	}


	@Override
	public final void setColorFilter (ColorFilter arg0) {
	}
}
