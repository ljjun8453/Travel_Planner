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

class MainActivity : AppCompatActivity() {
    lateinit var buttonTravelList: Button
    lateinit var buttonTravelMap: Button
    private lateinit var dbHelper: TravelDBHelper
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = TravelDBHelper(this)
        buttonTravelList = findViewById(R.id.buttonTravelList)
        buttonTravelMap = findViewById(R.id.buttonTravelMap)
        progressBar = findViewById(R.id.progressMain)
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
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuAdd -> {
                try {
                    AddEditActivity.start(this)
                } catch (_: Exception) {
                    Toast.makeText(this, R.string.error_screen_change, Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menuSortDate -> {
                TravelListFragment.sortMode = TravelListFragment.SortMode.DATE
                selectTab(true)
                showFragment(TravelListFragment(), true)
                true
            }
            R.id.menuSortPlace -> {
                TravelListFragment.sortMode = TravelListFragment.SortMode.PLACE
                selectTab(true)
                showFragment(TravelListFragment(), true)
                true
            }
            R.id.menuDeleteAll -> {
                confirmDeleteAll()
                true
            }
            R.id.menuInfo -> {
                try {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.app_name)
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
        Thread {
            val count = try {
                dbHelper.deleteAllTravels()
            } catch (_: Exception) {
                -1
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                if (count < 0) {
                    Toast.makeText(this, R.string.toast_delete_all_failed, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                Toast.makeText(this, getString(R.string.toast_delete_count, count), Toast.LENGTH_SHORT).show()
                selectTab(true)
                showFragment(TravelListFragment(), true)
            }
        }.start()
    }
}
