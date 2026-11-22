package com.buildcore.menudownloader;

import java.util.Locale;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

/** Перехватывает ".tab download" в чате (сообщение на сервер не уходит). */
public final class TabCommand {

	private TabCommand() {
	}

	public static void register() {
		ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
			String m = message.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
			if (m.equals(".tab download")) {
				TabExporter.export();
				return false; // не отправлять в чат сервера
			}
			return true;
		});
	}
}
