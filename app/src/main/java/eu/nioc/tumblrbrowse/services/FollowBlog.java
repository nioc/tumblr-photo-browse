package eu.nioc.tumblrbrowse.services;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.tumblr.jumblr.JumblrClient;

import eu.nioc.tumblrbrowse.R;

/**
 * Asynchronous task for following/unfollowing a blog
 */
public class FollowBlog extends AsyncTask<String, String, Boolean> {
    private Activity activity;
    private boolean actionIsFollow;
    private String toastedText;
    private Exception exception;

    public FollowBlog(Activity activity, boolean actionIsFollow) {
        this.activity = activity;
        this.actionIsFollow = actionIsFollow;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            //create authenticated client
            JumblrClient client = new JumblrClient(params[0], params[1]);
            client.setToken(params[2], params[3]);

            if (actionIsFollow) {
                //follow blog
                client.follow(params[4]);
                toastedText = activity.getString(R.string.alert_blog_followed, params[4]);
            } else {
                //unfollow blog
                client.unfollow(params[4]);
                toastedText = activity.getString(R.string.alert_blog_unfollowed, params[4]);
            }
        } catch (Exception e) {
            //catch error (mostly network issue)
            exception = e;
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (!result) {
            Toast.makeText(activity.getApplicationContext(), activity.getString(R.string.alert_request_blog_error, exception.getMessage()), Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(activity.getApplicationContext(), toastedText, Toast.LENGTH_SHORT).show();
    }
}
