
package com.reactlibrary;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class RNAudioRecorderModule extends ReactContextBaseJavaModule {

  private static final int RECORDER_SAMPLERATE = 8000;// 44100;
  private static final String TAG = "RNAudioRecorderModule";

  AudioRecord audioRecord;
  int bufferSize;
  boolean isRecording = false;
  ReactApplicationContext reactContext;

  public RNAudioRecorderModule(ReactApplicationContext reactContext) {
    super(reactContext);

    this.reactContext = reactContext;
    this.bufferSize = 800 *2;
    this.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
            RECORDER_SAMPLERATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.ENCODING_PCM_16BIT, bufferSize);

    AcousticEchoCanceler aec = AcousticEchoCanceler.create(this.audioRecord.getAudioSessionId());
    if(aec!=null){
      aec.setEnabled(true);
      Log.d(TAG, "AEC enalbe: " + aec.getEnabled());

    }
    NoiseSuppressor ns = NoiseSuppressor.create(this.audioRecord.getAudioSessionId());
    AutomaticGainControl agc = AutomaticGainControl.create(this.audioRecord.getAudioSessionId());
  }

  @Override
  public String getName() {
    return "RNAudioRecorder";
  }

  @ReactMethod
  public void start(){

    Log.i(TAG," Start Recording");
    if(isRecording) return;

    this.audioRecord.startRecording();
    isRecording = true;
    new Thread(new Runnable(){
      @Override
      public void run() {
        fetchAudioData();
      }
    }).start();
  }

  @ReactMethod
  public void stop(){

    Log.i(TAG," STOP Recording");
    isRecording = false;
    this.audioRecord.stop();
  }

  private void sendEvent(ReactContext reactContext,  String eventName, @Nullable WritableMap params) {
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class) .emit(eventName, params);
  }

  private void fetchAudioData(){

    byte data[] = new byte[1600];
    byte u_data[] = new byte[800];
    int read;

    File sdCard = Environment.getExternalStorageDirectory();
    File file = new File(sdCard.getAbsolutePath(), "audio.pcm");
    FileOutputStream f = null;
    int writeOffset = 0;

    try {
      f = new FileOutputStream(file);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    while (isRecording){

      read = this.audioRecord.read(data, 0, 1600);
      if(read<=0) continue;

      try {
        f.write(data);
      } catch (IOException e) {
        e.printStackTrace();
      }

      writeOffset += read;

      UlawEncoderInputStream.encode(data, 0, u_data, 0, 1600,8100);

      WritableMap params = Arguments.createMap();
      params.putString("base64", Base64.encodeToString(u_data, 0, 800, Base64.DEFAULT));
      params.putInt("length", read);
      sendEvent(this.reactContext, "onAudioPCMData", params);
    }

    try {
      f.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


}