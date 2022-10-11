package edu.moravian.csci215.shake_and_play

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import kotlin.math.abs


/** The jerk/acceleration difference required to detect a shake, in m/s^2 */
const val SHAKE_THRESHOLD = 3f

/** The minimum amount of time allowed between shakes, in nanoseconds */
const val MIN_TIME_BETWEEN_SHAKES = 1000000000L

class MainActivity : AppCompatActivity(), SensorEventListener {
    /** Last recorded acceleration */
    private val lastAcceleration = floatArrayOf(0f, 0f, 0f)
    /** Last time a shake was detected */
    private var timestampOfLastChange: Long = 0
    /** If this is the first event or not since resuming */
    private var isFirstEvent = true

    /** Media player instance */
    private var mediaPlayer: MediaPlayer? = null
    /** All of the known audio files */
    private val audios = intArrayOf(
        R.raw.train, R.raw.pew, R.raw.monkey, R.raw.kid_laugh, R.raw.dial_tone, R.raw.cow, R.raw.laugh
    )

    // TODO: Variables for the sensor
    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO: code to acquire the sensor manager and accelerometer
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    /** When the activity starts, we create the media player. */
    override fun onStart() {
        super.onStart()
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.train)
    }

    /** When the activity stops, we release the media player. */
    override fun onStop() {
        super.onStop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // TODO: properly listen for (and stop listening for) accelerometer events (also need to update isFirstEvent)
    override fun onResume()
    {
        super.onResume()
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        isFirstEvent = true

    }

    override fun onPause()
    {
        super.onPause()
        sensorManager?.unregisterListener(this, sensor)

    }

    override fun onDestroy()
    {
        super.onDestroy()
        sensorManager = null
        sensor = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (isShake(event.values, event.timestamp))
            {
                setAudioResource(audios.random())
                togglePlayback()
            }
        }
    }

    /**
     * Checks if the acceleration values in x, y, and z represent a "shake"
     * operation: the jerk (difference in acceleration values) is greater than
     * the SHAKE_THRESHOLD in at least 2 dimensions.
     *
     * @param acceleration array of accelerations in the x, y, and z directions
     * @param timestamp timestamp of when the acceleration values were generated
     * @return true if the data represents a shake
     */
    private fun isShake(acceleration: FloatArray, timestamp: Long): Boolean {
        val isShake =
            (!isFirstEvent && timestamp - timestampOfLastChange >= MIN_TIME_BETWEEN_SHAKES) &&
            acceleration.zip(lastAcceleration).count { (a, b) -> abs(a - b) > SHAKE_THRESHOLD } >= 2
        // save for comparing to next time
        acceleration.copyInto(lastAcceleration)
        isFirstEvent = false
        if (isShake) { timestampOfLastChange = timestamp }
        return isShake
    }

    /**
     * Toggles play back of the media player. If it is currently playing, it is
     * stopped. If it is not currently playing, it is started.
     */
    private fun togglePlayback() {
        mediaPlayer?.apply {
            if (isPlaying) {
                // if currently playing, reset to beginning and pause
                seekTo(0)
                pause()
            } else {
                // if not currently playing, start playing
                start()
            }
        }
    }

    /**
     * Sets the audio being played from a resource ID. This re-uses the current
     * media player object.
     * @param resourceId the resource audio for the audio, like R.raw.monkey.
     */
    private fun setAudioResource(@RawRes resourceId: Int) {
        val afd = resources.openRawResourceFd(resourceId)
        try {
            mediaPlayer?.apply {
                reset()
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                prepare()
                afd.close()
            }
        } catch (ex: IOException) {
            Log.e("MainActivity", "set audio resource failed", ex)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}