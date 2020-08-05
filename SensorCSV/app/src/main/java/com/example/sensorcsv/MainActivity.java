package com.example.sensorcsv;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
//import android.support.v7.app.ActionBarActivity;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    MediaPlayer player;

    public Button startButton;
    public Button stopButton;
    public Button saveButton;
    public Button touchButton;

    public TextView nameTv;
    public TextView xyzTv;
    public TextView infoTv;


    public static final String FileName = "Sensors.csv";
    public static final String FileNameGyro = "Test.csv";

    public StringBuilder dataBuffer;   // saves what we'll write to file
    public StringBuilder dataBufferTest;
    private StringBuilder dataBufferMag;

    public int lineCnt;
    public int gylinecnt;
    public int maglinecnt;

    public SensorManager mSensorManager;
    public Sensor mAccelerometer;
    public Sensor mGyroscope;
    public Sensor mMag;
    public Sensor mPressure;
    public Sensor mLinearAcc;
    public Sensor mMagnetic;
    public Sensor mGravity;

    public int status;
    //public int READINGRATE = 20000; //~50hz
    public int READINGRATE = 40000; // tinh theo 1/hz * 10^6 25hz
    public static final int REC_STARTED = 1;
    public static final int REC_STOPPED = 2;

    int noti = 0;
    int value = 0;
    int tmp = 0;
    public String delim = ",";
    public String acc,gyr,mag, liacc,total, pacc;
    double rmsacc,rmsgyr,rmsmag;
    public long starttime;
    public double countimess;
    public double Timestamp;
    private float[] gravityValues = null;
    private float[] magneticValues = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();
        isExternalStorageReadable();

        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setVisibility(View.INVISIBLE);
        saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setVisibility(View.INVISIBLE);
        touchButton = (Button) findViewById(R.id.touchButton);

        nameTv = (TextView) findViewById(R.id.filenameTextview);
        xyzTv = (TextView) findViewById(R.id.xyzTextview);
        infoTv = (TextView) findViewById(R.id.infoTextview);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mLinearAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGravity  = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        mSensorManager.registerListener(this, mAccelerometer, READINGRATE);
        mSensorManager.registerListener(this, mGyroscope, READINGRATE);
        mSensorManager.registerListener(this, mMag,READINGRATE);
        mSensorManager.registerListener(this, mPressure, READINGRATE);
        mSensorManager.registerListener(this, mLinearAcc, READINGRATE);
        mSensorManager.registerListener(this, mMagnetic, READINGRATE);
        mSensorManager.registerListener(this, mGravity, READINGRATE);

        saveButton.setVisibility(View.INVISIBLE);

//        mSensorManager.registerListener(this, mAccelerometer, mSensorManager.SENSOR_DELAY_FASTEST);
//        mSensorManager.registerListener(this, mGyroscope, mSensorManager.SENSOR_DELAY_FASTEST);
//        mSensorManager.registerListener(this, mMag,mSensorManager.SENSOR_DELAY_FASTEST);
//        mSensorManager.registerListener(this, mPressure, mSensorManager.SENSOR_DELAY_FASTEST);

//        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(this, mMag,SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_GAME);

        startButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                starttime = System.currentTimeMillis();
                status = REC_STARTED;
                dataBuffer = new StringBuilder();
                dataBufferTest = new StringBuilder();
                countimess = 0;
                lineCnt = 0;
                nameTv.setText("0");
                //xyzTv.setText("recording ...");
                String col1 = "Timestamp" + delim + "P"
                        + delim + "Gx" +delim + "Gy" + delim + "Gz"
                        + delim + "Mx" +delim + "My" + delim + "Mz"
                        + delim + "LiAx" +delim + "LiAy" + delim + "LiAz"
                        + delim + "Ax" + delim + "Ay" +delim + "Az"
                        + delim + "earthAx" + delim + "earthAy" + delim + "earthAz"
                        + delim + "Value" + delim + "Type" +"\n";
                dataBuffer.append(col1);
                //stopButton.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.VISIBLE);
                startButton.setVisibility(View.INVISIBLE);

            }
        });

        stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (status != REC_STARTED) {
                    String msg = "Start recording first";
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                status = REC_STOPPED;
                //Log.w("Information", dataBuffer.toString());
                String msg = "recorded " + lineCnt + " lines of data points";
                infoTv.setText(msg);
                xyzTv.setText("");
                nameTv.setText("Stop");

            }
        });

        saveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String msg = "recorded " + lineCnt + " lines of data points";
                infoTv.setText(msg);
                writeFile();
                nameTv.setText("Saved");
                infoTv.setText(" ");
                xyzTv.setText(" ");
                startButton.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.INVISIBLE);
                status = REC_STOPPED;
            }
        });
        touchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                noti++;

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(noti == 1){
                            Toast.makeText(MainActivity.this,"Single Click", Toast.LENGTH_SHORT).show();
                            value = 0;
                            nameTv.setText("0");
//                            if (player != null) {
//                                player.release();
//                                player = null;
//                                Toast.makeText(MainActivity.this, "MediaPlayer released", Toast.LENGTH_SHORT).show();
//                            }
                            if (player != null) {
                                player.pause();
                            }
                        }else if (noti == 2){
                            Toast.makeText(MainActivity.this,"Double Click", Toast.LENGTH_SHORT).show();
                            value = 1;
                            nameTv.setText("1");
                            if (player == null) {
                                player = MediaPlayer.create(MainActivity.this, R.raw.nnca);
                                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        if (player != null) {
                                            player.release();
                                            player = null;
                                            Toast.makeText(MainActivity.this, "MediaPlayer released", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }

                            player.start();
                        }
                        noti =0;

                    }
                },500);
            }
        });
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, READINGRATE);
        mSensorManager.registerListener(this, mGyroscope, READINGRATE);
        mSensorManager.registerListener(this, mMag, READINGRATE);
        mSensorManager.registerListener(this, mPressure,READINGRATE);
        mSensorManager.registerListener(this, mLinearAcc, READINGRATE);
        mSensorManager.registerListener(this, mMagnetic, READINGRATE);
        mSensorManager.registerListener(this, mGravity, READINGRATE);
