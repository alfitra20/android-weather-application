package com.example.alfitrarahman.lab3

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import android.util.Log
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


/**
 * Created by alfitrarahman on 13/2/18.
 */
open class WeatherFetcher(context: Context) : AsyncTask<Context, Void, String>() {

    private var temperature = ""
    private var longitude = ""
    private var latitude = ""
    private var pictureCode = ""
    private var city = ""
    private var temperatureHi = ""
    private var temperatureLo = ""
    private var windSpeed = ""
    private var windDirection = ""
    private var weatherDesc = ""
    private var TAG = "MainActivity"
    @SuppressLint("StaticFieldLeak")
    private var context: Context = context

    @RequiresApi(Build.VERSION_CODES.O)
    override fun doInBackground(vararg p0: Context?): String? {
        Log.v(TAG, "+++++++++++++++++++++ENTER EXECUTOR+++++++++++++++++++++++")
        val builder = Uri.parse("http://api.openweathermap.org/data/2.5/weather?q=Oulu,fi&units=metric&appid=26753181c875ca3aa17a39cbf5baa818").buildUpon()
        //Add Request query Parameters (if any)
        val contentType = "application/json"
        val url = builder.toString()
        val path = URL(url)
        val connection = path.openConnection() as HttpURLConnection
        val timeout = 60 * 1000
        connection.readTimeout = timeout // set request timeout
        connection.connectTimeout = timeout
        connection.requestMethod = "GET" //set HTTP method
        //Request query Header (if needed)
        connection.setRequestProperty("contentType", contentType)
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            //oast.makeText(this, "Connection failed to established", Toast.LENGTH_LONG).show()
        }

        val stream = connection.inputStream
        val result: String
        val br = BufferedReader(InputStreamReader(stream))
        val responseBody = StringBuilder("")
        try {
            br.lineSequence().forEach{
                responseBody.append(it)
            }
        }catch (e: IOException){
            e.printStackTrace()
        }finally {
            br.close()
        }
        stream.close()

        result = responseBody.toString()

        val jObj = JSONObject(result)

        temperature = jObj.getJSONObject("main").getString("temp")
        longitude = jObj.getJSONObject("coord").getString("lon")
        latitude = jObj.getJSONObject("coord").getString("lat")
        city = jObj.getString("name")
        temperatureHi = jObj.getJSONObject("main").getString("temp_max")
        temperatureLo = jObj.getJSONObject("main").getString("temp_min")
        windSpeed = jObj.getJSONObject("wind").getString("speed")
        windDirection = jObj.getJSONObject("wind").getString("deg")

        val resultList = jObj.getJSONArray("weather")
        for (i in 0 until resultList.length()) {
            val entry = resultList.getJSONObject(i)
            pictureCode = entry.getString("icon")
            weatherDesc = entry.getString("description")
        }
        val preferences = PreferenceManager.getDefaultSharedPreferences(this.context)
        val timeTemp = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        val time = timeTemp.format(formatter)
        preferences.edit()
                .putString("temperature", temperature)
                .putString("longitude", longitude)
                .putString("latitude", latitude)
                .putString("city", city)
                .putString("temperatureHi", temperatureHi)
                .putString("temperatureLo", temperatureLo)
                .putString("windSpeed", windSpeed)
                .putString("windDirection", windDirection)
                .putString("pictureCode", pictureCode)
                .putString("weatherDesc", weatherDesc)
                .putString("lastUpdated", time.toString())
                .apply()

      return "Success"
    }
}