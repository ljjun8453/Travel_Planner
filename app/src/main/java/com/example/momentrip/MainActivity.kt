package com.example.momentrip

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val buttonTravelList = findViewById<Button>(R.id.buttonTravelList)
        val buttonTravelMap = findViewById<Button>(R.id.buttonTravelMap)

        buttonTravelList.setOnClickListener {
            showFragment(TravelListFragment(), true)
        }

        buttonTravelMap.setOnClickListener {
            showFragment(TravelMapFragment(), true)
        }

        if (savedInstanceState == null) {
            showFragment(TravelListFragment(), false)
        }
    }

    private fun showFragment(fragment: Fragment, addToBackStack: Boolean) {
        try {
            val tag = fragment.javaClass.simpleName
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

            if (currentFragment?.javaClass?.simpleName == tag) {
                return
            }

            val transaction = supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment, tag)

            if (addToBackStack) {
                transaction.addToBackStack(tag)
            }

            transaction.commit()
        } catch (e: Exception) {
            Log.e(TAG, "showFragment error", e)
            Toast.makeText(this, "화면 전환 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
