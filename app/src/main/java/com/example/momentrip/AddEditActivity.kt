package com.example.momentrip

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class AddEditActivity : AppCompatActivity() {
    private lateinit var dbHelper: TravelDBHelper
    private lateinit var editPlace: EditText
    private lateinit var editVisitDate: EditText
    private lateinit var editMemo: EditText
    private lateinit var editLatitude: EditText
    private lateinit var editLongitude: EditText
    private lateinit var imagePhoto: ImageView
    private lateinit var progressBar: ProgressBar
    private var recordNo = 0
    private var selectedPhotoUri: String? = null
    private var cameraPhotoUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedPhotoUri = uri.toString()
                    takePersistableUriPermissionSafe(uri)
                    showPhoto(uri)
                    applyGpsFromPhoto(uri)
                }
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_image_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                cameraPhotoUri?.let { uri ->
                    selectedPhotoUri = uri.toString()
                    showPhoto(uri)
                    applyGpsFromPhoto(uri)
                }
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_image_failed, Toast.LENGTH_SHORT).show()
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
        progressBar = findViewById(R.id.progressAddEdit)

        findViewById<Button>(R.id.buttonPickGallery).setOnClickListener { openGallery() }
        findViewById<Button>(R.id.buttonTakePhoto).setOnClickListener { openCamera() }
        findViewById<Button>(R.id.buttonSave).setOnClickListener { saveRecord() }
        findViewById<Button>(R.id.buttonCancel).setOnClickListener { finish() }

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
        progressBar.visibility = View.VISIBLE
        Thread {
            val record = try {
                dbHelper.getTravel(recordNo)
            } catch (_: Exception) {
                null
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                if (record == null) {
                    Toast.makeText(this, R.string.toast_record_not_found, Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }
                editPlace.setText(record.place)
                editVisitDate.setText(record.visitDate)
                editMemo.setText(record.memo.orEmpty())
                editLatitude.setText(record.latitude?.toString().orEmpty())
                editLongitude.setText(record.longitude?.toString().orEmpty())
                selectedPhotoUri = record.photoUri
                selectedPhotoUri?.let { showPhoto(Uri.parse(it)) }
            }
        }.start()
    }

    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/webp"))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            galleryLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_gallery_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        try {
            val uri = createCameraUri() ?: run {
                Toast.makeText(this, R.string.toast_camera_failed, Toast.LENGTH_SHORT).show()
                return
            }
            cameraPhotoUri = uri
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (intent.resolveActivity(packageManager) == null) {
                Toast.makeText(this, R.string.toast_camera_failed, Toast.LENGTH_SHORT).show()
                return
            }
            cameraLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_camera_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCameraUri(): Uri? {
        return try {
            val imageDir = File(getExternalFilesDir(null), "travel_photos")
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }
            val imageFile = File(imageDir, "travel_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
        } catch (_: Exception) {
            null
        }
    }

    private fun saveRecord() {
        try {
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
            saveRecordToDatabase(record)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.toast_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRecordToDatabase(record: TravelRecord) {
        progressBar.visibility = View.VISIBLE
        Thread {
            val result = try {
                if (recordNo > 0) dbHelper.updateTravel(record).toLong() else dbHelper.insertTravel(record)
            } catch (_: Exception) {
                -1L
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                if (result > 0) {
                    Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, R.string.toast_save_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showPhoto(uri: Uri) {
        try {
            imagePhoto.setImageURI(uri)
        } catch (_: Exception) {
            imagePhoto.setImageResource(R.drawable.ic_launcher_foreground)
            Toast.makeText(this, R.string.toast_image_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyGpsFromPhoto(uri: Uri) {
        progressBar.visibility = View.VISIBLE
        Thread {
            val latLong = readGpsFromPhoto(uri)
            runOnUiThread {
                progressBar.visibility = View.GONE
                if (latLong == null) {
                    Toast.makeText(this, R.string.toast_gps_missing, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                editLatitude.setText(latLong[0].toDouble().toString())
                editLongitude.setText(latLong[1].toDouble().toString())
            }
        }.start()
    }

    private fun readGpsFromPhoto(uri: Uri): FloatArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val latLong = FloatArray(2)
                if (ExifInterface(stream).getLatLong(latLong)) latLong else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun takePersistableUriPermissionSafe(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val EXTRA_RECORD_NO = "record_no"

        fun start(context: Context, recordNo: Int = 0) {
            context.startActivity(Intent(context, AddEditActivity::class.java).putExtra(EXTRA_RECORD_NO, recordNo))
        }
    }
}
