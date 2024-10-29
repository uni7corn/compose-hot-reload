# Run Tests

```
./gradlew check
```

# Architecture Overview

## Orchestration

The communication between a build tool (e.g., Gradle), the Application and tools such as the IDE is handled by
the 'orchestration.' If not instructed otherwise, the Application under hot reload will start a new orchestration
server. All connected clients can exchange messages as broadcasts. For example, the build tool might broadcast
a 'ReloadClassesRequest' after recompiling.

Tests or tools like the IDE might create their own orchestration server and instruct the Application to
connect to this server in 'client mode' by providing the necessary system properties.

## Agent

When an application is launched in 'hot reload mode,' then a special Compose Hot Reload agent will be
added to the jvm execution. This agent will participate in the orchestration and reload classes on demand.

## Runtime

When an application is launched in 'hot reload mode' then a special 'dev' variant of the runtime will be
added to the runtime classpath. This variant will be able to communicate with the Agent and also
participate in the orchestration by listening for messages and broadcasting UI related messages
(such as UIRendered).

## Recompiler

When the dev runtime is launched, a Gradle Daemon is launched in continuous mode, which will connect
to the orchestration and send 'ReloadClassesRequests' when the actual .class files for the runtime classpath
changes.