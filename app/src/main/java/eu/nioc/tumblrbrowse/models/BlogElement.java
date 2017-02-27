package eu.nioc.tumblrbrowse.models;

/**
 * Lightweight version of Blog class for displaying in main activity
 */
public class BlogElement {
    public String avatarUrl;
    public String title;
    public String name;
    public Long updated;

    @Override
    public boolean equals(Object o) {
        //override equals method for comparing blogs by names
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlogElement that = (BlogElement) o;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
