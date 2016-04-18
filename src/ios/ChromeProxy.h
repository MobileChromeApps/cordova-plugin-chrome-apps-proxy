/*
 Header for ChromeProxy
 
 ChromeProxy receives the get, set, and clear commands from Cordova
 to enable and disable proxying.
*/

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import <Cordova/CDVInvokedUrlCommand.h>

@interface ChromeProxy : CDVPlugin

/* Responds to get Cordova command. */
- (void)get:(CDVInvokedUrlCommand *)command;

/* Responds to set Cordova command by setting 
 proxyDictionary to a socks5 or http proxy. 
 */
- (void)set:(CDVInvokedUrlCommand *)command;

/* Responds to clear Cordova command by clearing
 proxyDictionary.
 */
- (void)clear:(CDVInvokedUrlCommand *)command;

/* Sends plugin result with success or error. */
- (void)sendPluginResult:(CDVInvokedUrlCommand *)command withError:(NSString *)errorMessage;

/* The proxy settings that will be  set to the custom 
 NSURLSessionConfiguration's connectionProxyDictionary
 in ChromeProxyURLProtocol.
 */
+ (NSDictionary *)proxyDictionary;

/* Sets the scheme, host, and port for proxyDictionary. */
+ (void)setProxy:(NSString *)scheme withHost:(NSString *)host withPort:(NSNumber *)port;

@end
