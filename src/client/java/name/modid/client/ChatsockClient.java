package name.modid.client;

import net.fabricmc.api.ClientModInitializer;
import name.modid.Chatsock;

public class ChatsockClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ChatSocketServer.start();
		Chatsock.LOGGER.info("Chat socket listener initialized");
	}
}