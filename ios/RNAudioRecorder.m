
#import "RNAudioRecorder.h"

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
//    RCTLogInfo(@"Pretending to create an event %@ at %@", name, location);
}

RCT_EXPORT_METHOD(stop)
{
//    RCTLogInfo(@"Pretexnding to create an event %@ at %@", name, location);
}

@end
  
