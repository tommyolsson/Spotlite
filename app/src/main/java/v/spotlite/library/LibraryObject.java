package v.spotlite.library;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.Serializable;

public abstract class LibraryObject implements Serializable
{
    protected String name;
    protected SongSources sources;
    protected boolean handled;

    private Bitmap miniatureArt;
    private boolean hasArt = false;
    private String artPath;
    private boolean artLoad;

    public LibraryObject() {this.sources = new SongSources();}

    public String getName() {return this.name;}
    @Override public String toString() {return this.name;}
    public SongSources getSources() {return sources;}
    public boolean isHandled() {return handled;}
    public void setHandled(boolean handled) {this.handled = handled;}

    public void setArt(String path, Bitmap miniatureArt)
    {
        this.hasArt = true;
        this.miniatureArt = miniatureArt;
        this.artPath = path;
        this.artLoad = false;
    }
    public void setArtLoading() {this.artLoad = true;}
    public boolean getArtLoading() {return this.artLoad;}
    public Bitmap getArtMiniature() {return miniatureArt;}
    public Bitmap getArt() {return BitmapFactory.decodeFile(artPath);}
    public boolean hasArt() {return hasArt;}

    public String getType() {return this.getClass().getSimpleName();}

    public void setName(String name) {this.name = name;}
}
