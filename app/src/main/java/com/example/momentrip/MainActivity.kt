package com.example.momentrip

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.momentrip.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: TravelDBHelper

    private var currentNavMode = NavMode.HOME
    private var currentContentMode = ContentMode.CARD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.app_name)
        dbHelper = TravelDBHelper(this)

        binding.textMainTitle.setText(R.string.main_title)
        binding.textMainSubTitle.setText(R.string.main_subtitle)
        binding.buttonTravelList.setText(R.string.tab_list)
        binding.buttonTravelMap.setText(R.string.tab_map)
        binding.buttonCalendarView.setText(R.string.tab_calendar)

        binding.buttonTravelList.setOnClickListener {
            showCardView(true)
        }

        binding.buttonTravelMap.setOnClickListener {
            showMapView(true)
        }

        binding.buttonCalendarView.setOnClickListener {
            showCalendarView(true)
        }

        binding.buttonNavRecords.setOnClickListener {
            showRecordList(true)
        }

        binding.buttonNavHome.setOnClickListener {
            showHome(true)
        }

        binding.buttonNavPlans.setOnClickListener {
            showPlans(true)
        }

        registerBackButtonEvent()

        if (savedInstanceState == null) {
            showHome(false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuAddRecord -> {
                try {
                    AddEditActivity.start(this)
                } catch (_: Exception) {
                    Toast.makeText(this, R.string.error_screen_change, Toast.LENGTH_SHORT).show()
                }
                true
            }

            R.id.menuAddPlan -> {
                try {
                    AddEditPlanActivity.start(this)
                } catch (_: Exception) {
                    Toast.makeText(this, R.string.error_screen_change, Toast.LENGTH_SHORT).show()
                }
                true
            }

            R.id.menuSortDate -> {
                sortCurrentTabByDate()
                true
            }

            R.id.menuSortPlace -> {
                sortCurrentTabByTitle()
                true
            }

            R.id.menuDeleteAll -> {
                confirmDeleteAllCurrentTab()
                true
            }

            R.id.menuInfo -> {
                showAppInfo()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun registerBackButtonEvent() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitDialog()
                }
            }
        )
    }

    private fun showExitDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("앱 종료")
                .setMessage("정말 앱을 종료하시겠습니까?")
                .setNegativeButton("취소") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("확인") { _, _ ->
                    finishAffinity()
                }
                .show()
        } catch (_: Exception) {
            finishAffinity()
        }
    }

    private fun showFragment(fragment: Fragment, addToBackStack: Boolean) {
        try {
            val tag = fragment.javaClass.simpleName
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

            if (currentFragment != null && currentFragment.javaClass.simpleName == tag) {
                return
            }

            val transaction = supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment, tag)

            if (addToBackStack) {
                transaction.addToBackStack(tag)
            }

            transaction.commit()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.error_screen_change, Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectTab(mode: ContentMode) {
        binding.buttonTravelList.isSelected = mode == ContentMode.CARD
        binding.buttonTravelMap.isSelected = mode == ContentMode.MAP
        binding.buttonCalendarView.isSelected = mode == ContentMode.CALENDAR
    }

    private fun showHome(addToBackStack: Boolean) {
        currentNavMode = NavMode.HOME
        currentContentMode = ContentMode.CARD

        binding.buttonLayout.visibility = View.GONE
        selectBottomNav(NavMode.HOME)
        showFragment(HomeFragment(), addToBackStack)
    }

    private fun showRecordList(addToBackStack: Boolean) {
        currentNavMode = NavMode.RECORDS
        currentContentMode = ContentMode.CARD

        binding.buttonLayout.visibility = View.VISIBLE
        selectBottomNav(NavMode.RECORDS)
        selectTab(ContentMode.CARD)
        showFragment(TravelListFragment(), addToBackStack)
    }

    private fun showRecordMap(addToBackStack: Boolean) {
        currentNavMode = NavMode.RECORDS
        currentContentMode = ContentMode.MAP

        binding.buttonLayout.visibility = View.VISIBLE
        selectBottomNav(NavMode.RECORDS)
        selectTab(ContentMode.MAP)
        showFragment(TravelMapFragment(), addToBackStack)
    }

    private fun showRecordCalendar(addToBackStack: Boolean) {
        currentNavMode = NavMode.RECORDS
        currentContentMode = ContentMode.CALENDAR

        binding.buttonLayout.visibility = View.VISIBLE
        selectBottomNav(NavMode.RECORDS)
        selectTab(ContentMode.CALENDAR)
        showFragment(TravelCalendarFragment(), addToBackStack)
    }

    private fun showPlans(addToBackStack: Boolean) {
        currentNavMode = NavMode.PLANS
        currentContentMode = ContentMode.CARD

        binding.buttonLayout.visibility = View.VISIBLE
        selectBottomNav(NavMode.PLANS)
        selectTab(ContentMode.CARD)
        showFragment(TravelPlanFragment(), addToBackStack)
    }

    private fun showPlanMap(addToBackStack: Boolean) {
        currentNavMode = NavMode.PLANS
        currentContentMode = ContentMode.MAP

        binding.buttonLayout.visibility = View.VISIBLE
        selectBottomNav(NavMode.PLANS)
        selectTab(ContentMode.MAP)
        showFragment(PlanMapFragment(), addToBackStack)
    }

    private fun showPlanCalendar(addToBackStack: Boolean) {
        currentNavMode = NavMode.PLANS
        currentContentMode = ContentMode.CALENDAR

        binding.buttonLayout.visibility = View.VISIBLE
        selectBottomNav(NavMode.PLANS)
        selectTab(ContentMode.CALENDAR)
        showFragment(PlanCalendarFragment(), addToBackStack)
    }

    private fun showCardView(addToBackStack: Boolean) {
        if (currentNavMode == NavMode.PLANS) {
            showPlans(addToBackStack)
        } else {
            showRecordList(addToBackStack)
        }
    }

    private fun showMapView(addToBackStack: Boolean) {
        if (currentNavMode == NavMode.PLANS) {
            showPlanMap(addToBackStack)
        } else {
            showRecordMap(addToBackStack)
        }
    }

    private fun showCalendarView(addToBackStack: Boolean) {
        if (currentNavMode == NavMode.PLANS) {
            showPlanCalendar(addToBackStack)
        } else {
            showRecordCalendar(addToBackStack)
        }
    }

    private fun refreshCurrentScreen() {
        when (currentNavMode) {
            NavMode.RECORDS -> {
                when (currentContentMode) {
                    ContentMode.CARD -> showRecordList(false)
                    ContentMode.MAP -> showRecordMap(false)
                    ContentMode.CALENDAR -> showRecordCalendar(false)
                }
            }

            NavMode.PLANS -> {
                when (currentContentMode) {
                    ContentMode.CARD -> showPlans(false)
                    ContentMode.MAP -> showPlanMap(false)
                    ContentMode.CALENDAR -> showPlanCalendar(false)
                }
            }

            NavMode.HOME -> showHome(false)
        }
    }

    private fun selectBottomNav(mode: NavMode) {
        binding.buttonNavRecords.setTextColor(
            getColor(if (mode == NavMode.RECORDS) R.color.momentrip_teal_dark else R.color.momentrip_muted)
        )

        binding.buttonNavHome.setTextColor(getColor(R.color.white))

        binding.buttonNavPlans.setTextColor(
            getColor(if (mode == NavMode.PLANS) R.color.momentrip_teal_dark else R.color.momentrip_muted)
        )
    }

    private fun sortCurrentTabByDate() {
        when (currentNavMode) {
            NavMode.RECORDS -> {
                TravelListFragment.sortMode = TravelListFragment.SortMode.DATE
                showRecordList(false)
            }

            NavMode.PLANS -> {
                TravelPlanFragment.sortMode = TravelPlanFragment.SortMode.DATE
                showPlans(false)
            }

            NavMode.HOME -> {
                Toast.makeText(this, "여행기록 또는 여행계획 탭에서 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sortCurrentTabByTitle() {
        when (currentNavMode) {
            NavMode.RECORDS -> {
                TravelListFragment.sortMode = TravelListFragment.SortMode.PLACE
                showRecordList(false)
            }

            NavMode.PLANS -> {
                TravelPlanFragment.sortMode = TravelPlanFragment.SortMode.PLACE
                showPlans(false)
            }

            NavMode.HOME -> {
                Toast.makeText(this, "여행기록 또는 여행계획 탭에서 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteAllCurrentTab() {
        when (currentNavMode) {
            NavMode.RECORDS -> confirmDeleteAllRecords()
            NavMode.PLANS -> confirmDeleteAllPlans()
            NavMode.HOME -> {
                Toast.makeText(this, "여행기록 또는 여행계획 탭에서 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteAllRecords() {
        try {
            AlertDialog.Builder(this)
                .setTitle("여행기록 전체 삭제")
                .setMessage("현재 여행기록 탭의 모든 기록을 삭제하시겠습니까?")
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    deleteAllRecords()
                }
                .show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_delete_all_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteAllPlans() {
        try {
            AlertDialog.Builder(this)
                .setTitle("여행계획 전체 삭제")
                .setMessage("현재 여행계획 탭의 모든 계획을 삭제하시겠습니까?")
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    deleteAllPlans()
                }
                .show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_delete_all_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteAllRecords() {
        binding.progressMain.visibility = View.VISIBLE

        lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) {
                try {
                    dbHelper.deleteAllTravels()
                } catch (_: Exception) {
                    -1
                }
            }

            binding.progressMain.visibility = View.GONE

            if (count < 0) {
                Toast.makeText(this@MainActivity, R.string.toast_delete_all_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }

            Toast.makeText(this@MainActivity, getString(R.string.toast_delete_count, count), Toast.LENGTH_SHORT).show()
            refreshCurrentScreen()
        }
    }

    private fun deleteAllPlans() {
        binding.progressMain.visibility = View.VISIBLE

        lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) {
                try {
                    val plans = dbHelper.getAllPlans()
                    var deleteCount = 0

                    for (plan in plans) {
                        deleteCount += dbHelper.deletePlan(plan.no)
                    }

                    deleteCount
                } catch (_: Exception) {
                    -1
                }
            }

            binding.progressMain.visibility = View.GONE

            if (count < 0) {
                Toast.makeText(this@MainActivity, R.string.toast_delete_all_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }

            Toast.makeText(this@MainActivity, getString(R.string.toast_delete_count, count), Toast.LENGTH_SHORT).show()
            refreshCurrentScreen()
        }
    }

    private fun showAppInfo() {
        try {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_app_info_title)
                .setMessage(R.string.dialog_app_info_message)
                .setPositiveButton(R.string.action_ok, null)
                .show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show()
        }
    }

    enum class NavMode {
        RECORDS,
        HOME,
        PLANS
    }

    enum class ContentMode {
        CARD,
        MAP,
        CALENDAR
    }
}