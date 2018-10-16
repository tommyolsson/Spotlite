package v.spotlite.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaControllerCompat;
import v.spotlite.library.Song;
import v.spotlite.player.PlayerService;

import java.util.ArrayList;

import static java.lang.System.exit;

public class PlayerConnection
{
    private static Context applicationContext;

    /* connection to player and callbacks */
    private static ArrayList<Song> playOnConnect; private static int positionOnConnect;
    private static volatile PlayerService musicPlayer = null;
    public static MediaControllerCompat musicController;
    private static ServiceConnection musicConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            musicPlayer = ((PlayerService.PlayerBinder) service).getService();

            try{musicController = new MediaControllerCompat(applicationContext, musicPlayer.getSessionToken());}
            catch(Exception e) { exit(1); }

            if(playOnConnect != null) {musicPlayer.setCurrentPlaylist(playOnConnect, positionOnConnect); playOnConnect = null;}

            if(connectionCallback != null) connectionCallback.onConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            musicPlayer = null;
            applicationContext.unbindService(this);
            if(connectionCallback != null) connectionCallback.onDisconnected();
        }
    };

    /* connection callbacks */
    public interface Callback {void onConnected(); void onDisconnected();}
    private static Callback connectionCallback;

    public static boolean init(Callback connectionCallback, Context applicationContext)
    {
        if(musicPlayer != null) {if(connectionCallback != null) connectionCallback.onConnected(); return true;}

        PlayerConnection.applicationContext = applicationContext;
        Intent serv = new Intent(applicationContext, PlayerService.class);
        PlayerConnection.connectionCallback = connectionCallback;
        return applicationContext.bindService(serv, musicConnection, Context.BIND_ABOVE_CLIENT);
    }

    public static void start(ArrayList<Song> songs, int currentPos)
    {
        playOnConnect = songs;
        positionOnConnect = currentPos;

        Intent serv = new Intent(applicationContext, PlayerService.class);

        if(Build.VERSION.SDK_INT >= 26) applicationContext.startForegroundService(serv);
        else applicationContext.startService(serv);

        applicationContext.bindService(serv, musicConnection, Context.BIND_ABOVE_CLIENT);
    }

    public static PlayerService getService()
    {
        return musicPlayer;
    }
}
