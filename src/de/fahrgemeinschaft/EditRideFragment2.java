/**
 * Fahrgemeinschaft / Ridesharing App
 * Copyright (c) 2013 by it's authors.
 * Some rights reserved. See LICENSE.. 
 *
 */

package de.fahrgemeinschaft;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

public class EditRideFragment2 extends SherlockFragment 
        implements OnClickListener, OnDateSetListener {

    private static final String TAG = "Fahrgemeinschaft";
    private LinearLayout recurrence;

    @Override
    public View onCreateView(final LayoutInflater lI, ViewGroup p, Bundle b) {
        return lI.inflate(R.layout.fragment_ride_edit2, p, false);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        v.findViewById(R.id.btn_pick_date).setOnClickListener(this);
        v.findViewById(R.id.ic_pick_date).setOnClickListener(this);
        
        String[] weekDays = new DateFormatSymbols().getShortWeekdays();
        recurrence = (LinearLayout) v.findViewById(R.id.recurrence);
        LayoutParams layoutParams = dayButtonlayoutParams();
        for (int i = 1; i < weekDays.length; i++) {
            TextView day = makeDayButton(getActivity());
            day.setText(weekDays[i]);
            recurrence.addView(day);
            System.out.println(weekDays[i]);
        }
    }

    private TextView makeDayButton(Context ctx) {
        TextView day = new TextView(ctx);
        day.setOnClickListener(toggleSelectedState);
        day.setLayoutParams(dayButtonlayoutParams());
        day.setTextAppearance(getActivity(), R.style.dark_Bold);
        day.setBackgroundResource(R.drawable.btn_day);
        day.setGravity(Gravity.CENTER);
        return day;
    }


    OnClickListener toggleSelectedState = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            v.setSelected(!v.isSelected());
        }
    };

    @Override
    public void onClick(View v) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog d = new DatePickerDialog(
                getActivity(), this, year, month, day);
        d.setButton(DatePickerDialog.BUTTON_POSITIVE, getString(R.string.ready),
                (android.content.DialogInterface.OnClickListener) null); //java!
        d.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        Toast.makeText(getActivity(), "Date changed", 300).show();
    }



    private LayoutParams dayButtonlayoutParams() {
        LayoutParams lp = new LayoutParams(0, LayoutParams.MATCH_PARENT);
        int margin = getActivity().getResources() // dips
                .getDimensionPixelSize(R.dimen.padding_small);
        lp.leftMargin = margin;
        lp.rightMargin = margin;
        lp.topMargin = margin;
        lp.bottomMargin = margin;
        lp.weight = 1;
        return lp;
    }
}
