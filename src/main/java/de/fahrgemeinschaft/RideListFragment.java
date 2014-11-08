/**
 * Fahrgemeinschaft / Ridesharing App
 * Copyright (c) 2013 by it's authors.
 * Some rights reserved. See LICENSE..
 *
 */

package de.fahrgemeinschaft;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.teleportr.ConnectorService;
import org.teleportr.ConnectorService.ServiceCallback;
import org.teleportr.Ride;
import org.teleportr.Ride.COLUMNS;
import org.teleportr.Ride.Mode;
import org.teleportr.RidesProvider;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.fahrgemeinschaft.ContactProvider.CONTACT;
import de.fahrgemeinschaft.util.RideRowView;
import de.fahrgemeinschaft.util.SpinningZebraListFragment;
import de.fahrgemeinschaft.util.Util;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class RideListFragment extends SpinningZebraListFragment
            implements ServiceCallback<Ride> {

    private static final SimpleDateFormat date =
            new SimpleDateFormat("dd.MM.", Locale.GERMANY);
    private static final SimpleDateFormat day =
            new SimpleDateFormat("EEE", Locale.GERMANY);
    private static final String WHILE = " while ";
    private static final String SPACE = " ";
    private static final String EMPTY = "";
    private long currently_searching_date;
    private boolean isItThisFragment;
    private String[] split;

    @Override
    public void bindListItemView(View view, Cursor ride) {
        RideView v = (RideView) view;
        v.id = ride.getInt(0);
        v.from_place.setText(ride.getString(COLUMNS.FROM_NAME));
        split = ride.getString(COLUMNS.FROM_ADDRESS).split(", ");
        if (split.length > 1)
            v.from_city.setText(split[1]);
        else
            v.from_city.setText(ride.getString(COLUMNS.FROM_ADDRESS));

        v.to_place.setText(ride.getString(COLUMNS.TO_NAME));
        split = ride.getString(COLUMNS.TO_ADDRESS).split(", ");
        if (split.length > 1)
            v.to_city.setText(split[1]);
        else
            v.to_city.setText(ride.getString(COLUMNS.TO_ADDRESS));

        v.row.bind(ride, getActivity());

        long dep = ride.getLong(COLUMNS.DEPARTURE);
        if (isMyRide(ride)) {
            if ((isFuture(dep) && isActive(ride)) || isReoccuring(ride)) {
                v.showButtons();
                v.streifenhoernchen.setVisibility(View.GONE);
                v.grey_bg.setVisibility(View.GONE);
            } else {
                v.hideButtons();
                v.streifenhoernchen.setVisibility(View.VISIBLE);
                v.grey_bg.setVisibility(View.VISIBLE);
            }
        } else {
            v.hideButtons();
            v.streifenhoernchen.setVisibility(View.GONE);
            v.grey_bg.setVisibility(View.GONE);
        }
        dep = dep - currently_searching_date; // refresh spinning wheel
        if (ride.getShort(COLUMNS.DIRTY) > 0 || dep > 0 && dep < 24*3600000) {
            v.loading.setVisibility(View.VISIBLE);
        } else {
            v.loading.setVisibility(View.GONE);
        }
        if (ride.getString(COLUMNS.MODE).equals(Mode.CAR.name())) {
            v.mode.setImageResource(R.drawable.icn_mode_car);
        } else {
            v.mode.setImageResource(R.drawable.icn_mode_train);
        }
    }

    private boolean isReoccuring(Cursor ride) {
        return ride.getInt(COLUMNS.TYPE)
                    == FahrgemeinschaftConnector.TYPE_OFFER_REOCCURING;
    }

    private boolean isActive(Cursor ride) {
        return (ride.getInt(COLUMNS.ACTIVE) == 1);
    }

    private boolean isMyRide(Cursor ride) {
        return (ride.getString(COLUMNS.WHO).equals(EMPTY) ||
                ride.getString(COLUMNS.WHO).equals(PreferenceManager
                        .getDefaultSharedPreferences(getActivity())
                        .getString(CONTACT.USER, EMPTY)));
    }

    @Override
    public void onFail(Ride query, String reason) {
        if (onScreen) {
            Crouton.makeText(getActivity(), reason + WHILE
                    + day.format(query.getDep()) + SPACE
                    + date.format(query.getDep()), Style.ALERT).show();
            stopSpinning(reason);
        }
        currently_searching_date = 0;
    }

    @Override
    public void onProgress(Ride query, int how) {
        if (onScreen) {
            currently_searching_date = query.getDep();
            startSpinning(getString(R.string.searching),
                    day.format(currently_searching_date) + SPACE
                            + date.format(currently_searching_date));
        }
    }

    @Override
    public void onSuccess(Ride query, int numberOfRidesFound) {
        if (onScreen) {
            if (numberOfRidesFound == 0) {
                Toast.makeText(getActivity(), 
                        getString(R.string.nothing) + SPACE
                                + day.format(currently_searching_date) + SPACE
                                + date.format(currently_searching_date),
                                Toast.LENGTH_SHORT).show();
            }
            stopSpinning(getString(R.string.search_continue));
        }
        currently_searching_date = 0;
    }


    @Override
    public void onCreateContextMenu(ContextMenu m, View v, ContextMenuInfo i) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(((AdapterView.AdapterContextMenuInfo)i).position);
        getActivity().getMenuInflater().inflate(R.menu.ride_actions, m);
        if (isMyRide(cursor)) {
            MenuItem toggle_active = m.findItem(R.id.toggle_active);
            if (cursor.getInt(COLUMNS.ACTIVE) == 1) {
                toggle_active.setTitle(R.string.deactivate);
            } else {
                toggle_active.setTitle(R.string.activate);
            }
        } else {
            m.findItem(R.id.edit).setVisible(false);
            m.findItem(R.id.delete).setVisible(false);
            m.findItem(R.id.duplicate).setVisible(false);
            m.findItem(R.id.toggle_active).setVisible(false);
            m.findItem(R.id.duplicate_retour).setVisible(false);
        }
        isItThisFragment = true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!isItThisFragment) {
            return false;
        }
        isItThisFragment = false;
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        Cursor cursor = getCursor();
        cursor.moveToPosition(info.position);
        Ride ride = new Ride(cursor, getActivity());
        return Util.handleRideAction(item.getItemId(), ride, getActivity());
    }



    public static class RideView extends RelativeLayout
        implements OnClickListener, OnLongClickListener {

        View streifenhoernchen;
        View grey_bg;
        ProgressBar loading;
        TextView from_place;
        TextView from_city;
        TextView to_place;
        TextView to_city;
        RideRowView row;
        ImageView mode;
        int id;

        public RideView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            streifenhoernchen = findViewById(R.id.streifenhoernchen);
            Util.fixStreifenhoernchen(streifenhoernchen);
            from_place = (TextView) findViewById(R.id.from_place);
            from_city = (TextView) findViewById(R.id.from_city);
            loading = (ProgressBar) findViewById(R.id.loading);
            to_place = (TextView) findViewById(R.id.to_place);
            to_city = (TextView) findViewById(R.id.to_city);
            row = (RideRowView) findViewById(R.id.row);
            mode = (ImageView) findViewById(R.id.mode);
            grey_bg = findViewById(R.id.grey_bg);
        }

        public void showButtons() {
            findViewById(R.id.stub).setVisibility(View.VISIBLE);
            View edit = findViewById(R.id.edit);
            edit.setFocusable(false);
            edit.setOnClickListener(this);
            edit.setOnLongClickListener(this);
            findViewById(R.id.increase_seats).setOnClickListener(this);
            findViewById(R.id.decrease_seats).setOnClickListener(this);
            findViewById(R.id.increase_seats).setFocusable(false);
            findViewById(R.id.decrease_seats).setFocusable(false);
        }

        public void hideButtons() {
            findViewById(R.id.stub).setVisibility(View.GONE);
        }

        @Override
        public void onClick(View v) {
            Ride ride = new Ride(id, getContext());
            switch (v.getId()) {
            case R.id.edit:
                Util.handleRideAction(R.id.edit, ride, getContext());
                break;
            case R.id.increase_seats:
                if (ride.getSeats() <= 3) { // 4 is max / means many
                    ride.seats(ride.getSeats() + 1).dirty().store(getContext());
                    getContext().startService(
                        new Intent(getContext(), ConnectorService.class)
                                .setAction(ConnectorService.PUBLISH));
                }
                getContext().getContentResolver().update(RidesProvider
                        .getRidesUri(getContext()), null, null, null);
                break;
            case R.id.decrease_seats:
                if (ride.getSeats() >= 1) {
                    ride.seats(ride.getSeats() - 1).dirty().store(getContext());
                    getContext().startService(
                        new Intent(getContext(), ConnectorService.class)
                        .setAction(ConnectorService.PUBLISH));
                }
                getContext().getContentResolver().update(RidesProvider
                        .getRidesUri(getContext()), null, null, null);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            ((ListView) getParent()).showContextMenuForChild(this);
            return false;
        }
    }
}