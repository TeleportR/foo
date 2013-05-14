/**
 * Fahrgemeinschaft / Ridesharing App
 * Copyright (c) 2013 by it's authors.
 * Some rights reserved. See LICENSE.. 
 *
 */

package de.fahrgemeinschaft;

import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.Date;

import org.teleportr.ConnectorService;
import org.teleportr.Ride;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.Insert;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.fahrgemeinschaft.RideListFragment.ListItemClicker;

public class ResultsActivity extends SherlockFragmentActivity
        implements LoaderCallbacks<Cursor>,
            ListItemClicker, OnPageChangeListener {

    private static final String TAG = "Results";
    protected static final int RIDES = -1;
    private static final int JOBS = 0;
    private RideListFragment list;
    private Uri search_uri;
    private RideDetailsFragment details;
    int selected;
    private Ride query_ride;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "on create activity");
        setContentView(R.layout.activity_results);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        list = (RideListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.rides);
        details = new RideDetailsFragment();

        search_uri = getIntent().getData();
        query_ride = new Ride(search_uri);
        getSupportLoaderManager().initLoader(RIDES, null, this);
        if (savedInstanceState != null) {
            selected = savedInstanceState.getInt("selected");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
        list.startSpinningWheel();
        return new CursorLoader(this, search_uri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor rides) {
        ((CursorAdapter) list.getListAdapter()).swapCursor(rides);
        getSupportLoaderManager().restartLoader(JOBS, null, list);
        Log.d(TAG, "got rides: " + rides.getCount());
        details.setCursor(rides);
        if (rides.getCount() > 0) {
            rides.moveToLast();
            long latest_dep = rides.getLong(5);
            if (latest_dep > query_ride.getArr())
                query_ride.arr(latest_dep);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        Log.d(TAG, "onLoaderReset");
    }

    @Override
    public void onListItemClick(int position) {
        Cursor cursor = ((CursorAdapter)list.getListView().getAdapter()).getCursor();
        if (position == cursor.getCount()) {
            Log.d(TAG, " search later than " +query_ride.getArr());
            query_ride.arr(query_ride.getArr() + 2*24*3600*1000);
            search_uri = query_ride.store(this);
            list.startSpinningWheel();
            startService(new Intent(this, ConnectorService.class)
                    .setAction(ConnectorService.SEARCH));
        } else {
            selected = position;
            getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(
                    R.anim.slide_in_right, R.anim.do_nix,
                    R.anim.do_nix, R.anim.slide_out_right)
            .replace(R.id.container, details, null)
            .addToBackStack(null)
            .commit();
        }
    }

    @Override
    public void onPageSelected(int position) {
        selected = position;
    }

    public void contact(View v) {
        Cursor c = ((CursorAdapter) list.getListAdapter()).getCursor();
        c.moveToPosition(selected);
        Intent contact = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
        contact.putExtra(Insert.NAME, c.getString(1) + " -> " + c.getString(3));
        contact.putExtra(ContactsContract.Intents.EXTRA_FORCE_CREATE, true);
        ArrayList<Intent> intents = new ArrayList<Intent>();
        String[] who = c.getString(7).split("; ");
        for (int i = 0; i < who.length; i++) {
            String[] split = who[i].split("=");
            if (split.length > 1) {
                if (split[0].equals("mobile")) {
                    System.out.println(split[1]);
                    contact.putExtra(Insert.PHONE, split[1]);
                    Intent call = labeledIntent(callIntent(split[1]),
                            R.drawable.ic_call, "Call " + split[1]);
                    if (call != null) intents.add(call);
                    Intent sms = labeledIntent(smsIntent(split[1],
                            c.getString(1) + " -> " + c.getString(3)),
                            R.drawable.ic_sms, "SMS " + split[1]);
                    if (sms != null) intents.add(sms);
                } else if (split[0].equals("landline")) {
                    contact.putExtra(Insert.SECONDARY_PHONE, split[1]);
                    Intent call = labeledIntent(callIntent(split[1]),
                            R.drawable.ic_dial, "Call " + split[1]);
                    if (call != null) intents.add(call);
                } else if (split[0].equals("mail")) {
                    contact.putExtra(Insert.EMAIL, split[1]);
                    Intent mail = labeledIntent(mailIntent(split[1],
                            c.getString(1) + " --> " + c.getString(3)),
                            R.drawable.ic_mail, "Mail " + split[1]);
                    if (mail != null) intents.add(mail);
                }
            }
        }
        startActivity(Intent.createChooser(contact, "Kontakt")
                .putExtra(Intent.EXTRA_INITIAL_INTENTS,
                intents.toArray(new Parcelable[intents.size()])));
    }

    private Intent callIntent(String num) {
        Intent call = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + num));
        return call;
    }

    private Intent smsIntent(String num, String text) {
        Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("sms:" + num));
        sms.putExtra("sms_body", text + "\n noch Platz?");
        sms.addCategory(Intent.CATEGORY_DEFAULT);
//        sms.setType("vnd.android-dir/mms-sms");
        return sms;
    }

    private Intent mailIntent(String a, String text) {
        Intent mail = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"+ a));
        mail.putExtra(Intent.EXTRA_TEXT, text + "\n noch Platz frei?");
        mail.putExtra(Intent.EXTRA_SUBJECT, "Fahrgemeinschaft");
//        mail.setType("plain/text");
        return mail;
    }

    public LabeledIntent labeledIntent(Intent intent, int icon, String label) {
        System.out.println("resolving " + label);
        PackageManager pm = getPackageManager();
        ComponentName cmp = intent.resolveActivity(pm);
        if (cmp != null) {
            System.out.println("found " + cmp);
            intent.setComponent(cmp);
            Intent resolved = new Intent();
            resolved.setData(intent.getData());
            resolved.setComponent(cmp);
            resolved.setAction(intent.getAction());
            if (intent.getExtras() != null)
                resolved.putExtras(intent.getExtras());
            return new LabeledIntent(intent, getPackageName(), label, icon);
        }
        return null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("selected", selected);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        case R.id.profile:
            startActivity(new Intent(this, SettingsActivity.class)
                    .putExtra("profile", true));
            return true;
        case android.R.id.home:
            finish();
//            Intent intent = new Intent(this, MainActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            startActivity(intent);
            return true;
        
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {}

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {}

}
