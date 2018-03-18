# sim

This kotlin library provides basic means for running simple event driven simulations.

## Status

This software is in pre-release state. Every aspect of it may change without announcement or
notification or downward compatibility. As soon as version 1.0 is reached, all subsequent
changes for sub versions will be downward compatible. Breaking changes will then only occur
with a new major version with according deprecation marking.

## Include in gradle builds

To include this library in a gradle build, add

    repositories {
        ...
        maven { url "https://straightway.github.io/repo" }
    }

Then you can simply configure in your dependencies:

    dependencies {
        compile "straightway:sim<version>"
    }
