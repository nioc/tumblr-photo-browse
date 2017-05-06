package eu.nioc.tumblrbrowse.models;

/**
 * Simple EXIF model with icon and value
 */
public class PhotoExif {
    public int icon;
    public String value;

    public PhotoExif(int icon, String value) {
        this.icon = icon;
        this.value = value;
    }
}
