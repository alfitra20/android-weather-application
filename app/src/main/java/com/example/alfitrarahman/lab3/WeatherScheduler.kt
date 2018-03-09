package com.example.alfitrarahman.lab3

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.app.PendingIntent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.support.v4.app.TaskStackBuilder
import android.util.Log


/**
 * Created by alfitrarahman on 13/2/18.
 */
 class WeatherScheduler: JobService() {

    private lateinit var weatherFetcher: WeatherFetcher
    @SuppressLint("StaticFieldLeak")
    private var TAG = "MainActivity"

    override fun onStartJob(jobParameters: JobParameters?): Boolean {
        Log.v(TAG, "+++++++++++++++++++++ENTER SCHEDULER++++++++++++++++++++++++")
        weatherFetcher = @SuppressLint("StaticFieldLeak")
        object:WeatherFetcher(this){

            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)
                Log.v(TAG, "+++++++++++++++++++++ENTER SCHEDULER++++++++++++++++++++++++")
                val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                var mNotificationId = 1
                var temperature = preferences.getString("temperature","DEFAULT")
                var city = preferences.getString("city", "DEFAULT")
                var weatherDesc = preferences.getString("weatherDesc", "DEFAULT")
                var pictureCode = preferences.getString("pictureCode", "DEFAULT")

                if (Build.VERSION.SDK_INT >= 26) {
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

                    val resultIntent = Intent(applicationContext, MainActivity::class.java)
                    val stackBuilder = TaskStackBuilder.create(applicationContext)
                    stackBuilder.addParentStack(MainActivity::class.java)
                    stackBuilder.addNextIntent(resultIntent)
                    val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
                    mBuilder.setContentIntent(resultPendingIntent)
                    mNotificationManager.notify(1, mBuilder.build())
                    Log.v(TAG, "+++++++++++++++++++++NOTIFICATION CREATED++++++++++++++++++++++++")
                }else{

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
                    Log.v(TAG, "+++++++++++++++++++++NOTIFICATION CREATED++++++++++++++++++++++++")
                }

                jobFinished(jobParameters, false)
            }
        }
        Log.v(TAG, "+++++++++++++++++++++FINISH SCHEDULER++++++++++++++++++++++++")
        weatherFetcher.execute(applicationContext)
        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        weatherFetcher.cancel(true)
        return false
    }
}




