# Vote2Sleep

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3.0-blue.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.1%2B-orange.svg)](https://minecraft.net)
[![Java](https://img.shields.io/badge/Java-25%2B-red.svg)](https://adoptium.net/)

Vote2Sleep is a modern sleep voting plugin for Minecraft servers. Players can vote to skip night or storms using beds or commands, with BossBar progress, effects, PlaceholderAPI support, AFK detection, and Folia-safe scheduling.

<details>
<summary>📚 Table of Contents</summary>

- [📥 Downloads](#-downloads)
- [✅ Requirements](#-requirements)
- [✨ Features](#-features)
- [🚀 Installation](#-installation)
- [📋 Commands](#-commands)
- [🛡️ Permissions](#-permissions)
- [🔧 Configuration](#-configuration)
- [💬 Messages](#-messages)
- [📊 PlaceholderAPI](#-placeholderapi)
- [🗄️ Database](#-database)
- [🔌 Developer API](#-developer-api)
- [🏗️ Building from Source](#-building-from-source)
- [🤝 Contributing](#-contributing)
- [🐛 Bug Reports and Feature Requests](#-bug-reports-and-feature-requests)
- [📄 License](#-license)

</details>

## 📥 Downloads

- [GitHub Releases](https://github.com/k1ritoz/Vote2Sleep/releases)
- [Modrinth](https://modrinth.com/plugin/vote2sleep)

## ✅ Requirements

- Minecraft `26.1+`
- Java `25+`
- Bukkit, Spigot, Paper, Purpur, or Folia

This branch targets Minecraft `26.1+`. Older Minecraft `1.21.x` builds should be maintained separately.

## ✨ Features

- Sleep voting through beds or commands
- Night skip and storm clear voting
- Configurable vote percentage, minimum players, and maximum players
- Per-world enablement and per-world vote percentage
- BossBar, action bar, titles, sounds, and particle effects
- Optional dawn animation for night skips
- AFK players can be excluded from vote requirements
- PlaceholderAPI expansion
- SQLite statistics storage
- Auto-updating config and messages with backups only when needed
- Folia-aware scheduling for world, player, async, and region-specific work

## 🚀 Installation

1. Download the latest jar from [GitHub Releases](https://github.com/k1ritoz/Vote2Sleep/releases) or [Modrinth](https://modrinth.com/plugin/vote2sleep).
2. Place the jar in your server's `plugins` folder.
3. Restart the server.
4. Enable Vote2Sleep in each world with `/sleep enable`.
5. Review `plugins/Vote2Sleep/config.yml` and `plugins/Vote2Sleep/worlds.yml`.

## 📋 Commands

All aliases run the same main command: `/sleep`.

Aliases:

- `/sleep`
- `/v2s`
- `/vote2sleep`
- `/sleepvote`
- `/nightskip`

| Command | Permission | Description |
| --- | --- | --- |
| `/sleep` | `vote2sleep.use` | Vote to skip night or storm |
| `/sleep vote` | `vote2sleep.use` | Vote to skip night or storm |
| `/sleep status` | `vote2sleep.use` | Show the current vote status |
| `/sleep help` | `vote2sleep.use` | Show command help |
| `/sleep enable` | `vote2sleep.enable` | Enable Vote2Sleep in the current world |
| `/sleep disable` | `vote2sleep.disable` | Disable Vote2Sleep in the current world |
| `/sleep reload` | `vote2sleep.reload` | Reload configuration and messages |
| `/sleep stats` | `vote2sleep.stats` | Show plugin statistics |
| `/sleep cancel` | `vote2sleep.cancel` | Cancel current votes in the world |
| `/sleep force` | `vote2sleep.force` | Force skip night or storm |
| `/sleep config` | `vote2sleep.config` | View or modify supported settings in-game |
| `/sleep language` | `vote2sleep.admin` | Change the active messages language until restart/reload |

## 🛡️ Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `vote2sleep.*` | `op` | All Vote2Sleep permissions |
| `vote2sleep.use` | `true` | Basic command usage |
| `vote2sleep.admin` | `op` | Admin command group |
| `vote2sleep.reload` | `op` | Reload the plugin |
| `vote2sleep.enable` | `op` | Enable worlds |
| `vote2sleep.disable` | `op` | Disable worlds |
| `vote2sleep.force` | `op` | Force skip night or storm |
| `vote2sleep.cancel` | `op` | Cancel votes |
| `vote2sleep.stats` | `op` | View statistics |
| `vote2sleep.config` | `op` | Modify supported settings in-game |
| `vote2sleep.exempt` | `false` | Exclude a player from vote requirements |

## 🔧 Configuration

The main configuration is `config.yml`. World-specific settings are stored in `worlds.yml`.

### ⚙️ General settings

```yaml
settings:
  language: "en"
  vote-percentage: 0.5
  minimum-players: 1
  maximum-players: -1
  skip-delay-seconds: 3
  vote-timeout-seconds: 60
  allow-storm-skip: true
  allow-night-skip: true
  reset-statistics: true
  heal-players: false
  feed-players: false
  clear-weather: true
  bed-interaction: true
```

### 🖥️ Display settings

```yaml
display:
  boss-bar:
    enabled: true
    color: "BLUE"
    style: "SOLID"
  titles:
    enabled: true
  sounds:
    enabled: true
    vote-sound: "minecraft:block.note_block.chime"
    skip-sound: "minecraft:entity.experience_orb.pickup"
  actionbar:
    enabled: true
```

Sounds use modern Minecraft registry keys. Legacy enum-style names are normalized where possible.

### 💤 AFK detection

AFK players are ignored when calculating required votes. If `prevent-afk-voting` is enabled, AFK players also cannot cast/start a vote.

Vote2Sleep checks external hooks first. If no hook can determine the player's AFK state, the internal detector is used as a fallback.

```yaml
afk-detection:
  enabled: true
  prevent-afk-voting: true
  hooks:
    enabled: true
    essentials: true
    cmi: true
    placeholderapi:
      enabled: true
      placeholders:
        - "%essentials_afk%"
        - "%cmi_user_afk%"
      true-values: [ "true", "yes", "sim", "afk", "1", "on" ]
      false-values: [ "false", "no", "nao", "active", "not afk", "0", "off" ]
    generic-plugins:
      - "AFKPlus"
      - "AdvancedAFK"
      - "AntiAFKPlus"
    metadata: true
    sleeping-ignored: true
  internal:
    enabled: true
    timeout-seconds: 300
    track-look-movement: false
```

Supported AFK sources:

- EssentialsX
- CMI
- PlaceholderAPI placeholders
- AFKPlus, AdvancedAFK, AntiAFKPlus through generic reflection
- Player metadata flags
- Bukkit sleeping ignored flag
- Internal fallback timer

### 🌅 Dawn animation

```yaml
animation:
  dawn:
    enabled: false
    duration-seconds: 3
    animation-steps: 32
    title:
      enabled: true
    boss-bar:
      enabled: true
      color: "YELLOW"
      style: "SOLID"
    particles:
      enabled: true
      type: "ENCHANT"
    sounds:
      animation-sound: "minecraft:block.amethyst_block.chime"
      final-sound: "minecraft:entity.experience_orb.pickup"
```

When dawn animation is enabled, it handles the time progression and overrides the lightning-on-skip effect for night skips.

### 🌍 World settings

Vote2Sleep uses namespaced world keys on Minecraft `26.1+`, such as `minecraft:overworld`.

```yaml
worlds:
  "minecraft:overworld":
    enabled: true
    vote-percentage: 0.5
  "minecraft:the_nether":
    enabled: false
```

Use `/sleep enable` and `/sleep disable` to manage worlds without editing this file manually.

## 💬 Messages

Messages are stored in language-specific files:

- `messages_en.yml`
- `messages_pt_br.yml`

Messages support Minecraft color codes using `&` and custom placeholders such as `{current}`, `{required}`, `{remaining}`, `{world}`, and `{player}` where supported by the message.

## 📊 PlaceholderAPI

Vote2Sleep registers the `vote2sleep` PlaceholderAPI expansion when PlaceholderAPI is installed.

| Placeholder | Description |
| --- | --- |
| `%vote2sleep_current_votes%` | Current eligible votes in the player's world |
| `%vote2sleep_required_votes%` | Required votes in the player's world |
| `%vote2sleep_remaining_votes%` | Remaining votes required |
| `%vote2sleep_has_voted%` | Whether the player has voted |
| `%vote2sleep_has_voted_yes_no%` | Player vote status as Yes/No |
| `%vote2sleep_world_enabled%` | Whether Vote2Sleep is enabled in the world |
| `%vote2sleep_world_enabled_yes_no%` | World enabled status as Yes/No |
| `%vote2sleep_can_sleep%` | Whether night/storm skipping is currently possible |
| `%vote2sleep_can_sleep_yes_no%` | Sleep status as Yes/No |
| `%vote2sleep_is_night%` | Whether the world is currently night |
| `%vote2sleep_is_night_yes_no%` | Night status as Yes/No |
| `%vote2sleep_is_stormy%` | Whether the world is storming or thundering |
| `%vote2sleep_is_stormy_yes_no%` | Storm status as Yes/No |
| `%vote2sleep_progress_percentage%` | Vote progress as a percentage |
| `%vote2sleep_progress_decimal%` | Vote progress as a decimal |
| `%vote2sleep_world_name%` | Namespaced world key |
| `%vote2sleep_vote_percentage_setting%` | Configured vote percentage |
| `%vote2sleep_eligible_players%` | Eligible player count |
| `%vote2sleep_afk_players%` | AFK player count |
| `%vote2sleep_is_afk%` | Whether the player is AFK |
| `%vote2sleep_is_afk_yes_no%` | Player AFK status as Yes/No |
| `%vote2sleep_total_players%` | Total players in the world |
| `%vote2sleep_time_formatted%` | Formatted world time |
| `%vote2sleep_world_time%` | Raw world time |
| `%vote2sleep_world_time_12h%` | World time in 12h format |
| `%vote2sleep_world_time_24h%` | World time in 24h format |
| `%vote2sleep_plugin_version%` | Plugin version |
| `%vote2sleep_platform%` | Detected platform |
| `%vote2sleep_enabled_worlds_count%` | Number of enabled worlds |
| `%vote2sleep_database_enabled%` | Whether database storage is enabled |
| `%vote2sleep_database_enabled_yes_no%` | Database status as Yes/No |

## 🗄️ Database

Vote2Sleep currently supports SQLite only. When enabled, it stores skip events and vote details in `vote2sleep.db`.

```yaml
advanced:
  database:
    enabled: false
    type: "SQLITE"
```

MySQL is not included in the current implementation.

## 🔌 Developer API

```java
Vote2SleepAPI api = Vote2Sleep.getInstance().getApi();

boolean hasVoted = api.hasPlayerVoted(player);
int currentVotes = api.getCurrentVotes(world);
int requiredVotes = api.getRequiredVotes(world);
double progress = api.getVoteProgress(world);
boolean playerAfk = api.isPlayerAfk(player);

api.startSleepVote(player);
api.removeSleepVote(player);
api.clearVotes(world);
api.forceSkip(world, player);
```

Available event classes:

- `PreSleepVoteEvent`
- `SleepVoteRemovedEvent`
- `PreNightSkipEvent`
- `NightSkipEvent`

Example:

```java
@EventHandler
public void onPreSleepVote(PreSleepVoteEvent event) {
    if (event.getPlayer().hasPermission("example.block-sleep-vote")) {
        event.setCancelled(true);
    }
}

@EventHandler
public void onNightSkip(NightSkipEvent event) {
    getLogger().info("Night or storm skipped in " + event.getWorld().getKey().asString());
}
```

## 🏗️ Building from Source

### ✅ Prerequisites

- Java `25+`
- Gradle wrapper included in the repository

### 🔨 Build

```bash
git clone https://github.com/k1ritoz/Vote2Sleep.git
cd Vote2Sleep
./gradlew build
```

The compiled plugin jar is generated in `build/libs/`.

### 🧪 Test server

```bash
./gradlew runServer
```

The development server uses Minecraft `26.1.2`.

## 🤝 Contributing

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Push the branch.
5. Open a pull request.

Please update documentation when changing user-facing behavior, configuration, commands, permissions, placeholders, or API behavior.

## 🐛 Bug Reports and Feature Requests

Use [GitHub Issues](https://github.com/k1ritoz/Vote2Sleep/issues) for bugs, suggestions, and questions.

When reporting bugs, include:

- Server platform and version
- Java version
- Vote2Sleep version
- Relevant configuration
- Console errors or logs
- Steps to reproduce

## 📄 License

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.
