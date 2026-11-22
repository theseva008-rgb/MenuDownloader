package com.buildcore.menudownloader;

import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/** Переводит Component (цвета и стили Minecraft) в старый формат с кодами &a, &l, &#RRGGBB. */
public final class TextConverter {

	private TextConverter() {
	}

	public static String toLegacy(Component component) {
		StringBuilder out = new StringBuilder();
		String[] prevKey = {""};

		component.visit((style, text) -> {
			if (text.isEmpty()) {
				return Optional.empty();
			}

			String key = styleKey(style);
			if (!key.equals(prevKey[0])) {
				TextColor color = style.getColor();
				if (color != null) {
					out.append(colorCode(color));
				} else if (!prevKey[0].isEmpty()) {
					out.append("&r");
				}
				if (style.isBold()) out.append("&l");
				if (style.isItalic()) out.append("&o");
				if (style.isUnderlined()) out.append("&n");
				if (style.isStrikethrough()) out.append("&m");
				if (style.isObfuscated()) out.append("&k");
				prevKey[0] = key;
			}

			out.append(text.replace('§', '&'));
			return Optional.empty();
		}, Style.EMPTY);

		return out.toString();
	}

	/** Убирает коды &x и &#RRGGBB (для имён файлов и open_command). */
	public static String stripCodes(String legacy) {
		return legacy.replaceAll("&#[0-9A-Fa-f]{6}", "").replaceAll("&[0-9a-fk-orA-FK-OR]", "");
	}

	private static String styleKey(Style style) {
		TextColor color = style.getColor();
		StringBuilder sb = new StringBuilder();
		if (color != null) sb.append(color.getValue());
		sb.append('|');
		if (style.isBold()) sb.append('B');
		if (style.isItalic()) sb.append('I');
		if (style.isUnderlined()) sb.append('U');
		if (style.isStrikethrough()) sb.append('S');
		if (style.isObfuscated()) sb.append('O');
		return sb.toString().equals("|") ? "" : sb.toString();
	}

	private static String colorCode(TextColor color) {
		int rgb = color.getValue();
		for (ChatFormatting f : ChatFormatting.values()) {
			if (f.isColor() && f.getColor() != null && f.getColor().intValue() == rgb) {
				return "&" + f.getChar();
			}
		}
		return String.format("&#%06X", rgb);
	}
}
