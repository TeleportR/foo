/**
 * Fahrgemeinschaft / Ridesharing App
 * Copyright (c) 2013 by it's authors.
 * Some rights reserved. See LICENSE.. 
 *
 */

package de.fahrgemeinschaft;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.teleportr.Ride;
import org.teleportr.Ride.COLUMNS;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageCache;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.calciumion.widget.BasePagerAdapter;

public class RideDetailsFragment extends SherlockFragment
        implements Response.ErrorListener {

    private static final String TAG = "Details";
    private static final SimpleDateFormat day =
            new SimpleDateFormat("EE", Locale.GERMANY);
    private static final SimpleDateFormat lrdate =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY);
    private static final SimpleDateFormat lwdate =
            new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
    private static final SimpleDateFormat lwhdate =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY);
    private static final SimpleDateFormat date =
            new SimpleDateFormat("dd.MM.", Locale.GERMANY);
    private static SimpleDateFormat time =
            new SimpleDateFormat("HH:mm", Locale.GERMANY);
    private ViewPager pager;
    private RequestQueue queue;
    private Cursor cursor;
    private int selected;
    private static ImageLoader imageLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        final LruCache<String, Bitmap> mImageCache =
                new LruCache<String, Bitmap>(20);

        ImageCache imageCache = new ImageCache() {
            @Override
            public void putBitmap(String key, Bitmap value) {
                mImageCache.put(key, value);
            }

            @Override
            public Bitmap getBitmap(String key) {
                return mImageCache.get(key);
            }
        };

        queue = Volley.newRequestQueue(getActivity());
        imageLoader = new ImageLoader(queue, imageCache);
        queue.start();
    }

    @Override
    public View onCreateView(final LayoutInflater lI, ViewGroup p, Bundle b) {
        return lI.inflate(R.layout.fragment_ride_details, p, false);
    }

    @Override
    public void onViewCreated(View layout, Bundle savedInstanceState) {
        super.onViewCreated(layout, savedInstanceState);
        pager = (ViewPager) layout.findViewById(R.id.pager);
        pager.setAdapter(new BasePagerAdapter() {
            
            @Override
            public int getCount() {
                if (cursor == null)
                    return 0;
                else
                    return cursor.getCount();
            }
            
            @Override
            protected View getView(Object position, View v, ViewGroup parent) {
                if (v == null) {
                    v = getActivity().getLayoutInflater()
                            .inflate(R.layout.view_ride_details, null, false);
                }
                RideView view = (RideView) v;
                
                if (view.content.getChildCount() > 4)
                    view.content.removeViews(1, view.content.getChildCount()-4);
                cursor.moveToPosition((Integer) position);

                view.userId = cursor.getString(COLUMNS.WHO);
                view.name.setText("");
                view.url = null;
                view.from_place.setText(cursor.getString(COLUMNS.FROM_ADDRESS));
                view.to_place.setText(cursor.getString(COLUMNS.TO_ADDRESS));

                Date timestamp = new Date(cursor.getLong(COLUMNS.DEPARTURE));
                view.day.setText(day.format(timestamp));
                view.date.setText(date.format(timestamp));
                view.time.setText(time.format(timestamp));

                view.price.setText("" + (cursor.getInt(COLUMNS.PRICE) / 100));
                switch(cursor.getInt(COLUMNS.SEATS)){
                case 0:
                    ((ImageView) v.findViewById(R.id.seats_icon))
                            .setImageResource(R.drawable.icn_seats_white_full);
                    break;
                case 1:
                    ((ImageView) v.findViewById(R.id.seats_icon))
                            .setImageResource(R.drawable.icn_seats_white_1);
                    break;
                case 2:
                    ((ImageView) v.findViewById(R.id.seats_icon))
                            .setImageResource(R.drawable.icn_seats_white_2);
                    break;
                case 3:
                    ((ImageView) v.findViewById(R.id.seats_icon))
                            .setImageResource(R.drawable.icn_seats_white_3);
                    break;
                default:
                    ((ImageView) v.findViewById(R.id.seats_icon))
                            .setImageResource(R.drawable.icn_seats_white_many);
                    break;
                }
                try {
                    System.out.println("foo" + cursor.getString(COLUMNS.DETAILS));
                    view.details.setText(
                            Ride.getDetails(cursor).getString("comment"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                getActivity().getSupportLoaderManager()
                    .initLoader((int) cursor.getLong(0), null, view);
                
                view.avatar.setImageResource(R.drawable.icn_view_user);
                
                queue.add(new ProfileRequest(cursor.getString(COLUMNS.WHO),
                        view, RideDetailsFragment.this));
                
                return view;
            }

            @Override
            protected Object getItem(int position) {
                return position;
            }
        });
    }

    public void swapCursor(Cursor cursor) {
        this.cursor = cursor;
        if (pager != null)
            pager.getAdapter().notifyDataSetChanged();
    }

    public void setSelection(int position) {
        selected = position;
    }

    public int getSelection() {
        return selected;
    }

    @Override
    public void onResume() {
        super.onResume();
        pager.setCurrentItem(selected);
        pager.setOnPageChangeListener((OnPageChangeListener) getActivity());
    }



    static class RideView extends RelativeLayout
        implements LoaderCallbacks<Cursor>, Response.Listener<JSONObject> {

        TextView from_place;
        TextView to_place;
        TextView price;
        TextView day;
        TextView date;
        TextView time;
        TextView details;
        LinearLayout content;
        ImageView avatar;
        String userId;
        TextView name;
        private String url;
        TextView reg_date;
        TextView last_login;

        public RideView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            from_place = (TextView)
                    ((FrameLayout) findViewById(R.id.from_place)).getChildAt(1);
            FrameLayout to = (FrameLayout) findViewById(R.id.to_place);
            to_place = (TextView) to.getChildAt(1);
            ((ImageView)to.getChildAt(0)).setImageResource(R.drawable.shape_to);
            price = (TextView) findViewById(R.id.price);
            day = (TextView) findViewById(R.id.day);
            date = (TextView) findViewById(R.id.date);
            time = (TextView) findViewById(R.id.time);
            details = (TextView) findViewById(R.id.details);
            content = (LinearLayout) findViewById(R.id.content);
            avatar = (ImageView)findViewById(R.id.avatar);
            name = (TextView) findViewById(R.id.driver_name);
            reg_date = (TextView) findViewById(R.id.driver_registration_date);
            last_login = (TextView) findViewById(R.id.driver_active_date);
            
            avatar.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (url != null)
                        new ImageDialog(getContext()).show();
                }
            });
        }

        class ImageDialog extends Dialog implements OnClickListener {
            
            public ImageDialog(Context context) {
                super(context, R.style.ProfilePictureFullscreen);
            }

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                setContentView(R.layout.view_big_image);
                ImageView image = (ImageView) findViewById(R.id.image_large);
                image.setImageResource(R.drawable.ic_call);
                imageLoader.get(url, ImageLoader.getImageListener(image,
                              R.drawable.ic_loading, R.drawable.icn_view_none));
                ScaleAnimation s = new ScaleAnimation(0.3f, 1, 0.3f, 1);
                s.setDuration(300);
                s.setFillAfter(true);
                image.startAnimation(s);
                image.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                dismiss();
            };
        }

        @Override
        public Loader<Cursor> onCreateLoader(int ride_id, Bundle b) {
            Log.d(TAG, "loading subrides for ride " + ride_id);
            return new CursorLoader(getContext(), Uri.parse(
                    "content://de.fahrgemeinschaft/rides/" + ride_id + "/rides")
                    ,null, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> l, Cursor c) {
            Log.d(TAG, "finished loading subrides " + l.getId());
            for (int i = 1; i < c.getCount(); i++) {
                c.moveToPosition(i);
                FrameLayout view = (FrameLayout)
                        LayoutInflater.from(getContext())
                        .inflate(R.layout.view_place_bubble, null, false);
                ((TextView) view.getChildAt(1))
                        .setText("- " + c.getString(COLUMNS.FROM_NAME));
                ((ImageView) view.getChildAt(0))
                        .setImageResource(R.drawable.shape_via);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                view.setLayoutParams(lp);
                content.addView(view, i);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> l) {
            Log.d(TAG, "loader " + l.getId() + "reset.");
        }

        @Override
        public void onResponse(JSONObject json) {
            try {
              JSONObject user = json.getJSONObject("user");
              Log.d(TAG, "profile downloaded " + user.get("UserID"));
              if (user.getString("UserID").equals(userId)) {
                  JSONArray kvp = user.getJSONArray("KeyValuePairs");
                  name.setText(kvp.getJSONObject(1).getString("Value") + " " 
                          + kvp.getJSONObject(2).getString("Value"));
                  Date since = lrdate.parse(user.getString("RegistrationDate"));
                  Date logon = lrdate.parse(user.getString("LastvisitDate"));
                  reg_date.setText("Dabei seit: " + lwdate.format(since));
                  last_login.setText("Letzter Login: " + lwhdate.format(logon));
//                  JSONArray rgd = user.getJSONArray("RegistrationDate");
//                  reg_date.setText(user.getJSONArray("").getString("Value") + " " 
//                          + kvp.getJSONObject(2).getString("Value"));
//                  Log.d(TAG, kvp.toString());
                  if (!user.isNull("AvatarPhoto")) {
                      JSONObject photo = user.getJSONObject("AvatarPhoto");
                      String id = photo.getString("PhotoID");
                      String path = photo.getString("PathTo");
                      url = "http://service.fahrgemeinschaft.de//"
                              + "ugc/pa/" + path +"/"+ id + "_big.jpg";
                      imageLoader.get(url, ImageLoader.getImageListener(avatar,
                              R.drawable.ic_loading, R.drawable.icn_view_none));
                  }
              }
          } catch (JSONException e) {
              e.printStackTrace();
          } catch (ParseException e) {
              e.printStackTrace();
          }
        }
    }

    @Override
    public void onErrorResponse(VolleyError err) {
        Log.d(TAG, err.toString());
        err.printStackTrace();
    }

    static class ProfileRequest extends JsonObjectRequest { 

        private static HashMap<String, String> headers;

        static {
            headers = new HashMap<String, String>();  
            headers.put("apikey", FahrgemeinschaftConnector.APIKEY);  
        }

        public ProfileRequest( String userid,
                Listener<JSONObject> listener, ErrorListener errorListener) {
            super(Method.GET, "http://service.fahrgemeinschaft.de/user/"
                + userid, null, listener, errorListener);
            setShouldCache(Boolean.TRUE);
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return headers;
        };
        
        @Override
        protected Response<JSONObject> parseNetworkResponse(NetworkResponse res) {
            return Response.success(super.parseNetworkResponse(res).result,
                    parseIgnoreCacheHeaders(res));
            
        }
    }

    public static Cache.Entry parseIgnoreCacheHeaders(NetworkResponse response) {
        long now = System.currentTimeMillis();

        Map<String, String> headers = response.headers;
        long serverDate = 0;
        String serverEtag = null;
        String headerValue;

        headerValue = headers.get("Date");
        if (headerValue != null) {
//            serverDate = parseDateAsEpoch(headerValue);
        }

        serverEtag = headers.get("ETag");

        final long cacheHitButRefreshed = 3 * 60 * 1000; // in 3 minutes cache will be hit, but also refreshed on background
        final long cacheExpired = 24 * 60 * 60 * 1000; // in 24 hours this cache entry expires completely
        final long softExpire = now + cacheHitButRefreshed;
        final long ttl = now + cacheExpired;

        Cache.Entry entry = new Cache.Entry();
        entry.data = response.data;
        entry.etag = serverEtag;
        entry.softTtl = softExpire;
        entry.ttl = ttl;
        entry.serverDate = serverDate;
        entry.responseHeaders = headers;

        return entry;
    }
}
