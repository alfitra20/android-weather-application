package com.example.alfitrarahman.lab3

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Build.VERSION_CODES.O
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class MainActivity : AppCompatActivity() {
    private val JOB_ID = 101
    private lateinit var jobScheduler: JobScheduler
    private lateinit var jobInfo: JobInfo

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
    private var lastUpdated = ""
    private var TAG = "MainActivity"

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val componentName = ComponentName(this, WeatherScheduler::class.java)
        val builder =  JobInfo.Builder(JOB_ID, componentName)
        var minimum :Long = 600000
        var flex :Long = 700000
        if (Build.VERSION.SDK_INT >= 24) {
            builder.setPeriodic(minimum, flex)
        }else{
            builder.setPeriodic(minimum)
        }
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        builder.setPersisted(true)
        jobInfo = builder.build()
        jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(jobInfo)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        temperature = preferences.getString("temperature", "")

        if (temperature == ""){
            initializeWeather(this).execute()
            displayData()
            if (Build.VERSION.SDK_INT >= 26) {
                displayNotification26()
            }else{
                displayNotification()
            }
        }
        else {
            displayData()
        }
        refreshButton.setOnClickListener {
            initializeWeather(this).execute()
            displayData()
            initializeWeather(this).cancel(true)
        }
    }

   private fun displayData(){
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        temperature = preferences.getString("temperature", "")
        city = preferences.getString("city", "")
        weatherDesc = preferences.getString("weatherDesc", "")
        longitude = preferences.getString("longitude", "")
        latitude = preferences.getString("latitude", "")
        pictureCode = preferences.getString("pictureCode", "")
        temperatureHi = preferences.getString("temperatureHi", "")
        temperatureLo = preferences.getString("temperatureLo", "")
        windSpeed = preferences.getString("windSpeed", "")
        windDirection = preferences.getString("windDirection", "")
        lastUpdated = preferences.getString("lastUpdated", "")

        locationText.text = this.city
        temperatureText.text = this.temperature+" c"
        Log.v(TAG, "++++++++++++++++++http://openweathermap.org/img/w/"+pictureCode)
        Picasso.with(this).load("http://openweathermap.org/img/w/"+pictureCode+".png").into(weatherImage)
        weatherText.text = this.weatherDesc
        temperatureHiLoText.text = this.temperatureHi+" | "+ this.temperatureLo
        windText.text = windSpeed+" | "+ this.windDirection
        lastUpdateText.text = this.lastUpdated
   }

    private fun displayNotification(){
        var mNotificationId = 1
        val CHANNEL_ID = "my_channel_01"
        var uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) as Uri

        val mBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                //.setSmallIcon(Picasso.with(applicationContext).load("http://openweathermap.org/img/w/"+pictureCode+".png").fetch())
                .setSmallIcon(R.id.icon)
                .setContentTitle(temperature)
                .setContentText(city +" - "+ weatherDesc)
                .setSound(uri)

        // Creates an explicit intent for an Activity in your app
        val resultIntent = Intent(applicationContext, MainActivity::class.java)
        val stackBuilder = TaskStackBuilder.create(applicationContext)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(resultIntent)
        val resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        mBuilder.setContentIntent(resultPendingIntent)
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(mNotificationId, mBuilder.build())
    }

    @RequiresApi(26)
    private fun displayNotification26(){
        val id = "1"
        val name = "my Channel"
        val description = "Description"
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel(id, name, importance)
        mChannel.description = description
        mChannel.enableLights(true)
        mChannel.lightColor = Color.RED
        mChannel.enableVibration(true)
        mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)

        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.createNotificationChannel(mChannel)


        val mBuilder = Notification.Builder(applicationContext)
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .setContentTitle(temperature)
                .setContentText(city +" - "+ weatherDesc)
                .setChannelId(id)

        val resultIntent = Intent(this, MainActivity::class.java)
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(resultIntent)
        val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        mBuilder.setContentIntent(resultPendingIntent)
        mNotificationManager.notify(1, mBuilder.build())
    }

    private class initializeWeather(context: Context) : AsyncTask<Void, Void, String>(){
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
        private var lastUpdated = ""
        private var TAG = "MainActivity"
        private var contextRef: Context = context

        @RequiresApi(O)
        override fun doInBackground(vararg p0: Void?): String? {
            Log.v(TAG, "ENTER INITIALIZER")
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
                //longToast("Connection failed to established")
            }

            val stream = connection.inputStream
            val result: String
            val br = BufferedReader(InputStreamReader(stream, "UTF-8"))
            val responseBody = StringBuilder("")
            try {
                br.lineSequence().forEach{
                    responseBody.append(it)
                }
            }catch (e:IOException){
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

            Log.v(TAG, "temperature = " + temperature)

            if (contextRef != null) {
                Log.v(TAG, "++++++++++++++++++CONTEXT NOT NULL+++++++++++++++++++++++++++")
                val preferences = PreferenceManager.getDefaultSharedPreferences(this.contextRef)
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
                return "Executed"
            }else{
                return "ERROR"
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        jobScheduler.cancel(JOB_ID)
    }

}