//        mSensorManager.registerListener(this, mAccelerometer, mSensorManager.SENSOR_DELAY_FASTEST);
//        mSensorManager.registerListener(this, mGyroscope, mSensorManager.SENSOR_DELAY_FASTEST);
//        mSensorManager.registerListener(this, mMag,mSensorManager.SENSOR_DELAY_FASTEST);
//        mSensorManager.registerListener(this, mPressure, mSensorManager.SENSOR_DELAY_FASTEST);

//        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(this, mMag,SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_GAME);
    }
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    double roundTwoDecimals(double d)
    {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(d));
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
            Toast.makeText(this, "MediaPlayer released", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String timeStamp = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - starttime));
        Sensor sensor = event.sensor;
        if (status == REC_STARTED) {
            if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                //Timestamp = roundTwoDecimals(countimess);
                xyzTv.setText(timeStamp);
                Log.d("Timestamp", timeStamp);
                double x = event.values[0];
                pacc = delim + x;
            } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                double x = event.values[0];
                double y = event.values[1];
                double z = event.values[2];
                gyr = pacc + delim + x + delim + y + delim + z;
                //Log.d("test_gyr", Float.toString(x));
            } else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                double x = event.values[0];
                double y = event.values[1];
                double z = event.values[2];
                mag = gyr + delim + x + delim + y + delim + z;
                //Log.d("test_mag", Float.toString(x));
            } else if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                double x = event.values[0];
                double y = event.values[1];
                double z = event.values[2];
                liacc = mag + delim + x + delim + y + delim + z;
                //Log.d("test_LiAcc", Float.toString(x));
            } else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                acc = x + delim + y + delim + z;
            }
            if ((gravityValues != null) && (magneticValues != null)
                    && (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {

                float[] deviceRelativeAcceleration = new float[4];
                deviceRelativeAcceleration[0] = event.values[0];
                deviceRelativeAcceleration[1] = event.values[1];
                deviceRelativeAcceleration[2] = event.values[2];
                deviceRelativeAcceleration[3] = 0;

                // Change the device relative acceleration values to earth relative values
                // X axis -> East
                // Y axis -> North Pole
                // Z axis -> Sky

                float[] R = new float[16], I = new float[16], earthAcc = new float[16];

                SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);

                float[] inv = new float[16];

                android.opengl.Matrix.invertM(inv, 0, R, 0);
                android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);
                Log.d("Acceleration", "Values: (" + earthAcc[0] + ", " + earthAcc[1] + ", " + earthAcc[2] + ")");

                float x  = earthAcc[0];
                float y  = earthAcc[1];
                float z  = earthAcc[2];

                String line = timeStamp + liacc + delim + acc + delim + x + delim + y + delim + z + delim + value + "\n";
                dataBuffer.append(line);

            } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                gravityValues = event.values;
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticValues = event.values;
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ignore
    }
    private void writeFileTest() {

        String state;
        state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            File Root = Environment.getExternalStorageDirectory();
            File Dir = new File(Root.getAbsolutePath());
            if (Dir.exists())
            {
                Dir.mkdir();
            }
            File file = new File(Dir,FileNameGyro);
            try {
                file.createNewFile();
                FileOutputStream fos;
                byte[] data = dataBufferTest.toString().getBytes();
                fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeFile() {

        String state;
        state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            File Root = Environment.getExternalStorageDirectory();
            File Dir = new File(Root.getAbsolutePath());
            if (Dir.exists())
            {
                Dir.mkdir();
            }
            File file = new File(Dir,FileName);
            try {
                file.createNewFile();
                FileOutputStream fos;
                byte[] data = dataBuffer.toString().getBytes();
                fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    double RMS(double x,double y,double z)
    {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private void checkAndRequestPermissions() {
        String[] permissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 1);
        }
    }

}
