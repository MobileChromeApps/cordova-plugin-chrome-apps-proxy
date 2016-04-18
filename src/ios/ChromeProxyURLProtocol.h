/*
 Header for ChromeProxyURLProtocol
 
 ChromeProxyURLProtocol is a subclass of NSURLProtocol that intercepts URL requests
 and sends them through a http or socks5 proxy server which is set up in ChromeProxy.
 */

#import "ChromeProxy.h"
#import <Foundation/Foundation.h>

@interface ChromeProxyURLProtocol : NSURLProtocol <NSURLSessionDelegate, NSURLSessionTaskDelegate, NSURLSessionDataDelegate>

@property (nonatomic, strong) NSURLSession *session;
@property (nonatomic, strong) NSURLSessionTask *task;

/*
 Required NSURLProtocol method that intercepts every request and returns
 whether we can handle the request or not.
 */
+ (BOOL)canInitWithRequest:(NSURLRequest *)request;

/*
 Required NSURLProtocol method that returns a canonical version of a request
 that we're handling.
 */
+ (NSURLRequest *)canonicalRequestForRequest:(NSURLRequest *)request;

/*
 Required NSURLProtocol method that starts our protocol-specific loading
 of the request.
 */
- (void)startLoading;

/*
 Required NSURLProtocol method that stops our protocol-specific loading
 of the request.
 */
- (void)stopLoading;

/*
 Required NSURLSessionDataDelegate method that tells the delegate that the
 data task received the initial reply from the server.
 */
- (void)URLSession:(NSURLSession *)session dataTask:(NSURLSessionDataTask *)dataTask didReceiveResponse:(NSURLResponse *)response completionHandler:(void (^)(NSURLSessionResponseDisposition))completionHandler;

/*
 Required NSURLSessionDataDelegate method that tells the delegate that the
 data task has received some of the expected data.
 */
- (void)URLSession:(NSURLSession *)session dataTask:(NSURLSessionDataTask *)dataTask didReceiveData:(NSData *)data;

/*
 Required NSURLSessionTaskDelegate method that tells the delegate
 that the task finished transferring data and if it completed with an error.
 */
- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task didCompleteWithError:(NSError *)error;

@end
