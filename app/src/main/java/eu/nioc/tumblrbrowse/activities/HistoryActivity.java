package eu.nioc.tumblrbrowse.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import java.util.List;

import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.TumblrBrowse;
import eu.nioc.tumblrbrowse.adapters.HistoryAdapter;
import eu.nioc.tumblrbrowse.models.BlogHistory;

/**
 * This activity display blogs browsing history
 */
public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        //get current connected blog
        SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), 0);
        String currentBlog = settings.getString("currentBlog", null);
        if (currentBlog == null) {
            //no blog chosen, return to login activity
            ((TumblrBrowse) getApplication()).goToAccount();
        }

        //set toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_history);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(currentBlog);
        getSupportActionBar().setSubtitle(getString(R.string.history_activity_subtitle));

        //get history
        BlogHistory blogHistory = new BlogHistory(this, currentBlog);
        List history = blogHistory.getEntries();
        RecyclerView historyView = (RecyclerView) findViewById(R.id.historyView);
        HistoryAdapter adapter = new HistoryAdapter(this, history);
        historyView.setAdapter(adapter);
        historyView.setLayoutManager(new LinearLayoutManager(this));
    }
}
