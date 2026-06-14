package com.example.momentrip

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class TravelDBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.execSQL(CREATE_TABLE_TRAVEL)
            db.execSQL(CREATE_TABLE_PLAN)
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            if (oldVersion < 2) {
                db.execSQL(CREATE_TABLE_PLAN)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onUpgrade error", e)
        }
    }

    fun insertTravel(record: TravelRecord): Long {
        return try {
            writableDatabase.insert(TABLE_TRAVEL, null, createContentValues(record))
        } catch (e: Exception) {
            Log.e(TAG, "insertTravel error", e)
            -1L
        }
    }

    fun getAllTravels(): ArrayList<TravelRecord> {
        return try {
            getTravelsByQuery("SELECT * FROM $TABLE_TRAVEL", null)
        } catch (e: Exception) {
            Log.e(TAG, "getAllTravels error", e)
            arrayListOf()
        }
    }

    fun getAllTravelsOrderByDate(): ArrayList<TravelRecord> {
        return try {
            getTravelsByQuery("SELECT * FROM $TABLE_TRAVEL ORDER BY $SQL_COLUMN_VISIT_DATE ASC", null)
        } catch (e: Exception) {
            Log.e(TAG, "getAllTravelsOrderByDate error", e)
            arrayListOf()
        }
    }

    fun getAllTravelsOrderByPlace(): ArrayList<TravelRecord> {
        return try {
            getTravelsByQuery("SELECT * FROM $TABLE_TRAVEL ORDER BY $SQL_COLUMN_PLACE ASC", null)
        } catch (e: Exception) {
            Log.e(TAG, "getAllTravelsOrderByPlace error", e)
            arrayListOf()
        }
    }

    fun getTravel(no: Int): TravelRecord? {
        return try {
            readableDatabase.query(
                TABLE_TRAVEL,
                null,
                "$SQL_COLUMN_NO = ?",
                arrayOf(no.toString()),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursorToTravelRecord(cursor)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTravel error", e)
            null
        }
    }

    fun updateTravel(record: TravelRecord): Int {
        return try {
            writableDatabase.update(
                TABLE_TRAVEL,
                createContentValues(record),
                "$SQL_COLUMN_NO = ?",
                arrayOf(record.no.toString())
            )
        } catch (e: Exception) {
            Log.e(TAG, "updateTravel error", e)
            0
        }
    }

    fun deleteTravel(no: Int): Int {
        return try {
            writableDatabase.delete(TABLE_TRAVEL, "$SQL_COLUMN_NO = ?", arrayOf(no.toString()))
        } catch (e: Exception) {
            Log.e(TAG, "deleteTravel error", e)
            0
        }
    }

    fun deleteAllTravels(): Int {
        return try {
            writableDatabase.delete(TABLE_TRAVEL, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAllTravels error", e)
            0
        }
    }

    fun getTravelsWithLocation(): ArrayList<TravelRecord> {
        return try {
            getTravelsByQuery(
                "SELECT * FROM $TABLE_TRAVEL WHERE $SQL_COLUMN_LATITUDE IS NOT NULL AND $SQL_COLUMN_LONGITUDE IS NOT NULL",
                null
            )
        } catch (e: Exception) {
            Log.e(TAG, "getTravelsWithLocation error", e)
            arrayListOf()
        }
    }

    fun insertPlan(plan: TravelPlan): Long {
        return try {
            writableDatabase.insert(TABLE_PLAN, null, createPlanContentValues(plan))
        } catch (e: Exception) {
            Log.e(TAG, "insertPlan error", e)
            -1L
        }
    }

    fun getAllPlans(): ArrayList<TravelPlan> {
        return try {
            val plans = arrayListOf<TravelPlan>()
            readableDatabase.rawQuery("SELECT * FROM $TABLE_PLAN ORDER BY $SQL_COLUMN_PLAN_DATE ASC", null).use { cursor ->
                while (cursor.moveToNext()) {
                    plans.add(cursorToTravelPlan(cursor))
                }
            }
            plans
        } catch (e: Exception) {
            Log.e(TAG, "getAllPlans error", e)
            arrayListOf()
        }
    }

    fun deletePlan(no: Int): Int {
        return try {
            writableDatabase.delete(TABLE_PLAN, "$SQL_COLUMN_NO = ?", arrayOf(no.toString()))
        } catch (e: Exception) {
            Log.e(TAG, "deletePlan error", e)
            0
        }
    }

    private fun getTravelsByQuery(sql: String, selectionArgs: Array<String>?): ArrayList<TravelRecord> {
        val records = arrayListOf<TravelRecord>()

        readableDatabase.rawQuery(sql, selectionArgs).use { cursor ->
            while (cursor.moveToNext()) {
                records.add(cursorToTravelRecord(cursor))
            }
        }

        return records
    }

    private fun createContentValues(record: TravelRecord): ContentValues {
        val values = ContentValues()
        values.put(COLUMN_PLACE, record.place)
        values.put(COLUMN_VISIT_DATE, record.visitDate)
        putNullableString(values, COLUMN_MEMO, record.memo)
        putNullableString(values, COLUMN_PHOTO_URI, record.photoUri)
        putNullableDouble(values, COLUMN_LATITUDE, record.latitude)
        putNullableDouble(values, COLUMN_LONGITUDE, record.longitude)
        return values
    }

    private fun createPlanContentValues(plan: TravelPlan): ContentValues {
        val values = ContentValues()
        values.put(COLUMN_PLACE, plan.place)
        values.put(COLUMN_PLAN_DATE, plan.planDate)
        putNullableString(values, COLUMN_MEMO, plan.memo)
        return values
    }

    private fun cursorToTravelRecord(cursor: Cursor): TravelRecord {
        val memoIndex = cursor.getColumnIndexOrThrow(COLUMN_MEMO)
        val photoUriIndex = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_URI)
        val latitudeIndex = cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)
        val longitudeIndex = cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)

        return TravelRecord(
            no = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NO)),
            place = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLACE)),
            visitDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VISIT_DATE)),
            memo = if (cursor.isNull(memoIndex)) null else cursor.getString(memoIndex),
            photoUri = if (cursor.isNull(photoUriIndex)) null else cursor.getString(photoUriIndex),
            latitude = if (cursor.isNull(latitudeIndex)) null else cursor.getDouble(latitudeIndex),
            longitude = if (cursor.isNull(longitudeIndex)) null else cursor.getDouble(longitudeIndex)
        )
    }

    private fun cursorToTravelPlan(cursor: Cursor): TravelPlan {
        val memoIndex = cursor.getColumnIndexOrThrow(COLUMN_MEMO)

        return TravelPlan(
            no = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NO)),
            place = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLACE)),
            planDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLAN_DATE)),
            memo = if (cursor.isNull(memoIndex)) null else cursor.getString(memoIndex)
        )
    }

    private fun putNullableString(values: ContentValues, key: String, value: String?) {
        if (value == null) {
            values.putNull(key)
        } else {
            values.put(key, value)
        }
    }

    private fun putNullableDouble(values: ContentValues, key: String, value: Double?) {
        if (value == null) {
            values.putNull(key)
        } else {
            values.put(key, value)
        }
    }

    companion object {
        private const val DATABASE_NAME = "momentrip.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_TRAVEL = "travel_record"
        private const val TABLE_PLAN = "travel_plan"
        private const val COLUMN_NO = "no"
        private const val COLUMN_PLACE = "place"
        private const val COLUMN_VISIT_DATE = "visit_date"
        private const val COLUMN_PLAN_DATE = "plan_date"
        private const val COLUMN_MEMO = "memo"
        private const val COLUMN_PHOTO_URI = "photo_uri"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val SQL_COLUMN_NO = "\"$COLUMN_NO\""
        private const val SQL_COLUMN_PLACE = "\"$COLUMN_PLACE\""
        private const val SQL_COLUMN_VISIT_DATE = "\"$COLUMN_VISIT_DATE\""
        private const val SQL_COLUMN_PLAN_DATE = "\"$COLUMN_PLAN_DATE\""
        private const val SQL_COLUMN_MEMO = "\"$COLUMN_MEMO\""
        private const val SQL_COLUMN_PHOTO_URI = "\"$COLUMN_PHOTO_URI\""
        private const val SQL_COLUMN_LATITUDE = "\"$COLUMN_LATITUDE\""
        private const val SQL_COLUMN_LONGITUDE = "\"$COLUMN_LONGITUDE\""
        private const val TAG = "TravelDBHelper"
        private const val CREATE_TABLE_TRAVEL =
            "CREATE TABLE $TABLE_TRAVEL (" +
                "$SQL_COLUMN_NO INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$SQL_COLUMN_PLACE TEXT NOT NULL, " +
                "$SQL_COLUMN_VISIT_DATE TEXT NOT NULL, " +
                "$SQL_COLUMN_MEMO TEXT, " +
                "$SQL_COLUMN_PHOTO_URI TEXT, " +
                "$SQL_COLUMN_LATITUDE REAL, " +
                "$SQL_COLUMN_LONGITUDE REAL" +
                ")"
        private const val CREATE_TABLE_PLAN =
            "CREATE TABLE IF NOT EXISTS $TABLE_PLAN (" +
                "$SQL_COLUMN_NO INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$SQL_COLUMN_PLACE TEXT NOT NULL, " +
                "$SQL_COLUMN_PLAN_DATE TEXT NOT NULL, " +
                "$SQL_COLUMN_MEMO TEXT" +
                ")"
    }
}
