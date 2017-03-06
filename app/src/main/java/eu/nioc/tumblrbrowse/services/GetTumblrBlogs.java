package eu.nioc.tumblrbrowse.services;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.Blog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private List<BlogElement> previousBlogs;

    public GetTumblrBlogs(MainActivity activity, List<BlogElement> blogs) {
        this.activity = activity;
        this.previousBlogs = blogs;
    }

    protected List<BlogElement> doInBackground(String... params) {
        List<BlogElement> localBlogs = new ArrayList<>();
        try {
            //create authenticated client
            JumblrClient client = new JumblrClient(params[0], params[1]);
            client.setToken(params[2], params[3]);

            //get followed blogs (including avatar in specified size)
            final int AVATAR_SIZE = activity.getResources().getInteger(R.integer.blog_avatar_pixels_size);
            final int LIMIT = 20;

            int offset = 0;
            List<Blog> blogs;

            do {
                Map<String, String> options = new HashMap<>();
                options.put("offset", String.valueOf(offset));
                options.put("limit", String.valueOf(LIMIT));
                blogs = client.userFollowing(options);
                for (Blog blog : blogs) {
                    BlogElement blogElement = new BlogElement();
                    blogElement.title = blog.getTitle();
                    blogElement.name = blog.getName();
                    blogElement.updated = blog.getUpdated();
                    int index = this.previousBlogs.indexOf(blogElement);
                    //if existing, get last refreshed timestamp
                    if (index != -1 && this.previousBlogs.get(index).last_refresh != null) {
                        blogElement.last_refresh = this.previousBlogs.get(index).last_refresh;
                    }
                    //request avatar only for new blogs
                    if (index == -1 || this.previousBlogs.get(index).avatarUrl == null) {
                        try {
                            blogElement.avatarUrl = blog.avatar(AVATAR_SIZE);
                        } catch (Exception e) {
                            blogElement.avatarUrl = null;
                        }
                    } else {
                        blogElement.avatarUrl = this.previousBlogs.get(index).avatarUrl;
                    }
                    //add blog only if it is not already in list (Tumblr issue when a followed blog is deactivated)
                    if (!localBlogs.contains(blogElement)) {
                        localBlogs.add(blogElement);
                    }
                }
                offset += blogs.size();
                //repeat request if response include 20 blogs
            } while (blogs.size() == LIMIT);

            //order by updated timestamp, newest first
            Collections.sort(localBlogs, new Comparator<BlogElement>() {
                @Override
                public int compare(BlogElement o1, BlogElement o2) {
                    return (int) (o2.updated - o1.updated);
                }
            });

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
