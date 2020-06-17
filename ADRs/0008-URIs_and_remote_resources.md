# URIs/URLs and Remote Resources

## Status
ACCEPTED (17/06/2020)

Proposed by: Alex (11/06/2020)

Discussed with: 

## Context

Currently, the CLI allows users to launch a pipeline based on a configuration file that is hosted locally, such as:
```text
konduit serve -c /some/path/to/config.json
```

However, in addition to serving local files, we want to support serving of pipelines that are hosted remotely:
```text
konduit serve -c https://www.somewebsite.com/pipelines/config.json
konduit serve -c s3://s3-eu-west-1.amazonaws.com/myaccount/models/myconfig.yaml
konduit serve -c wasb://mycontainer@myaccount.blob.core.windows.net/directory/myconfig.json
```

In addition to launching a server from a configuration hosted at a remote location, we also want the ability to load models
that are hosted at some non-local URI.  
That is: when loading any model type, the location of the model is specified in terms of a URI in the configuration
(such as the `modelUri` field in DL4JModelPipelineStep and TensorFlowPipelineStep) which needs to be downloaded (and perhaps cached).

Consider the following simple use case:
```text
$ pip install konduit-serving
$ konduit serve -c https://serving.konduit.ai/pipelines/images/classification/mobilenetv2.json
```
Assume the model specified in mobilenetv2.json is hosted remotely also at say `https://serving.konduit.ai/models/mobilenetv2.pb`.  
This use case (remote configuration and remote model) should "just work".

