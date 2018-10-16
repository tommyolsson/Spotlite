package v.spotlite.library;

public class Song extends LibraryObject
{
    private Artist artist;
    private Album album;
    private int track;
    private int year;
    long duration;

    //local library options
    String format;
    String path;

    public Song(String title, Artist artist, Album album, int albumTrack, long duration, int year)
    {
        this.name = title;
        this.artist = artist;
        this.album = album;
        this.track = albumTrack;
        this.duration = duration;
        this.year = year;
    }

    public void setFormat(String s) {format = s;}
    public void setPath(String s) {path = s;}
    public String getFormat() {return format;}
    public String getPath() {return path;}
    public String getTitle() {return getName();}
    public Artist getArtist() {return artist;}
    public Album getAlbum() {return album;}
    public int getYear() {return year;}
    public int getTrackNumber() {return track;}
    public long getDuration() {return duration;}
}
