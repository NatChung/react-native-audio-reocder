
import { 
    NativeModules, 
    DeviceEventEmitter ,
    NativeEventEmitter,
    Platform
} from 'react-native';

const { RNAudioRecorder } = NativeModules;

const emitter = Platform.OS == 'ios' ? (new NativeEventEmitter(RNAudioRecorder)) : DeviceEventEmitter;
// export default RNAudioRecorder;
export default class AudioRecorder{

    constructor(props){
        this._onData = null
        emitter.addListener('onAudioPCMData', event => {
            if(this._onData) this._onData(event)
          });
    }

    start(){
        RNAudioRecorder.start()
    }

    stop(){
        RNAudioRecorder.stop()
    }

    set onData(callback){
        this._onData = callback;
    }
}

