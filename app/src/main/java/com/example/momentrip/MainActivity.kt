package com.example.momentrip

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    lateinit var buttonTravelList: Button
    lateinit var buttonTravelMap: Button
    private lateinit var buttonLayout: View
    private lateinit var buttonNavRecords: Button
    private lateinit var buttonNavHome: Button
    private lateinit var buttonNavPlans: Button
    private lateinit var dbHelper: TravelDBHelper
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = TravelDBHelper(this)
        buttonTravelList = findViewById(R.id.buttonTravelList)
        buttonTravelMap = findViewById(R.id.buttonTravelMap)
        buttonLayout = findViewById(R.id.buttonLayout)
        buttonNavRecords = findViewById(R.id.buttonNavRecords)
        buttonNavHome = findViewById(R.id.buttonNavHome)
        buttonNavPlans = findViewById(R.id.buttonNavPlans)
        progressBar = findViewById(R.id.progressMain)
        findViewById<TextView>(R.id.textMainTitle).setText(R.string.main_title)
        findViewById<TextView>(R.id.textMainSubTitle).setText(R.string.main_subtitle)
        buttonTravelList.setText(R.string.tab_list)
        buttonTravelMap.setText(R.string.tab_map)

        buttonTravelList.setOnClickListener {
            showRecordList(true)
        }

        buttonTravelMap.setOnClickListener {
            showRecordMap(true)
        }

        buttonNavRecords.setOnClickListener {
            showRecordList(true)
        }

        buttonNavHome.setOnClickListener {
            showHome(true)
        }

        buttonNavPlans.setOnClickListener {
            showPlans(true)
        }

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
            R.id.menuAdd -> {
                showCreateDialog()
                true
            }
            R.id.menuSortDate -> {
                TravelListFragment.sortMode = TravelListFragment.SortMode.DATE
                showRecordList(true)
                true
            }
            R.id.menuSortPlace -> {
                TravelListFragment.sortMode = TravelListFragment.SortMode.PLACE
                showRecordList(true)
                true
            }
            R.id.menuDeleteAll -> {
                confirmDeleteAll()
                true
            }
            R.id.menuInfo -> {
                try {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_app_info_title)
                        .setMessage(R.string.dialog_app_info_message)
                        .setPositiveButton(R.string.action_ok, null)
                        .show()
                } catch (_: Exception) {
                    Toast.makeText(this, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
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

    private fun selectTab(isListSelected: Boolean) {
        buttonTravelList.isSelected = isListSelected
        buttonTravelMap.isSelected = !isListSelected
    }

    private fun showHome(addToBackStack: Boolean) {
        buttonLayout.visibility = View.GONE
        selectBottomNav(NavMode.HOME)
        showFragment(HomeFragment(), addToBackStack)
    }

    private fun showRecordList(addToBackStack: Boolean) {
        buttonLayout.visibility = View.VISIBLE
        selectBottomNav(NavMode.RECORDS)
        selectTab(true)
        showFragment(TravelListFragment(), addToBackStack)
    }

    private fun showRecordMap(addToBackStack: Boolean) {
        buttonLayout.visibility = View.VISIBLE
        selectBottomNav(NavMode.RECORDS)
        selectTab(false)
        showFragment(TravelMapFragment(), addToBackStack)
    }

    private fun showPlans(addToBackStack: Boolean) {
        buttonLayout.visibility = View.GONE
        selectBottomNav(NavMode.PLANS)
        showFragment(TravelPlanFragment(), addToBackStack)
    }

    private fun selectBottomNav(mode: NavMode) {
        buttonNavRecords.setTextColor(getColor(if (mode == NavMode.RECORDS) R.color.momentrip_teal_dark else R.color.momentrip_muted))
        buttonNavHome.setTextColor(getColor(if (mode == NavMode.HOME) R.color.momentrip_ink else R.color.momentrip_navy))
        buttonNavPlans.setTextColor(getColor(if (mode == NavMode.PLANS) R.color.momentrip_teal_dark else R.color.momentrip_muted))
    }

    private fun showCreateDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_create_title)
                .setItems(arrayOf(getString(R.string.dialog_create_record), getString(R.string.dialog_create_plan))) { _, which ->
                    if (which == 0) {
                        AddEditActivity.start(this)
                    } else {
                        AddEditPlanActivity.start(this)
                    }
                }
                .show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.error_dialog_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteAll() {
        try {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_all_title)
                .setMessage(R.string.dialog_delete_all_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    deleteAllRecords()
                }
                .show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_delete_all_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteAllRecords() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) {
                try {
                    dbHelper.deleteAllTravels()
                } catch (_: Exception) {
                    -1
                }
            }

            progressBar.visibility = View.GONE
            if (count < 0) {
                Toast.makeText(this@MainActivity, R.string.toast_delete_all_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            Toast.makeText(this@MainActivity, getString(R.string.toast_delete_count, count), Toast.LENGTH_SHORT).show()
            showRecordList(true)
        }
    }

    enum class NavMode {
        RECORDS,
        HOME,
        PLANS
    }
}
