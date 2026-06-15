package com.example.momentrip

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.momentrip.databinding.ActivityTravelEditBinding
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTravelEditBinding
    private lateinit var dbHelper: TravelDBHelper

    private var recordNo = 0
    private val selectedPhotoUris = mutableListOf<String>()
    private var cameraPhotoUri: Uri? = null

    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedPlaceName = ""

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val uris = mutableListOf<Uri>()
                val clipData = result.data?.clipData

                if (clipData != null) {
                    for (index in 0 until clipData.itemCount) {
                        uris.add(clipData.getItemAt(index).uri)
                    }
                } else {
                    result.data?.data?.let { uri ->
                        uris.add(uri)
                    }
                }

                if (uris.isNotEmpty()) {
                    selectedPhotoUris.clear()

                    uris.forEach { uri ->
                        selectedPhotoUris.add(uri.toString())
                        takePersistableUriPermissionSafe(uri)
                    }

                    showPhoto(uris.first())
                    applyGpsFromPhotos(uris)
                }
            }
        } catch (_: Exception) {
            Toast.makeText(this, "이미지를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            showDefaultPhoto()
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = cameraPhotoUri ?: return@registerForActivityResult

                selectedPhotoUris.clear()
                selectedPhotoUris.add(uri.toString())

                showPhoto(uri)
                applyGpsFromPhotos(listOf(uri))
            }
        } catch (_: Exception) {
            Toast.makeText(this, "카메라 사진을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            showDefaultPhoto()
        }
    }

    private val locationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult

                val place = data.getStringExtra(LocationPickerActivity.EXTRA_PLACE).orEmpty()
                val latitude = data.getDoubleExtra(LocationPickerActivity.EXTRA_LATITUDE, Double.NaN)
                val longitude = data.getDoubleExtra(LocationPickerActivity.EXTRA_LONGITUDE, Double.NaN)

                if (!latitude.isNaN() && !longitude.isNaN()) {
                    selectedPlaceName = place
                    selectedLatitude = latitude
                    selectedLongitude = longitude

                    if (binding.editPlace.text.toString().trim().isEmpty() && place.isNotBlank()) {
                        binding.editPlace.setText(place)
                    }

                    updateSelectedLocationText()
                }
            }
        } catch (_: Exception) {
            Toast.makeText(this, "위치 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTravelEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbHelper = TravelDBHelper(this)
        recordNo = intent.getIntExtra(EXTRA_RECORD_NO, 0)

        binding.buttonPickGallery.setOnClickListener {
            openGallery()
        }

        binding.buttonTakePhoto.setOnClickListener {
            openCamera()
        }

        binding.buttonSave.setOnClickListener {
            saveRecord()
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }

        binding.editVisitDate.setOnClickListener {
            showDateTimePicker()
        }

        binding.buttonSelectLocation.setOnClickListener {
            openLocationPicker()
        }

        showDefaultPhoto()
        updateSelectedLocationText()

        if (recordNo > 0) {
            title = "여행 기록 수정"
            loadRecord()
        } else {
            title = "새 여행 기록"
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
        binding.progressAddEdit.visibility = View.VISIBLE

        lifecycleScope.launch {
            val record = withContext(Dispatchers.IO) {
                try {
                    dbHelper.getTravel(recordNo)
                } catch (_: Exception) {
                    null
                }
            }

            binding.progressAddEdit.visibility = View.GONE

            if (record == null) {
                Toast.makeText(this@AddEditActivity, "기록을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            binding.editPlace.setText(record.place)
            binding.editVisitDate.setText(record.visitDate)
            binding.editMemo.setText(record.memo.orEmpty())

            selectedLatitude = record.latitude
            selectedLongitude = record.longitude
            selectedPlaceName = record.place

            selectedPhotoUris.clear()
            selectedPhotoUris.addAll(TravelPhotoStore.split(record.photoUri))

            val firstPhoto = selectedPhotoUris.firstOrNull()

            if (firstPhoto.isNullOrBlank()) {
                showDefaultPhoto()
            } else {
                showPhoto(Uri.parse(firstPhoto))
            }

            updateSelectedLocationText()
        }
    }

    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/webp"))
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }

            galleryLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "갤러리를 열지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        try {
            val uri = createCameraUri()

            if (uri == null) {
                Toast.makeText(this, "카메라 파일을 만들지 못했습니다.", Toast.LENGTH_SHORT).show()
                return
            }

            cameraPhotoUri = uri

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(packageManager) == null) {
                Toast.makeText(this, "카메라 앱을 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return
            }

            cameraLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "카메라를 실행하지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCameraUri(): Uri? {
        return try {
            val imageDir = File(getExternalFilesDir(null), "travel_photos")

            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }

            val imageFile = File(imageDir, "travel_${System.currentTimeMillis()}.jpg")

            FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                imageFile
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun showDateTimePicker() {
        try {
            DateTimeRangePicker.show(this) { value ->
                binding.editVisitDate.setText(value)
            }
        } catch (_: Exception) {
            Toast.makeText(this, "날짜 선택창을 열지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openLocationPicker() {
        try {
            locationLauncher.launch(LocationPickerActivity.createIntent(this))
        } catch (_: Exception) {
            Toast.makeText(this, "위치 선택 화면으로 이동하지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRecord() {
        try {
            val place = binding.editPlace.text.toString().trim()
            val visitDate = binding.editVisitDate.text.toString().trim()

            if (place.isEmpty() || visitDate.isEmpty()) {
                Toast.makeText(this, R.string.toast_input_required, Toast.LENGTH_SHORT).show()
                return
            }

            val record = TravelRecord(
                no = recordNo,
                place = place,
                visitDate = visitDate,
                memo = binding.editMemo.text.toString().trim().ifEmpty { null },
                photoUri = TravelPhotoStore.join(selectedPhotoUris),
                latitude = selectedLatitude,
                longitude = selectedLongitude
            )

            saveRecordToDatabase(record)
        } catch (_: Exception) {
            Toast.makeText(this, "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRecordToDatabase(record: TravelRecord) {
        binding.progressAddEdit.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    if (recordNo > 0) {
                        dbHelper.updateTravel(record).toLong()
                    } else {
                        dbHelper.insertTravel(record)
                    }
                } catch (_: Exception) {
                    -1L
                }
            }

            binding.progressAddEdit.visibility = View.GONE

            if (result > 0) {
                Toast.makeText(this@AddEditActivity, R.string.toast_saved, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@AddEditActivity, "저장하지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPhoto(uri: Uri) {
        try {
            binding.imagePhoto.setImageURI(uri)
        } catch (_: Exception) {
            showDefaultPhoto()
            Toast.makeText(this, "이미지를 표시하지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDefaultPhoto() {
        try {
            binding.imagePhoto.setImageResource(R.drawable.momentrip1)
        } catch (_: Exception) {
            binding.imagePhoto.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

    private fun applyGpsFromPhotos(uris: List<Uri>) {
        binding.progressAddEdit.visibility = View.VISIBLE

        lifecycleScope.launch {
            val latLong = withContext(Dispatchers.IO) {
                try {
                    uris.firstNotNullOfOrNull { uri ->
                        readGpsFromPhoto(uri)
                    }
                } catch (_: Exception) {
                    null
                }
            }

            binding.progressAddEdit.visibility = View.GONE

            if (latLong == null) {
                Toast.makeText(this@AddEditActivity, "사진에 GPS 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            selectedLatitude = latLong[0].toDouble()
            selectedLongitude = latLong[1].toDouble()

            updateSelectedLocationText()
        }
    }

    private fun updateSelectedLocationText() {
        val latitude = selectedLatitude
        val longitude = selectedLongitude

        binding.textSelectedLocation.text = if (latitude == null || longitude == null) {
            getString(R.string.selected_location_empty)
        } else {
            val placeName = selectedPlaceName.ifBlank {
                binding.editPlace.text.toString().trim()
            }

            if (placeName.isBlank()) {
                "위도 $latitude · 경도 $longitude"
            } else {
                "$placeName · 위도 $latitude · 경도 $longitude"
            }
        }
    }

    private fun readGpsFromPhoto(uri: Uri): FloatArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val latLong = FloatArray(2)

                if (ExifInterface(stream).getLatLong(latLong)) {
                    latLong
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun takePersistableUriPermissionSafe(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val EXTRA_RECORD_NO = "record_no"

        fun start(context: Context, recordNo: Int = 0) {
            val intent = Intent(context, AddEditActivity::class.java)
            intent.putExtra(EXTRA_RECORD_NO, recordNo)
            context.startActivity(intent)
        }
    }
}