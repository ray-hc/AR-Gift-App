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
    boolean hideArrowTrue;

    public GiftAdapter(@NonNull Context context, int rInt, ArrayList<String> giftTitles) {
        super(context, rInt, giftTitles);

        packageName = context.getPackageName();
        res = context.getResources();
        this.giftTitles = giftTitles;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // make a randomized list of the color options.
        colors = new ArrayList<>();

        // a brute fix for occasional IOB errors
        for (int drawId : GIFT_COLORS) {
            colors.add(drawId);
        }

        for (int i = 0; i < (giftTitles.size()); i++) {
            colors.add(GIFT_COLORS[i%(Math.min(GIFT_COLORS.length, giftTitles.size()))]);
        }
    }

    public void hideArrow(boolean val) {
        hideArrowTrue = val;
        Log.d("rhc","arrow hid");
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
            String[] giftTitleSplit = giftTitle.split("\\|");
            Log.d("LPC", "gift title: "+giftTitle+", at pos: "+pos);


            //truncate last 3 characters in giftTitleSplit[1] as opened tags
            TextView senderTitle = view.findViewById(R.id.sender_info);
            TextView msgTitle = view.findViewById(R.id.message_info);

            if(giftTitleSplit.length ==2) {
                senderTitle.setText(giftTitleSplit[0]);
                msgTitle.setText(giftTitleSplit[1]);
            }
            else msgTitle.setText(giftTitleSplit[0]);

            ImageView imageView = view.findViewById(R.id.gift_icon);
            imageView.setImageResource(
                    colors.get(pos)
            );

            // Replace opened gift images with confetti :)
            if (giftTitleSplit.length == 2 && giftTitleSplit[1].endsWith(Globals.OLD_GIFT)) {
                    imageView.setImageResource(R.drawable.confetti);
                    msgTitle.setText(giftTitleSplit[1].substring(
                            0, giftTitleSplit[1].length() - Globals.OLD_GIFT.length()));
            } else if (giftTitleSplit.length == 2 && giftTitleSplit[1].endsWith(Globals.NEW_GIFT)) {
                msgTitle.setText(giftTitleSplit[1].substring(
                        0, giftTitleSplit[1].length() - Globals.OLD_GIFT.length()));
            }

            if (hideArrowTrue) {
                View arrow = view.findViewById(R.id.arrowClick);
                arrow.setVisibility(View.INVISIBLE);
            }
        }
        return view;
    }
}

