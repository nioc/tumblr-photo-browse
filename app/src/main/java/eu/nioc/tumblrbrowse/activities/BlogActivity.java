package eu.nioc.tumblrbrowse.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fivehundredpx.greedolayout.GreedoLayoutManager;
import com.fivehundredpx.greedolayout.GreedoSpacingItemDecoration;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.adapters.ExifAdapter;
import eu.nioc.tumblrbrowse.adapters.PostsListAdapter;
import eu.nioc.tumblrbrowse.models.BlogElement;
import eu.nioc.tumblrbrowse.models.UnitPhotoPost;
import eu.nioc.tumblrbrowse.services.FollowBlog;
import eu.nioc.tumblrbrowse.services.GetExif;
import eu.nioc.tumblrbrowse.services.GetTumblrBlogPosts;
import eu.nioc.tumblrbrowse.services.LikeBlogPost;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

import static eu.nioc.tumblrbrowse.TumblrBrowse.BT_BLOG_NAME;
import static eu.nioc.tumblrbrowse.TumblrBrowse.BT_BLOG_TITLE;
import static eu.nioc.tumblrbrowse.TumblrBrowse.TUMBLR_API_CONSUMER_KEY;
import static eu.nioc.tumblrbrowse.TumblrBrowse.TUMBLR_API_CONSUMER_SECRET;

/**
 * This activity display photos of the intent required blog as a list
 */
