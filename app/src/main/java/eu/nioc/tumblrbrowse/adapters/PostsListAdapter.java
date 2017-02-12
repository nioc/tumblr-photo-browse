package eu.nioc.tumblrbrowse.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.util.List;

import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.utils.VolleySingleton;
import eu.nioc.tumblrbrowse.models.UnitPhotoPost;

/**
 *  Photos list adapter used in blog activity, using Volley library
 */
public class PostsListAdapter extends BaseAdapter {
    private List<UnitPhotoPost> listData;
    private LayoutInflater layoutInflater;

    private ImageLoader mImageLoader = VolleySingleton.getInstance().getImageLoader();

    public PostsListAdapter(Context context, List<UnitPhotoPost> listData) {
        this.listData = listData;
        layoutInflater = LayoutInflater.from(context);
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
            convertView = layoutInflater.inflate(R.layout.list_posts_row, null);
            holder = new ViewHolder();
            holder.noteCountView = (TextView) convertView.findViewById(R.id.noteCount);
            holder.originView = (TextView) convertView.findViewById(R.id.origin);
            holder.thumbnailView = (NetworkImageView)convertView.findViewById(R.id.thumbnail);
            holder.likedView = (ImageView) convertView.findViewById(R.id.liked);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //get requested blog element
        UnitPhotoPost photoPost = listData.get(position);

        //set photo notes count
        holder.noteCountView.setText(convertView.getResources().getQuantityString(R.plurals.placeholder_text_note, photoPost.getNoteCount().intValue(), photoPost.getNoteCount()));

        //indicate original blog
        holder.originView.setText(photoPost.getRebloggedFromName());

        //handle liked indicator
        holder.likedView.setImageResource(R.drawable.ic_heart_outline);
        if (photoPost.isLiked()) {
            holder.likedView.setImageResource(R.drawable.ic_heart);
        }

        //add photo thumbnail
        //@TODO: replace Volley with Picasso
        if (holder.thumbnailView != null) {
            if (mImageLoader == null) {
                mImageLoader = VolleySingleton.getInstance().getImageLoader();
            }
            //get the highest alternative size under 501 px
            int chosenSizeIndex = -1;
             for(int i = 0; i < photoPost.getPhoto().getSizes().size(); i++)  {
                 if (chosenSizeIndex == -1 && photoPost.getPhoto().getSizes().get(i).getWidth() < 501) {
                     chosenSizeIndex = i;
                 }
            }
            //set image source
            holder.thumbnailView.setImageUrl(photoPost.getPhoto().getSizes().get(chosenSizeIndex).getUrl(), mImageLoader);
        }
        return convertView;
    }

    /**
     * Local class used for viewHolder pattern
     */
    static class ViewHolder {
        TextView noteCountView;
        TextView originView;
        NetworkImageView thumbnailView;
        ImageView likedView;
    }
}
