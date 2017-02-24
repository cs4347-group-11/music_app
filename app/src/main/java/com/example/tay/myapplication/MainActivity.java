package com.example.tay.myapplication;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mRotation;
    private SoundPool soundPool;

    private boolean loaded;
    private int sampleSoundId;

    private TextView uiValueX;
    private TextView uiValueY;
    private TextView uiValueZ;

    private TextView uiRotateValueX;
    private TextView uiRotateValueY;
    private TextView uiRotateValueZ;

    private static final int SHAKE_UPPER_THRESHOLD = 800;
    private static final int SHAKE_LOWER_THRESHOLD = 700;

    float lastX;
    float lastY;
    float lastZ;
    long prevTime;
    float prevSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId,
                                       int status) {
                loaded = true;
            }
        });
        sampleSoundId = soundPool.load(this, R.raw.drum_sample_one, 1);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uiValueX = (TextView) findViewById(R.id.accelerometerValueX);
        uiValueY = (TextView) findViewById(R.id.accelerometerValueY);
        uiValueZ = (TextView) findViewById(R.id.accelerometerValueZ);

        uiRotateValueX = (TextView) findViewById(R.id.rotationValueX);
        uiRotateValueY = (TextView) findViewById(R.id.rotationValueY);
        uiRotateValueZ = (TextView) findViewById(R.id.rotationValueZ);

        prevTime = System.currentTimeMillis();
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

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                long currentTime = System.currentTimeMillis();
                long timeDifference = currentTime - prevTime;

                float valueX = event.values[0];
                float valueY = event.values[1];
                float valueZ = event.values[2];

                uiValueX.setText(String.valueOf(valueX));
                uiValueY.setText(String.valueOf(valueY));
                uiValueZ.setText(String.valueOf(valueZ));

                float speed = (valueZ - lastZ) / timeDifference * 10000;
                //Log.d("Shake Speed & Time Diff", (String.valueOf(speed) + " " + String.valueOf(timeDifference)));

                if (prevSpeed < SHAKE_LOWER_THRESHOLD && speed > SHAKE_UPPER_THRESHOLD && timeDifference > 45) {
                    Log.d("Shake", String.valueOf(speed));
                    Toast.makeText(this.getApplicationContext(), "Shake detected.", Toast.LENGTH_SHORT).show();
                    AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                    float actualVolume = (float) audioManager
                            .getStreamVolume(AudioManager.STREAM_MUSIC);
                    float maxVolume = (float) audioManager
                            .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    float volume = actualVolume / maxVolume;
                    // Is the sound loaded already?
                    if (loaded) {
                        soundPool.play(sampleSoundId, volume, volume, 1, 0, 1f);
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

            case Sensor.TYPE_GYROSCOPE:
                double gyroValueX = event.values[0] / Math.PI * 180;
                double gyroValueY = event.values[1] / Math.PI * 180;
                double gyroValueZ = event.values[2] / Math.PI * 180;

                uiRotateValueX.setText(String.valueOf(gyroValueX));
                uiRotateValueY.setText(String.valueOf(gyroValueY));
                uiRotateValueZ.setText(String.valueOf(gyroValueZ));

                break;

            default:

        }
    }
}
