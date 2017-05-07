package eu.nioc.tumblrbrowse;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import eu.nioc.tumblrbrowse.activities.AccountActivity;

/**
 * Main application class, hosting constants and common functions
 */
public class TumblrBrowse extends Application {

    public static final String BT_BLOG_TITLE = "eu.nioc.tumblrbrowse.blogTitle";
    public static final String BT_BLOG_NAME = "eu.nioc.tumblrbrowse.blogName";
    public static String TUMBLR_API_CONSUMER_KEY;
    public static String TUMBLR_API_CONSUMER_SECRET;

    private static TumblrBrowse mInstance;
    private static Context mAppContext;


    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        TUMBLR_API_CONSUMER_KEY = BuildConfig.TUMBLR_API_CONSUMER_KEY;
        TUMBLR_API_CONSUMER_SECRET = BuildConfig.TUMBLR_API_CONSUMER_SECRET;

        this.setAppContext(getApplicationContext());
    }

    public static TumblrBrowse getInstance(){
        return mInstance;
    }
    public static Context getAppContext() {
        return mAppContext;
    }
    private void setAppContext(Context mAppContext) {
        this.mAppContext = mAppContext;
    }

    public void goToAccount() {
        Intent intent = new Intent(this, AccountActivity.class);
        startActivity(intent);
    }
}
