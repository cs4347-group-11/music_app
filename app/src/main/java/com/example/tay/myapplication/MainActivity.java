package com.example.tay.myapplication;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    /*
     * Sensor variables to be used in-app.
     *
     * List of sensors we don't have across all phones:
     * a) Gyroscope
     * b) Game Rotation
     * c) Geomagnetic Rotation
     */
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mRotation;
    private Sensor mRotationVector;

    private AudioManager audioManager;

    /*
     * Variables for striking motion detection and handling
     */
    // Variables for managing striking motion sound generation
    private SoundPool strikeSoundPool;
    private boolean strikeSoundLoaded;
    private int sampleStrikeSoundId;

    // Variables for detecting downward-striking motion
    private static final int SHAKE_UPPER_THRESHOLD = 800;
    private static final int SHAKE_LOWER_THRESHOLD = 700;

    float lastX;
    float lastY;
    float lastZ;
    long prevTime;
    float prevSpeed;

    float strikingVolume;
    float maxStrikingVolume;


    /*
     * Variables for rotate-left-and-right gesture detection and handling
     */
    // Variables for managing rotate-90-degrees-left motion sound generation
    private boolean offsetOneSoundLoaded;
    private int sampleOffsetOneSoundId;

    // Variables for managing rotate-180-degrees-left motion sound generation
    private boolean offsetTwoSoundLoaded;
    private int sampleOffsetTwoSoundId;

    // Variables for detecting left/right rotation
    double lastRoll;
    long prevRollTime;

    private static final int LEVEL_OFFSET_0 = 0;
    private static final int LEVEL_OFFSET_1 = 1;
    private static final int LEVEL_OFFSET_2 = 2;
    int toPlay;

    /*
     * UI elements to pass sensor readings to.
     */
    // For Accelerometer sensor
    private TextView uiValueX;
    private TextView uiValueY;
    private TextView uiValueZ;

    // For Orientation sensor
    private TextView uiRotateValueX;
    private TextView uiRotateValueY;
    private TextView uiRotateValueZ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initializes all the required sensors.
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        // Initializes the sound playing system.
        strikeSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        strikeSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId,
                                       int status) {
                if (sampleId == sampleStrikeSoundId) {
                    strikeSoundLoaded = true;
                } else if (sampleId == sampleOffsetOneSoundId) {
                    offsetOneSoundLoaded = true;
                } else if (sampleId == sampleOffsetTwoSoundId) {
                    offsetTwoSoundLoaded = true;
                }
            }
        });
        sampleStrikeSoundId = strikeSoundPool.load(this, R.raw.drum_sample_one, 1);
        sampleOffsetOneSoundId = strikeSoundPool.load(this, R.raw.clap_crushed, 1);
        sampleOffsetTwoSoundId = strikeSoundPool.load(this, R.raw.clap_slapper, 1);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        strikingVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxStrikingVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
         * Initializes all the necessary UI elements to pass sensor readings to.
         */
        uiValueX = (TextView) findViewById(R.id.accelerometerValueX);
        uiValueY = (TextView) findViewById(R.id.accelerometerValueY);
        uiValueZ = (TextView) findViewById(R.id.accelerometerValueZ);
        uiRotateValueX = (TextView) findViewById(R.id.rotationValueX);
        uiRotateValueY = (TextView) findViewById(R.id.rotationValueY);
        uiRotateValueZ = (TextView) findViewById(R.id.rotationValueZ);

        prevTime = System.currentTimeMillis();
        prevRollTime = System.currentTimeMillis();
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, 100000, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mRotation, 100000, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        long currentTime = System.currentTimeMillis();
        long timeDifference;

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                timeDifference = currentTime - prevTime;

                float valueX = event.values[0];
                float valueY = event.values[1];
                float valueZ = event.values[2];

                uiValueX.setText(String.valueOf(valueX));
                uiValueY.setText(String.valueOf(valueY));
                uiValueZ.setText(String.valueOf(valueZ));

                float speed = (valueX + valueZ - lastX - lastZ) / timeDifference * 10000;

                if (prevSpeed < SHAKE_LOWER_THRESHOLD && speed > SHAKE_UPPER_THRESHOLD && timeDifference > 45) {
                    Log.d("Shake", String.valueOf(speed));
                    //Toast.makeText(this.getApplicationContext(), "Shake detected.", Toast.LENGTH_SHORT).show();

                    float volume = strikingVolume / maxStrikingVolume;
                    // Is the sound loaded already?
                    if (toPlay == LEVEL_OFFSET_0 && strikeSoundLoaded) {
                        strikeSoundPool.play(sampleStrikeSoundId, volume, volume, 1, 0, 1f);
                    } else if (toPlay == LEVEL_OFFSET_1 && offsetOneSoundLoaded) {
                        strikeSoundPool.play(sampleOffsetOneSoundId, volume, volume, 1, 0, 1f);
                    } else if (toPlay == LEVEL_OFFSET_2 && offsetTwoSoundLoaded) {
                        strikeSoundPool.play(sampleOffsetTwoSoundId, volume, volume, 1, 0, 1f);
                    }
                }

                lastX = valueX;
                lastY = valueY;
                lastZ = valueZ;
                prevTime = currentTime;
                prevSpeed = speed;

                break;

            case Sensor.TYPE_ROTATION_VECTOR:
                double rotateValueX = event.values[0] / Math.PI * 180;
                double rotateValueY = event.values[1] / Math.PI * 180;
                double rotateValueZ = event.values[2] / Math.PI * 180;

                uiRotateValueX.setText(String.valueOf(rotateValueX));
                uiRotateValueY.setText(String.valueOf(rotateValueY));
                uiRotateValueZ.setText(String.valueOf(rotateValueZ));

                break;

            case Sensor.TYPE_ORIENTATION:
                timeDifference = currentTime - prevTime;

                /*
                 * Azimuth, angle between the magnetic north direction and the y-axis,
                 * around the z-axis (0 to 359). 0=North, 90=East, 180=South, 270=West
                 */
                double azimuth = event.values[0];

                /*
                 * Pitch, rotation around x-axis (-180 to 180), with positive values
                 * when the z-axis moves toward the y-axis.
                 */
                double pitch = event.values[1];

                /*
                 * Roll, rotation around the y-axis (-90 to 90) increasing as the
                 * device moves clockwise.
                 */
                double roll = event.values[2];

                if ((roll >= 25.0) && (pitch > -90.0)) {
                    Log.d("Face Left", String.valueOf(roll));
                    //Toast.makeText(this.getApplicationContext(), "Phone is facing left.", Toast.LENGTH_SHORT).show();
                    toPlay = LEVEL_OFFSET_1;
                } else if ((roll < 25.0) && (pitch > -90.0)) {
                    Log.d("Face Up", String.valueOf(roll));
                    //Toast.makeText(this.getApplicationContext(), "Phone is facing up.", Toast.LENGTH_SHORT).show();
                    toPlay = LEVEL_OFFSET_0;
                } /*else if ((roll < 45.0) && (pitch < -90.0) && timeDifference > 15) {
                    Log.d("Face Down", String.valueOf(roll));
                    //Toast.makeText(this.getApplicationContext(), "Phone is facing down.", Toast.LENGTH_SHORT).show();
                    toPlay = LEVEL_OFFSET_2;
                }*/

                uiRotateValueX.setText(String.valueOf(azimuth));
                uiRotateValueY.setText(String.valueOf(pitch));
                uiRotateValueZ.setText(String.valueOf(roll));

                lastRoll = roll;
                prevRollTime = currentTime;

            default:

        }
    }
}