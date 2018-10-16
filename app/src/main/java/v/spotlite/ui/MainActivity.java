package v.spotlite.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import v.spotlite.R;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.*;
import android.widget.*;
import v.spotlite.R;
import v.spotlite.library.*;
import v.spotlite.player.PlayerService;
import v.spotlite.ui.adapters.LibraryObjectAdapter;
import v.spotlite.ui.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private static final int EXT_PERM_REQUEST_CODE = 0x42;

    /* music controller and callbacks */
    private PlayerService musicPlayer;
    private boolean musicCallbacksRegistered = false;
    private MediaControllerCompat.Callback musicCallbacks = new MediaControllerCompat.Callback()
    {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state)
        {
            if(state.getState() == PlaybackStateCompat.STATE_STOPPED) {hideCurrentPlay(); return;}

            if(musicPlayer != null)
                showCurrentPlay(musicPlayer.getCurrentSong(), musicPlayer.isPlaying());
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata)
        {
            if(musicPlayer != null)
                showCurrentPlay(musicPlayer.getCurrentSong(), musicPlayer.isPlaying());
        }
    };
    private PlayerConnection.Callback connectionCallbacks = new PlayerConnection.Callback()
    {
        @Override
        public void onConnected()
        {
            musicPlayer = PlayerConnection.getService();

            if(!musicCallbacksRegistered)
            {
                PlayerConnection.musicController.registerCallback(musicCallbacks);
                musicCallbacksRegistered = true;
            }
        }

        @Override
        public void onDisconnected()
        {
            musicPlayer = null;
            musicCallbacksRegistered = false;
            hideCurrentPlay();
        }
    };

    /* current activity context (instanceState) */
    private static final int CONTEXT_NONE = 0;
    private static final int CONTEXT_ARTISTS = 1;
    private static final int CONTEXT_ALBUMS = 2;
    private static final int CONTEXT_SONGS = 3;
    private static final int CONTEXT_PLAYLISTS = 4;
    private static final int CONTEXT_SEARCH = 5;
    private int currentContext = CONTEXT_NONE;

    /* specific context (back button) handling */
    private static Bundle backBundle, back2Bundle;
    private static LibraryObject backObject, back2Object;
    private static boolean fromPlaylists;
    private static LibraryObject currentObject = null;
    //for tag activity to edit song, we need to keep 'more' object here
    static LibraryObject selectedObject = null;

    /* currently playing display */
    private RelativeLayout currentPlay;
    private TextView currentPlayTitle;
    private TextView currentPlaySubtitle;
    private ImageView currentPlayImage;
    private ImageView currentPlayAction;
    private String currentPlaylistName = "";
    private boolean currentPlayShown = false;
    private boolean needShowCurrentPlay = false;
    private MenuItem syncButton;
    private MenuItem backButton;

    /* main list view */
    private ListView mainListView;
    private ListView.OnItemClickListener mainListViewListener = new ListView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            switch(currentContext)
            {
                case CONTEXT_SONGS:
                    ArrayList<Song> songs = new ArrayList<Song>(((LibraryObjectAdapter)mainListView.getAdapter()).getObjectList());
                    setPlaylist(songs, position);

                    Intent intent = new Intent(MainActivity.this, PlayActivity.class);
                    intent.putExtra("currentplaylistname", currentPlaylistName);
                    startActivity(intent);
                    break;
                case CONTEXT_PLAYLISTS:
                    fromPlaylists = true;
                    backBundle = new Bundle(); saveInstanceState(backBundle); backObject = currentObject;
                    Playlist currentPlaylist = (Playlist) ((LibraryObjectAdapter)mainListView.getAdapter()).getObjects().get(position);
                    currentObject = currentPlaylist;
                    setContentToSongs(currentPlaylist.getContent(), currentPlaylist.getName());

                    currentPlaylistName = currentPlaylist.getName();
                    break;
            }
        }
    };
    private ImageView.OnClickListener mainListViewMoreListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            final LibraryObject object = (LibraryObject) v.getTag();

            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick(MenuItem item)
                {
                    switch(item.getItemId())
                    {
                        case R.id.action_play:
                            ArrayList<Song> playlist = new ArrayList<Song>();
                            if(object instanceof Song) playlist.add((Song) object);
                            else if(object instanceof Album) playlist.addAll(((Album) object).getSongs());
                            else if(object instanceof Artist) for(Album a : ((Artist) object).getAlbums()) playlist.addAll(a.getSongs());
                            else if(object instanceof Playlist) playlist.addAll(((Playlist) object).getContent());
                            setPlaylist(playlist, 0);
                            break;

                        case R.id.action_play_next:
                            ArrayList<Song> playlist1 = new ArrayList<Song>();
                            if(object instanceof Song) playlist1.add((Song) object);
                            else if(object instanceof Album) playlist1.addAll(((Album) object).getSongs());
                            else if(object instanceof Artist) for(Album a : ((Artist) object).getAlbums()) playlist1.addAll(a.getSongs());
                            else if(object instanceof Playlist) playlist1.addAll(((Playlist) object).getContent());
                            playNext(playlist1);
                            Toast.makeText(MainActivity.this, playlist1.size() + " " + getString(R.string.added_next_ok), Toast.LENGTH_SHORT).show();
                            break;

                    }
                    return false;
                }
            });
            getMenuInflater().inflate(R.menu.menu_object_more, popupMenu.getMenu());

            if(currentContext == CONTEXT_PLAYLISTS)
            {
                popupMenu.getMenu().findItem(R.id.action_add_to_list).setVisible(false);
            }

            popupMenu.show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        android.support.v7.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        // Raden nedan döljer ikonen. TILLFÄLLIG lösning. Ska försökas få bort helt
        toggle.setDrawerIndicatorEnabled(false);
        toggle.syncState();

        mainListView = (ListView) findViewById(R.id.libraryList);
        mainListView.setOnItemClickListener(mainListViewListener);

        currentPlay = (RelativeLayout) findViewById(R.id.currentPlay);
        currentPlay.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, PlayActivity.class);
                startActivity(intent);
            }
        });
        currentPlayTitle = currentPlay.findViewById(R.id.element_title);
        currentPlaySubtitle = currentPlay.findViewById(R.id.element_subtitle);
        currentPlayImage = currentPlay.findViewById(R.id.element_image);
        currentPlayAction = currentPlay.findViewById(R.id.element_action);
        currentPlayAction.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(musicPlayer.isPlaying()) PlayerConnection.musicController.getTransportControls().pause();
                else PlayerConnection.musicController.getTransportControls().play();
            }
        });

        restoreInstanceState(savedInstanceState, currentObject);

        //delay currentPlay showing
        mainListView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                if(needShowCurrentPlay)
                {
                    showCurrentPlay(musicPlayer.getCurrentSong(), musicPlayer.isPlaying());
                    needShowCurrentPlay = false;
                    mainListView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });

    }

    @Override
    protected void onStart()
    {
        super.onStart();
        PlayerConnection.init(connectionCallbacks, getApplicationContext());
        LibraryService.configureLibrary(getApplicationContext());
        checkPermission();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        musicCallbacksRegistered = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        syncButton = menu.findItem(R.id.action_sync);
        backButton = menu.findItem(R.id.action_back);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        else if(id == R.id.action_back)
        {
            // Goes to home screen when pressed
            setContentToPlaylists();
            backButton.setVisible(false);
            return true;
        }
        else if(id == R.id.action_sync)
        {
            if(LibraryService.synchronization)
            {
                LibraryService.syncThread.interrupt();
                syncButton.setIcon(R.drawable.baseline_sync_24);
                LibraryService.registerInit();
                return true;
            }

            syncButton.setIcon(R.drawable.baseline_cancel_24);
            //devices with little screens : change name
            syncButton.setTitle(R.string.cancel);

            LibraryService.synchronizeLibrary(new LibraryService.SynchronizeCallback()
            {
                @Override
                public void synchronizeDone()
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            syncButton.setIcon(R.drawable.baseline_sync_24);
                            syncButton.setTitle(R.string.sync);
                        }
                    });
                }
                @Override
                public void synchronizeFail(int error)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            syncButton.setIcon(R.drawable.baseline_sync_24);
                            syncButton.setTitle(R.string.sync);
                            switch (error)
                            {
                                case LibraryService.ERROR_LOADING_NOT_DONE:
                                    Toast.makeText(MainActivity.this, getText(R.string.sync_fail_load), Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }
                    });
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* Perform permission check and read library */
    private void checkPermission()
    {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
        {
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
            {
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                {
                    // Show an alert dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(getString(R.string.please_grant_permission_msg));
                    builder.setTitle(getString(R.string.please_grant_permission_title));
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXT_PERM_REQUEST_CODE);
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.setOnShowListener(new DialogInterface.OnShowListener()
                    {
                        @Override
                        public void onShow(DialogInterface arg0)
                        {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
                        }
                    });
                    dialog.show();
                }
                else
                {
                    // Request permission
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXT_PERM_REQUEST_CODE);
                }
            }
            else startLibService();
        }
        else startLibService();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == EXT_PERM_REQUEST_CODE)
        {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                startLibService();
            else
            {
                Toast.makeText(this, getString(R.string.please_grant_permission_msg), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    public void startLibService()
    {
        if(LibraryService.getArtists().size() == 0)
        {
            Intent service = new Intent(this, LibraryService.class);
            startService(service);

            LibraryService.registerInit();
        }
        if(currentContext == CONTEXT_NONE) setContentToPlaylists();
    }

    /* UI Change methods (Artists/Albums/Songs/Playlists...) */

    private void setContentToSongs(List<Song> songs, String title)
    {
        this.setTitle(title);
        currentContext = CONTEXT_SONGS;
        backButton.setVisible(true);
        LibraryObjectAdapter adapter = new LibraryObjectAdapter(this, songs);
        adapter.registerMoreClickListener(mainListViewMoreListener);
        mainListView.setAdapter(adapter);
    }
    private void setContentToPlaylists()
    {
        this.setTitle(getResources().getString(R.string.playlists));
        currentContext = CONTEXT_PLAYLISTS;
        LibraryObjectAdapter adapter = new LibraryObjectAdapter(this, LibraryService.getPlaylists());
        adapter.registerMoreClickListener(mainListViewMoreListener);
        mainListView.setAdapter(adapter);
    }


    /* currently playing */
    private void showCurrentPlay(Song song, boolean play)
    {
        if(song == null) return;

        if(!currentPlayShown)
        {
            //show
            currentPlay.setVisibility(View.VISIBLE);
            currentPlayShown = true;
        }

        // update informations
        currentPlayTitle.setText(song.getTitle());
        currentPlaySubtitle.setText(song.getArtist().getName());
        if(song.getAlbum().hasArt()) currentPlayImage.setImageBitmap(song.getAlbum().getArtMiniature());
        else currentPlayImage.setImageResource(R.drawable.baseline_album_24);

        if(play) currentPlayAction.setImageResource(R.drawable.baseline_pause_white_24);
        else currentPlayAction.setImageResource(R.drawable.baseline_play_arrow_white_24);
    }
    private void hideCurrentPlay()
    {
        if(currentPlayShown)
        {
            currentPlay.setVisibility(View.INVISIBLE);
            mainListView.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
            mainListView.requestLayout();
            currentPlayShown = false;
        }
    }

    /* actions */
    private void setPlaylist(ArrayList<Song> songs, int currentPos)
    {
        if(songs.size() == 0)
        {
            Toast.makeText(this, getText(R.string.empty), Toast.LENGTH_SHORT).show();
            return;
        }

        if(musicPlayer == null) PlayerConnection.start(songs, currentPos);
        else musicPlayer.setCurrentPlaylist(songs, currentPos);
    }
    private void playNext(ArrayList<Song> songs)
    {
        if(musicPlayer == null) PlayerConnection.start(songs, 0);
        else musicPlayer.addNextToPlaylist(songs);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    private void saveInstanceState(Bundle bundle)
    {
        if(bundle == null) return;

        Log.println(Log.WARN, "DEBUG", "SaveInstanceState : " + currentObject);

        bundle.putInt("currentContext", currentContext);
        bundle.putBoolean("fromPlaylists", fromPlaylists);
        bundle.putInt("listSelection", mainListView.getFirstVisiblePosition());
        bundle.putBoolean("currentPlayShown", currentPlayShown);
    }
    private void restoreInstanceState(Bundle bundle, LibraryObject currentObject)
    {
        if(bundle == null)
        {
            if(PlayerConnection.getService() != null) needShowCurrentPlay = true;
            return;
        }

        int restoreContext = bundle.getInt("currentContext");
        fromPlaylists = bundle.getBoolean("fromPlaylists");

        MainActivity.currentObject = currentObject;

        switch(restoreContext)
        {

            case CONTEXT_SONGS:
                if(currentObject == null) setContentToSongs(LibraryService.getSongs(), getString(R.string.songs));
                else if(fromPlaylists) setContentToSongs(((Playlist) currentObject).getContent(), ((Playlist) currentObject).getName());
                else setContentToSongs(((Album) currentObject).getSongs(), ((Album) currentObject).getName());
                break;

            case CONTEXT_PLAYLISTS:
                setContentToPlaylists();
                break;
        }

        mainListView.setSelection(bundle.getInt("listSelection"));

        if((bundle.getBoolean("currentPlayShown")) && PlayerConnection.getService() != null)
        {
            needShowCurrentPlay = true;
        }
    }

}
