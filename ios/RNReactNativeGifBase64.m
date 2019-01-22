
#import "RNReactNativeGifBase64.h"

@implementation RNReactNativeGifBase64

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()
RCT_EXPORT_METHOD(getBase64String:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback)
{
    callback("Got this file");
}
@end
  
