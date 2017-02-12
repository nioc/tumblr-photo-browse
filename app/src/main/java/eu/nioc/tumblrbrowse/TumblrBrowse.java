package eu.nioc.tumblrbrowse;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import eu.nioc.tumblrbrowse.activities.AccountActivity;

/**
 * Created by nicolas on 04/02/17.
 */
public class TumblrBrowse extends Application {

    public static final String BT_BLOG_TITLE = "eu.nioc.tumblrbrowse.blogTitle";
    public static final String BT_BLOG_NAME = "eu.nioc.tumblrbrowse.blogName";

    private static TumblrBrowse mInstance;
    private static Context mAppContext;


    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

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
