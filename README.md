# MenuDownloader by BuildCore

Клиентский Fabric-мод для Minecraft **1.21.11**. Над любым меню-сундуком сервера появляются кнопки **DeluxeMenus** и **BMenu**. По нажатию меню сохраняется в `.minecraft/mods/MenuDownloader/` в виде `.txt`.

## Сборка
Нужны Java 21 и IntelliJ IDEA.

1. Сгенерируй пустой проект на https://fabricmc.net/develop/template (Minecraft 1.21.11, Mojang mappings), чтобы получить папку `gradle/`, `gradlew` и `gradlew.bat` (Gradle wrapper).
2. Скопируй их в эту папку (или перенеси `src/` и три файла `build.gradle`, `gradle.properties`, `settings.gradle` в сгенерированный проект).
3. Открой проект в IntelliJ, дождись загрузки Gradle.
4. Запусти `./gradlew build`. Готовый мод будет в `build/libs/MenuDownloader-1.0.0.jar`.
5. Положи jar и **Fabric API** в папку `mods`.

Если Gradle ругается на версию Fabric API или Loom, возьми актуальные значения для 1.21.11 на fabricmc.net/develop и поменяй их в `gradle.properties`.

## Ограничения
- Экспортируются: материал, слот, количество, название, описание (lore). Это то, что сервер присылает клиенту.
- Команды (`click_commands`), условия и плейсхолдеры клиент не видит, их нужно дописать вручную.
- Головы игроков экспортируются как `PLAYER_HEAD` без текстуры.
- Формат BMenu пока заглушка (общий дамп): нужен пример реального конфига BMenu.
