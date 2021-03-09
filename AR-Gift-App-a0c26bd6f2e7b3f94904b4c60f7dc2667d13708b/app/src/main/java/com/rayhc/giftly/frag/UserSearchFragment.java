package com.rayhc.giftly.frag;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.rayhc.giftly.R;
import com.rayhc.giftly.util.User;
import com.rayhc.giftly.util.UserManager;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.android.gms.tasks.Tasks.await;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserSearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserSearchFragment extends Fragment {
    private User currentUser;
    private String displayUserID;

    public UserSearchFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UserSearchFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UserSearchFragment newInstance(String param1, String param2) {
        UserSearchFragment fragment = new UserSearchFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        displayUserID = sharedPref.getString("userId",null);
        Query query = db.child("users").orderByChild("userId").equalTo(displayUserID);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = UserManager.snapshotToUser(snapshot, displayUserID);
                Log.d("iandebug", "current User: " + currentUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_user_search, container, false);
        ListView lv = (ListView) view.findViewById(R.id.resultList);
        EditText et = (EditText) view.findViewById(R.id.editTextTextPersonName);

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }


            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                new Thread(){
                    List<User> users;
                    Runnable runnable = () -> {
                        users = users.stream()
                                .filter(u -> !u.getFriends().containsKey(displayUserID))
                                .filter(u -> !u.getReceivedFriends().containsKey(displayUserID))
                                .collect(Collectors.toList());
                        replaceAdapter();
                        lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                if(currentUser != null){
                                    UserManager.sendFriendRequest(currentUser, users.get(position).getUserId());
                                    Toast toast = Toast.makeText(getContext(), "Sent friend request to " + users.get(position).getName(), Toast.LENGTH_SHORT);
                                    toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
                                    toast.show();

                                    users.remove(position);
                                    replaceAdapter();
                                }
                            }
                        });
                    };

                    public void replaceAdapter(){
                        String[] items = users.stream().map(User::getName).toArray(String[]::new);
                        ArrayAdapter adapter = new ArrayAdapter<String>(getActivity(),
                                android.R.layout.simple_list_item_1,
                                items);
                        lv.setAdapter(adapter);
                    }

                    Handler handler = new Handler();

                    @Override
                    public void run(){
                        try {
                            users = UserManager.searchUsersByEmail(s.toString());
                            Log.d("iandebug", "" + users.size());
                            handler.post(runnable);
                        } catch (Exception e) {
                            Log.d("iandebug", "" + e);
                            e.printStackTrace();
                        }
                    }
                }.start();
            }

            @Override
            public void afterTextChanged(Editable s){}

        });
        return view;
    }
}