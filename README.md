# ChatSock

ChatSock is a client-side Fabric mod for Minecraft 26.1. It sends each line received over a local TCP socket as a chat message from the connected player and forwards incoming player chat back over that connection.

## Use

Start Minecraft with the mod installed and join a world or server. The socket listener binds to the loopback interface on port `25576`; set the JVM system property `-Dchatsock.port=PORT` to use another port.

Connect to `127.0.0.1:25576` and send UTF-8 text followed by a newline. Each non-empty line is sent as one chat message. Messages longer than 256 characters are ignored. Incoming player chat is sent to the connected client as UTF-8 text followed by a newline; embedded line breaks are replaced with spaces. The listener accepts one connection at a time and accepts another after the current connection closes.

For example, this Python snippet sends one message:

```python
import socket

with socket.create_connection(("127.0.0.1", 25576)) as connection:
	connection.sendall("Hello from the socket!\n".encode("utf-8"))
```

## Setup

1. Install Java 25 or later.
2. Use the Fabric installer to install Fabric Loader for Minecraft 26.1, then start the Fabric profile once.
3. Download Fabric API for Minecraft 26.1 and put its `.jar` file in the Minecraft `mods` folder.
4. Get the ChatSock mod jar. To build it from this source project, run `./gradlew build` (Windows: `gradlew.bat build`) from the project folder. The mod jar will be `build/libs/chatsock-1.0.0.jar`. Do not use the `-sources.jar` file.
5. Put `chatsock-1.0.0.jar` in the same `mods` folder. Create the folder if it does not exist.
6. Launch the Fabric 26.1 profile. Join a world or server; ChatSock starts listening on port `25576`.

The `mods` folder is inside the Minecraft game directory. On Linux, the default location is `~/.minecraft/mods`.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
