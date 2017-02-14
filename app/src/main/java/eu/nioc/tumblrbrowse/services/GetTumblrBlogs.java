package eu.nioc.tumblrbrowse.services;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.Blog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.activities.MainActivity;
import eu.nioc.tumblrbrowse.models.BlogElement;

/**
 * Asynchronous task for getting following blogs of a specific blog (account)
 */
public class GetTumblrBlogs extends AsyncTask<String, Object, List<BlogElement>> {

    private Activity activity;
    private Exception exception;

    public GetTumblrBlogs(MainActivity activity) {
        this.activity = activity;
    }

    protected List<BlogElement> doInBackground(String... params) {
        List<BlogElement> localBlogs = new ArrayList<>();
        try {
            //create authenticated client
            JumblrClient client = new JumblrClient(params[0], params[1]);
            client.setToken(params[2], params[3]);

            //get followed blogs (including avatar in specified size)
            int avatarSize = activity.getResources().getInteger(R.integer.blog_avatar_pixels_size);

            int offset = 0;
            List<Blog> blogs;
            do {
                Map<String, String> options = new HashMap<>();
                options.put("offset", String.valueOf(offset));
                blogs = client.userFollowing(options);
                for (Blog blog : blogs) {
                    BlogElement blogElement = new BlogElement();
                    blogElement.title = blog.getTitle();
                    blogElement.avatarUrl = blog.avatar(avatarSize);
                    blogElement.name = blog.getName();
                    blogElement.updated = blog.getUpdated();
                    localBlogs.add(blogElement);
                }
                offset += blogs.size();
                //repeat request if response include 20 blogs
            } while (blogs.size() == 20);

        } catch (Exception e) {
            exception = e;
        }
        return localBlogs;
    }

    //@TODO : add progress feedback
    /*
    protected void onProgressUpdate(Integer... progress) {
        setProgressPercent(progress[0]);
    }
    */

    protected void onPostExecute(List<BlogElement> localBlogs) {
        if (exception != null) {
            //display exception message to user
            Toast.makeText(this.activity, activity.getString(R.string.alert_request_blog_error, exception.getMessage()), Toast.LENGTH_SHORT).show();
        }
        ((MainActivity)this.activity).refreshBlogs(localBlogs);
    }
}
