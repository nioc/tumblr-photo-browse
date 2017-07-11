package eu.nioc.tumblrbrowse.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import eu.nioc.tumblrbrowse.TumblrBrowse;
import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.adapters.BlogsListAdapter;
import eu.nioc.tumblrbrowse.models.BlogElement;
import eu.nioc.tumblrbrowse.services.FollowBlog;
import eu.nioc.tumblrbrowse.services.GetTumblrBlogs;

import static eu.nioc.tumblrbrowse.TumblrBrowse.TUMBLR_API_CONSUMER_KEY;
import static eu.nioc.tumblrbrowse.TumblrBrowse.TUMBLR_API_CONSUMER_SECRET;

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
                //temporarily update last refreshed timestamp (is stored as soon as blogs list is refreshed or blog posts are received)
                blogs.get(position).last_refresh = System.currentTimeMillis() / 1000;
                blogsListAdapter.notifyDataSetChanged();
                //go to blog activity
                Intent intent = new Intent(MainActivity.this, BlogActivity.class);
                intent.putExtra(TumblrBrowse.BT_BLOG_NAME, ((BlogElement) blogsListView.getItemAtPosition(position)).name);
                intent.putExtra(TumblrBrowse.BT_BLOG_TITLE, ((BlogElement) blogsListView.getItemAtPosition(position)).title);
                startActivity(intent);
            }
        });

        //request a blogs refresh on startup
        getFollowings();
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
                //account management (choose, add or remove)
                ((TumblrBrowse) getApplication()).goToAccount();
                return true;

            case R.id.btn_blogs:
                //refresh followed blogs
                getFollowings();
                return true;

            case R.id.btn_my_blog_posts:
                //see current blog posts
                Intent intent = new Intent(this, BlogActivity.class);
                intent.putExtra(TumblrBrowse.BT_BLOG_NAME, currentBlog);
                intent.putExtra(TumblrBrowse.BT_BLOG_TITLE, currentBlog);
                startActivity(intent);
                return true;

            case R.id.btn_history:
                //see current blog posts
                Intent historyIntent = new Intent(this, HistoryActivity.class);
                historyIntent.putExtra(TumblrBrowse.BT_BLOG_NAME, currentBlog);
                startActivity(historyIntent);
                return true;

            case R.id.btn_export_preferences:
                //open pop up for choosing action
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                //prepare dialog
                builder.setIcon(R.drawable.ic_backup)
                        .setTitle(R.string.backup_confirm_title)
                        .setMessage(getString(R.string.backup_confirm_message, currentBlog))
                        //"Yes" button will save preferences
                        .setPositiveButton(R.string.backup_export_button, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                exportPreferences(currentBlog);
                            }
                        })
                        //"Neutral" button will discard dialog
                        .setNeutralButton(R.string.backup_discard_button, null);
                //add "import" button if there is a previous save for this blog
                if (new File(getString(R.string.backup_filename, Environment.getExternalStorageDirectory().getPath(), getPackageName(), currentBlog)).exists()) {
                    //"No" button will restore previous preferences
                    builder.setNegativeButton(R.string.backup_import_button, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            importPreferences(currentBlog);
                        }
                    });
                }
                //show dialog
                builder.show();
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

        new GetTumblrBlogs(this, blogs).execute(
                TUMBLR_API_CONSUMER_KEY,
                TUMBLR_API_CONSUMER_SECRET,
                oauthToken,
                oauthVerifier
        );

        if (actionProgressItem != null) {
            actionProgressItem.setVisible(true);
        }
    }

    /**
     * Store and refresh blogs list with provided collection
     * @param blogs Blogs collection updated
     */
    public void refreshBlogs(List<BlogElement> blogs) {
        if (blogs != null) {
            //store blogs list if not null
            Gson gson = new Gson();
            Type listOfTestObject = new TypeToken<List<BlogElement>>() {}.getType();
            SharedPreferences settings = getSharedPreferences(currentBlog, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("blogs", gson.toJson(blogs, listOfTestObject));
            editor.apply();

            //update blogs list
            this.blogs.clear();
            this.blogs.addAll(blogs);
            blogsListAdapter.notifyDataSetChanged();
        }
        //remove progress bar
        if (actionProgressItem != null) {
            actionProgressItem.setVisible(false);
        }
    }

    /**
     * Export and save user preferences (history, ...)
     *
     * @param exportedBlog Blog to export
     * @return result of the export
     */
    public boolean exportPreferences(String exportedBlog) {
        //get all shared preferences for current account in a map
        SharedPreferences exportedBlogSharedPreferences = getSharedPreferences(exportedBlog, 0);
        Map<String, ?> exportedBlogPreferences = exportedBlogSharedPreferences.getAll();
        try {
            //try to save preferences in an external storage file
            File file = new File(getString(R.string.backup_filename, Environment.getExternalStorageDirectory().getPath(), getPackageName(), exportedBlog));//, System.currentTimeMillis() / 1000
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
                //external storage is mounted and writable
                file.createNewFile();
                ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file));
                output.writeObject(exportedBlogPreferences);
                output.close();
                //notify user
                Toast.makeText(this, getString(R.string.alert_export_preferences_success), Toast.LENGTH_SHORT).show();
                //preferences are saved in external storage
                return true;
            }
            //notify user
            Toast.makeText(this, getString(R.string.alert_export_preferences_fail_storage), Toast.LENGTH_SHORT).show();
            //can not reach external storage
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        //notify user
        Toast.makeText(this, getString(R.string.alert_export_preferences_fail_error), Toast.LENGTH_SHORT).show();
        //error during export
        return false;
    }

    /**
     * Import a previous saved user preferences
     *
     * @param importedBlog Blog to export
     * @return result of the import
     */
    @SuppressWarnings({"unchecked"})
    public boolean importPreferences(String importedBlog) {
        File file = new File(getString(R.string.backup_filename, Environment.getExternalStorageDirectory().getPath(), getPackageName(), importedBlog));
        try {
            ObjectInputStream input = new ObjectInputStream(new FileInputStream(file));
            SharedPreferences.Editor currentBlogEditor = getSharedPreferences(importedBlog, 0).edit();
            currentBlogEditor.clear();
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                //for each entry in preferences map, find its type and set the key to value
                Object value = entry.getValue();
                String key = entry.getKey();
                if (value instanceof Boolean)
                    currentBlogEditor.putBoolean(key, (Boolean) value);
                else if (value instanceof Float)
                    currentBlogEditor.putFloat(key, (Float) value);
                else if (value instanceof Integer)
                    currentBlogEditor.putInt(key, (Integer) value);
                else if (value instanceof Long)
                    currentBlogEditor.putLong(key, (Long) value);
                else if (value instanceof String)
                    currentBlogEditor.putString(key, ((String) value));
            }
            //finish by closing input and committing preferences
            currentBlogEditor.commit();
            input.close();
            //notify user
            Toast.makeText(this, getString(R.string.alert_import_preferences_success), Toast.LENGTH_SHORT).show();
            //preferences are imported
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            //notify user
            Toast.makeText(this, getString(R.string.alert_import_preferences_fail_not_found), Toast.LENGTH_SHORT).show();
            return false;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        //notify user
        Toast.makeText(this, getString(R.string.alert_import_preferences_fail_error), Toast.LENGTH_SHORT).show();
        //error during import
        return false;
    }
}
