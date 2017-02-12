package eu.nioc.tumblrbrowse.models;

import com.tumblr.jumblr.types.Photo;
import com.tumblr.jumblr.types.PhotoPost;

/**
 * Lightweight and public version of PhotoPost class for using in blog activity
 */
public class UnitPhotoPost extends PhotoPost {
    public Long note_count, timestamp;
    public String reblog_key, reblogged_from_name;
    public Boolean liked;
    public Photo photo;

    public Long getNoteCount() {
        return note_count;
    }
    public Boolean isLiked() {
        return liked;
    }
    public Photo getPhoto() {
        return photo;
    }
    public String getRebloggedFromName() {
        return reblogged_from_name;
    }
    public Long getTimestamp() {
        return timestamp;
    }
}
