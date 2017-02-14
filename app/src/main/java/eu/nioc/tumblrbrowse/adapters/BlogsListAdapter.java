package eu.nioc.tumblrbrowse.adapters;

import android.app.Activity;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.models.BlogElement;

/**
 * Blogs list adapter used in main activity
 */
public class BlogsListAdapter extends BaseAdapter {
    private List<BlogElement> listData;
    private Context context;

    public BlogsListAdapter(Context context, List<BlogElement> listData) {
        this.listData = listData;
        this.context = context;
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            //create convertView if not exists
            convertView = ((Activity)context).getLayoutInflater().inflate(R.layout.list_blogs_row, parent, false);
            holder = new ViewHolder();
            holder.titleView = (TextView) convertView.findViewById(R.id.title);
            holder.updatedView = (TextView) convertView.findViewById(R.id.updated);
            holder.avatarView = (ImageView)convertView.findViewById(R.id.avatar);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //get requested blog element
        BlogElement blogElement = listData.get(position);

        //set blog title
        holder.titleView.setText(blogElement.name);

        //set blog updated since time
        if (blogElement.updated != null) {
            holder.updatedView.setText(DateUtils.getRelativeTimeSpanString(blogElement.updated * 1000, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        }

        //load blog avatar with Picasso lib and fit it into ImageView
        Picasso.with(context)
                .load(blogElement.avatarUrl)
                .noFade()
                .placeholder(R.drawable.ic_sync)
                .error(R.drawable.ic_warning)
                .fit()
                .tag(context)
                .into(holder.avatarView);

        return convertView;
    }

    /**
     * Local class used for viewHolder pattern
     */
    private static class ViewHolder {
        TextView titleView, updatedView;
        ImageView avatarView;
    }
}
