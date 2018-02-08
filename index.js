
import { 
    NativeModules, 
    DeviceEventEmitter 
} from 'react-native';

const { RNAudioRecorder } = NativeModules;

// export default RNAudioRecorder;
export default class AudioRecorder{

    constructor(props){
        this._onData = null
        DeviceEventEmitter.addListener('onAudioPCMData', event => {
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

