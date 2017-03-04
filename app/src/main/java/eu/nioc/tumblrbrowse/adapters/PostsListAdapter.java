package eu.nioc.tumblrbrowse.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.fivehundredpx.greedolayout.GreedoLayoutSizeCalculator;
import com.squareup.picasso.Picasso;
import com.tumblr.jumblr.types.PhotoSize;

import java.util.List;

import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.activities.BlogActivity;
import eu.nioc.tumblrbrowse.models.UnitPhotoPost;

/**
 *  Photos list adapter used in blog activity, using Picasso and GreedoLayout libraries
 */
public class PostsListAdapter extends RecyclerView.Adapter<PostsListAdapter.PostViewHolder> implements GreedoLayoutSizeCalculator.SizeCalculatorDelegate {
    private List<UnitPhotoPost> listData;
    private final LayoutInflater layoutInflater;
    private Context mContext;

    public PostsListAdapter(Context context, List<UnitPhotoPost> listData) {
        this.listData = listData;
        mContext = context;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

    @Override
    public int getItemViewType(int position) {
        //return reference to the layout resource
        return R.layout.list_posts_row;
    }

    @Override
    public double aspectRatioForIndex(int index) {
        if (index >= getItemCount()) {
            return 1.0;
        }
        //get photo size
        PhotoSize size = listData.get(index).getPhoto().getOriginalSize();
        //calculate image ratio
        return (double) size.getWidth() / size.getHeight();
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //create holder from layout
        return new PostViewHolder(layoutInflater.inflate(viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(final PostViewHolder holder, final int position) {
        //get requested blog element
        UnitPhotoPost photoPost = listData.get(position);

        //get the highest alternative size under 501 px
        int chosenSizeIndex = -1;
        for(int i = 0; i < photoPost.getPhoto().getSizes().size(); i++)  {
            if (chosenSizeIndex == -1 && photoPost.getPhoto().getSizes().get(i).getWidth() < 501) {
                chosenSizeIndex = i;
            }
        }

        //add onclick listener on image for opening fullscreen pager
        holder.thumbnailView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((BlogActivity) mContext).openPager(holder.getAdapterPosition());
            }
        });

        //set image with Picasso
        Picasso.with(mContext)
                .load(photoPost.getPhoto().getSizes().get(chosenSizeIndex).getUrl())
                .error(R.drawable.ic_warning)
                .into(holder.thumbnailView);
    }

    /**
     * Local class used for viewHolder pattern
     */
    class PostViewHolder extends RecyclerView.ViewHolder {
        private ImageView thumbnailView;

        PostViewHolder(View itemView) {
            super(itemView);
            //bind views
            thumbnailView = (ImageView) itemView;
        }
    }
}
