# Ashlord Plugin

A small Paper plugin that grants a red **Ashlord** rank to the online player with the highest balance. The plugin checks every 5 minutes by default (configurable) and updates the prefix and tab-list name for the current holder.

## Requirements
- Paper server (1.21+)
- Vault and an economy plugin (e.g., EssentialsX, iConomy)

## Features
- Periodic check (default every 300 seconds) for the highest balance among *online* players
- Assigns a bold red `Ashlord` prefix using team prefix and player-list name
- Command `/ashlord check` to force a check and `/ashlord reload` to reload config

## Config (`config.yml`)
- `interval-seconds`: How often to check (default 300)
- `ashlord-color`: Hex color for the prefix (without `#`, default `FF2D2D`)
- `prefix-text`: Text to show as the prefix (default `Ashlord`)
- `announce`: Whether to send a small message to players when they gain/lose the title

## Installation
1. Build the plugin with Maven (`mvn package`) or Gradle (`gradle build` / `./gradlew build`).
2. Put the generated JAR (`target/Ashlord-1.0.0.jar`) into your server's `plugins` folder.
3. Start the server (Vault + economy plugin should be present).

## Notes
- Only **online** players are considered for the Ashlord title.
- The plugin uses Vault for balances — ensure Vault and an economy plugin are installed.

Enjoy! ✨
