package com.example.konferenzraumanzeige;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.konferenzraumanzeige.databinding.FragmentCalendarBinding;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.microsoft.graph.models.DateTimeTimeZone;
import com.microsoft.graph.models.Event;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * The Calendar Fragment shows whether the room is occupied and a list of following appointments of the current day.
 * Through a reload Button the status and view gets synchronised.
 * The Logout Button removes the Microsoft account and shows the Login Fragment again.
 * The add Button opens the NewEvent Fragment.
 */

public class CalendarFragment extends Fragment {

    private static final String TIME_ZONE = "timeZone";
    Calendar calendar1 = Calendar.getInstance();
    SimpleDateFormat formatter1;
    private Handler uiHandler;
    private FragmentCalendarBinding binding;
    private String mTimeZone = Information.getInstance().getUserTimeZone();
    private List<Event> mEventList;
    private Boolean mOccupied = false;
    private TextView mOccupiedView;
    ZonedDateTime startOfDay;
    ZonedDateTime endOfDay;
    GraphHelper graphHelper;

    public CalendarFragment() {
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        //wenn Arguments nicht null sind, wird mTimeZone mit dem String TIME_ZONE belegt, also einfach nur "timeZone"?
        super.onCreate(savedInstanceState);
        uiHandler = new Handler();
        mEventList = new LinkedList<>();
        formatter1 = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        if (getArguments() != null) {
            mTimeZone = getArguments().getString(TIME_ZONE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //zeigt fragment frei an und einen container
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        mOccupiedView = binding.besetztStatus;
        addButtons();
        graphHelper = GraphHelper.getInstance();
        setDay();
        createGraphHelper();
        return binding.getRoot();
    }

    private void createGraphHelper() {
        graphHelper
                .getCalendarView(startOfDay, endOfDay, mTimeZone)
                .thenAccept(eventList -> {
                    fillmEventList(eventList);
                    addEventsToList();
                    setOccupied();
                })
                .exceptionally(exception -> {
                    Log.e("GRAPH", "Error getting events", exception);
                    Snackbar.make(requireView(),
                            Objects.requireNonNull(exception.getMessage()),
                            BaseTransientBottomBar.LENGTH_LONG).show();
                    return null;
                });
    }

    private void setOccupied() {
        if (mEventList != null) {
            Information.getInstance().setEventList(mEventList);
            synchronizeAppointment(mEventList);
            uiHandler.post(() -> {
                binding.animationView.setVisibility(View.INVISIBLE);
                if (mOccupied) {
                    layoutBesetzt();
                } else {
                    layoutFrei();
                }
            });
        }
    }

    private void layoutFrei() {
        mOccupiedView.setText(R.string.roomFree);
        mOccupiedView.setTextSize(120);
        mOccupiedView.setBackgroundColor(getResources().getColor(R.color.green, requireActivity().getTheme()));
    }

    private void layoutBesetzt() {
        mOccupiedView.setText(R.string.roomOccupied);
        mOccupiedView.setTextSize(90);
        mOccupiedView.setBackgroundColor(getResources().getColor(R.color.red, requireActivity().getTheme()));
    }

    private void fillmEventList(List<Event> eventList) {
        mEventList.clear();
            for (int i = 0; i < eventList.size(); i++) {
                String mEndTime = getLocalDateTimeString(Objects.requireNonNull(eventList.get(i).end));
                String mDeviceTime = formatter1.format(calendar1.getTime());
                if (mEndTime.compareTo(mDeviceTime) > 0) {
                    mEventList.add(eventList.get(i));
                }
            }

        if (mEventList.isEmpty()) {
            uiHandler.post(() -> {
                binding.termin.setVisibility(View.VISIBLE);
                layoutFrei();
            });
        }
    }

    private void setDay() {
        ZoneId tzId = GraphToIana.getZoneIdFromWindows(Information.getInstance().getUserTimeZone());
        startOfDay = ZonedDateTime.now(tzId)
                .truncatedTo(ChronoUnit.DAYS)
                .withZoneSameInstant(ZoneId.of("UTC"));

        endOfDay = startOfDay.plusDays(1);
    }

    private void addButtons() {
        ImageButton mAddButton = binding.button2;
        mAddButton.setOnClickListener(view -> add());
        ImageButton mRefreshButton = binding.buttonRefresh;
        mRefreshButton.setOnClickListener(view -> refresh());
        ImageButton mSignOutButton = binding.buttonSignOut;
        mSignOutButton.setOnClickListener(view -> signOut());
    }

    private void signOut() {
        AuthenticationHelper.getInstance().signOut();
        Navigation.findNavController(requireActivity().findViewById(R.id.nav_host_fragment)).navigate(R.id.action_calendarFragment_to_homeFragment);

    }

    private void refresh() {
        binding.termin.setVisibility(View.INVISIBLE);
        binding.animationView.setVisibility(View.VISIBLE);
        createGraphHelper();
    }

    private void add() {
        Navigation.findNavController(requireActivity().findViewById(R.id.nav_host_fragment)).navigate(R.id.action_calendarFragment_to_newEventFragment);
    }

    private void addEventsToList() {
        //neue ListView namens eventListView wird mit dem xml event_list verbunden
        // erstellt neuen EventListAdapter mit layout event_list_item und mEventList
        //setzt den Adapter auf die listView
        requireActivity().runOnUiThread(() -> {

            ListView eventListView = requireView().findViewById(R.id.eventlist);

            EventListAdapter listAdapter = new EventListAdapter(getActivity(),
                    R.layout.event_list_item, mEventList);

            eventListView.setAdapter(listAdapter);
        });
    }

    private void synchronizeAppointment(List<Event> eList) {
        mOccupied =false;
        Calendar calendar1 = Calendar.getInstance();
        String currentDate = formatter1.format(calendar1.getTime());

        for (int i = 0; i <= eList.size() - 1; i++) {

            String eventStart = getLocalDateTimeString(Objects.requireNonNull(eList.get(i).start));
            String eventEnd = getLocalDateTimeString(Objects.requireNonNull(eList.get(i).end));

            if (eventStart.compareTo(currentDate) <= 0) {
                if (eventEnd.compareTo(currentDate) >= 0) {
                    mOccupied = true;
                }
            }
        }
    }

    private String getLocalDateTimeString(DateTimeTimeZone dateTime) {
        ZonedDateTime localDateTime = LocalDateTime.parse(dateTime.dateTime)
                .atZone(GraphToIana.getZoneIdFromWindows(dateTime.timeZone));

        return (String.format("%s %s",
                localDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                localDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))));
    }
}
