package com.example.momentrip

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.momentrip.databinding.FragmentSimpleCalendarBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TravelCalendarFragment : Fragment() {
    private var _binding: FragmentSimpleCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: TravelDBHelper

    private var records = listOf<TravelRecord>()
    private var selectedDate = today()
    private var displayYear = Calendar.getInstance().get(Calendar.YEAR)
    private var displayMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSimpleCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dbHelper = TravelDBHelper(requireContext())

        binding.textCalendarTitle.setText(R.string.calendar_record_title)
        binding.textCalendarDescription.setText(R.string.calendar_description)

        binding.buttonPrevMonth.setOnClickListener {
            moveMonth(-1)
        }

        binding.buttonNextMonth.setOnClickListener {
            moveMonth(1)
        }

        render()
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
    }

    override fun onDestroyView() {
        dbHelper.close()
        _binding = null
        super.onDestroyView()
    }

    private fun loadRecords() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Pair(dbHelper.getAllTravels(), false)
                } catch (_: Exception) {
                    Pair(emptyList<TravelRecord>(), true)
                }
            }

            if (!isAdded || _binding == null) {
                return@launch
            }

            if (result.second) {
                Toast.makeText(requireContext(), R.string.toast_load_failed, Toast.LENGTH_SHORT).show()
            }

            records = result.first
            render()
        }
    }

    private fun moveMonth(value: Int) {
        displayMonth += value

        if (displayMonth < 1) {
            displayMonth = 12
            displayYear--
        } else if (displayMonth > 12) {
            displayMonth = 1
            displayYear++
        }

        selectedDate = String.format("%04d-%02d-01", displayYear, displayMonth)
        render()
    }

    private fun render() {
        if (_binding == null) {
            return
        }

        drawCalendar()
        renderSelectedDateItems()
    }

    private fun drawCalendar() {
        val currentBinding = _binding ?: return
        val grid = currentBinding.calendarGrid

        grid.removeAllViews()
        grid.columnCount = 7

        currentBinding.textCalendarMonth.text = String.format("%04d년 %d월", displayYear, displayMonth)

        val eventDates = makeEventDateSet()
        val weekNames = arrayOf("일", "월", "화", "수", "목", "금", "토")

        weekNames.forEach { name ->
            val textView = makeCalendarCell(name, true)
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.momentrip_muted))
            grid.addView(textView)
        }

        val calendar = Calendar.getInstance()
        calendar.set(displayYear, displayMonth - 1, 1)

        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val emptyCount = firstDayOfWeek - 1
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        repeat(emptyCount) {
            grid.addView(makeEmptyCell())
        }

        for (day in 1..lastDay) {
            val date = String.format("%04d-%02d-%02d", displayYear, displayMonth, day)
            val hasEvent = eventDates.contains(date)
            val isSelected = selectedDate == date

            val textView = makeCalendarCell(day.toString(), false)

            when {
                isSelected -> {
                    textView.setTextColor(Color.WHITE)
                    textView.typeface = Typeface.DEFAULT_BOLD
                    textView.background = makeCircleDrawable("#FFB15A")
                }

                hasEvent -> {
                    textView.setTextColor(Color.WHITE)
                    textView.typeface = Typeface.DEFAULT_BOLD
                    textView.background = makeCircleDrawable("#4BAFE0")
                }

                else -> {
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.momentrip_ink))
                    textView.background = null
                }
            }

            textView.setOnClickListener {
                selectedDate = date
                render()
            }

            grid.addView(textView)
        }
    }

    private fun renderSelectedDateItems() {
        val selectedItems = records.filter { record ->
            isDateIncluded(record.visitDate, selectedDate)
        }

        binding.textCalendarItems.text = if (selectedItems.isEmpty()) {
            "선택한 날짜: $selectedDate\n\n등록된 여행 기록이 없습니다."
        } else {
            val itemText = selectedItems.joinToString("\n\n") { record ->
                "제목: ${record.place}\n날짜: ${record.visitDate}\n메모: ${record.memo ?: getString(R.string.item_no_memo)}"
            }

            "선택한 날짜: $selectedDate\n\n$itemText"
        }
    }

    private fun makeEventDateSet(): HashSet<String> {
        val result = hashSetOf<String>()

        records.forEach { record ->
            addDateRangeToSet(result, record.visitDate)
        }

        return result
    }

    private fun addDateRangeToSet(set: HashSet<String>, rawDate: String) {
        try {
            val parts = rawDate.split("~")
            val startDate = parts[0].trim().take(10)
            val endDate = if (parts.size > 1) {
                parts[1].trim().take(10)
            } else {
                startDate
            }

            if (startDate.length < 10 || endDate.length < 10) {
                return
            }

            var current = startDate

            while (current <= endDate) {
                set.add(current)
                val next = nextDate(current)

                if (next == current) {
                    break
                }

                current = next
            }
        } catch (_: Exception) {
            if (rawDate.length >= 10) {
                set.add(rawDate.take(10))
            }
        }
    }

    private fun isDateIncluded(rawDate: String, selectedDate: String): Boolean {
        return try {
            val parts = rawDate.split("~")
            val startDate = parts[0].trim().take(10)
            val endDate = if (parts.size > 1) {
                parts[1].trim().take(10)
            } else {
                startDate
            }

            selectedDate >= startDate && selectedDate <= endDate
        } catch (_: Exception) {
            rawDate.contains(selectedDate)
        }
    }

    private fun nextDate(date: String): String {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
            val parsedDate = formatter.parse(date) ?: return date
            val calendar = Calendar.getInstance()

            calendar.time = parsedDate
            calendar.add(Calendar.DAY_OF_MONTH, 1)

            formatter.format(calendar.time)
        } catch (_: Exception) {
            date
        }
    }

    private fun makeCalendarCell(text: String, isHeader: Boolean): TextView {
        val textView = TextView(requireContext())
        val size = dp(44)

        val params = GridLayout.LayoutParams()
        params.width = 0
        params.height = size
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(2, 4, 2, 4)

        textView.layoutParams = params
        textView.text = text
        textView.gravity = Gravity.CENTER
        textView.textSize = if (isHeader) 13f else 15f
        textView.includeFontPadding = false

        if (isHeader) {
            textView.typeface = Typeface.DEFAULT_BOLD
        }

        return textView
    }

    private fun makeEmptyCell(): TextView {
        val textView = makeCalendarCell("", false)
        textView.background = null
        return textView
    }

    private fun makeCircleDrawable(color: String): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(Color.parseColor(color))
        return drawable
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private fun today(): String {
            val calendar = Calendar.getInstance()

            return String.format(
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
}