package v.spotlite.library;

import java.util.ArrayList;

public class Playlist extends LibraryObject
{
    private ArrayList<Song> content;

    private boolean mine = true;
    private String owner;
    private Object ownerID;
    private boolean collaborative = false;
    String path;

    public Playlist(String name, ArrayList<Song> content)
    {
        this.name = name;
        this.content = content;
    }

    public ArrayList<Song> getContent() {return content;}
    public void setOwner(String owner, Object ownerID)
    {
        this.mine = false;
        this.owner = owner;
        this.ownerID = ownerID;
    }

    public void setCollaborative() {this.collaborative = true;}
    public boolean isMine() {return this.mine;}
    public boolean isCollaborative() {return this.collaborative;}
    public String getOwner() {return this.owner;}
    public Object getOwnerID() {return this.ownerID;}

    public String getPath() {return path;}
    public void setPath(String s) {path = s;}
}
