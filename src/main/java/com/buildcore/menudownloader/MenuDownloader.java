package com.buildcore.menudownloader;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;

/**
 * MenuDownloader by BuildCore.
 * Adds "DeluxeMenus" and "BMenu" buttons above chest-style server menus.
 */
public class MenuDownloader implements ClientModInitializer {

	public static final String MOD_NAME = "MenuDownloader";

	private static final int BUTTON_WIDTH = 84;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;

	@Override
	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof ContainerScreen gui)) {
				return;
			}

			int rows = gui.getMenu().getRowCount();
			int imageHeight = 114 + rows * 18; // размер ванильного окна сундука
			int left = (scaledWidth - 176) / 2;
			int top = (scaledHeight - imageHeight) / 2;
			int y = Math.max(2, top - BUTTON_HEIGHT - GAP);

			Screens.getButtons(screen).add(
					Button.builder(Component.literal("DeluxeMenus"),
									b -> MenuExporter.export(gui, MenuExporter.Format.DELUXEMENUS))
							.bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT)
							.build());

			Screens.getButtons(screen).add(
					Button.builder(Component.literal("BMenu"),
									b -> MenuExporter.export(gui, MenuExporter.Format.BMENU))
							.bounds(left + BUTTON_WIDTH + GAP, y, BUTTON_WIDTH, BUTTON_HEIGHT)
							.build());
		});
	}
}
