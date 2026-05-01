[![JetBrains incubator project](https://jb.gg/badges/official.svg)](https://github.com/JetBrains#jetbrains-on-github)

# JBR API

**_JBR API_** is an interface for the functionality specific to 
[JetBrains Runtime](https://github.com/JetBrains/JetBrainsRuntime) aka JBR.
JBR API provides a standalone jar with classes and interfaces representing various APIs
allowing the client code to be compiled against any JDK, while enjoying unique
features provided by JBR at run time without worrying about compatibility and runtime errors.

## Quickstart

Any feature exposed via JBR API begins with a **_service_**, which is a basic
unit of JBR API. Each service has three related methods in the `JBR` class:
* `JBR.get<NAME>()` - returns the service instance if it's supported or `null`.
* `JBR.get<NAME>(Extensions...)` - returns the service instance with the set of optional extensions enabled (see [below](#extensions)).
* `JBR.is<NAME>Supported()` - a convenience method equivalent to `JBR.get<NAME>() != null`.

```java
if (JBR.isSomeServiceSupported()) {
    JBR.getSomeService().doSomething();
}
// or
SomeService service = JBR.getSomeService();
if (service != null) {
    service.doSomething();
}
```
> <picture>
>   <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/Mqxx/GitHub-Markdown/f167aefa480e8d37e9941a25f0b40981b74a47be/blockquotes/badge/light-theme/tip.svg">
>   <img alt="Tip" src="https://raw.githubusercontent.com/Mqxx/GitHub-Markdown/f167aefa480e8d37e9941a25f0b40981b74a47be/blockquotes/badge/dark-theme/tip.svg">
> </picture><br>
>
> More details with a list of available services can be found in the
> [javadoc](https://jetbrains.github.io/JetBrainsRuntimeApi).

### Extensions

API methods marked with `@Extension` are *optional*, meaning that the service would still be
considered supported even if some of its extension methods are not.
Such extensions must be explicitly enabled when retrieving the service with `JBR.get<NAME>(Extensions...)`.
Extension methods may appear not only in services but in regular interfaces too.
In that case the set of enabled extensions is implicitly propagated to objects retrieved from that service.

> <picture>
>   <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/Mqxx/GitHub-Markdown/f167aefa480e8d37e9941a25f0b40981b74a47be/blockquotes/badge/light-theme/example.svg">
>   <img alt="Example" src="https://raw.githubusercontent.com/Mqxx/GitHub-Markdown/f167aefa480e8d37e9941a25f0b40981b74a47be/blockquotes/badge/dark-theme/example.svg">
> </picture><br>
>
> ```java
> SomeService service;
> Foo foo;
> 
> service = JBR.getSomeService();
> foo = service.getFoo();
> foo.bar(); // UnsupportedOperationException: Foo.bar - extension BAR is disabled
> 
> service = JBR.getSomeService(Extensions.BAR);
> foo = service.getFoo();
> foo.bar(); // OK
> ```

## Versioning

JBR API releases follow [semantic versioning](https://semver.org).
API and implementation versions can be retrieved from the `JBR` class:
* `JBR.getApiVersion()` - the version of `jbr-api.jar` currently used.
* `JBR.getImplVersion()` - the version of JBR API implemented by the current runtime.

> <picture>
>   <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/Mqxx/GitHub-Markdown/f167aefa480e8d37e9941a25f0b40981b74a47be/blockquotes/badge/light-theme/info.svg">
>   <img alt="Info" src="https://raw.githubusercontent.com/Mqxx/GitHub-Markdown/f167aefa480e8d37e9941a25f0b40981b74a47be/blockquotes/badge/dark-theme/info.svg">
> </picture><br>
>
>  _Versions should not be used for any purpose other than logging._
>
> Neither the API nor implementation versions are used in compatibility
> checks or when determining the service availability.
> 
> However, you can assume that when
> *impl.major == api.major && impl.minor >= api.minor*,
> all services currently present in that JBR API are **guaranteed** to be supported
> by that implementation.
> 

## References

* [JBR API documentation](https://jetbrains.github.io/JetBrainsRuntimeApi)
* [JBR API development guide](CONTRIBUTING.md)
* [JetBrainsRuntime (JBR)](https://github.com/JetBrains/JetBrainsRuntime)

## Experimental Skia Interop

`JBRSkia` is an experimental macOS-first service for tightly versioned interop
between JBR and Skiko. It exposes a paint-scoped `ScopedSkiaCanvas` so Compose
can eventually draw into the JBR-owned Metal/Skia destination during Swing
painting.

Clients must read `JBRSkia.ABI_ID` and `JBRSkia.BUILD_ID` reflectively before
acquiring `JBR.getJBRSkia()`. If either value is incompatible, or if the service
is unavailable, clients must fall back to their existing rendering path.

Current PoC command ABI is 83. In addition to typed effect descriptors for
tint, color-matrix, and lighting color filters, this ABI exposes
`COMMAND_SAVE_LAYER_COLOR_FILTER_REF`, which lets a saveLayer paint reference a
previously defined descriptor handle without sharing raw Skia pointers, and
`COMMAND_DRAW_IMAGE_REF_COLOR_FILTER_REF`, which applies descriptor-backed color
filters to cached image draws. It also exposes
`COMMAND_SAVE_LAYER_BLEND_COLOR_FILTER_REF` for descriptor-backed layer color
filters combined with direct Skia blend modes. ABI 81 adds
`getCommandCapabilities64High()` as an empty second capability word so future
commands can negotiate support without overloading the already-full low word.
ABI 82 uses that high word for `COMMAND_SAVE_LAYER_IMAGE_FILTER_REF` and
`COMMAND_EFFECT_DESCRIPTOR_BLUR_IMAGE_FILTER`, the first JBR-owned image-filter
descriptor used by graphics-layer `BlurEffect` command replay. ABI 83 adds
`COMMAND_EFFECT_DESCRIPTOR_OFFSET_IMAGE_FILTER` for simple graphics-layer
`OffsetEffect` command replay.
