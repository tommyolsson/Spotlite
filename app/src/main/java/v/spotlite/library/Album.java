package v.spotlite.library;

import java.util.ArrayList;

public class Album extends LibraryObject
{
    public static final int minatureSize = 80;

    private ArrayList<Song> songs;
    private Artist artist;

    public Album(String name, Artist artist)
    {
        this.artist = artist;
        this.name = name;
        this.songs = new ArrayList<Song>();
    }

    public ArrayList<Song> getSongs() {return songs;}
    public Artist getArtist() {return artist;}

    public void addSong(Song song) {this.songs.add(song);}
}
