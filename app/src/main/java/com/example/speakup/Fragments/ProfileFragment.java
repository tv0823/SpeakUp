package com.example.speakup.Fragments;

import static android.content.Context.MODE_PRIVATE;
import static com.example.speakup.FBRef.refAuth;
import static com.example.speakup.FBRef.refRecordings;
import static com.example.speakup.FBRef.refUsers;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.speakup.Activities.HelpAndAboutActivity;
import com.example.speakup.Activities.WelcomeScreenActivity;
import com.example.speakup.Objects.Recording;
import com.example.speakup.Objects.User;
import com.example.speakup.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {
    private String uid;

    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment ProfileFragment.
     */
    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView userNameTv = view.findViewById(R.id.userNameTv);
        TextView dateTv = view.findViewById(R.id.dateTv);
        TextView avgScoreTv = view.findViewById(R.id.avgScoreTv);
        TextView recordingCountTv = view.findViewById(R.id.recordingCountTv);
        MaterialButton btnHelpAndAbout = view.findViewById(R.id.btnHelp);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        // Check if user is logged in to avoid crash
        if (refAuth.getCurrentUser() != null) {
            uid = refAuth.getCurrentUser().getUid();
        }

        setUserData(userNameTv, dateTv);
        setRecordingCountAndAvgScore(recordingCountTv, avgScoreTv);

        btnHelpAndAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireActivity(), HelpAndAboutActivity.class);
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logOut(v);
            }
        });

        return view;
    }

    /**
     * Fetches and displays basic user information like username and join date.
     *
     * @param userNameTv The TextView to display the username.
     * @param dateTv     The TextView to display the join date.
     */
    private void setUserData(TextView userNameTv, TextView dateTv) {
        ProgressDialog pD = new ProgressDialog(requireActivity());
        pD.setMessage("Loading user data...");
        pD.setCancelable(false);
        pD.show();

        refUsers.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dS) {
                User user = dS.getValue(User.class);
                if (user != null) {
                    userNameTv.setText(user.getUsername());

                    FirebaseUserMetadata metadata = refAuth.getCurrentUser().getMetadata();
                    if (metadata != null) {
                        long creationTimestamp = metadata.getCreationTimestamp();
                        Calendar creationDate = Calendar.getInstance();
                        creationDate.setTimeInMillis(creationTimestamp);

                        String dateStr = String.format("Joined %d/%d/%d",
                                creationDate.get(Calendar.DAY_OF_MONTH),
                                (creationDate.get(Calendar.MONTH) + 1),
                                creationDate.get(Calendar.YEAR));

                        dateTv.setText(dateStr);
                    }
                }
                pD.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                pD.dismiss();
            }
        });
    }

    private void setRecordingCountAndAvgScore(TextView recordingCountTv, TextView avgScoreTv) {
        refRecordings.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dS) {
                int totalCount = 0;
                double totalScore = 0;

                // Iterate through questionId nodes
                for (DataSnapshot questionSnapshot : dS.getChildren()) {
                    // Iterate through recordingId nodes
                    for (DataSnapshot recordingSnapshot : questionSnapshot.getChildren()) {
                        Recording recording = recordingSnapshot.getValue(Recording.class);
                        if (recording != null) {
                            totalCount++;
                            totalScore += recording.getScore();
                        }
                    }
                }

                // Update UI on the main thread
                recordingCountTv.setText(String.valueOf(totalCount));
                if (totalCount > 0) {
                    int avg = (int) (totalScore / totalCount);
                    avgScoreTv.setText(String.valueOf(avg));
                } else {
                    avgScoreTv.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void logOut(View view) {
        refAuth.signOut();

        // Clear the "stayConnected" preference
        SharedPreferences settings = requireActivity().getSharedPreferences("STATUS", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("stayConnected", false);
        editor.commit();

        Intent intent = new Intent(requireActivity(), WelcomeScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        requireActivity().finish();
    }
}
