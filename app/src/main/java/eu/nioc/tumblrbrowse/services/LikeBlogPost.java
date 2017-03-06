package eu.nioc.tumblrbrowse.services;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.tumblr.jumblr.JumblrClient;

import java.util.List;

import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.activities.BlogActivity;
import eu.nioc.tumblrbrowse.models.UnitPhotoPost;

/**
 * Asynchronous task for liking a post
 */
public class LikeBlogPost extends AsyncTask<String, String, Boolean> {
    private Activity activity;
    private List<UnitPhotoPost> photoPosts;

    public LikeBlogPost(Activity activity, List<UnitPhotoPost> photoPosts) {
        this.activity = activity;
        this.photoPosts = photoPosts;
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
        //update liked post indicator, as we processed each photo of photoset as a different post, we have to check all photoPosts
        for (int i = 0; i < photoPosts.size(); i++) {
            if (photoPosts.get(i).getId().equals(Long.valueOf(params[4]))) {
                //current photo belongs to the liked post
                photoPosts.get(i).liked = true;
            }
        }
        return true;
    }
    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            Toast.makeText(activity.getApplicationContext(), activity.getString(R.string.alert_liked), Toast.LENGTH_SHORT).show();
            ((BlogActivity) this.activity).setBlogPosts(photoPosts);
        } else {
            Toast.makeText(activity.getApplicationContext(), R.string.alert_like_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
