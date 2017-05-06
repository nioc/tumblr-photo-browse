package eu.nioc.tumblrbrowse.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.models.PhotoExif;

/**
 * EXIF adapter
 */
public class ExifAdapter extends RecyclerView.Adapter<ExifAdapter.ViewHolder> {
    private ArrayList exifList;

    public ExifAdapter(ArrayList exifList) {
        this.exifList = exifList;
    }

    @Override
    public ExifAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(ExifAdapter.ViewHolder holder, int position) {
        //get exif entry
        PhotoExif exif = (PhotoExif) exifList.get(position);
        //set data in views
        holder.iconView.setImageResource(exif.icon);
        holder.valueView.setText(exif.value);
    }

    @Override
    public int getItemViewType(int position) {
        //return reference to the layout resource
        return R.layout.list_exif_row;
    }

    @Override
    public int getItemCount() {
        return exifList.size();
    }

    /**
     * Local class used for viewHolder pattern
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconView;
        private TextView valueView;

        ViewHolder(View itemView) {
            super(itemView);
            //bind views
            iconView = (ImageView) itemView.findViewById(R.id.exif_icon);
            valueView = (TextView) itemView.findViewById(R.id.exif_value);
        }
    }
}
