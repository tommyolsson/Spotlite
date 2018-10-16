package v.spotlite.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import retrofit.RetrofitError;
import v.spotlite.R;
import v.spotlite.library.Source;
import v.spotlite.ui.MainActivity;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;

public class SourcesActivity extends AppCompatActivity {

    private static int SPOTIFY_REQUEST_CODE = 1337;

    private TextView username;
    private Button connectButton;
    private Button disconnectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sources);

        username = (TextView) findViewById(R.id.username);
        connectButton = (Button) findViewById(R.id.connect_button);
        disconnectButton = (Button) findViewById(R.id.disconnect_button);

        if(Source.SOURCE_SPOTIFY.isAvailable())
        {
            username.setText(getText(R.string.loggedOn)  + " " + Source.SOURCE_SPOTIFY.getUserName() );
            connectButton.setVisibility(View.GONE);
            disconnectButton.setVisibility(View.VISIBLE);

        }
        else
        {
            username.setText(getText(R.string.notLoggedOn));
            disconnectButton.setVisibility(View.GONE);
            connectButton.setVisibility(View.VISIBLE);
        }

        //reload songs from source
        SharedPreferences accountsPrefs = getSharedPreferences(SettingsActivity.PREFERENCES_ACCOUNT_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = accountsPrefs.edit();
        editor.apply();


        ImageView homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(SourcesActivity.this, MainActivity.class));
            }
        });

        ImageView refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (SPOTIFY_REQUEST_CODE == requestCode) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if(response.getType() == AuthenticationResponse.Type.CODE)
            {
                final String code = response.getCode();
                Thread t = new Thread()
                {
                    public void run()
                    {
                        Looper.prepare();
                        try
                        {
                            URL apiUrl = new URL("https://accounts.spotify.com/api/token");
                            HttpsURLConnection urlConnection = (HttpsURLConnection) apiUrl.openConnection();
                            urlConnection.setDoInput(true);
                            urlConnection.setDoOutput(true);
                            urlConnection.setRequestMethod("POST");

                            //write POST parameters
                            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                            BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(out, "UTF-8"));
                            writer.write("grant_type=authorization_code&");
                            writer.write("code=" + code + "&");
                            writer.write("redirect_uri=" + Source.SOURCE_SPOTIFY.SPOTIFY_REDIRECT_URI + "&");
                            writer.write("client_id=" + Source.SOURCE_SPOTIFY.SPOTIFY_CLIENT_ID + "&");
                            writer.write("client_secret=" + "964b940ee3bb4a628e4d30d925cbde99");
                            writer.flush();
                            writer.close();
                            out.close();

                            urlConnection.connect();

                            System.out.println("[AUTH] Result: " + urlConnection.getResponseCode() + " " + urlConnection.getResponseMessage());

                            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                            String result = reader.readLine();
                            reader.close();
                            result = result.substring(1);
                            result = result.substring(0, result.length()-1);
                            String[] results = result.split(",");
                            for(String param : results)
                            {
                                if(param.startsWith("\"access_token\":\""))
                                {
                                    param = param.replaceFirst("\"access_token\":\"", "");
                                    param = param.replaceFirst("\"", "");
                                    Source.SOURCE_SPOTIFY.SPOTIFY_USER_TOKEN = param;
                                    SharedPreferences pref = getSharedPreferences(SettingsActivity.PREFERENCES_ACCOUNT_FILE_NAME, Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = pref.edit();
                                    editor.putString("spotify_token", Source.SOURCE_SPOTIFY.SPOTIFY_USER_TOKEN);
                                    editor.commit();
                                }
                                else if(param.startsWith("\"refresh_token\":\""))
                                {
                                    param = param.replaceFirst("\"refresh_token\":\"", "");
                                    param = param.replaceFirst("\"", "");
                                    Source.SOURCE_SPOTIFY.SPOTIFY_REFRESH_TOKEN = param;
                                    SharedPreferences pref = getSharedPreferences(SettingsActivity.PREFERENCES_ACCOUNT_FILE_NAME, Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = pref.edit();
                                    editor.putString("spotify_refresh_token", Source.SOURCE_SPOTIFY.SPOTIFY_REFRESH_TOKEN);
                                    editor.commit();
                                }
                            }

                            Source.SOURCE_SPOTIFY.setAvailable(true);
                            SharedPreferences pref = getSharedPreferences(SettingsActivity.PREFERENCES_ACCOUNT_FILE_NAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putInt("spotify_prior", Source.SOURCE_SPOTIFY.getPriority());
                            editor.apply();

                            Source.SOURCE_SPOTIFY.spotifyApi.setAccessToken(Source.SOURCE_SPOTIFY.SPOTIFY_USER_TOKEN);

                            try
                            {
                                Source.SOURCE_SPOTIFY.mePrivate = Source.SOURCE_SPOTIFY.spotifyApi.getService().getMe();
                            }
                            catch(RetrofitError e)
                            {
                                e.printStackTrace();
                            }

                            Source.SOURCE_SPOTIFY.getPlayer().init();
                            Toast.makeText(SourcesActivity.this, getText(R.string.pls_resync), Toast.LENGTH_SHORT).show();
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                };
                t.start();

            }
            else
            {
                System.err.println("Wrong reponse received.\n");
                System.err.println("Error : " + response.getError());
            }
        }
    }


    public void connectButton(View view) {
        Log.i("Information", "Connect button clicked");

        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(Source.SOURCE_SPOTIFY.SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.CODE,
                        Source.SOURCE_SPOTIFY.SPOTIFY_REDIRECT_URI).setShowDialog(true);
        builder.setScopes(new String[]{"user-read-private", "streaming", "user-read-email", "user-follow-read",
                "playlist-read-private", "playlist-read-collaborative", "user-library-read"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(SourcesActivity.this, SPOTIFY_REQUEST_CODE, request);

    }

    public void disconnectButton(View view) {
        Log.i("Information", "Disconnect button clicked");
        if(Source.SOURCE_SPOTIFY.isAvailable())
        {
            //disconnect
            Source.SOURCE_SPOTIFY.disconnect();

            Toast.makeText(SourcesActivity.this, getText(R.string.disconnect_ok), Toast.LENGTH_SHORT).show();

            return;
        }
    }

}
