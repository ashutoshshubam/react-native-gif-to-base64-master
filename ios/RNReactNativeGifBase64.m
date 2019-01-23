
#import "RNReactNativeGifBase64.h"
#import "YYImage.h"

@implementation RNReactNativeGifBase64

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()
RCT_EXPORT_METHOD(getBase64String:(NSDictionary *)options callback:(RCTResponseSenderBlock)callback)
{
    NSArray *gifArray = [options objectForKey:@"gifArr"];
    NSArray *facesArray = [options objectForKey:@"faceArr"];

    if(gifArray.count > 0 && facesArray.count >0){
        NSArray *base64 = [self convertNewGIF:gifArray faces:facesArray];
        callback(base64);
    }else{
        callback(@[@"Please send valid paramters"]);
    }
}

-(NSArray*)convertNewGIF:(NSArray*)gifArray faces:(NSArray*)faceArray{
    
    NSMutableArray *newGIFArr = [NSMutableArray array];
    
    for (NSDictionary *gifJSON in gifArray){
        
        NSString *gifURL = [gifJSON valueForKey:@"url_gif"];
        NSString *gif_id = [gifJSON valueForKey:@"giphy_id"];
        
        NSData *gifdata = [self downloadGIFfromServer:gifURL withID:gif_id];
        NSArray *faceImages = [self downloadFaceImagesFromServer:faceArray];
        
        NSArray *mapper = [gifJSON objectForKey:@"maps"];
        CGFloat ratio = [[gifJSON valueForKey:@"ratio"] floatValue];
        
        NSString *base64 = [self createNewGif:gifdata faceImages:faceImages mapping:mapper ratioValue:ratio];
        
        [newGIFArr addObject:base64];
    }
    
    return newGIFArr;
}

- (NSURL *)applicationDocumentsDirectory
{
    return [[[NSFileManager defaultManager] URLsForDirectory:NSDocumentDirectory
                                                   inDomains:NSUserDomainMask] lastObject];
}

-(NSData*)downloadGIFfromServer:(NSString*)url withID:(NSString*)gif_id{
    
    NSData *gifdata;
    
    NSURL *docuemntURL = [self applicationDocumentsDirectory];
    docuemntURL = [docuemntURL URLByAppendingPathComponent:gif_id];
    
    if ([[NSFileManager defaultManager]fileExistsAtPath:docuemntURL.path]){
        
        gifdata = [NSData dataWithContentsOfURL:docuemntURL];
        
    }else{
        
        gifdata = [NSData dataWithContentsOfURL:[NSURL URLWithString:url]];
        
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            [gifdata writeToFile:docuemntURL.path atomically:YES];
        });
    }
    
    return gifdata;
}

-(NSArray *)downloadFaceImagesFromServer:(NSArray*)facesArr{
    
    NSMutableArray *faces = [NSMutableArray array];
    
    for(NSDictionary *dict in facesArr){
        
        UIImage *faceImg;
        
        NSString *urlString = [dict valueForKey:@"url"];
        
        NSURL *url = [NSURL URLWithString:urlString];
        
        NSURL *docuemntURL = [self applicationDocumentsDirectory];
        
        docuemntURL = [docuemntURL URLByAppendingPathComponent:urlString];
        
        if ([[NSFileManager defaultManager]fileExistsAtPath:docuemntURL.path]){
            
            faceImg = [UIImage imageWithData:[NSData dataWithContentsOfURL:docuemntURL]];
            
        }else{
            
            NSData *imgData = [NSData dataWithContentsOfURL:url];
            
            faceImg = [UIImage imageWithData:imgData];
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                [imgData writeToFile:docuemntURL.path atomically:YES];
            });
        }
        
        [faces addObject:faceImg];
    }
    
    return faces;
}

-(NSString*)createNewGif:(NSData*)gif_data faceImages:(NSArray*)faceImagesArr mapping:(NSArray*)mapping ratioValue:(CGFloat)ratio{
    
    YYImageDecoder *decoder = [YYImageDecoder decoderWithData:gif_data scale:1.0];
    
    UIImage *_originalImage = [decoder frameAtIndex:0 decodeForDisplay:NO].image;
    
    UIGraphicsBeginImageContextWithOptions(_originalImage.size, NO, 1.0);
    
    YYImageEncoder *webpEncoder = [[YYImageEncoder alloc] initWithType:YYImageTypeWebP];
    webpEncoder.loopCount = 0;
    webpEncoder.quality = 1.0;
    
    for(int i=0; i<mapping.count; i++){
        
        UIImage *_originalImage = [decoder frameAtIndex:i decodeForDisplay:NO].image;
        
        [_originalImage drawInRect:CGRectMake(0.f, 0.f, _originalImage.size.width, _originalImage.size.height)];
        
        NSPredicate *bPredicate = [NSPredicate predicateWithFormat:@"SELF.frame_number == %d",i];
        
        NSArray *facesArr = [mapping filteredArrayUsingPredicate:bPredicate];
        
        for (NSDictionary* data in facesArr){
            
            int faceNumber = [[data valueForKey:@"face_number"] intValue];
            
            CGFloat x = [[data valueForKey:@"x"] floatValue];
            CGFloat y = [[data valueForKey:@"y"] floatValue];
            
            CGFloat zoom = [[data valueForKey:@"zoom"] floatValue];
            CGFloat angle = [[data valueForKey:@"angle"] floatValue];
            
            CGFloat newWidth = 400 * ratio * (zoom/100);
            CGFloat newHeight = 400 * ratio * (zoom/100);
            
            x = x - (newWidth / 2);
            y = y - (newHeight / 2);
            
            UIImage *logo = [self imageWithImage:faceImagesArr[faceNumber] convertToSize:CGSizeMake(newWidth, newHeight) rotationAngle:angle];
            
            [logo drawInRect:CGRectMake(x, y, newWidth, newHeight)];
        }
        
        NSTimeInterval timeinterval = [decoder frameDurationAtIndex:i];
        
        // store the image
        UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
        
        [webpEncoder addImage:newImage duration:timeinterval];
    }
    
    UIGraphicsEndImageContext();
    
    NSData *webpData = [webpEncoder encode];
    
    return  [webpData base64EncodedStringWithOptions:0];
}

- (UIImage *)imageWithImage:(UIImage *)image convertToSize:(CGSize)size rotationAngle:(CGFloat)degrees {
    
    CGFloat radian = degrees * (M_PI/ 180);
    UIGraphicsBeginImageContext(size);
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextTranslateCTM( context, 0.5f * size.width, 0.5f * size.height ) ;
    CGContextRotateCTM( context,radian) ;
    CGContextTranslateCTM( context, -0.5f * size.width, -0.5f * size.height ) ;
    [image drawInRect:CGRectMake(0, 0, size.width, size.height)];
    UIImage *destImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return destImage;
}

@end
  
