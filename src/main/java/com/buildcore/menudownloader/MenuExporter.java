package com.buildcore.menudownloader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/** Читает содержимое открытого меню и сохраняет его в txt в папку mods/MenuDownloader. */
public final class MenuExporter {

	public enum Format {
		DELUXEMENUS("DeluxeMenus"),
		BMENU("BMenu");

		public final String label;

		Format(String label) {
			this.label = label;
		}
	}

	private record Entry(int slot, String material, int amount, String name, List<String> lore) {
	}

	private MenuExporter() {
	}

	public static void export(ContainerScreen screen, Format format) {
		Minecraft mc = Minecraft.getInstance();
		try {
			ChestMenu menu = screen.getMenu();
			int size = menu.getRowCount() * 9;
			String title = TextConverter.toLegacy(screen.getTitle());

			List<Entry> entries = new ArrayList<>();
			for (int i = 0; i < size; i++) {
				ItemStack stack = menu.getSlot(i).getItem();
				if (stack.isEmpty()) {
					continue;
				}

				String material = BuiltInRegistries.ITEM.getKey(stack.getItem())
						.getPath().toUpperCase(Locale.ROOT);

				Component customName = stack.get(DataComponents.CUSTOM_NAME);
				String name = customName != null ? TextConverter.toLegacy(customName) : null;

				List<String> lore = new ArrayList<>();
				ItemLore itemLore = stack.get(DataComponents.LORE);
				if (itemLore != null) {
					for (Component line : itemLore.lines()) {
						lore.add(TextConverter.toLegacy(line));
					}
				}

				entries.add(new Entry(i, material, stack.getCount(), name, lore));
			}

			String content = switch (format) {
				case DELUXEMENUS -> buildDeluxeMenus(title, size, entries);
				case BMENU -> buildBMenu(title, size, entries);
			};

			String safeTitle = TextConverter.stripCodes(title)
					.replaceAll("[^A-Za-z0-9А-Яа-яЁё]+", "_")
					.replaceAll("^_+|_+$", "");
			if (safeTitle.isEmpty()) {
				safeTitle = "menu";
			}
			String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

			Path dir = FabricLoader.getInstance().getGameDir()
					.resolve("mods").resolve(MenuDownloader.MOD_NAME);
			Files.createDirectories(dir);
			Path file = dir.resolve(format.label + "_" + safeTitle + "_" + time + ".txt");
			Files.writeString(file, content, StandardCharsets.UTF_8);

			message(mc, "[" + MenuDownloader.MOD_NAME + "] " + format.label
					+ ": сохранено в " + file);
		} catch (IOException | RuntimeException e) {
			message(mc, "[" + MenuDownloader.MOD_NAME + "] Ошибка экспорта: " + e);
		}
	}

	private static void message(Minecraft mc, String text) {
		if (mc.player != null) {
			mc.player.displayClientMessage(Component.literal(text), false);
		}
	}

	// ---------- DeluxeMenus ----------

	private static String buildDeluxeMenus(String title, int size, List<Entry> entries) {
		String openCommand = TextConverter.stripCodes(title).toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "");
		if (openCommand.isEmpty()) {
			openCommand = "menu";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("# Exported by MenuDownloader by BuildCore\n");
		sb.append("# Команды, условия и действия по клику клиенту не видны - допиши их вручную.\n");
		sb.append("menu_title: ").append(q(title)).append('\n');
		sb.append("open_command: ").append(openCommand).append('\n');
		sb.append("size: ").append(size).append('\n');
		sb.append("items:\n");

		for (Entry e : entries) {
			sb.append("  'slot_").append(e.slot()).append("':\n");
			sb.append("    material: ").append(e.material()).append('\n');
			sb.append("    slot: ").append(e.slot()).append('\n');
			if (e.amount() > 1) {
				sb.append("    amount: ").append(e.amount()).append('\n');
			}
			if (e.name() != null) {
				sb.append("    display_name: ").append(q(e.name())).append('\n');
			}
			if (!e.lore().isEmpty()) {
				sb.append("    lore:\n");
				for (String line : e.lore()) {
					sb.append("    - ").append(q(line)).append('\n');
				}
			}
		}
		return sb.toString();
	}

	// ---------- BMenu ----------

	/**
	 * ЗАГЛУШКА: точный формат конфигов BMenu пока неизвестен.
	 * Пока пишется общий дамп. Когда будет пример настоящего BMenu-конфига,
	 * замени содержимое этого метода.
	 */
	private static String buildBMenu(String title, int size, List<Entry> entries) {
		StringBuilder sb = new StringBuilder();
		sb.append("# Exported by MenuDownloader by BuildCore\n");
		sb.append("# ВНИМАНИЕ: формат BMenu ещё не реализован, это общий дамп меню.\n");
		sb.append("title: ").append(q(title)).append('\n');
		sb.append("size: ").append(size).append('\n');
		sb.append("items:\n");

		for (Entry e : entries) {
			sb.append("  ").append(e.slot()).append(":\n");
			sb.append("    material: ").append(e.material()).append('\n');
			sb.append("    amount: ").append(e.amount()).append('\n');
			if (e.name() != null) {
				sb.append("    name: ").append(q(e.name())).append('\n');
			}
			if (!e.lore().isEmpty()) {
				sb.append("    lore:\n");
				for (String line : e.lore()) {
					sb.append("      - ").append(q(line)).append('\n');
				}
			}
		}
		return sb.toString();
	}

	/** YAML-строка в одинарных кавычках. */
	private static String q(String s) {
		return "'" + s.replace("'", "''") + "'";
	}
}
