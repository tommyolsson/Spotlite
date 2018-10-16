package v.spotlite.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import v.spotlite.R;
import v.spotlite.library.LibraryService;
import v.spotlite.library.Song;
import v.spotlite.player.PlayerService;
import v.spotlite.ui.adapters.LibraryObjectAdapter;
import v.spotlite.ui.settings.SettingsActivity;

import java.util.ArrayList;

public class PlayActivity extends AppCompatActivity
{
    private static final float DELTA_X_MIN = 350;

    private PlayerService musicPlayer;
    boolean isDisplayingAlbumArt = true;
    /* activity components */
    private ImageView albumView;
    private TextView songTitle;
    private TextView songArtist;
    private TextView playlistPosition;
    private TextView songDuration;
    private TextView songCurrentPosition;
    private ImageView playAction;
    private ImageView playlistAction;
    private ImageView shuffleAction;
    private ImageView prevAction;
    private ImageView nextAction;
    private ImageView repeatAction;
    private SeekBar seekBar;
    private LibraryObjectAdapter playlistAdapter;
    private ListView.OnItemClickListener playlistViewListener = new ListView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            musicPlayer.setCurrentPosition(position);
        }
    };


    private ImageView.OnTouchListener albumDragListener = new ImageView.OnTouchListener()
    {
        float touchStartX = 0;
        float touchStartY = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            switch(event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                {
                    //image was touched, start touch action
                    touchStartX = event.getX();
                    touchStartY = event.getY();
                }
                case MotionEvent.ACTION_UP:
                {
                    //image was released, look at diff and change song if enough
                    float touchDeltaX = event.getX() - touchStartX;
                    if(touchDeltaX >= DELTA_X_MIN)
                    {
                        //swipe back
                        if(PlayerConnection.getService().getCurrentPosition() > 5000) onPrevClicked(v);
                        onPrevClicked(v);
                    }
                    else if(touchDeltaX <= -DELTA_X_MIN)
                    {
                        //swipe next
                        onNextClicked(v);
                    }
                }
                case MotionEvent.ACTION_MOVE:
                {
                    //TODO : animate during touch event
                }
            }
            return true;
        }
    };

    /* music player callbacks (UI refresh) */
    private MediaControllerCompat.Callback musicCallbacks = new MediaControllerCompat.Callback()
    {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state)
        {
            refreshState(state);
        }

        /*
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata)
        {
            super.onMetadataChanged(metadata);
        }

        @Override
        public void onRepeatModeChanged(int repeatMode)
        {
            super.onRepeatModeChanged(repeatMode);
        }

        @Override
        public void onShuffleModeChanged(boolean enabled)
        {
            super.onShuffleModeChanged(enabled);
        }
        */
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play);
        android.support.v7.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Gets currentPlaylistName intent from MainActivity and changes actionbarname to the name
        Intent i = getIntent();
        String currentPlaylistName = i.getStringExtra("currentplaylistname");
        getSupportActionBar().setTitle(currentPlaylistName);

        //get all components
        albumView = (ImageView) findViewById(R.id.album_display);
        songTitle = (TextView) findViewById(R.id.textview_title);
        songArtist = (TextView) findViewById(R.id.textview_subtitle);
        playlistPosition = (TextView) findViewById(R.id.textview_playlist_pos);
        songDuration = (TextView) findViewById(R.id.song_duration);
        songCurrentPosition = (TextView) findViewById(R.id.song_position);
        playAction = (ImageView) findViewById(R.id.play_button);
        shuffleAction = (ImageView) findViewById(R.id.shuffle_button);
        prevAction = (ImageView) findViewById(R.id.prev_button);
        nextAction = (ImageView) findViewById(R.id.next_button);
        repeatAction = (ImageView) findViewById(R.id.repeat_button);
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        albumView.setOnTouchListener(albumDragListener);

        // Changes textcolor
        songTitle.setTextColor(getResources().getColor(R.color.colorAccent));
        songArtist.setTextColor(getResources().getColor(R.color.colorDarkGrey));
        songDuration.setTextColor(getResources().getColor(R.color.colorDarkGrey));
        songCurrentPosition.setTextColor(getResources().getColor(R.color.colorDarkGrey));

        LibraryService.configureLibrary(getApplicationContext());
        if(!PlayerConnection.init(new PlayerConnection.Callback()
        {
            @Override
            public void onConnected()
            {
                PlayerConnection.musicController.registerCallback(musicCallbacks);
                musicPlayer = PlayerConnection.getService();

                refreshState(musicPlayer.getPlayerState());
            }

            @Override
            public void onDisconnected() {finish();}
        }, getApplicationContext())) finish();

        //setup handler that will keep seekBar and playTime in sync
        final Handler handler = new Handler();
        this.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                int pos = 0;
                if(musicPlayer != null) pos = musicPlayer.resolveCurrentSongPosition();
                int posMns = (pos / 60000) % 60000;
                int posScs = pos % 60000 / 1000;
                String songPos = String.format("%02d:%02d",  posMns, posScs);
                songCurrentPosition.setText(songPos);

                seekBar.setProgress(pos);

                handler.postDelayed(this, 200);
            }
        });
        //setup listener that will update time on seekbar clicked
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if(fromUser) musicPlayer.seekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.play, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_settings)
        {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshState(PlaybackStateCompat state)
    {
        Song currentSong = musicPlayer.getCurrentSong();

        //set album view / playlistView
        if(currentSong == null) return;
        if(currentSong.getAlbum().hasArt()) albumView.setImageBitmap(musicPlayer.getCurrentArt());
        else albumView.setImageResource(R.drawable.baseline_album_24);

        if(playlistAdapter == null)
        {
            playlistAdapter = new LibraryObjectAdapter(this, musicPlayer.getCurrentPlaylist());
            playlistAdapter.setMoreImage(R.drawable.baseline_menu_24);
            playlistAdapter.repaintSongBackground();
            playlistAdapter.setSelectedPosition(musicPlayer.getCurrentPosition());
        }
        else
        {
            playlistAdapter.resetList(musicPlayer.getCurrentPlaylist());
            playlistAdapter.setSelectedPosition(musicPlayer.getCurrentPosition());
            playlistAdapter.notifyDataSetChanged();
        }

        //set song info
        songTitle.setText(currentSong.getTitle());
        songArtist.setText(currentSong.getArtist().getName());

        playlistPosition.setText((musicPlayer.getCurrentPosition()+1) + "/" + musicPlayer.getCurrentPlaylist().size());

        int dur = musicPlayer.resolveCurrentSongDuration();
        int durMns = (dur / 60000) % 60000;
        int durScs = dur % 60000 / 1000;
        String songDur = String.format("%02d:%02d",  durMns, durScs);
        songDuration.setText(songDur);
        seekBar.setMax(dur);

        //set play button icon
        if(musicPlayer.isPlaying()) playAction.setImageResource(R.drawable.baseline_pause_white_24);
        else playAction.setImageResource(R.drawable.baseline_play_arrow_white_24);

        //set shuffle button icon
        if(musicPlayer.isShuffleEnabled()) shuffleAction.setImageResource(R.drawable.baseline_shuffle_white_24);
        else shuffleAction.setImageResource(R.drawable.baseline_shuffle_black_24);

        //set repeat button icon
        int repeatMode = musicPlayer.getRepeatMode();
        if(repeatMode == PlaybackStateCompat.REPEAT_MODE_NONE) repeatAction.setImageResource(R.drawable.baseline_repeat_black_24);
        else if(repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) repeatAction.setImageResource(R.drawable.baseline_repeat_one_white_24);
        else repeatAction.setImageResource(R.drawable.baseline_repeat_white_24);
    }

    /* button actions */
    public void onPlayClicked(View v)
    {
        if(musicPlayer != null && PlayerConnection.musicController != null)
        {
            if(musicPlayer.isPlaying()) PlayerConnection.musicController.getTransportControls().pause();
            else PlayerConnection.musicController.getTransportControls().play();
        }
    }
    public void onPrevClicked(View v)
    {
        PlayerConnection.musicController.getTransportControls().skipToPrevious();
    }
    public void onNextClicked(View v)
    {
        PlayerConnection.musicController.getTransportControls().skipToNext();
    }
    public void onRepeatClicked(View v)
    {
        int currentRepeatMode = musicPlayer.getRepeatMode();

        if(currentRepeatMode == PlaybackStateCompat.REPEAT_MODE_NONE) currentRepeatMode = PlaybackStateCompat.REPEAT_MODE_ONE;
        else if(currentRepeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) currentRepeatMode = PlaybackStateCompat.REPEAT_MODE_ALL;
        else if(currentRepeatMode == PlaybackStateCompat.REPEAT_MODE_ALL) currentRepeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;

        PlayerConnection.musicController.getTransportControls().setRepeatMode(currentRepeatMode);

        /* manually refresh UI */
        if(currentRepeatMode == PlaybackStateCompat.REPEAT_MODE_NONE) repeatAction.setImageResource(R.drawable.baseline_repeat_black_24);
        else if(currentRepeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) repeatAction.setImageResource(R.drawable.baseline_repeat_one_white_24);
        else repeatAction.setImageResource(R.drawable.baseline_repeat_white_24);
    }
    public void onShuffleClicked(View v)
    {
        boolean shuffle = !musicPlayer.isShuffleEnabled();
        PlayerConnection.musicController.getTransportControls().setShuffleMode(0);

        /* manually refresh UI */
        if(shuffle) shuffleAction.setImageResource(R.drawable.baseline_shuffle_white_24);
        else shuffleAction.setImageResource(R.drawable.baseline_shuffle_black_24);
    }
    public void onPlaylistClicked(View v)
    {
        isDisplayingAlbumArt = !isDisplayingAlbumArt;

        if(isDisplayingAlbumArt)
        {
            albumView.setVisibility(View.VISIBLE);
        }
        else
        {
            albumView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
