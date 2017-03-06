package eu.nioc.tumblrbrowse.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.TumblrBrowse;
import eu.nioc.tumblrbrowse.activities.BlogActivity;
import eu.nioc.tumblrbrowse.models.BlogHistory;

/**
 * History adapter used for display browsed blogs
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List entries;
    private Context context;

    public HistoryAdapter(Context context, List entries) {
        this.context = context;
        this.entries = entries;
    }

    @Override
    public HistoryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(HistoryAdapter.ViewHolder holder, int position) {
        //get history entry
        final BlogHistory.BlogHistoryEntry entry = (BlogHistory.BlogHistoryEntry) entries.get(position);
        //set data in views
        holder.blogView.setText(entry.blog);
        holder.timestampView.setText(DateUtils.getRelativeTimeSpanString(entry.timestamp * 1000, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        //set click listener for opening blog
        holder.historyRowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //go to blog activity
                Intent intent = new Intent(context, BlogActivity.class);
                intent.putExtra(TumblrBrowse.BT_BLOG_NAME, entry.blog);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        //return reference to the layout resource
        return R.layout.list_history_row;
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    /**
     * Local class used for viewHolder pattern
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView blogView;
        private TextView timestampView;
        private View historyRowView;

        ViewHolder(View itemView) {
            super(itemView);
            //bind views
            blogView = (TextView) itemView.findViewById(R.id.historyBlogTitle);
            timestampView = (TextView) itemView.findViewById(R.id.historyTimestamp);
            historyRowView = itemView.findViewById(R.id.historyRow);
        }
    }
}
