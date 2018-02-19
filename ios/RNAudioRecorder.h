
#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif

#import <React/RCTEventEmitter.h>

@interface RNAudioRecorder : RCTEventEmitter <RCTBridgeModule>
- (void)appendAudioData:(const void *)bytes length:(NSUInteger)length;
@end
  
