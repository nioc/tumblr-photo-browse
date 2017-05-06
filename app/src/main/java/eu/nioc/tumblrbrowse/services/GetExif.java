package eu.nioc.tumblrbrowse.services;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDescriptor;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.activities.BlogActivity;
import eu.nioc.tumblrbrowse.models.PhotoExif;
import eu.nioc.tumblrbrowse.models.UnitPhotoPost;

/**
 * Asynchronous task for requesting EXIF data
 */
public class GetExif extends AsyncTask<UnitPhotoPost, String, UnitPhotoPost> {
    private Activity activity;
    private Exception exception;

    public GetExif(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected UnitPhotoPost doInBackground(UnitPhotoPost... params) {
        UnitPhotoPost photo = params[0];
        photo.exif = new ArrayList<>();
        try {
            //get stream (probably in LRU cache but we can not be sure)
            InputStream input = new URL(photo.getPhoto().getOriginalSize().getUrl()).openStream();
            try {
                ArrayList<PhotoExif> exifList = new ArrayList<>();
                //get Exif
                Metadata metadata = ImageMetadataReader.readMetadata(input);
                ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                ExifSubIFDDirectory exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                ArrayList devices = new ArrayList();
                String camera;
                if (exifIFD0Directory != null) {
                    camera = exifIFD0Directory.getString(ExifIFD0Directory.TAG_MODEL);
                    if (camera != null) {
                        devices.add(camera);
                    }
                }
                if (exifSubIFDDirectory != null) {
                    ExifSubIFDDescriptor descriptor = new ExifSubIFDDescriptor(exifSubIFDDirectory);

                    camera = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_MODEL);
                    if (camera != null) {
                        devices.add(camera);
                    }
                    String lens = exifSubIFDDirectory.getString(ExifSubIFDDirectory.TAG_LENS_MODEL);
                    if (lens != null) {
                        devices.add(lens);
                    }
                    String device = StringUtils.join(devices, " - ");
                    exifList.add(new PhotoExif(R.drawable.ic_camera, device));
                    exifList.add(new PhotoExif(R.drawable.ic_focal, descriptor.getFocalLengthDescription()));
                    exifList.add(new PhotoExif(R.drawable.ic_iris, descriptor.getApertureValueDescription()));
                    exifList.add(new PhotoExif(R.drawable.ic_timer, descriptor.getShutterSpeedDescription()));
                    exifList.add(new PhotoExif(R.drawable.ic_light, descriptor.getIsoEquivalentDescription()));
                    Date dateOriginal = exifSubIFDDirectory.getDateOriginal();
                    if (dateOriginal != null) {
                        exifList.add(new PhotoExif(R.drawable.ic_calendar, android.text.format.DateFormat.getMediumDateFormat(activity.getApplicationContext()).format(dateOriginal)));
                    }
                }
                //keep only exif with value
                for (PhotoExif exif : exifList) {
                    if (exif.value != null && !exif.value.isEmpty()) {
                        photo.exif.add(exif);
                    }
                }
            } catch (ImageProcessingException e) {
                exception = e;
            }
        } catch (IOException e) {
            //catch error (probably network issue)
            exception = e;
        }
        return photo;
    }

    @Override
    protected void onPostExecute(UnitPhotoPost photo) {
        if (exception != null) {
            //display exception message to user
            Toast.makeText(this.activity, exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
        //callback to activity with photos list
        ((BlogActivity) this.activity).displayPhotoPostExif(photo);
    }
}
