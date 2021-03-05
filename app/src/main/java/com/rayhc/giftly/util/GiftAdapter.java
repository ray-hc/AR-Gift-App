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

public class GiftAdapter extends ArrayAdapter<String> {

    public static final int[] GIFT_COLORS = new int[] {R.drawable.gift_blue, R.drawable.gift_green,
            R.drawable.gift_pink, R.drawable.gift_yellow, R.drawable.gift_purp};

    String packageName;
    Resources res;
    ArrayList<Integer> colors;
    ArrayList<String> giftTitles;
    LayoutInflater inflater;

    public GiftAdapter(@NonNull Context context, int rInt, ArrayList<String> giftTitles) {
        super(context, rInt, giftTitles);

        packageName = context.getPackageName();
        res = context.getResources();
        this.giftTitles = giftTitles;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // make a randomized list of the color options.
        colors = new ArrayList<>();

        for (int drawId : GIFT_COLORS) {
            colors.add(drawId);
            Log.d(Globals.TAG, ""+drawId);
        }
        Collections.shuffle(colors);

        //
        for (int i = 0; i < giftTitles.size() - colors.size(); i++) {
            colors.add(colors.get(i % colors.size()));
        }

        Log.d(Globals.TAG, colors.size()+" is size of colors");
    }

    public int getCount() {
        return giftTitles.size();
    }
    public String getItem(int pos) {
        return giftTitles.get(pos);
    }
    public long getItemId(int pos) {
        return pos;
    }

    // Create list view that shows details about each entry.
    @Override
    public View getView (int pos, View convertView, ViewGroup parent) {
        View view = convertView;

        if (convertView == null) { // if not already cached
            view = inflater.inflate(R.layout.single_gift, null); // make a new view
            String giftTitle = getItem(pos);

            TextView entryTitle = view.findViewById(R.id.sender_info);
            entryTitle.setText(giftTitle);

            ImageView imageView = view.findViewById(R.id.gift_icon);
            imageView.setImageResource(
                    colors.get(pos)
            );
        }
        return view;
    }
}

