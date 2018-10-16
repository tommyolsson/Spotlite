package v.spotlite.library;

import java.util.ArrayList;

public class Artist extends LibraryObject
{
    private final ArrayList<Album> albums;

    public Artist(String name)
    {
        this.name = name;
        this.albums = new ArrayList<>();
    }

    public ArrayList<Album> getAlbums() {return albums;}

    public void addAlbum(Album album) {this.albums.add(album);}

    public int getSongCount()
    {
        int songCount = 0;

        synchronized (albums)
        {
            for (Album album : albums)
            {
                songCount += album.getSongs().size();
            }
        }

        return songCount;
    }
}
