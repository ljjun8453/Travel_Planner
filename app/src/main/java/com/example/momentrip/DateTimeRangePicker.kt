package com.example.momentrip

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.appcompat.app.AlertDialog
import java.util.Calendar

object DateTimeRangePicker {
    fun show(context: Context, onSelected: (String) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(R.string.hint_visit_date)
            .setItems(arrayOf(context.getString(R.string.date_pick_one_day), context.getString(R.string.date_pick_period))) { _, which ->
                if (which == 0) {
                    pickDateTime(context) { start ->
                        onSelected(format(start))
                    }
                } else {
                    pickDateTime(context) { start ->
                        pickDateTime(context) { end ->
                            onSelected("${format(start)} ~ ${format(end)}")
                        }
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun pickDateTime(context: Context, onPicked: (Calendar) -> Unit) {
        val date = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                date.set(Calendar.YEAR, year)
                date.set(Calendar.MONTH, month)
                date.set(Calendar.DAY_OF_MONTH, day)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        date.set(Calendar.HOUR_OF_DAY, hour)
                        date.set(Calendar.MINUTE, minute)
                        date.set(Calendar.SECOND, 0)
                        date.set(Calendar.MILLISECOND, 0)
                        onPicked(date)
                    },
                    date.get(Calendar.HOUR_OF_DAY),
                    date.get(Calendar.MINUTE),
                    true
                ).show()
            },
            date.get(Calendar.YEAR),
            date.get(Calendar.MONTH),
            date.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun format(calendar: Calendar): String {
        return String.format(
            "%04d-%02d-%02d %02d:%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }
}
