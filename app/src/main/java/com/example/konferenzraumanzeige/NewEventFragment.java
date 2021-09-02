package com.example.konferenzraumanzeige;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * The newEvent Fragment opens a view where you can create a new Event in the Calendar of the logged in Microsoft Account.
 * Subject, Attendees and an additional body can be added through typing in text fields.
 * Start and End use the EditTextDateTimePicker.
 * Through a button you can just close the Fragment and go back to the Calendar Fragment.
 * Through another Button you can create the event with the data from the textViews.
 */

public class NewEventFragment extends Fragment {

    private TextInputLayout mSubject;
    private TextInputLayout mAttendees;
    private TextInputLayout mEndInputLayout;
    private TextInputLayout mBody;
    private EditTextDateTimePicker mStartPicker;
    private EditTextDateTimePicker mEndPicker;
    private Handler mHandler;

    public NewEventFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View newEventView = inflater.inflate(R.layout.fragment_new_event, container, false);
        mHandler = new Handler();
        ImageButton mbackButton = newEventView.findViewById(R.id.backButton);
        mbackButton.setOnClickListener(view -> back());
        ZoneId userTimeZone = GraphToIana.getZoneIdFromWindows(Information.getInstance().getUserTimeZone());

        mSubject = newEventView.findViewById(R.id.neweventsubject);
        mAttendees = newEventView.findViewById(R.id.neweventattendees);
        mBody = newEventView.findViewById(R.id.neweventbody);


        TextInputLayout mStartInputLayout = newEventView.findViewById(R.id.neweventstartdatetime);
        mStartInputLayout.setPlaceholderText("Date");
        mStartPicker = new EditTextDateTimePicker(getContext(),
            mStartInputLayout.getEditText(),
            userTimeZone, true);

        mEndInputLayout = newEventView.findViewById(R.id.neweventenddatetime);
        mEndInputLayout.setPlaceholderText("Date");
        mEndPicker = new EditTextDateTimePicker(getContext(),
            mEndInputLayout.getEditText(),
            userTimeZone, false);

        Button createButton = newEventView.findViewById(R.id.createevent);
        createButton.setOnClickListener(v -> {
            // Clear any errors
            mSubject.setErrorEnabled(false);
            mEndInputLayout.setErrorEnabled(false);

            createEvent();
        });

        return newEventView;
    }

    private void back() {
        Navigation.findNavController(requireActivity().findViewById(R.id.nav_host_fragment)).navigate(R.id.action_newEventFragment_to_calendarFragment);
    }

    private void createEvent() {
        String subject = Objects.requireNonNull(mSubject.getEditText()).getText().toString();
        String attendees = Objects.requireNonNull(mAttendees.getEditText()).getText().toString();
        String body = Objects.requireNonNull(mBody.getEditText()).getText().toString();

        ZonedDateTime startDateTime = mStartPicker.getZonedDateTime();
        ZonedDateTime endDateTime = mEndPicker.getZonedDateTime();

        // Validate
        boolean isValid = true;
        // Subject is required
        if (subject.isEmpty()) {
            isValid = false;
            mSubject.setError(getString(R.string.error_subject));
        }

        // End must be after start
        if (!endDateTime.isAfter(startDateTime)) {
            isValid = false;
            mEndInputLayout.setError(getString(R.string.error_end));
        }

        if (isValid) {
            // Split the attendees string into an array
            String[] attendeeArray = attendees.split(";");

            GraphHelper.getInstance()
                .createEvent(subject,
                    startDateTime,
                    endDateTime,
                    Information.getInstance().getUserTimeZone(),
                    attendeeArray,
                    body)
                .thenAccept(newEvent -> {
                    Snackbar.make(requireView(),
                        getString(R.string.event_created),
                        BaseTransientBottomBar.LENGTH_SHORT).show();
                    mHandler.post(this::back);
                })
                .exceptionally(exception -> {
                    Log.e("GRAPH", "Error creating event", exception);
                    Snackbar.make(requireView(),
                            Objects.requireNonNull(exception.getMessage()),
                        BaseTransientBottomBar.LENGTH_LONG).show();
                    return null;
                });
        }
    }
}
