# microBean™ Construct

[![Maven Central](https://img.shields.io/maven-central/v/org.microbean/microbean-construct.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.microbean/microbean-construct)

The microBean™ Construct project provides classes and interfaces assisting with Java constructs such as types and
elements.

Among other things, this project enables constructs modeled by the `javax.lang.model.*` packages to be accessed at
runtime in a thread-safe manner. This permits the same constructs to be used at build time (e.g. by annotation
processors) and at runtime (e.g. by tools and environments that need to perform reflection-like activities without
actually loading and initializing classes), using only official and sanctioned APIs.

# Status

This project is currently experimental, in a pre-alpha state, and unsuitable for production use.

# Compatibility

**Until further notice, this project's APIs are subject to frequent backwards-incompatible signature and behavior
changes, regardless of project version and without notice.**

# Requirements

microBean™ Construct requires a Java runtime of version 21 or higher.

# Installation

microBean™ Construct is available on [Maven Central](https://search.maven.org/). Include microBean™ Construct as a Maven
dependency:

```xml
<dependency>
  <groupId>org.microbean</groupId>
  <artifactId>microbean-construct</artifactId>
  <!--
    Always check https://search.maven.org/artifact/org.microbean/microbean-construct
    for up-to-date available versions.
  -->
  <version>0.0.20</version>
</dependency>
```

# Documentation

Full documentation is available at
[microbean.github.io/microbean-construct](https://microbean.github.io/microbean-construct/).

# References

* This project is tangentially and purely coincidentally related to [JEP 119](https://openjdk.org/jeps/119).
* A seemingly shelved attempt to implement the `javax.lang.model.*` constructs in terms of reflective constructs can be
  found in [JDK-8004133](https://bugs.openjdk.org/browse/JDK-8004133).
