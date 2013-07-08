/**
 * Fahrgemeinschaft / Ridesharing App
 * Copyright (c) 2013 by it's authors.
 * Some rights reserved. See LICENSE..
 *
 */

package de.fahrgemeinschaft;

import org.teleportr.ConnectorService;
import org.teleportr.Ride;
import org.teleportr.Ride.COLUMNS;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.CursorAdapter;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.fahrgemeinschaft.util.Util;
import de.fahrgemeinschaft.util.SpinningZebraListFragment.ListFragmentCallback;

public class ResultsActivity extends SherlockFragmentActivity
       implements ListFragmentCallback, OnPageChangeListener {

    public static final Uri MY_RIDES_URI =
            Uri.parse("content://de.fahrgemeinschaft/myrides");
    public static final Uri BG_JOBS_URI =
            Uri.parse("content://de.fahrgemeinschaft/jobs/search");
    public RideDetailsFragment details;
    public RideListFragment results;
    public RideListFragment myrides;
    public Ride query;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("");
        setContentView(R.layout.activity_results);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        results = (RideListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.rides);
        details = new RideDetailsFragment();
        myrides = new RideListFragment();
        myrides.setSpinningEnabled(false);
        query = new Ride(getIntent().getData());
        results.load(getIntent().getData());

    }

   

    @Override
    public void onLoadFinished(Fragment fragment, Cursor cursor) {
        if (fragment.getId() == R.id.rides) {
            setTitle("Results");
            details.swapCursor(cursor);
            if (cursor.getCount() > 0) {
                cursor.moveToLast();
                long latest_dep = cursor.getLong(COLUMNS.DEPARTURE);
                System.out.println(" already until " + latest_dep);
                if (latest_dep > query.getArr()) // inc time window
                    query.arr(cursor.getLong(COLUMNS.DEPARTURE));
            }
        } else {
            setTitle("MyRides");
            details.swapCursor(cursor);
        }
    }

    @Override
    public void onListItemClick(int position) {
        details.setSelection(position);
        showFragment(details);
    }

    @Override
    public void onPageSelected(final int position) {
        results.getListView().setSelection(position);
        details.setSelection(position);
    }

    @Override
    public void onSpinningWheelClick() {
        System.out.println(" increase beyond " + query.getArr());
        query.arr(query.getArr() + 2 * 24 * 3600 * 1000).store(this);
        startService(new Intent(this, ConnectorService.class)
                .setAction(ConnectorService.SEARCH));
    }


    public void contact(View v) {
        Cursor cursor = ((CursorAdapter) results.getListAdapter()).getCursor();
        cursor.moveToPosition(details.getSelection());
        Util.openContactOptionsChooserDialog(this, cursor);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.my_rides:
            showFragment(myrides);
            myrides.load(MY_RIDES_URI);
            return true;
        case R.id.settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        case R.id.profile:
            startActivity(new Intent(this, SettingsActivity.class)
                    .putExtra("profile", true));
            return true;
        case android.R.id.home:
            if (getSupportFragmentManager().getBackStackEntryCount() > 0)
                getSupportFragmentManager().popBackStack();
            else finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right, R.anim.do_nix,
                R.anim.do_nix, R.anim.slide_out_right)
            .replace(R.id.container, fragment, null)
            .addToBackStack(null)
        .commit();
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {}

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {}
}
