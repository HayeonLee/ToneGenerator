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
    private TextView mTxt2;
    private SensorManager mSM;
    private Sensor myGravity;

    //
    private final int duration = 1000; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private int freqOfTone = 440; // hz
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
        mSM = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        myGravity = mSM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
/*		mSM.registerListener(mySensorListener, myGravity,
				SensorManager.SENSOR_DELAY_NORMAL);*/

        task = new BackgroundTask();
        task.execute(1);
        //mHandler.sendEmptyMessage(0);
    }

    @Override
    protected void onStart() {
        super.onStart();
/*		mSM.registerListener(mySensorListener, myGravity,
				SensorManager.SENSOR_DELAY_GAME);*/

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
                mTxt1.setText(Float.toString(event.values[0]));

                freqOfTone = (int)Math.abs(event.values[0])*50 + 100;


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
            if(audioTrack!=null) {
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

            mTxt2.setText(Integer.toString(freqOfTone));
        }
        protected void onPostExecute(Integer result) {

        }

        protected void onCancelled() {

        }
    }
}
