package eu.nioc.tumblrbrowse.utils;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Local class extending ViewPager used as a workaround for the PhotoView issue
 */
public class HackyViewPager extends ViewPager {

    public HackyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            //no need to get the stack trace...
            return false;
        }
    }
}
