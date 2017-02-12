package eu.nioc.tumblrbrowse.services;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.tumblr.jumblr.JumblrClient;

/**
 * Asynchronous task for liking a post
 */
public class LikeBlogPost extends AsyncTask<String, String, Boolean> {
    private Activity activity;

    public LikeBlogPost(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            //create authenticated client
            JumblrClient client = new JumblrClient(params[0], params[1]);
            client.setToken(params[2], params[3]);

            //like post blog
            client.like(Long.valueOf(params[4]), params[5]);

        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            Toast.makeText(activity.getApplicationContext(), "Liked", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity.getApplicationContext(), "Like failed", Toast.LENGTH_SHORT).show();
        }
    }
}
