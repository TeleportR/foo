package de.fahrgemeinschaft.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import de.fahrgemeinschaft.R;

public class AutoCompletePicker extends AutoCompleteTextView
        implements OnFocusChangeListener, View.OnClickListener {

    private boolean erstesmal = true;

    public AutoCompletePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnFocusChangeListener(this);
        setOnClickListener(this);
    }
    
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            showDropDown();
            if (getAdapter() != null)
                performFiltering("", 0);
        }
    }

    @Override
    public void onClick(View v) {
        showDropDown();
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }

    @Override
    public void onFilterComplete(int count) {
        System.out.println("complete " + count);
        if (erstesmal) {
            Cursor cursor = (Cursor) getAdapter().getItem(0);
            if (cursor != null && cursor.getCount() > 0)
                replaceText((cursor).getString(1));
        }
        erstesmal = false;
        super.onFilterComplete(count);
    }

    public void setAutocompleteUri(final Uri uri) {
        setAdapter(new CursorAdapter(getContext(), null, false) {

            @Override
            public View newView(Context ctx, Cursor c, ViewGroup r) {
                return LayoutInflater.from(ctx).inflate(
                        R.layout.contacts_list_entry, r, false);
            }

            @Override
            public void bindView(View v, Context arg1, Cursor c) {
                ((TextView) v.findViewById(R.id.contacts_value))
                        .setText(c.getString(1));
            }

            @Override
            public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
                if (constraint == null) constraint = "";
                return getContext().getContentResolver()
                        .query(uri.buildUpon().appendQueryParameter(
                                "q", constraint.toString()).build(),
                                null, null, null, null);
            }

            @Override
            public CharSequence convertToString(Cursor cursor) {
                return cursor.getString(1);
            }
        });
        performFiltering("", 0);
    }
}
