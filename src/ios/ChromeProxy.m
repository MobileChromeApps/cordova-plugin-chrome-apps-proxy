/*
 Implementation for ChromeProxy
 
 ChromeProxy receives the get, set, and clear commands from Cordova
 to enable and disable proxying.
 */

#import "ChromeProxy.h"
#import "ChromeProxyURLProtocol.h"
#import <Cordova/CDVPluginResult.h>
#import <Cordova/CDVUserAgentUtil.h>

@implementation ChromeProxy

static NSString *LOG_TAG = @"[ChromeProxy]";
static NSDictionary *proxyDictionary;

+ (void)load {
    NSLog(@"%@ Registering nsurlprotocol class", LOG_TAG);
    [NSURLProtocol registerClass:[ChromeProxyURLProtocol class]];
}

+ (void)setProxy:(NSString *)scheme withHost:(NSString *)host withPort:(NSNumber *)port {
    proxyDictionary = @{
                        [NSString stringWithFormat:@"%@Enable", scheme]:@YES,
                        [NSString stringWithFormat:@"%@Proxy", scheme]:host,
                        [NSString stringWithFormat:@"%@Port", scheme]:port,
                        };
    NSLog(@"%@ Set proxyDictionary: %@", LOG_TAG, proxyDictionary);
}

+ (NSDictionary *)proxyDictionary {
    return proxyDictionary;
}

- (void)get:(CDVInvokedUrlCommand *)command {
    NSLog(@"%@ Cordova called the get command", LOG_TAG);
    [self sendPluginResult:command];
}

- (void)set:(CDVInvokedUrlCommand *)command {
    NSLog(@"%@ Setting proxy", LOG_TAG);
    
    NSString* errorMessage;
    NSDictionary* commandValue = [command argumentAtIndex:0];
    NSDictionary* singleProxy = commandValue[@"value"][@"rules"][@"singleProxy"];
    
    if (!singleProxy) {
        errorMessage = @"Failed to parse cordova command for singleProxy";
        return [self sendPluginResult:command withError:errorMessage];
    }

    // TODO: Implement bypassList support
    NSString* proxyHost = singleProxy[@"host"];
    NSNumber* proxyPort = singleProxy[@"port"];
    if (proxyHost && proxyPort) {
        if ([singleProxy[@"scheme"] isEqual:@"socks5"]) {
            [ChromeProxy setProxy:@"SOCKS" withHost:proxyHost withPort:proxyPort];
        } else if ([singleProxy[@"scheme"] isEqual:@"http"]) {
            [ChromeProxy setProxy:@"HTTP" withHost:proxyHost withPort:proxyPort];
        } else {
            errorMessage = @"Proxy scheme must be socks5 or http";
            return [self sendPluginResult:command withError:errorMessage];
        }
    } else {
        errorMessage = @"Failed to parse singleProxy for host and port";
        return [self sendPluginResult:command withError:errorMessage];
    }
    
    [self sendPluginResult:command];
}

- (void)clear:(CDVInvokedUrlCommand *)command {
    NSLog(@"%@ Clearing proxy settings", LOG_TAG);
    proxyDictionary = nil;
    [self sendPluginResult:command];
}

- (void)sendPluginResult:(CDVInvokedUrlCommand *)command {
    [self sendPluginResult:command withError:nil];
}

- (void)sendPluginResult:(CDVInvokedUrlCommand *)command withError:(NSString *)errorMessage {
    CDVPluginResult* pluginResult;
    if (errorMessage) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMessage];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    [pluginResult setKeepCallback:@YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
