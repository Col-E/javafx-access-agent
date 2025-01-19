# JavaFX Thread Access Checking Agent

This agent instruments one or more packages to insert checks around calls to JavaFX methods.
These checks record if the call was made on the JavaFX application thread.
When a call is made off the JavaFX application thread any registered `AccessCheckListener` is notified.
You should register at least one listener via the `AccessCheck` class.

## Usage

1. Add `javafx-access-agent` to your application's classpath
2. Add `javafx-access-agent` as a VM argument `-javaagent:javafx-access-agent.jar=com.example.myapp;org.example.library`
3. Register a `AccessCheckListener` in `AccessCheck` in your application to handle logging off-thread calls in your desired way
    - You should do this in your application's `main` method, or at the earliest point of execution you have control over
4. Run your application