package com.rayhc.giftly.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.rayhc.giftly.R;

import java.util.ArrayList;
import java.util.Collections;

public class LinkAdapter extends ArrayAdapter<String> {

    String packageName;
    Resources res;
    ArrayList<String> links;
    LayoutInflater inflater;

    public LinkAdapter(@NonNull Context context, int rInt, ArrayList<String> links) {
        super(context, rInt, links);

        packageName = context.getPackageName();
        res = context.getResources();
        this.links = links;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return links.size();
    }
    public String getItem(int pos) { return links.get(pos); }
    public long getItemId(int pos) {
        return pos;
    }

    // Create list view that shows details about each entry.
    @Override
    public View getView (int pos, View convertView, ViewGroup parent) {
        View view = convertView;

        if (convertView == null) { // if not already cached
            view = inflater.inflate(R.layout.link_entry, null); // make a new view
        }

        TextView text = view.findViewById(R.id.link_url);
        text.setText(getItem(pos));

        return view;
    }
}

