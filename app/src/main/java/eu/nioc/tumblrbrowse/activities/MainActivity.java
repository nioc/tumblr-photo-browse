package eu.nioc.tumblrbrowse.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import eu.nioc.tumblrbrowse.TumblrBrowse;
import eu.nioc.tumblrbrowse.BuildConfig;
import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.adapters.BlogsListAdapter;
import eu.nioc.tumblrbrowse.models.BlogElement;
import eu.nioc.tumblrbrowse.services.GetTumblrBlogs;

/**
 * This activity display user's following blogs as a list, providing a way of browsing within them
 */
public class MainActivity extends AppCompatActivity {

    private String currentBlog;
    private List<BlogElement> blogs;
    private BlogsListAdapter blogsListAdapter;
    private MenuItem actionProgressItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get current connected blog
        SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), 0);
        currentBlog = settings.getString("currentBlog", null);
        if (currentBlog == null) {
            //no blog chosen, return to login activity
            ((TumblrBrowse)getApplication()).goToAccount();
        }

        //set toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_account_multiple);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(currentBlog);
        getSupportActionBar().setSubtitle(getString(R.string.main_activity_subtitle));

        //get previous blogs list of this account
        SharedPreferences blogSettings = getSharedPreferences(currentBlog, 0);
        String strBlogs = blogSettings.getString("blogs", "[]");
        Gson gson = new Gson();
        blogs = gson.fromJson(strBlogs, new TypeToken<List<BlogElement>>(){}.getType());

        //set blogs list
        final GridView blogsListView = (GridView) findViewById(R.id.blogs);
        blogsListAdapter = new BlogsListAdapter(this, blogs);
        blogsListView.setAdapter(blogsListAdapter);
        blogsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                //on click, browse this specific blog
                Intent intent = new Intent(MainActivity.this, BlogActivity.class);
                intent.putExtra(TumblrBrowse.BT_BLOG_NAME, ((BlogElement) blogsListView.getItemAtPosition(position)).name);
                intent.putExtra(TumblrBrowse.BT_BLOG_TITLE, ((BlogElement) blogsListView.getItemAtPosition(position)).title);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar, menu);
        actionProgressItem = menu.findItem(R.id.miActionProgress);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_accounts:
                ((TumblrBrowse)getApplication()).goToAccount();
                return true;

            case R.id.btn_blogs:
                getFollowings();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Ask for a following blogs with an asynchronous task
     */
    private void getFollowings() {
        SharedPreferences settings = getSharedPreferences(currentBlog, 0);
        String oauthToken = settings.getString("oauthToken", null);
        String oauthVerifier = settings.getString("oauthVerifier", null);

        new GetTumblrBlogs(this).execute(
                BuildConfig.TUMBLR_API_CONSUMER_KEY,
                BuildConfig.TUMBLR_API_CONSUMER_SECRET,
                oauthToken,
                oauthVerifier
        );

        actionProgressItem.setVisible(true);
    }

    /**
     * Store and refresh blogs list with provided collection
     * @param blogs Blogs collection updated
     */
    public void refreshBlogs(List<BlogElement> blogs) {
        //store blogs list
        Gson gson = new Gson();
        Type listOfTestObject = new TypeToken<List<BlogElement>>(){}.getType();
        SharedPreferences settings = getSharedPreferences(currentBlog, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("blogs", gson.toJson(blogs, listOfTestObject));
        editor.apply();

        //update blogs list
        this.blogs.clear();
        this.blogs.addAll(blogs);
        blogsListAdapter.notifyDataSetChanged();
        actionProgressItem.setVisible(false);
    }
}
