package eu.nioc.tumblrbrowse.models;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import eu.nioc.tumblrbrowse.R;

/**
 * This object store blog consultations history
 */
public class BlogHistory {
    private Context context;
    private String account;

    public BlogHistory(Context context, String account) {
        this.context = context;
        this.account = account;
    }

    public void addEntry(String blog, int page) {
        //read logs
        SharedPreferences accountSettings = context.getSharedPreferences(account, 0);
        Gson gson = new Gson();
        List<BlogHistoryEntry> history = gson.fromJson(accountSettings.getString("history", "[]"), new TypeToken<List<BlogHistoryEntry>>() {}.getType());
        //add entry to history (first value)
        history.add(0, new BlogHistoryEntry(blog, page));
        //limit to N entries
        final int HISTORY_DEEP = context.getResources().getInteger(R.integer.history_deep);
        if (history.size() > HISTORY_DEEP) {
            history = history.subList(0, HISTORY_DEEP);
        }
        //save logs
        SharedPreferences.Editor editor = accountSettings.edit();
        editor.putString("history", gson.toJson(history, new TypeToken<List<BlogHistoryEntry>>() {}.getType()));
        editor.apply();
    }

    public List getEntries() {
        //read logs
        SharedPreferences accountSettings = context.getSharedPreferences(account, 0);
        Gson gson = new Gson();
        return gson.fromJson(accountSettings.getString("history", "[]"), new TypeToken<List<BlogHistoryEntry>>() {}.getType());
    }


    public class BlogHistoryEntry {
        public long timestamp;
        public String blog;
        public int page;

        BlogHistoryEntry(String blog, int page) {
            this.blog = blog;
            this.page = page;
            this.timestamp = System.currentTimeMillis() / 1000;
        }
    }
}
