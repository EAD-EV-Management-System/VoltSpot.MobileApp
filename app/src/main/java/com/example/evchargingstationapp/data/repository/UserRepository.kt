package com.example.evchargingstationapp.data

import android.content.Context
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.example.evchargingstationapp.data.local.UserDbHelper
import com.example.evchargingstationapp.model.User
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class UserRepository(private val context: Context) {
    private val db = UserDbHelper(context)
    private val prefs = PrefsHelper(context)

    // helper: send POST with JSON body and parse response as JSONObject (or null)
    private fun postJson(urlString: String, jsonBody: JSONObject): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val out = BufferedWriter(OutputStreamWriter(conn.outputStream, "UTF-8"))
            out.write(jsonBody.toString())
            out.flush()
            out.close()

            val code = conn.responseCode
            val input = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = input.bufferedReader().use { it.readText() }
            JSONObject(text)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            conn?.disconnect()
        }
    }

    // register: send to server, on success save to local SQLite + prefs
    fun registerOnServer(serverBaseUrl: String, user: User, password: String, callback: (success:Boolean, message:String)->Unit) {
        Thread {
            val body = JSONObject().apply {
                put("nic", user.nic)
                put("name", user.name)
                put("email", user.email)
                put("password", password) // if you use passwords; adapt if not
            }
            val resp = postJson("$serverBaseUrl/api/owners/register", body)
            if (resp != null && resp.optBoolean("ok", false)) {
                val owner = resp.optJSONObject("owner")
                val token = resp.optString("token", "")
                // save token and user locally
                prefs.saveToken(token)
                prefs.saveNic(user.nic)
                // Insert owner into SQLite (use any returned data as ground truth)
                val serverUser = User(
                    nic = owner.optString("nic", user.nic),
                    name = owner.optString("name", user.name),
                    email = owner.optString("email", user.email),
                    password = password,  // 👈 add password from register/login
                    isActive = if (owner.optBoolean("isActive", true)) 1 else 0
                )
                db.insertOrUpdate(serverUser)
                // callback on main thread
                (context as? android.app.Activity)?.runOnUiThread { callback(true, "Registered") } ?: callback(true, "Registered")
            } else {
                val msg = resp?.optString("message") ?: "Server error"
                (context as? android.app.Activity)?.runOnUiThread { callback(false, msg) } ?: callback(false, msg)
            }
        }.start()
    }

    // login: similar
    fun login(serverBaseUrl: String, nic: String, password: String, callback: (success:Boolean, message:String)->Unit) {
        Thread {
            val body = JSONObject().apply {
                put("nic", nic)
                put("password", password)
            }
            val resp = postJson("$serverBaseUrl/api/owners/login", body)
            if (resp != null && resp.optBoolean("ok", false)) {
                val owner = resp.optJSONObject("owner")
                val token = resp.optString("token", "")
                prefs.saveToken(token)
                prefs.saveNic(nic)
                val serverUser = User(
                    nic = owner.optString("nic", nic),
                    name = owner.optString("name", ""),
                    email = owner.optString("email", ""),
                    password = password,  // 👈 add password from register/login
                    isActive = if (owner.optBoolean("isActive", true)) 1 else 0
                )
                db.insertOrUpdate(serverUser)
                (context as? android.app.Activity)?.runOnUiThread { callback(true, "Login success") } ?: callback(true, "Login success")
            } else {
                val msg = resp?.optString("message") ?: "Login failed"
                (context as? android.app.Activity)?.runOnUiThread { callback(false, msg) } ?: callback(false, msg)
            }
        }.start()
    }

    // local-only helpers
    fun getLocalUser(nic: String): User? = db.getUser(nic)
    fun deactivateLocalUser(nic: String) = db.setStatus(nic, 0)
}
