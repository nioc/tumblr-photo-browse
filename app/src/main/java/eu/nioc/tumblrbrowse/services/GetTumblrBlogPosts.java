package eu.nioc.tumblrbrowse.services;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.Blog;
import com.tumblr.jumblr.types.Photo;
import com.tumblr.jumblr.types.PhotoPost;
import com.tumblr.jumblr.types.Post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.activities.BlogActivity;
import eu.nioc.tumblrbrowse.models.BlogHistory;
import eu.nioc.tumblrbrowse.models.UnitPhotoPost;

/**
 * Asynchronous task for getting posts of a specific blog
 */
public class GetTumblrBlogPosts extends AsyncTask<String, String, List<UnitPhotoPost>> {

    private Activity activity;
    private Exception exception;
    private Boolean hasMorePosts;
    private String account;

    public GetTumblrBlogPosts(Activity activity, String account) {
        this.activity = activity;
        hasMorePosts = false;
        this.account = account;
    }

    @Override
    protected void onPreExecute() {
    }

    protected List<UnitPhotoPost> doInBackground(String... params) {
        List<UnitPhotoPost> photoPosts = new ArrayList<>();
        try {
            //create authenticated client
            JumblrClient client = new JumblrClient(params[0], params[1]);
            client.setToken(params[2], params[3]);

            //get blog's photo posts with reblog information from provided offset
            Blog blog = client.blogInfo(params[4]);
            Map<String, String> options = new HashMap<String, String>();
            options.put("type", "photo");
            options.put("offset", params[5]);
            options.put("reblog_info", "true");
            List<Post> posts = blog.posts(options);

            //determine if there is further posts to request (request should returns less than 20 posts if end is reached)
            hasMorePosts = (posts.size() == 20);

            for (Post post : posts) {
                if (post.getType().equals("photo")) {
                    //cast to PhotoPost for access to specific attributes
                    PhotoPost photoPost = (PhotoPost) post;
                    //handle each photos of a single post (required for displaying all photo of a set)
                    for (Photo photo : photoPost.getPhotos()) {
                        UnitPhotoPost unitPhotoPost = new UnitPhotoPost();
                        unitPhotoPost.setId(photoPost.getId());
                        unitPhotoPost.note_count = photoPost.getNoteCount();
                        unitPhotoPost.setBlogName(photoPost.getBlogName());
                        unitPhotoPost.reblog_key = photoPost.getReblogKey();
                        unitPhotoPost.liked = photoPost.isLiked();
                        unitPhotoPost.setCaption(photoPost.getCaption());
                        unitPhotoPost.timestamp = photoPost.getTimestamp();
                        unitPhotoPost.reblogged_from_name = photoPost.getRebloggedFromName();
                        unitPhotoPost.photo = photo;
                        //add unit photo to the list
                        photoPosts.add(unitPhotoPost);
                    }
                }
            }
        } catch (Exception e) {
            exception = e;
        }

        //Log access
        BlogHistory BlogHistory = new BlogHistory(this.activity, account);
        BlogHistory.addEntry(params[4], 1 + Integer.parseInt(params[5]) / 20);
        return photoPosts;
    }

    //@TODO : add progress feedback
    /*
    protected void onProgressUpdate(Integer... progress) {
        setProgressPercent(progress[0]);
    }
    */

    protected void onPostExecute(List<UnitPhotoPost> photoPosts) {
        if (exception != null) {
            //display exception message to user
            Toast.makeText(this.activity, activity.getString(R.string.alert_request_blog_error, exception.getMessage()), Toast.LENGTH_SHORT).show();
        }
        //callback to activity with photos list
        ((BlogActivity)this.activity).populatePosts(photoPosts, hasMorePosts);
    }
}