public class BlogActivity extends AppCompatActivity {
    private int blogOffset;
    private String blogName;
    private String currentBlog;
    private List<UnitPhotoPost> posts;
    private PostsListAdapter postsListAdapter;
    private PhotoPagerAdapter photoPagerAdapter;
    private boolean isRequesting, hasMorePosts;
    private String oauthToken, oauthVerifier;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blog);

        //get blog name parameter from application intent
        Intent intent = getIntent();
        blogName = intent.getStringExtra(BT_BLOG_NAME);

        if (blogName == null) {
            //activity was called without blog name, try to get blog name parameter from an URL intent (assuming blog name is the host subdomain)
            Uri uri = intent.getData();
            blogName = uri.getHost().replace(".tumblr.com", "");
        }

        if (blogName == null) {
            //no blog chosen, return to main activity
            Intent intentReturn = new Intent(this, MainActivity.class);
            startActivity(intentReturn);
            finish();
        }

        //initialize parameters
        blogOffset = 0;
        posts = new ArrayList<>();

        //set toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_account);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(blogName);

        //get connected blog
        SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), 0);
        currentBlog = settings.getString("currentBlog", null);

        //get authentication information
        SharedPreferences blogSettings = getSharedPreferences(currentBlog, 0);
        oauthToken = blogSettings.getString("oauthToken", null);
        oauthVerifier = blogSettings.getString("oauthVerifier", null);

        //request blog posts in an asynchronous task
        requestPosts();

        //set adapter
        postsListAdapter = new PostsListAdapter(this, posts);
        //set layout manager with wished row height
        final GreedoLayoutManager layoutManager = new GreedoLayoutManager(postsListAdapter);
        layoutManager.setMaxRowHeight(getResources().getInteger(R.integer.photo_max_row_height));
        //set view
        RecyclerView postsListView = (RecyclerView) findViewById(R.id.posts);
        postsListView.setLayoutManager(layoutManager);
        postsListView.setAdapter(postsListAdapter);

        //add spacing between images
        postsListView.addItemDecoration(new GreedoSpacingItemDecoration(getResources().getInteger(R.integer.photo_spacing)));

        //prepare the infinite scroll of photos list
        postsListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (!isRequesting && hasMorePosts) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();
                    if (pastVisibleItems + visibleItemCount >= totalItemCount) {
                        //user has reached the end of scroll, if there are more posts to display, request its
                        requestPosts();
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.blog_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //@TODO : add actions
            case R.id.btn_bookmark:
                return true;

            case R.id.btn_follow:
                new FollowBlog(BlogActivity.this, true).execute(
                        TUMBLR_API_CONSUMER_KEY,
                        TUMBLR_API_CONSUMER_SECRET,
                        oauthToken,
                        oauthVerifier,
                        this.blogName
                );
                return true;

            case R.id.btn_unfollow:
                new AlertDialog.Builder(this)
                        //prepare dialog
                        .setIcon(R.drawable.ic_warning)
                        .setTitle(R.string.unfollow_blog_title)
                        .setMessage(getString(R.string.unfollow_blog_confirm, blogName))
                        //"Yes" button will execute a network request
                        .setPositiveButton(R.string.unfollow_blog_confirm_yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                new FollowBlog(BlogActivity.this, false).execute(
                                        TUMBLR_API_CONSUMER_KEY,
                                        TUMBLR_API_CONSUMER_SECRET,
                                        oauthToken,
                                        oauthVerifier,
                                        blogName
                                );
                            }
                        })
                        //Do nothing
                        .setNegativeButton(R.string.unfollow_blog_confirm_no, null)
                        //show dialog
                        .show();
                return true;

            case R.id.btn_blog_info:
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.pagerLayout).getVisibility() == View.VISIBLE) {
            //handle the back action on fullscreen photo
            closePhoto();
            return;
        }
        super.onBackPressed();
    }

    /**
     * Callback method call after requesting the photos to display its in grid
     * @param retrievedPosts photos collection retrieved from the request
     * @param hasMorePosts indicate if there is further posts to retrieve on a next request
     */
    public void populatePosts(List<UnitPhotoPost> retrievedPosts, boolean hasMorePosts) {
        //update offset and status
        this.hasMorePosts = hasMorePosts;
        if (retrievedPosts != null) {
            blogOffset += retrievedPosts.size();
            isRequesting = false;

            //update grid
            posts.addAll(retrievedPosts);
            setBlogPosts(posts);

            //store refresh timestamp for followed blogs
            //get followed blogs
            SharedPreferences blogSettings = getSharedPreferences(currentBlog, 0);
            String strBlogs = blogSettings.getString("blogs", "[]");
            Gson gson = new Gson();
            List<BlogElement> blogs = gson.fromJson(strBlogs, new TypeToken<List<BlogElement>>(){}.getType());
            //check if current refreshed blog is followed
            BlogElement blogElement = new BlogElement();
            blogElement.name = blogName;
            int index = blogs.indexOf(blogElement);
            if (index != -1) {
                //store last refreshed timestamp
                blogs.get(index).last_refresh = System.currentTimeMillis() / 1000;
                Type listOfTestObject = new TypeToken<List<BlogElement>>(){}.getType();
                SharedPreferences.Editor editor = blogSettings.edit();
                editor.putString("blogs", gson.toJson(blogs, listOfTestObject));
                editor.apply();
            }
        }
    }

    /**
     * Callback method call after requesting EXIF
     *
     * @param photoPost photo post updated with EXIF
     */
    public void displayPhotoPostExif(UnitPhotoPost photoPost) {
        if (photoPost.exif != null && !photoPost.exif.isEmpty()) {
            //display EXIF in specific layout
            LinearLayout exifLayoutView = (LinearLayout) findViewById(R.id.exifLayout);
            exifLayoutView.setVisibility(View.VISIBLE);
            RecyclerView exifView = (RecyclerView) findViewById(R.id.exif);
            ExifAdapter adapter = new ExifAdapter(photoPost.exif);
            exifView.setAdapter(adapter);
            exifView.setLayoutManager(new LinearLayoutManager(this));
            //handle click for closing
            exifLayoutView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            v.setVisibility(View.INVISIBLE);
                        }
                    }
            );
        } else {
            findViewById(R.id.btn_photo_exif).setVisibility(View.GONE);
        }
    }

    /**
     * Close fullscreen pager
     */
    private void closePhoto() {
        findViewById(R.id.posts).setVisibility(View.VISIBLE);
        findViewById(R.id.pagerLayout).setVisibility(View.GONE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        toolbar.setVisibility(View.VISIBLE);
    }

    /**
     * Do an authenticated request for getting photo posts
     */
    private void requestPosts() {
        isRequesting = true;
        new GetTumblrBlogPosts(BlogActivity.this, currentBlog).execute(
                TUMBLR_API_CONSUMER_KEY,
                TUMBLR_API_CONSUMER_SECRET,
                oauthToken,
                oauthVerifier,
                blogName,
                String.valueOf(blogOffset)
        );
    }


    public void openPager (int position) {
        //click on a photo, open view pager in fullscreen
        findViewById(R.id.pagerLayout).setVisibility(View.VISIBLE);
        findViewById(R.id.posts).setVisibility(View.GONE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        toolbar.setVisibility(View.GONE);

        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        photoPagerAdapter = new PhotoPagerAdapter(posts);
        mViewPager.setAdapter(photoPagerAdapter);
        //set the view pager on the selected photo
        mViewPager.setCurrentItem(position);
        //update photo data and listeners
        updateFullscreenPhotoData(posts.get(position));

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                //update photo data and listeners
                updateFullscreenPhotoData(posts.get(position));

                //check if new request is required
                if (position == posts.size() - 1) {
                    requestPosts();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //nothing to do
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //nothing to do
            }
        });
    }

    /**
     * Update information displayed on fullscreen pager
     * @param selectedPhoto the photo displayed on fullscreen pager
     */
    private void updateFullscreenPhotoData(final UnitPhotoPost selectedPhoto) {
        //hide EXIF and caption layouts
        findViewById(R.id.exifLayout).setVisibility(View.INVISIBLE);
        findViewById(R.id.captionLayout).setVisibility(View.INVISIBLE);
        //display EXIF and caption hidden buttons
        findViewById(R.id.btn_photo_exif).setVisibility(View.VISIBLE);
        findViewById(R.id.btn_photo_caption).setVisibility(View.VISIBLE);
        String origin = selectedPhoto.getRebloggedFromName();
        if (origin == null) {
            //photo is from the current blog, get its name
            origin = selectedPhoto.getBlogName();
        }
        ((TextView) findViewById(R.id.photo_origin)).setText(getString(R.string.post_dialog_title, origin));
        ((TextView) findViewById(R.id.photo_timestamp)).setText(DateUtils.getRelativeTimeSpanString(selectedPhoto.getTimestamp()*1000, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        ImageView likeView = (ImageView) findViewById(R.id.btn_photo_like);
        if (selectedPhoto.liked) {
            //photo is already liked
            likeView.setImageResource(R.drawable.ic_heart);
            likeView.setOnClickListener(null);
        } else {
            //photo is not liked, add onClickListener for posting your love
            likeView.setImageResource(R.drawable.ic_heart_outline);
            likeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //"Like" button will execute a network request
                    new LikeBlogPost(BlogActivity.this, posts).execute(
                            TUMBLR_API_CONSUMER_KEY,
                            TUMBLR_API_CONSUMER_SECRET,
                            oauthToken,
                            oauthVerifier,
                            selectedPhoto.getId().toString(),
                            selectedPhoto.reblog_key
                    );
                }
            });
        }
        //handle "Return" button
        findViewById(R.id.btn_photo_return).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closePhoto();
            }
        });
        //handle "Original" button
        findViewById(R.id.btn_photo_original).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Open the original blog of the photo in new activity
                Intent intent = new Intent(view.getContext(), BlogActivity.class);
                intent.putExtra(BT_BLOG_NAME, selectedPhoto.getRebloggedFromName());
                intent.putExtra(BT_BLOG_TITLE, selectedPhoto.getRebloggedFromName());
                startActivity(intent);
            }
        });
        //handle "Caption" button
        findViewById(R.id.btn_photo_caption).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String caption = selectedPhoto.getCaption();
                if (caption != null && !caption.isEmpty()) {
                    //display post caption in specific layout
                    LinearLayout captionLayoutView = (LinearLayout) findViewById(R.id.captionLayout);
                    captionLayoutView.setVisibility(View.VISIBLE);
                    TextView captionView = (TextView) findViewById(R.id.caption);
                    //set text with HTML format (require for handle links)
                    captionView.setText(Html.fromHtml(caption));
                    captionView.setMovementMethod(LinkMovementMethod.getInstance());
                    //handle click for closing
                    captionLayoutView.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    v.setVisibility(View.INVISIBLE);
                                }
                            }
                    );
                } else {
                    findViewById(R.id.btn_photo_caption).setVisibility(View.GONE);
                }
            }
        });
        //Handle "EXIF" button
        findViewById(R.id.btn_photo_exif).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new GetExif(BlogActivity.this).execute(selectedPhoto);
            }
        });
    }

    /**
     * Refresh all posts with provided list
     * @param posts list containing all photo posts
     */
    public void setBlogPosts(List<UnitPhotoPost> posts) {
        this.posts = posts;
        postsListAdapter.notifyDataSetChanged();

        //update pager if exists
        if (photoPagerAdapter != null) {
            photoPagerAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Local class extending PagerAdapter for include image loading and listener
     */
    private class PhotoPagerAdapter extends PagerAdapter {

        private List<UnitPhotoPost> sDrawables;

        PhotoPagerAdapter(List<UnitPhotoPost> listData) {
            sDrawables = listData;
        }

        @Override
        public int getCount() {
            return sDrawables.size();
        }

        @Override
        public View instantiateItem(ViewGroup container, final int position) {
            PhotoView photoView = new PhotoView(container.getContext());
            //load full size image with Picasso lib into the photoView
            Picasso.with(container.getContext())
                    .load(sDrawables.get(position).getPhoto().getOriginalSize().getUrl())
                    .placeholder(R.drawable.ic_sync)
                    .error(R.drawable.ic_warning)
                    .into(photoView);
            container.addView(photoView, ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.MATCH_PARENT);

            photoView.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                @Override
                public void onViewTap(final View view, float x, float y) {
                    //on photo click, display or hide data section
                    View photoDataView = findViewById(R.id.photo_data);
                    if (photoDataView.getVisibility() == View.VISIBLE) {
                        photoDataView.setVisibility(View.GONE);
                    } else {
                        photoDataView.setVisibility(View.VISIBLE);
                    }
                }
            });

            return photoView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}
