package v.spotlite.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Toast;
import v.spotlite.R;
import v.spotlite.library.LibraryService;
import v.spotlite.library.Source;
import v.spotlite.ui.MainActivity;

public class SettingsActivity extends AppCompatActivity
{
    public static final String PREFERENCES_ACCOUNT_FILE_NAME = "accounts";
    public static final String PREFERENCES_GENERAL_FILE_NAME = "general";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public final void onActivityResult(final int requestCode, final int resultCode, final Intent resultData)
    {
        if (resultCode == AppCompatActivity.RESULT_OK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            //{
            // Get Uri from Storage Access Framework.
            Uri treeUri = resultData.getData();

            // Persist URI in shared preference so that you can use it later.
            SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES_GENERAL_FILE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("sdcard_uri", treeUri.toString());
            editor.apply();

            LibraryService.TREE_URI = treeUri;

            // Persist access permissions, so you dont have to ask again
            final int takeFlags = resultData.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            Toast.makeText(this, getString(R.string.perm_granted) + " : " + treeUri, Toast.LENGTH_LONG).show();
            //}
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
        {
            getPreferenceManager().setSharedPreferencesName(PREFERENCES_GENERAL_FILE_NAME);

            addPreferencesFromResource(R.xml.preferences);

        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference)
        {
            if(preference.getKey().equals("sources_screen"))
            {
                Intent intent = new Intent(getActivity(), SourcesActivity.class);
                startActivity(intent);
            }
            else if(preference.getKey().equals("save_playlists_to_library"))
            {
                SharedPreferences generalPrefs = getActivity().getSharedPreferences(SettingsActivity.PREFERENCES_GENERAL_FILE_NAME, Context.MODE_PRIVATE);
                LibraryService.SAVE_PLAYLISTS_TO_LIBRARY = generalPrefs.getBoolean("save_playlist_to_library", false);
                Toast.makeText(getActivity(), getText(R.string.pls_resync), Toast.LENGTH_SHORT).show();
            }
            return super.onPreferenceTreeClick(preference);
        }
    }
}
