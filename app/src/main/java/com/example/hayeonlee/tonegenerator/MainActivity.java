package com.example.hayeonlee.tonegenerator;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

// makerj.tistory.com

public class MainActivity extends Activity {
    private TextView mTxt1;
    private TextView mTxt2,mTxt4,mTxt5;
    private SensorManager mSM;
    private Sensor myGravity;

    //
    private final int duration = 1000; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private int freqOfTone = 440; // hz - If change this value, then tone is changed
    private int pastFreq = freqOfTone;
    private final byte generatedSnd[] = new byte[2 * numSamples];


    AudioTrack audioTrack;

    //
    BackgroundTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTxt1 = (TextView) findViewById(R.id.textView1);
        mTxt2 = (TextView) findViewById(R.id.textView2);
        mTxt4 = (TextView) findViewById(R.id.textView4);
        mTxt5 = (TextView) findViewById(R.id.textView5);
        mSM = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        myGravity = mSM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        task = new BackgroundTask();
        task.execute(1);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onPause() {
        super.onPause();

        mSM.unregisterListener(mySensorListener);
    }

    public SensorEventListener mySensorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {

            if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            {
                mTxt1.setText(Float.toString(event.values[0])); // x value of accelerometer
                mTxt4.setText(Float.toString(event.values[1])); // y value of accelerometer
                mTxt5.setText(Float.toString(event.values[2])); // z value of accelerometer

                freqOfTone = (int)Math.abs(event.values[0])*50 + 100; // change frequency of tone by x value
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    void genTone(int freqOfTone){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    void playSound(){
        try {
            if(audioTrack!=null) { //resource release
                audioTrack.stop();
                audioTrack.release();
            }

            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                    AudioTrack.MODE_STATIC);
            audioTrack.write(generatedSnd, 0, generatedSnd.length);
            audioTrack.play();
        }catch (IllegalStateException e){
            audioTrack.release();

        }
        //}
    }

    //for non block UI
    class BackgroundTask extends AsyncTask<Integer , Integer , Integer> {
        protected void onPreExecute() {
            mSM.registerListener(mySensorListener, myGravity,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        protected Integer doInBackground(Integer ... values) {
            while (isCancelled() == false) {
                if(pastFreq!=freqOfTone) {
                    pastFreq = freqOfTone;
                    genTone(freqOfTone);
                    playSound();
                    //mHandler.sendEmptyMessageDelayed(0, 1000);
                    publishProgress(freqOfTone);
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {}
            }

            return freqOfTone;
        }

        protected void onProgressUpdate(Integer ... values) {

            mTxt2.setText("Frequency = "+Integer.toString(freqOfTone)+"Hz");
        }
        protected void onPostExecute(Integer result) {

        }

        protected void onCancelled() {

        }
    }
}
