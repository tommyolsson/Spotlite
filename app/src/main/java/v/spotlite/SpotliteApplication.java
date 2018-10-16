package v.spotlite;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import v.spotlite.ui.settings.SettingsActivity;

public class SpotliteApplication extends Application
{
    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        //load saved theme
        SharedPreferences generalPrefs = base.getSharedPreferences(SettingsActivity.PREFERENCES_GENERAL_FILE_NAME, Context.MODE_PRIVATE);
    }
}
