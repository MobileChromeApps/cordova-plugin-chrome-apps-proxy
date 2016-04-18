/*
 Implementation for ChromeProxyURLProtocol
 
 ChromeProxyURLProtocol is a subclass of NSURLProtocol that intercepts URL requests
 and sends them through a http or socks5 proxy server which is set up in ChromeProxy.
 */

#import "ChromeProxyURLProtocol.h"

@implementation ChromeProxyURLProtocol

static NSString *LOG_TAG = @"[ChromeProxyURLProtocol]";
static NSString *requestHandledKey = @"ChromeProxyURLProtocolHandledKey";

+ (BOOL)canInitWithRequest:(NSURLRequest *)request {
    // Don't handle request if it's already been handled by this protocol.
    if ([NSURLProtocol propertyForKey:requestHandledKey inRequest:request]) return NO;
    
    // Handle request if we haven't handled it yet and we've already
    // set up a proxy server in ChromeProxy.
    if ([ChromeProxy proxyDictionary]) return YES;
    
    // Don't handle this request if we haven't set up a proxy server.
    return NO;
}

+ (NSURLRequest *)canonicalRequestForRequest:(NSURLRequest *)request {
    return request;
}

- (void)startLoading {
    // Set up the NSURLSessionConfiguration with proxy settings from ChromeProxy.
    NSURLSessionConfiguration *config = [NSURLSessionConfiguration defaultSessionConfiguration];
    config.connectionProxyDictionary = [ChromeProxy proxyDictionary];
    config.protocolClasses = @[[ChromeProxyURLProtocol class]];
    
    // Tell the protocol that we've handled the request.
    NSMutableURLRequest *newRequest = [self.request mutableCopy];
    [NSURLProtocol setProperty:@YES forKey:requestHandledKey inRequest:newRequest];
    
    // Load task with the request.
    self.session = [NSURLSession sessionWithConfiguration:config delegate:self delegateQueue:nil];
    self.task = [self.session dataTaskWithRequest:newRequest];
    NSLog(@"%@ Loading URL %@", LOG_TAG, newRequest.URL.absoluteString);
    [self.task resume];
}

- (void)stopLoading {
    [self.task cancel];
    self.task = nil;
}

- (void)URLSession:(NSURLSession *)session dataTask:(NSURLSessionDataTask *)dataTask didReceiveResponse:(NSURLResponse *)response completionHandler:(void (^)(NSURLSessionResponseDisposition))completionHandler {
    [self.client URLProtocol:self didReceiveResponse:response cacheStoragePolicy:NSURLCacheStorageNotAllowed];
    completionHandler(NSURLSessionResponseAllow);
}

- (void)URLSession:(NSURLSession *)session dataTask:(NSURLSessionDataTask *)dataTask didReceiveData:(NSData *)data {
    [self.client URLProtocol:self didLoadData:data];
}

- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task didCompleteWithError:(NSError *)error {
    if (error) {
        NSLog(@"%@ URLSession didCompleteWithError: %@", LOG_TAG, error.description);
        [self.client URLProtocol:self didFailWithError:error];
    } else {
        [self.client URLProtocolDidFinishLoading:self];
    }
}

@end
