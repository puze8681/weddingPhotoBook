package kr.puze.weddingphotobook

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException

class PrefUtil(context: Context) {
    private var preferences: SharedPreferences = context.getSharedPreferences("Data", Context.MODE_PRIVATE)
    private var editor: SharedPreferences.Editor = preferences.edit()

    var KEY = "key"

    fun setStringArrayPref(key: String, values: ArrayList<String>) {
        val a = JSONArray()
        for (i in 0 until values.size) {
            a.put(values[i])
        }
        if (values.isNotEmpty()) {
            editor.putString(key, a.toString())
        } else {
            editor.putString(key, null)
        }
        editor.apply()
    }

    fun getStringArrayPref(key: String): ArrayList<String> {
        val json = preferences.getString(key, null)
        val urls = ArrayList<String>()
        if (json != null) {
            try {
                val a = JSONArray(json)
                for (i in 0 until a.length()) {
                    val url = a.optString(i)
                    urls.add(url)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return urls
    }

    fun addStringArrayPref(key: String, values: String){
        val list = getStringArrayPref(key)
        list.add(values)
        setStringArrayPref(key, list)
    }

    fun deleteStringArrayPref(key: String, position: Int){
        val list = getStringArrayPref(key)
        list.removeAt(position)
        setStringArrayPref(key, list)
    }
}