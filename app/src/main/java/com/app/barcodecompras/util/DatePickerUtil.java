package com.app.barcodecompras.util;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DatePickerUtil {

    public static void showDatePickerDialog(Context context, EditText targetEditText) {

        final Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                context,
                (view, selectedYear, selectedMonth, selectedDay) -> {

                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    SimpleDateFormat sdf = new SimpleDateFormat("EEE yyyy-MM-dd", Locale.getDefault());
                    targetEditText.setText(sdf.format(selectedDate.getTime()));
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    public static String getDataHoraAtual() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }
}