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
        findViewById<TextView>(R.id.textMainTitle).text = "여행을 기록하는 시간"
        findViewById<TextView>(R.id.textMainSubTitle).text = "사진, 메모, 방문일을 한곳에 정리하고 오래 남길 순간을 빠르게 찾아보세요."
        buttonTravelList.text = "기록 목록"
        buttonTravelMap.text = "지도 보기"

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
        menu.add(0, MENU_ADD, 0, "새 기록")
        menu.add(0, MENU_SORT_DATE, 1, "날짜순 정렬")
        menu.add(0, MENU_SORT_PLACE, 2, "이름순 정렬")
        menu.add(0, MENU_DELETE_ALL, 3, "전체 삭제")
        menu.add(0, MENU_INFO, 4, "앱 정보")
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
                    .setTitle("Momentrip")
                    .setMessage("SQLiteOpenHelper 기반 여행 기록 앱입니다.")
                    .setPositiveButton("확인", null)
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
            Toast.makeText(this, "화면 전환 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectTab(isListSelected: Boolean) {
        buttonTravelList.isSelected = isListSelected
        buttonTravelMap.isSelected = !isListSelected
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(this)
            .setTitle("전체 삭제")
            .setMessage("저장된 모든 여행 기록을 삭제할까요?")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                val count = dbHelper.deleteAllTravels()
                Toast.makeText(this, "${count}개 기록을 삭제했습니다.", Toast.LENGTH_SHORT).show()
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
