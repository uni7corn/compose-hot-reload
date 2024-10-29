This module represents the actual JVM implementation of the hot reload runtime.

# Source Sets/Compilations

## main

Represents the runtime for regular production launches of a compose APP:
The JvmDevelopmentEntryPoint is a no-op and does nothing.

## dev

Represents the variant of the runtime which is loaded in dev/hot runs of the App.
Will contain the actual code relevant for hot-reloading

## Shared

This compilation contains code which is shared across main & dev.  
The compiled classes will be available in the dev and in the main artifacts