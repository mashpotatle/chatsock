package name.modid.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import net.minecraft.client.Minecraft;
import name.modid.Sockchat;

final class ChatSocketServer {
	private static final int DEFAULT_PORT = 25576;
	private static final int MAX_MESSAGE_LENGTH = 256;
	private static final Object CLIENT_LOCK = new Object();
	private static Socket activeClient;
	private static BufferedWriter activeWriter;

	private ChatSocketServer() {
	}

	static void start() {
		int port = getPort();
		Thread listener = new Thread(() -> listen(port), "sockchat-socket-listener");
		listener.setDaemon(true);
		listener.start();
	}

	private static int getPort() {
		String configuredPort = System.getProperty("sockchat.port");
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

		Sockchat.LOGGER.warn("Invalid sockchat.port '{}'; using {}", configuredPort, DEFAULT_PORT);
		return DEFAULT_PORT;
	}

	private static void listen(int port) {
		try (ServerSocket server = new ServerSocket()) {
			server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
			Sockchat.LOGGER.info("Chat socket listening on {}:{}", server.getInetAddress().getHostAddress(), port);

			while (!Thread.currentThread().isInterrupted()) {
				try (Socket client = server.accept()) {
					setActiveClient(client);
					try {
						readMessages(client);
					} finally {
						clearActiveClient(client);
					}
				} catch (IOException exception) {
						Sockchat.LOGGER.warn("Chat socket client disconnected with an error", exception);
				}
			}
		} catch (IOException exception) {
			Sockchat.LOGGER.error("Could not start chat socket listener on port {}", port, exception);
		}
	}

	private static void setActiveClient(Socket client) throws IOException {
		synchronized (CLIENT_LOCK) {
			activeClient = client;
			activeWriter = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
		}
		Sockchat.LOGGER.info("Chat socket client connected");
	}

	private static void clearActiveClient(Socket client) {
		synchronized (CLIENT_LOCK) {
			if (activeClient == client) {
				activeClient = null;
				activeWriter = null;
			}
		}
			Sockchat.LOGGER.info("Chat socket client disconnected");
	}

	static void broadcast(String message) {
		String singleLineMessage = message.replace('\r', ' ').replace('\n', ' ');
		synchronized (CLIENT_LOCK) {
			if (activeWriter == null) {
				return;
			}

			try {
				activeWriter.write(singleLineMessage);
				activeWriter.newLine();
				activeWriter.flush();
			} catch (IOException exception) {
					Sockchat.LOGGER.warn("Could not send chat message to socket client", exception);
				try {
					activeClient.close();
				} catch (IOException closeException) {
						Sockchat.LOGGER.debug("Could not close disconnected chat socket client", closeException);
				}
				activeClient = null;
				activeWriter = null;
			}
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
			Sockchat.LOGGER.warn("Ignoring chat socket message longer than {} characters", MAX_MESSAGE_LENGTH);
				continue;
			}

			String chatMessage = message;
			Minecraft.getInstance().execute(() -> {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player == null) {
					Sockchat.LOGGER.warn("Ignoring chat socket message because the player is not in a world");
					return;
				}
				minecraft.player.connection.sendChat(chatMessage);
			});
		}
	}
}