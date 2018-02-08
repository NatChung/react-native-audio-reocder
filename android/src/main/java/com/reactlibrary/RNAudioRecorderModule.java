
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

  static AudioRecord audioRecord = null;
  static final int bufferSize = 800 *2;
  static boolean isRecording = false;
  ReactApplicationContext reactContext;

  public RNAudioRecorderModule(ReactApplicationContext reactContext) {
    super(reactContext);

    this.reactContext = reactContext;
    if(RNAudioRecorderModule.audioRecord == null){
      RNAudioRecorderModule.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
            RECORDER_SAMPLERATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.ENCODING_PCM_16BIT, bufferSize);

      AcousticEchoCanceler.create(RNAudioRecorderModule.audioRecord.getAudioSessionId());
      NoiseSuppressor.create(RNAudioRecorderModule.audioRecord.getAudioSessionId());
      AutomaticGainControl.create(RNAudioRecorderModule.audioRecord.getAudioSessionId());
    }
  }

  @Override
  public String getName() {
    return "RNAudioRecorder";
  }

  @ReactMethod
  public void start(){

    Log.i(TAG," Start Recording");
    if(isRecording) return;

    RNAudioRecorderModule.audioRecord.startRecording();
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
    RNAudioRecorderModule.audioRecord.stop();
  }

  private void sendEvent(ReactContext reactContext,  String eventName, @Nullable WritableMap params) {
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class) .emit(eventName, params);
  }

  private void fetchAudioData(){

    byte data[] = new byte[1600];
    byte u_data[] = new byte[800];
    int read;

    while (isRecording){

      read = RNAudioRecorderModule.audioRecord.read(data, 0, 1600);
      if(read<=0) continue;

      UlawEncoderInputStream.encode(data, 0, u_data, 0, 1600,8100);
      WritableMap params = Arguments.createMap();
      params.putString("base64", Base64.encodeToString(u_data, Base64.NO_WRAP));
      params.putInt("volume", this.getVolume(data, read));
      sendEvent(this.reactContext, "onAudioPCMData", params);
    }

  }

  private int getVolume(byte[] data, int length){
    double average = 0.0;
    for(int i=0;i<(length/2);i++){
      short s = (short) (data[i*2] << 8  |  data[i*2+1]);
      average += Math.abs(s);
    }

    return (int)Math.round(average/(length/2));
  }


}