package name.modid.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import net.minecraft.client.Minecraft;
import name.modid.Chatsock;

final class ChatSocketServer {
	private static final int DEFAULT_PORT = 25576;
	private static final int MAX_MESSAGE_LENGTH = 256;

	private ChatSocketServer() {
	}

	static void start() {
		int port = getPort();
		Thread listener = new Thread(() -> listen(port), "chatsock-socket-listener");
		listener.setDaemon(true);
		listener.start();
	}

	private static int getPort() {
		String configuredPort = System.getProperty("chatsock.port");
		if (configuredPort == null) {
			return DEFAULT_PORT;
		}

		try {
			int port = Integer.parseInt(configuredPort);
			if (port >= 1 && port <= 65535) {
				return port;
			}
		} catch (NumberFormatException ignored) {
		}

		Chatsock.LOGGER.warn("Invalid chatsock.port '{}'; using {}", configuredPort, DEFAULT_PORT);
		return DEFAULT_PORT;
	}

	private static void listen(int port) {
		try (ServerSocket server = new ServerSocket()) {
			server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
			Chatsock.LOGGER.info("Chat socket listening on {}:{}", server.getInetAddress().getHostAddress(), port);

			while (!Thread.currentThread().isInterrupted()) {
				try (Socket client = server.accept()) {
					readMessages(client);
				} catch (IOException exception) {
					Chatsock.LOGGER.warn("Chat socket client disconnected with an error", exception);
				}
			}
		} catch (IOException exception) {
			Chatsock.LOGGER.error("Could not start chat socket listener on port {}", port, exception);
		}
	}

	private static void readMessages(Socket client) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
		String message;
		while ((message = reader.readLine()) != null) {
			if (message.isBlank()) {
				continue;
			}
			if (message.length() > MAX_MESSAGE_LENGTH) {
				Chatsock.LOGGER.warn("Ignoring chat socket message longer than {} characters", MAX_MESSAGE_LENGTH);
				continue;
			}

			String chatMessage = message;
			Minecraft.getInstance().execute(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null) {
					Chatsock.LOGGER.warn("Ignoring chat socket message because the player is not in a world");
					return;
				}
				minecraft.player.connection.sendChat(chatMessage);
			});
		}
	}
}