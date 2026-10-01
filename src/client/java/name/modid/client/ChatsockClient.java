package name.modid.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import name.modid.Chatsock;

public class ChatsockClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, profile, chatType, receptionTimestamp) ->
				ChatSocketServer.broadcast(message.getString()));
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				ChatSocketServer.broadcast(message.getString());
			}
		});
		ChatSocketServer.start();
		Chatsock.LOGGER.info("Chat socket listener initialized");
	}
}