Overall, we will aim to support the following URI types for both configurations and model locations:
* HTTP/HTTPS, HTTPS + basic authentication, and _maybe_ other options like HTTPS + OAuth 2.0?
* FTP, FTP + basic authentication
* HDFS (Hadoop filesystem, hdfs://)
* Amazon S3 (s3://)
* Azure (Storage account / blob storage - wasb://, wasbs://)
* Google Cloud Storage (gs://)

For all of these, we simply need the ability to download a single file given the URI. At this point, uploading, directory
listing, etc is not required.

The supported types of URIs should be extensible also - i.e., users should be able to add other types if needed.

## Decision

We utilize standard Java APIs for this: URI, URL, URLConnection and InputStream:
```java
URI u = URI.create("hdfs://somepath:12345/pipelines/config.json");
URL url = u.toURL();
URLConnection connection = url.openConnection();
try(InputStream is = connection.getInputStream()){
    //Read data
}
```

There are three issues we need to resolve in this approach.
1. URI --> URL - i.e., we need protocol handlers for each protocol type  
   A reminder: URIs describe a resource; URLs additionally provide a mechanism to access the resource.
2. Ensuring the protocol handlers are on the classpath when required  
   Having every protocol handler always on the classpath is a simple solution/option here, though doing so will pollute
   the classpath with a bunch of additional dependencies most users won't need, and risks introducing  dependency divergence
   (version) issues.
3. Authentication - how do we deal with authentication (which may be optionally present or absent in many URI types)  
   Note also we may need multiple sets of different credentials for launching the one pipeline.  
   For example, we may require one set of credentials for `https://somewebsite.com/myconfig.json` and an entirely different
   set of credentials for `https://othersite.com/mymodel.pb`. 

### URI --> URL + Protocol Handlers

For the "standard" protocol types such as HTTPS and FTP, these will be built into `konduit-serving-pipeline`.
The other protocols (requiring 3rd party dependencies - `hdfs://`, `s3://` etc) will be placed into separate modules with
names `konduit-serving-protocol/konduit-serving-<protocol_name>` i.e., all protocols are in their own module under the
`konduit-serving-protocol` parent module, with name (for example, for Azure `wasb://` and `wasbs://`) `konduit-serving-azure`.

In each of these modules (and also in konduit-serving-pipeline) we will provide the following:
* `SomeURLStreamHandler extends java.net.URLStreamHandler`
* `SomeURLConnection extends java.net.URLConnection`

Some of the underlying dependencies (S3 or Azure APIs for example) may provide these already.

Finally, we need to actually register these 2 new classes so they are used when we attempt to do the URI --> URL conversion.

Apparently there are 2 ways to do this: https://accu.org/index.php/journals/1434
* Register a factory
* Register via system property

The system property approach is somewhat ugly, but might be the easiest solution. If possible, we will set it at runtime
using System.setProperty before anything that would attempt to read a URL would be used.
For the content of that system property (i.e., the fully qualified class names), we can likely simply collect all available
handlers using a mechanism similar to what konduit-serving-meta already uses for collecting all available pipeline step types.


The alternative is to use URL.setURLStreamHandlerFactory but this has the unfortunate design of being callable only once
for the entire JVM: https://docs.oracle.com/javase/7/docs/api/java/net/URL.html#setURLStreamHandlerFactory(java.net.URLStreamHandlerFactory)
i.e., the second call to URL.setURLStreamHandlerFactory will throw an exception.
This is also an option but risks being incompatible with any 3rd party dependencies that also call this method.  


See also: https://www.oreilly.com/library/view/learning-java/1565927184/apas02.html


### Authentication and Credentials 

Unfortunately, credentials are likely going to be protocol-specific, with different ways of setting the credentials
for different protocols. For example, Azure may require environment variables so set for example an access key, whereas
other protocols may require system properties or some other mechanism.  

We will need to look into the available authentication methods for each of the protocols we want to support.
For now, where system properties or environment variables can be used, we'll provide a mechanism to do so when launching
a server (or calling the CLI).


### Protocols for CLI

Unless the dependencies for the other protocol types (s3, azure, hdfs, etc) are especially large (file size) or are problematic
(dependency problems) we will simply include all of the protocol types by default in the CLI.

If there are problems with this "always available in CLI" design, we will switch to an alternative approach:
* Only the standard protocols (http, ftp etc) are available by default
* If another protocol is required (s3, wasbs, hdfs etc) we download the appropriate module dependencies using the build tool
* Each module has an entry point used for downloading files, which we run to download the file   
  That is: we launch a new, temporary process from the CLI that simply calls the module's main method in order to download
  the configuration file and then exit 

Note however that we'll probably need this "alternative approach" in the future anyway to enable us to support custom protocols
via the CLI.

### Protocols for Runtime (Model Servers)

There are two use cases here, as discussed in the ADR 0006-CI_Launching_vs_Build.md.  
First is the "immediate CLI launch case", and second is the "deployment artifact" launch case.

For both cases, we will start by always including all protocol modules as per the CLI.  
And again, if this proves problematic, we will consider an alternative.

Alternative approach, to be implemented if necessary: We will use the same approach that we use already for determining
modules and dependencies for servers via the build tool.  
That is: We will look at the provided configuration (JSON/YAML) and find any relevant URIs. From that, we will determine
the modules needed. For example, if the configuration contains `"modelUri" : "s3://some/path/to/model.pb` then we know
we need to include the `konduit-serving-s3` module when launching the server.



### Extensibility - Custom Protocol Types

The likely approach for custom protocols is to use the (soon to be added) `additionalDependencies` / `additionalJars` options
for the CLI and build tool. By setting one of these, a user can make a protocol handler for their custom type available
both for the CLI and at runtime.

### File Caching

An additional consideration here is file caching.  
Suppose I'm launching a server from a remote URI. I shut down and then immediately restart the server.  
Upon that restart: should I download the model again or not?
For large models, this could be a significant problem - we don't want to redundantly download a model every time we launch
a server. 

To account for this, we will cache downloaded models in say `~/.konduit_cache`.  
Then, when launching servers, we will:
1. Check this cache. If no file exists, download as normal
2. If a file does exist, we will open a URLConnection and check that both of the values returned by `URLConnection.getLastModified()`
   and `URLConnection.getContentLength()`. If either differs, we will delete the old file and download from scratch.
  
Note that if opening the URL fails after say 3 retries (hence we can't check if the cached file is the latest): by default
we will provide a warning and use the cached version anyway. This has the downside of potentially using an old model,
for a server, but reduces the probability of intermittent network problems causing a server launch failure even when
the model we've got cached is totally fine.  
However, this "use anyway" default behaviour should be switchable to "fail if we can't validate" via some system property
or environment variable - perhaps `konduit.serving.cache.validation.warnonly` set to true by default, false if "exception
on validation failure" is preferred instead.

To implement this, when downloading a model into the cache, we need to record (in a separate file) the content length and
last modified values. We should also record the most recent time a cache entry was accessed, which will allow us to delete
old entries (for example, those that haven't been used for say 60 days or more by default). We will thus implement a method
to check and clear the cache once per JVM launch of a server, after the cache has been used/accessed.   

To implement this caching for downloading models, we will add a `URIResolver` class to `konduit-serving-pipeline` with
`public static File getFile(URI uri)` and `getFile(String uri)` methods that handles the downloading, caching, etc.
This URIResolver class should then be used within each of the pipeline step runners to launch models.

Note that when no scheme is provided for the `getFile(String)` method, URIResolver should assume it's a local file (and
hence add the `file://` scheme to the provided path). Users shouldn't get obscure URI syntax exceptions when trying to
launch from a local file if they forget to add `file://` to the start of every single path. 

Finally, as a workaround for potentially stale cache entries, we will implement a system property `konduit.serving.cache.clear`
that, when set to true, will clear the cache before launching. This should be rarely needed, but will be available if the
lastModified and contentLength checks fail to identify an updated remote file for some reason.  

## Consequences 

### Advantages

* Users can launch servers and automatically download models, when the configuration/model is specified in terms of a
  remote URI - and it "just works"
* It is possible to directly launch remote pipelines/models that are not publicly available (i.e., password protected etc)
* Caching system to avoid redundant downloads

  
### Disadvantages

* More modules to be added to Konduit Serving
* Authentication adds complexity for both implementation and users

## Discussion

