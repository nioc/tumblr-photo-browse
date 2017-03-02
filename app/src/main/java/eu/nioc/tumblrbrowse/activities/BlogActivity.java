package eu.nioc.tumblrbrowse.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import eu.nioc.tumblrbrowse.BuildConfig;
import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.adapters.PostsListAdapter;
import eu.nioc.tumblrbrowse.models.UnitPhotoPost;
import eu.nioc.tumblrbrowse.services.FollowBlog;
import eu.nioc.tumblrbrowse.services.GetTumblrBlogPosts;
import eu.nioc.tumblrbrowse.services.LikeBlogPost;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

import static eu.nioc.tumblrbrowse.TumblrBrowse.BT_BLOG_NAME;
import static eu.nioc.tumblrbrowse.TumblrBrowse.BT_BLOG_TITLE;

/**
 * This activity display photos of the intent required blog as a list
 */
public class BlogActivity extends AppCompatActivity {
    private int blogOffset;
    private String blogName;
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

        //get parameters from intent
        Intent intent = getIntent();
        String blogTitle = intent.getStringExtra(BT_BLOG_TITLE);
        blogName = intent.getStringExtra(BT_BLOG_NAME);

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
        String currentBlog = settings.getString("currentBlog", null);

        //get authentication information
        SharedPreferences blogSettings = getSharedPreferences(currentBlog, 0);
        oauthToken = blogSettings.getString("oauthToken", null);
        oauthVerifier = blogSettings.getString("oauthVerifier", null);

        //request blog posts in an asynchronous task
        requestPosts();

        //set adapter
        GridView postsListView = (GridView) findViewById(R.id.posts);
        postsListAdapter = new PostsListAdapter(this, posts);
        postsListView.setAdapter(postsListAdapter);
        postsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                //click on a photo, open view pager in fullscreen
                findViewById(R.id.pager).setVisibility(View.VISIBLE);
                findViewById(R.id.posts).setVisibility(View.GONE);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                toolbar.setVisibility(View.GONE);

                ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
                photoPagerAdapter = new PhotoPagerAdapter(posts);
                mViewPager.setAdapter(photoPagerAdapter);
                //set the view pager on the selected photo
                mViewPager.setCurrentItem(position);
                mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

                    @Override
                    public void onPageSelected(int position) {
                        if (position == posts.size() - 1) {
                            Log.d("DRAWER", "END");
                            requestPosts();
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {}
                });
            }
        });

        //prepare the infinite scroll of photos list
        postsListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
            {
                if (!isRequesting && hasMorePosts && (firstVisibleItem + visibleItemCount >= totalItemCount)) {
                    //user has reached the end of scroll, if there are more posts to display, request its
                    requestPosts();
                }
            }
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState){
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
                        BuildConfig.TUMBLR_API_CONSUMER_KEY,
                        BuildConfig.TUMBLR_API_CONSUMER_SECRET,
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
                                        BuildConfig.TUMBLR_API_CONSUMER_KEY,
                                        BuildConfig.TUMBLR_API_CONSUMER_SECRET,
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
        if (findViewById(R.id.pager).getVisibility() == View.VISIBLE) {
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
            postsListAdapter.notifyDataSetChanged();

            //update pager if exists
            if (photoPagerAdapter != null) {
                photoPagerAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Close fullscreen pager
     */
    private void closePhoto() {
        findViewById(R.id.posts).setVisibility(View.VISIBLE);
        findViewById(R.id.pager).setVisibility(View.GONE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        toolbar.setVisibility(View.VISIBLE);
    }

    /**
     * Do an authenticated request for getting photo posts
     */
    private void requestPosts() {
        isRequesting = true;
        new GetTumblrBlogPosts(BlogActivity.this).execute(
                BuildConfig.TUMBLR_API_CONSUMER_KEY,
                BuildConfig.TUMBLR_API_CONSUMER_SECRET,
                oauthToken,
                oauthVerifier,
                blogName,
                String.valueOf(blogOffset)
        );
    }

    /**
     * Local class extending PagerAdapter for include image loading and listener
     */
    private class PhotoPagerAdapter extends PagerAdapter {

        private List<UnitPhotoPost> sDrawables;

        public PhotoPagerAdapter(List<UnitPhotoPost> listData) {
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
                    //on photo click, display modal for acting on post
                    final UnitPhotoPost photo = sDrawables.get(position);
                    new AlertDialog.Builder(view.getContext())
                            //prepare dialog
                            .setIcon(R.drawable.ic_camera)
                            .setTitle(getString(R.string.post_dialog_title, photo.getRebloggedFromName()))
                            .setMessage(DateUtils.getRelativeTimeSpanString(photo.getTimestamp()*1000, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS) + "\n" + Html.fromHtml(photo.getCaption()))
                            //"Like" post button will execute a network request
                            .setPositiveButton("Like", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    new LikeBlogPost(BlogActivity.this).execute(
                                            BuildConfig.TUMBLR_API_CONSUMER_KEY,
                                            BuildConfig.TUMBLR_API_CONSUMER_SECRET,
                                            oauthToken,
                                            oauthVerifier,
                                            photo.getId().toString(),
                                            photo.reblog_key
                                    );
                                }
                            })
                            //"Save" photo button will locally save the photo
                            .setNeutralButton("Save", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //@TODO : add download and store media
                                    Toast.makeText(getApplicationContext(), "You clicked on Save", Toast.LENGTH_SHORT).show();
                                }
                            })
                            //Open the original blog of the photo
                            .setNegativeButton("View origin blog", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(view.getContext(), BlogActivity.class);
                                    intent.putExtra(BT_BLOG_NAME, photo.getRebloggedFromName());
                                    intent.putExtra(BT_BLOG_TITLE, photo.getRebloggedFromName());
                                    startActivity(intent);
                                }
                            })
                            //show dialog
                            .show();
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

