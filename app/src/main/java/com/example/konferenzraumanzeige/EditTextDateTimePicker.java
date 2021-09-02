package com.example.konferenzraumanzeige;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.microsoft.graph.models.DateTimeTimeZone;
import com.microsoft.graph.models.Event;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Objects;

/**
 * The EditTextDateTimePicker lets you pick a date and a time for your appointment.
 * It automatically shows a possible next appointment with a standard length of 1 hour.
 * The start time will be set to the next time the room isn't occupied.
 * The end time to one hour later or if the room is blocked due this time as long as possible without interfering with already booked appointments.
 */

public class EditTextDateTimePicker implements View.OnClickListener,
        DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener {

    private final Context mContext;
    private final EditText mEditText;
    private ZonedDateTime mDateTime;
    private ZonedDateTime mTemporaryTime;
    private List<Event> mEventList;
    private DateTimeFormatter formatter;
    private Boolean mOccupied =false;
    private final Information mInf;
    private Integer mElementIndex;

    EditTextDateTimePicker(Context context, EditText editText, ZoneId zoneId, Boolean start) {
        mContext = context;
        mEditText = editText;
        mEditText.setOnClickListener(this);
        formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        mInf = Information.getInstance();
        mEventList = mInf.getEventList();

        // aktuelle Zeit wird auf die nächste Viertel Stunde aufgerundet
        mDateTime = ZonedDateTime.now(zoneId).withSecond(0).withNano(0);
        int offset = 15 - (mDateTime.getMinute() % 15);
        if (offset != 15) {
            mDateTime = mDateTime.plusMinutes(offset);
        }
        if(start){ startUpdateText();}
        else{endUpdateText();}
    }

    @Override
    public void onClick(View v) {
        // First, show a date picker
        DatePickerDialog dialog = new DatePickerDialog(mContext,
            this,
            mDateTime.getYear(),
            mDateTime.getMonthValue()-1,
            mDateTime.getDayOfMonth());

        dialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        // Update the stored date/time with the new date
        //Monat +1, da DateTime Monate bei 0 anfängt zu zählen
        mDateTime = mDateTime.withYear(year).withMonth(month+1).withDayOfMonth(dayOfMonth);

        // Show a time picker
        TimePickerDialog dialog = new TimePickerDialog(mContext,
            this,
            mDateTime.getHour(),
            mDateTime.getMinute(),
            true);

        dialog.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Update the stored date/time with the new time
        mDateTime = mDateTime.withHour(hourOfDay).withMinute(minute);
        // Update the text in the EditText
        UpdateText();
    }

    private void UpdateText() {
        mEditText.setText(mDateTime.format(formatter));
    }

    public ZonedDateTime getZonedDateTime() {
        return mDateTime;
    }

    private void startUpdateText() {
        mEventList = mInf.getEventList();
        if(!mEventList.isEmpty()) {
            if(getLocalDateTimeString(mEventList.get(0).start).compareTo(mDateTime.format(formatter))<=0) {
                //Wenn die Zeit, zu der der nächste Termin startet, früher ist als die aktuelle Uhrzeit
                //wird die Startzeit auf das Ende des Termins gesetzt
                assert mEventList.get(0).end != null;
                mDateTime = parse(Objects.requireNonNull(mEventList.get(0).end));
                //falls die Liste mehr als ein Event enthält, wird geschaut wann der Raum als nächstes frei ist
                if(mEventList.size()>=2) {
                    mDateTime = displayTime(mEventList);
                }
            }
        }
        formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        Information.getInstance().setmTime(mDateTime);
        mEditText.setText(mDateTime.format(formatter));
    }

    //durchsucht die Liste bis Events nicht mehr exakt hintereinander hängen
    private ZonedDateTime displayTime(List<Event> mList){
        for(int x = 0; x < mList.size()-1; x++){
            if(getLocalDateTimeString(mList.get(x).end).compareTo(getLocalDateTimeString(mList.get(x+1).start))<0){
                return parse(Objects.requireNonNull(mList.get(x).end));
            }
        }
        return parse(Objects.requireNonNull(mList.get(mList.size() - 1).end));
    }

    private void endUpdateText() {
        mDateTime = mInf.getmTime().plusMinutes(60);
        synchronizeAppointment(mEventList,mDateTime);
        while(mOccupied){
            mDateTime=mDateTime.minusMinutes(15);
            mOccupied = false;
            synchronizeAppointment(mEventList,mDateTime);
        }

        for(int j = 0;  j<11; j++ ){
            mOccupied = false;
            mElementIndex = null;
            synchronizeAppointment(mEventList, mDateTime.minusMinutes(j*5));
            if(mOccupied){
                //mDateTime = mInf.getmTime().plusMinutes(5);
                if(mElementIndex!=null){
                    mTemporaryTime = parse(Objects.requireNonNull(mEventList.get(mElementIndex).start));
                }
            }
        }
        if(mTemporaryTime!=null){
            mDateTime = mTemporaryTime;
        }
        mTemporaryTime = null;
        mEditText.setText(mDateTime.format(formatter));
    }

    private void synchronizeAppointment(List<Event> pList, ZonedDateTime pTime) {

        String currentDate = pTime.format(formatter);
            for (int i = 0; i <= pList.size() - 1; i++) {

                String eventstart = getLocalDateTimeString(pList.get(i).start);
                String eventend = getLocalDateTimeString(pList.get(i).end);

                if (eventstart.compareTo(currentDate) < 0) {
                    if (eventend.compareTo(currentDate) >= 0) {
                        mOccupied = true;
                        mElementIndex = i;
                    }
                }
            }
    }

    private String getLocalDateTimeString(DateTimeTimeZone dateTime) {
        return (String.format("%s %s",
                parse(dateTime).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                parse(dateTime).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))));
    }

    private ZonedDateTime parse(DateTimeTimeZone dateTime) {
        return LocalDateTime.parse(dateTime.dateTime)
                .atZone(GraphToIana.getZoneIdFromWindows(dateTime.timeZone));
    }

}


