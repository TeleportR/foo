/**
 * Fahrgemeinschaft / Ridesharing App
 * Copyright (c) 2013 by it's authors.
 * Some rights reserved. See LICENSE..
 *
 */

package de.fahrgemeinschaft;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

public abstract class EndlessSpinningZebraListFragment
    extends SherlockListFragment implements LoaderCallbacks<Cursor> {

    abstract void bindListItemView(View view, Cursor cursor);

    private boolean spinningEnabled = true;

    public boolean isSpinningEnabled() {
        return spinningEnabled;
    }

    public void setSpinningEnabled(boolean spinningEnabled) {
        this.spinningEnabled = spinningEnabled;
    }

    private boolean spinning;
    private View wheel;
    private RotateAnimation rotate;
    private Uri uri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(final LayoutInflater lI, ViewGroup p, Bundle b) {
        return lI.inflate(R.layout.fragment_ride_list, p, false);
    }

    @Override
    public void onViewCreated(View layout, Bundle savedInstanceState) {
        super.onViewCreated(layout, savedInstanceState);

        setListAdapter(new CursorAdapter(getActivity(), null, 0) {

            @Override
            public int getCount() {
                if (spinningEnabled)
                    return super.getCount() + 1;
                else return super.getCount();
            }

            @Override
            public int getViewTypeCount() {
                if (spinningEnabled) return 2;
                else return 1;
            }

            @Override
            public int getItemViewType(int position) {
                if (!spinningEnabled || position < getCount() - 1)
                    return 0;
                else
                    return 1;
            }

            @Override
            public View getView(int position, View v, ViewGroup parent) {
                if (!spinningEnabled || position < getCount() - 1)
                    v = super.getView(position, v, parent);
                else {
                    if (v == null) {
                        v = getLayoutInflater(null).inflate(
                                R.layout.loading, parent, false);
                        if (spinning) startSpinning();
                        else stopSpinning();
                    }
                }
                if (position % 2 == 0) {
                    v.setBackgroundColor(getResources().getColor(
                            R.color.medium_green));
                } else {
                    v.setBackgroundColor(getResources().getColor(
                            R.color.almost_medium_green));
                }
                return v;
            }
            
            @Override
            public View newView(Context ctx, Cursor rides, ViewGroup parent) {
                return getLayoutInflater(null).inflate(
                        R.layout.view_ride_list_entry, parent, false);
            }
            
            @Override
            public void bindView(View view, Context ctx, Cursor ride) {
                if (ride.getPosition() == ride.getCount()) return;
                bindListItemView(view, ride);
            }
        });
        rotate = new RotateAnimation(
                0f, 360f, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(600);
        rotate.setRepeatMode(Animation.RESTART);
        rotate.setRepeatCount(Animation.INFINITE);

        if (uri != null) {
            getLoaderManager().initLoader(0, null, this);
        }
    }

    public void load(Uri uri) {
        this.uri = uri;
        if (getActivity() != null)
            getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle b) {
        Log.d("DEBUG", "create loader " + uri);
        return new CursorLoader(getActivity(), uri, null, null, null, null);
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor rides) {
        ((ListFragmentCallback) getActivity()).onLoadFinished(this, rides);
        ((CursorAdapter) getListAdapter()).swapCursor(rides);
    }

    public void startSpinning() {
        if (!spinning && getView() != null) {
            wheel = getView().findViewById(R.id.progress);
            if (wheel != null) wheel.startAnimation(rotate);
        }
        spinning = true;
    }

    public void stopSpinning() {
        if (wheel != null) {
            wheel.clearAnimation();
            spinning = false;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // TODO Auto-generated method stub
    }
    public interface ListFragmentCallback {
        public void onLoadFinished(Fragment fragment, Cursor cursor);
        public void onListItemClick(int position);
        public void onSpinningWheelClick();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Cursor c = ((CursorAdapter) getListView().getAdapter()).getCursor();
        if (position != c.getCount()) {
            ((ListFragmentCallback) getActivity()).onListItemClick(position);
        } else {
            ((ListFragmentCallback) getActivity()).onSpinningWheelClick();
        }
    }
}
