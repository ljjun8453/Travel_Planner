package com.example.momentrip

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class TravelEditActivity : AppCompatActivity() {
    private lateinit var dbHelper: TravelDBHelper
    private lateinit var editPlace: EditText
    private lateinit var editVisitDate: EditText
    private lateinit var editMemo: EditText
    private lateinit var editLatitude: EditText
    private lateinit var editLongitude: EditText
    private lateinit var imagePhoto: ImageView
    private var recordNo = 0
    private var selectedPhotoUri: String? = null

    private val photoPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedPhotoUri = uri.toString()
                takePersistableUriPermissionSafe(uri)
                imagePhoto.setImageURI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_travel_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbHelper = TravelDBHelper(this)
        recordNo = intent.getIntExtra(EXTRA_RECORD_NO, 0)
        editPlace = findViewById(R.id.editPlace)
        editVisitDate = findViewById(R.id.editVisitDate)
        editMemo = findViewById(R.id.editMemo)
        editLatitude = findViewById(R.id.editLatitude)
        editLongitude = findViewById(R.id.editLongitude)
        imagePhoto = findViewById(R.id.imagePhoto)

        findViewById<Button>(R.id.buttonPickPhoto).setOnClickListener { pickPhoto() }
        findViewById<Button>(R.id.buttonSave).setOnClickListener { saveRecord() }

        if (recordNo > 0) {
            title = getString(R.string.edit_title_update)
            loadRecord()
        } else {
            title = getString(R.string.edit_title_new)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun loadRecord() {
        val record = dbHelper.getTravel(recordNo) ?: run {
            Toast.makeText(this, R.string.toast_record_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        editPlace.setText(record.place)
        editVisitDate.setText(record.visitDate)
        editMemo.setText(record.memo.orEmpty())
        editLatitude.setText(record.latitude?.toString().orEmpty())
        editLongitude.setText(record.longitude?.toString().orEmpty())
        selectedPhotoUri = record.photoUri
        selectedPhotoUri?.let { imagePhoto.setImageURI(Uri.parse(it)) }
    }

    private fun pickPhoto() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/webp"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        photoPicker.launch(intent)
    }

    private fun saveRecord() {
        val place = editPlace.text.toString().trim()
        val visitDate = editVisitDate.text.toString().trim()
        if (place.isEmpty() || visitDate.isEmpty()) {
            Toast.makeText(this, R.string.toast_place_date_required, Toast.LENGTH_SHORT).show()
            return
        }

        val record = TravelRecord(
            no = recordNo,
            place = place,
            visitDate = visitDate,
            memo = editMemo.text.toString().trim().ifEmpty { null },
            photoUri = selectedPhotoUri,
            latitude = editLatitude.text.toString().trim().toDoubleOrNull(),
            longitude = editLongitude.text.toString().trim().toDoubleOrNull()
        )
        val result = if (recordNo > 0) dbHelper.updateTravel(record).toLong() else dbHelper.insertTravel(record)
        if (result > 0) {
            Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, R.string.toast_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun takePersistableUriPermissionSafe(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
        }
    }

    companion object {
        private const val EXTRA_RECORD_NO = "record_no"

        fun start(context: Context, recordNo: Int = 0) {
            context.startActivity(Intent(context, TravelEditActivity::class.java).putExtra(EXTRA_RECORD_NO, recordNo))
        }
    }
}
