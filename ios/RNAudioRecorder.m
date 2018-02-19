
#import "RNAudioRecorder.h"
#import <AVFoundation/AVFoundation.h>

//Copy from mCamViewZ
#define            kNumberRecordBuffers        15
typedef struct AQRecorderState
{
    AudioStreamBasicDescription  mRecordFormat ;
    AudioQueueRef                mQueue ;
    AudioQueueBufferRef          mBuffers[kNumberRecordBuffers] ;
    BOOL                         isRunning ;
} AudioRecorder ;
static AudioRecorder aRecorder ;

static void recordingCallback (
                        void                                    *inUserData,
                        AudioQueueRef                           inAudioQueue,
                        AudioQueueBufferRef                     inBuffer,
                        const AudioTimeStamp                   *inStartTime,
                        UInt32                                  inNumPackets,
                        const AudioStreamPacketDescription      *inPacketDesc
                        )
{
    if( aRecorder.isRunning )
    {
        [(__bridge RNAudioRecorder *)inUserData appendAudioData:inBuffer->mAudioData length:inNumPackets];
        OSStatus result = AudioQueueEnqueueBuffer (inAudioQueue, inBuffer, 0,NULL);
        if( result ){ printf("\n Error, result = %d\n", (int)result ) ;}
    }
}

@interface RNAudioRecorder()
{
    NSMutableData *audioData;
}


@end

@implementation RNAudioRecorder



- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}


RCT_EXPORT_MODULE()

- (NSArray<NSString *> *)supportedEvents
{
    return @[@"onAudioPCMData"];
}

RCT_EXPORT_METHOD(start)
{
    if(aRecorder.isRunning == YES) return;
    
    AudioStreamBasicDescription * audioFormat = &( aRecorder.mRecordFormat ) ;
    audioFormat->mSampleRate = 8000 ;
    audioFormat->mFormatID = kAudioFormatULaw ;
    audioFormat->mChannelsPerFrame = 1 ;
    OSStatus result = AudioQueueNewInput (audioFormat,
                                          recordingCallback,
                                          (__bridge void * _Nullable)(self),
                                          NULL,                                   // run loop
                                          NULL,                                   // run loop mode
                                          0,                                              // flags
                                          &(aRecorder.mQueue)
                                          );
    if( result ){
        return ;
    }
    for(int i = 0; i < kNumberRecordBuffers; ++i)
    {
        result = AudioQueueAllocateBuffer(aRecorder.mQueue, 800, &aRecorder.mBuffers[i]) ;
        if( result ) {
            fprintf(stderr, "\n [%s:%d] AudioQueueAllocateBuffer() Error!!\n", __FUNCTION__, __LINE__ ) ;
            return ;
        }
        result = AudioQueueEnqueueBuffer(aRecorder.mQueue, aRecorder.mBuffers[i], 0, NULL) ;
        if( result ){
            fprintf(stderr, "\n [%s:%d] AudioQueueEnqueueBuffer() Error!!\n", __FUNCTION__, __LINE__ ) ;
            return ;
        }
    }
    aRecorder.isRunning = 1 ;
    result = AudioQueueStart(aRecorder.mQueue, NULL) ;
    if( result ) {
        return ;
    }
}

RCT_EXPORT_METHOD(stop)
{
    if(aRecorder.isRunning == NO) return;
    
    aRecorder.isRunning = 0 ;
    AudioQueueStop(aRecorder.mQueue, TRUE) ;
    AudioQueueDispose(aRecorder.mQueue, TRUE) ;
}


- (void)appendAudioData:(const void *)bytes length:(NSUInteger)length
{
    if(audioData == NULL){
        audioData = [[NSMutableData alloc] init];
    }
    
    [audioData appendBytes:bytes length:length];
    if(audioData.length >= 800){
        NSRange range = NSMakeRange(0, 799);
        NSData *data = [audioData subdataWithRange:range];
        NSString *base64Encoded = [data base64EncodedStringWithOptions:0];
        [self sendEventWithName:@"onAudioPCMData" body:@{@"base64":base64Encoded}];
        [audioData replaceBytesInRange:range withBytes:NULL length:0];
    }
}

@end
  
