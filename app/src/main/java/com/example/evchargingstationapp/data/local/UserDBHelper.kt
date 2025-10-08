package com.example.evchargingstationapp.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.evchargingstationapp.model.User

class UserDbHelper(context: Context) :
    SQLiteOpenHelper(context, "ev_users.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE users (
                nic TEXT PRIMARY KEY,
                firstName TEXT,
                lastName TEXT,
                email TEXT,
                phoneNumber TEXT,
                password TEXT,
                status INTEGER
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    fun insertUser(user: User): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nic", user.nic)
            put("firstName", user.firstName)
            put("lastName", user.lastName)
            put("email", user.email)
            put("phoneNumber", user.phoneNumber)
            put("password", user.password)
            put("status", user.isActive)
        }
        val id = db.insert("users", null, values)
        db.close()
        return id != -1L
    }

    fun getUser(nic: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            "users",
            arrayOf("nic", "firstName", "lastName", "email", "phoneNumber", "password", "status"),
            "nic=? AND status=1",
            arrayOf(nic),
            null, null, null
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                nic = cursor.getString(0),
                firstName = cursor.getString(1),
                lastName = cursor.getString(2),
                email = cursor.getString(3),
                phoneNumber = cursor.getString(4),
                password = cursor.getString(5),
                isActive = cursor.getInt(6)
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun updateUser(user: User): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("firstName", user.firstName)
            put("lastName", user.lastName)
            put("email", user.email)
            put("phoneNumber", user.phoneNumber)
            put("password", user.password)
        }
        val rows = db.update("users", values, "nic=?", arrayOf(user.nic))
        db.close()
        return rows > 0
    }

    fun deactivateUser(nic: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply { put("status", 0) }
        val rows = db.update("users", values, "nic=?", arrayOf(nic))
        db.close()
        return rows > 0
    }

    fun setStatus(nic: String, isActive: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply { put("status", isActive) }
        val rows = db.update("users", values, "nic=?", arrayOf(nic))
        db.close()
        return rows > 0
    }

    fun insertOrUpdate(user: User): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("nic", user.nic)
            put("firstName", user.firstName)
            put("lastName", user.lastName)
            put("email", user.email)
            put("phoneNumber", user.phoneNumber)
            put("password", user.password ?: "") // handle null
            put("status", user.isActive)
        }
        val rows = db.update("users", values, "nic=?", arrayOf(user.nic))
        if (rows == 0) {
            // no existing user → insert
            val id = db.insert("users", null, values)
            return id != -1L
        }
        return true
    }

}
