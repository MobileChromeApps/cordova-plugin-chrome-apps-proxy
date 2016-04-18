# chrome.proxy Plugin (experimental)

This plugin provides the ability to set a proxy for HTTP(S) and FTP traffic
generated within the app (by the browser runtime).  It does not affect any other
apps on the system.

## Status

This plugin is experimental and not yet well-tested.

On Android, this plugin provides a partial implementation of the chrome.proxy
API.  Only the "fixed_servers" and "direct" modes are supported.  Only the
"http" and "socks5" schemes are supported, and the bypassList setting is only
supported for the "http" scheme.

On iOS, only the "http" and "socks5" schemes are supported.

Currently, bypassList is not supported on either platform.

Other platforms are not supported.

## Reference

The API reference is [here](https://developer.chrome.com/extensions/proxy).

# Release Notes

## 0.0.1
- First implementation