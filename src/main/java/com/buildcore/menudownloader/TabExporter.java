package com.buildcore.menudownloader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.buildcore.menudownloader.mixin.PlayerTabOverlayAccessor;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Команда ".tab download": сохраняет в mods/TAB всё, что сервер (плагин TAB и похожие)
 * показывает клиенту: шапку/подвал таба, список игроков, скорборд, команды (префиксы/суффиксы).
 * Настоящие настройки плагина (условия, плейсхолдеры, анимации) клиенту не видны.
 */
public final class TabExporter {

	private static final int MAX_SIDEBAR_LINES = 15;

	private TabExporter() {
	}

	public static void export() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.getConnection() == null) {
			message(mc, "[" + MenuDownloader.MOD_NAME + "] Сначала зайди на сервер.");
			return;
		}

		try {
			Scoreboard sb = mc.level.getScoreboard();
			PlayerTabOverlay tab = mc.gui.getTabList();
			PlayerTabOverlayAccessor acc = (PlayerTabOverlayAccessor) (Object) tab;

			List<String> header = lines(acc.menudownloader$getHeader());
			List<String> footer = lines(acc.menudownloader$getFooter());

			// --- игроки в табе ---
			List<String> players = new ArrayList<>();
			for (PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
				players.add(TextConverter.toLegacy(tab.getNameForDisplay(info)));
			}

			// --- скорборд ---
			Objective sidebar = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
			String sidebarTitle = sidebar != null ? TextConverter.toLegacy(sidebar.getDisplayName()) : null;
			List<String> sidebarLines = new ArrayList<>();
			List<Integer> sidebarScores = new ArrayList<>();
			if (sidebar != null) {
				List<PlayerScoreEntry> entries = new ArrayList<>(sb.listPlayerScores(sidebar));
				entries.removeIf(e -> e.owner().startsWith("#"));
				entries.sort(Comparator.<PlayerScoreEntry>comparingInt(PlayerScoreEntry::value).reversed());
				int count = 0;
				for (PlayerScoreEntry e : entries) {
					if (count++ >= MAX_SIDEBAR_LINES) {
						break;
					}
					PlayerTeam team = sb.getPlayersTeam(e.owner());
					Component line = PlayerTeam.formatNameForTeam(team, e.ownerName());
					sidebarLines.add(TextConverter.toLegacy(line));
					sidebarScores.add(e.value());
				}
			}

			Objective belowName = sb.getDisplayObjective(DisplaySlot.BELOW_NAME);
			Objective listObj = sb.getDisplayObjective(DisplaySlot.LIST);

			// --- команды (префиксы/суффиксы, сортировка в табе идёт по имени команды) ---
			List<PlayerTeam> teams = new ArrayList<>(sb.getPlayerTeams());
			teams.sort(Comparator.comparing(PlayerTeam::getName));

			Path dir = FabricLoader.getInstance().getGameDir().resolve("mods").resolve("TAB");
			Files.createDirectories(dir);

			Files.writeString(dir.resolve("config.yml"),
					buildConfig(header, footer, sidebarTitle, sidebarLines), StandardCharsets.UTF_8);
			Files.writeString(dir.resolve("groups.yml"), buildGroups(teams), StandardCharsets.UTF_8);
			Files.writeString(dir.resolve("snapshot.txt"),
					buildSnapshot(header, footer, players, sidebarTitle, sidebarLines, sidebarScores,
							belowName, listObj, teams),
					StandardCharsets.UTF_8);

			message(mc, "[" + MenuDownloader.MOD_NAME + "] TAB: сохранено в " + dir);
		} catch (IOException | RuntimeException e) {
			message(mc, "[" + MenuDownloader.MOD_NAME + "] Ошибка экспорта TAB: " + e);
		}
	}

	private static List<String> lines(Component component) {
		List<String> out = new ArrayList<>();
		if (component == null) {
			return out;
		}
		String text = TextConverter.toLegacy(component);
		if (text.isEmpty()) {
			return out;
		}
		for (String line : text.split("\\R", -1)) {
			out.add(line);
		}
		return out;
	}

	private static void message(Minecraft mc, String text) {
		if (mc.player != null) {
			mc.player.displayClientMessage(Component.literal(text), false);
		}
	}

	// ---------- config.yml ----------

	private static String buildConfig(List<String> header, List<String> footer,
									  String sidebarTitle, List<String> sidebarLines) {
		StringBuilder sb = new StringBuilder();
		sb.append("# Exported by MenuDownloader by BuildCore (.tab download)\n");
		sb.append("# Это снимок того, что сервер показал клиенту. Условия, плейсхолдеры,\n");
		sb.append("# анимации и права клиенту не видны, допиши их вручную.\n");
		sb.append("# Структура секций близка к TAB 5, при необходимости подгони под свою версию.\n\n");

		sb.append("header-footer:\n");
		sb.append("  enabled: true\n");
		sb.append("  designs:\n");
		sb.append("    default:\n");
		appendList(sb, "      ", "header", header);
		appendList(sb, "      ", "footer", footer);

		sb.append("\nscoreboard:\n");
		if (sidebarTitle == null) {
			sb.append("  enabled: false\n");
		} else {
			sb.append("  enabled: true\n");
			sb.append("  scoreboards:\n");
			sb.append("    exported:\n");
			sb.append("      title: ").append(TextConverter.yamlQuote(sidebarTitle)).append('\n');
			appendList(sb, "      ", "lines", sidebarLines);
		}
		return sb.toString();
	}

	private static void appendList(StringBuilder sb, String indent, String key, List<String> items) {
		if (items.isEmpty()) {
			sb.append(indent).append(key).append(": []\n");
			return;
		}
		sb.append(indent).append(key).append(":\n");
		for (String item : items) {
			sb.append(indent).append("- ").append(TextConverter.yamlQuote(item)).append('\n');
		}
	}

	// ---------- groups.yml ----------

	private static String buildGroups(List<PlayerTeam> teams) {
		StringBuilder sb = new StringBuilder();
		sb.append("# Exported by MenuDownloader by BuildCore (.tab download)\n");
		sb.append("# Группы собраны из команд (teams), которые сервер создал для игроков.\n");
		sb.append("# Имя команды определяет порядок в табе (TAB часто пишет туда номер сортировки).\n\n");

		for (PlayerTeam t : teams) {
			String prefix = TextConverter.toLegacy(t.getPlayerPrefix());
			String suffix = TextConverter.toLegacy(t.getPlayerSuffix());
			if (prefix.isEmpty() && suffix.isEmpty()) {
				continue;
			}
			sb.append(TextConverter.yamlQuote(t.getName())).append(":\n");
			sb.append("  tabprefix: ").append(TextConverter.yamlQuote(prefix)).append('\n');
			sb.append("  tabsuffix: ").append(TextConverter.yamlQuote(suffix)).append('\n');
			sb.append("  tagprefix: ").append(TextConverter.yamlQuote(prefix)).append('\n');
			sb.append("  tagsuffix: ").append(TextConverter.yamlQuote(suffix)).append('\n');
		}
		return sb.toString();
	}

	// ---------- snapshot.txt ----------

	private static String buildSnapshot(List<String> header, List<String> footer, List<String> players,
									   String sidebarTitle, List<String> sidebarLines, List<Integer> scores,
									   Objective belowName, Objective listObj, List<PlayerTeam> teams) {
		StringBuilder sb = new StringBuilder();
		sb.append("# Полный снимок (MenuDownloader by BuildCore)\n\n");

		sb.append("== Шапка таба ==\n");
		header.forEach(l -> sb.append(l).append('\n'));
		sb.append("\n== Подвал таба ==\n");
		footer.forEach(l -> sb.append(l).append('\n'));

		sb.append("\n== Игроки в табе (").append(players.size()).append(") ==\n");
		players.forEach(l -> sb.append(l).append('\n'));

		sb.append("\n== Скорборд (sidebar) ==\n");
		if (sidebarTitle == null) {
			sb.append("(нет)\n");
		} else {
			sb.append("Заголовок: ").append(sidebarTitle).append('\n');
			for (int i = 0; i < sidebarLines.size(); i++) {
				sb.append(scores.get(i)).append(" | ").append(sidebarLines.get(i)).append('\n');
			}
		}

		sb.append("\n== Объект под именем (below_name) ==\n");
		sb.append(belowName == null ? "(нет)\n"
				: belowName.getName() + " | " + TextConverter.toLegacy(belowName.getDisplayName()) + '\n');

		sb.append("\n== Объект в списке игроков (list) ==\n");
		sb.append(listObj == null ? "(нет)\n"
				: listObj.getName() + " | " + TextConverter.toLegacy(listObj.getDisplayName()) + '\n');

		sb.append("\n== Команды (").append(teams.size()).append(") ==\n");
		for (PlayerTeam t : teams) {
			sb.append(t.getName())
					.append(" | prefix=").append(TextConverter.toLegacy(t.getPlayerPrefix()))
					.append(" | suffix=").append(TextConverter.toLegacy(t.getPlayerSuffix()))
					.append(" | игроки=").append(t.getPlayers())
					.append('\n');
		}
		return sb.toString();
	}
}
