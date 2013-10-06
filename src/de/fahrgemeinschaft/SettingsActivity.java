/**
 * Fahrgemeinschaft / Ridesharing App
 * Copyright (c) 2013 by it's authors.
 * Some rights reserved. See LICENSE.. 
 *
 */

package de.fahrgemeinschaft;

import org.teleportr.ConnectorService;
import org.teleportr.RidesProvider;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import de.fahrgemeinschaft.util.Util;
import de.fahrgemeinschaft.util.WebActivity;

public class SettingsActivity extends SherlockPreferenceActivity
        implements OnSharedPreferenceChangeListener {

    private static final String DONATE_URL = "http://www.sonnenstreifen.de/" +
                                "kunden/fahrgemeinschaft/spendenstand.php";
    private static final String REMEMBER = "remember_password";
    private SharedPreferences prefs;
    private boolean radius_changed;

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.settings);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        addPreferencesFromResource(R.xml.settings);
        setSummaries();
    }

    @SuppressWarnings("deprecation")
    void setSummaries() {
        ListPreference list = (ListPreference) findPreference("refresh");
        if (list.getEntry() != null) {
            list.setSummary(getResources().getString(
                    R.string.refresh_description, list.getEntry()));
        } else Log.d("settings", "no refresh entry");
        findPreference("about").setIntent(Util.aboutIntent(this));
        findPreference("donate").setIntent(new Intent(this, WebActivity.class)
            .setData(Uri.parse(DONATE_URL)));
        findPreference("special").setIntent(new Intent(this,
                SettingsSpecialActivity.class));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("radius_from") || key.equals("radius_to")) {
            radius_changed = true;
        } else if (key.equals("refresh")) {
            setSummaries();
        } else if (key.equals(REMEMBER) && !prefs.getBoolean(key, false)) {
            prefs.edit().remove("password").commit();
        }
    }

    @Override
    protected void onPause() {
        if (radius_changed) {
            Log.d(MainActivity.TAG, "clear cache");
            getContentResolver().delete(
                    RidesProvider.getRidesUri(this), null, null);
            startService(new Intent(this, ConnectorService.class)
                    .setAction(ConnectorService.SEARCH));
            prefs.edit().putLong("cleanup",
                    System.currentTimeMillis()).commit();
        }
        overridePendingTransition(R.anim.do_nix, R.anim.slide_out_top);
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        overridePendingTransition(R.anim.do_nix, R.anim.slide_out_top);
        return true;
    }
}
