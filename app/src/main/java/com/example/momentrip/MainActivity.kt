package com.example.momentrip

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {
    lateinit var buttonTravelList: Button
    lateinit var buttonTravelMap: Button
    private lateinit var dbHelper: TravelDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = TravelDBHelper(this)
        buttonTravelList = findViewById(R.id.buttonTravelList)
        buttonTravelMap = findViewById(R.id.buttonTravelMap)
        findViewById<TextView>(R.id.textMainTitle).setText(R.string.main_title)
        findViewById<TextView>(R.id.textMainSubTitle).setText(R.string.main_subtitle)
        buttonTravelList.setText(R.string.tab_list)
        buttonTravelMap.setText(R.string.tab_map)

        buttonTravelList.setOnClickListener {
            selectTab(true)
            showFragment(TravelListFragment(), true)
        }

        buttonTravelMap.setOnClickListener {
            selectTab(false)
            showFragment(TravelMapFragment(), true)
        }

        if (savedInstanceState == null) {
            selectTab(true)
            showFragment(TravelListFragment(), false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_ADD, 0, getString(R.string.menu_add))
        menu.add(0, MENU_SORT_DATE, 1, getString(R.string.menu_sort_date))
        menu.add(0, MENU_SORT_PLACE, 2, getString(R.string.menu_sort_place))
        menu.add(0, MENU_DELETE_ALL, 3, getString(R.string.menu_delete_all))
        menu.add(0, MENU_INFO, 4, getString(R.string.menu_info))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_ADD -> {
                TravelEditActivity.start(this)
                true
            }
            MENU_SORT_DATE -> {
                TravelListFragment.sortMode = TravelListFragment.SortMode.DATE
                selectTab(true)
                showFragment(TravelListFragment(), true)
                true
            }
            MENU_SORT_PLACE -> {
                TravelListFragment.sortMode = TravelListFragment.SortMode.PLACE
                selectTab(true)
                showFragment(TravelListFragment(), true)
                true
            }
            MENU_DELETE_ALL -> {
                confirmDeleteAll()
                true
            }
            MENU_INFO -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.dialog_app_info_message)
                    .setPositiveButton(R.string.action_ok, null)
                    .show()
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

    private fun confirmDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_all_title)
            .setMessage(R.string.dialog_delete_all_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                val count = dbHelper.deleteAllTravels()
                Toast.makeText(this, getString(R.string.toast_delete_count, count), Toast.LENGTH_SHORT).show()
                selectTab(true)
                showFragment(TravelListFragment(), true)
            }
            .show()
    }

    companion object {
        private const val MENU_ADD = 1
        private const val MENU_SORT_DATE = 2
        private const val MENU_SORT_PLACE = 3
        private const val MENU_DELETE_ALL = 4
        private const val MENU_INFO = 5
    }
}
