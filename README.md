# soapstone
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://api.travis-ci.com/alfasoftware/soapstone.svg?branch=master)](https://travis-ci.com/alfasoftware/soapstone)

An adapter for exposing JAX-WS services as JSON over HTTP.

## Summary

Soapstone is designed to facilitate the creation of a JAX-RS resource which provides access to an existing set of JAX-WS defined web services using 
JSON and standard HTTP methods.

### Why?

The API exposed by soapstone will not be a truly RESTful API, as it simply exposes an existing SOAP API. So why provide it?

There are legitimate reasons to use SOAP rather than REST for a particular API, but there are also notable drawbacks, particularly around ease of 
integration with third-party services or of hacking together demos. Having more flexibility in accessing your operations can only be a good thing.

### How?

Soapstone aims to be simple in its operation, and to make as few assumptions about the underlying JAX-WS implementation as possible. It provides a 
JAX-RS resource, SoapstoneService, and a builder to construct that service, SoapstoneServiceBuilder. The builder requires you to provide a map 
of JAX-WS annotated classes and paths to give you control over how the classes are gathered and instantiated, and the paths over which they are 
exposed. Reflection is then used to identify the classes and methods to invoke for particular requests. Exposed methods are limited according
to JAX-WS standards to prevent exposure of anything unexpected.

Jackson's powerful JAXB annotation support is used to handle the mapping of Java types to JSON and vice-versa. Additionally there is support for 
mapping HTTP headers to SOAP headers and simple parameters may be handled as query parameters.

All operations are supported via POST. Additionally, patterns can be defined to allow access to matching operations by certain other methods. E.g., 
operations matching "get*" via GET, operations matching "update*" via PUT.

An ExceptionMapper interface is provided to allow customisation of the mapping of exceptions coming from web service method invocations to 
web application exceptions.

## Examples

A set of examples for usage of the builder and of the services themselves will follow shortly.
