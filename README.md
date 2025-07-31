# Vote2Sleep

A modern, feature-rich sleep voting plugin for Minecraft servers that allows players to collectively vote to skip night and storms.

<details>
<summary>📚 Table of Contents</summary>

- [Features](#-features)
- [Installation](#-installation)
- [Commands](#-commands)
- [Configuration](#-configuration)
- [API Usage](#-api-usage)
- [PlaceholderAPI Support](#-placeholderapi-placeholders)
- [Permissions](#-permissions)
- [Multi-Platform Support](#-multi-platform-support)
- [Statistics & Database](#-statistics--database)
- [Events API](#-events-api)
- [Building from Source](#-building-from-source)
- [Contributing](#-contributing)
- [Bug Reports & Feature Requests](#-bug-reports--feature-requests)
- [Changelog](#-changelog)
- [License](#-license)
- [Support](#-support)

</details>

## ✨ Features

### Core Functionality
- **Sleep Voting System**: Players can vote by entering beds or using commands
- **Storm Skipping**: Vote to clear storms during the day
- **Night Skipping**: Vote to skip night time
- **Configurable Requirements**: Set custom vote percentages and player limits
- **Real-time Updates**: Boss bars, action bars, and titles show voting progress

### Platform Support
- **Multi-Platform**: Supports Bukkit, Spigot, Paper, and Folia
- **Version Compatibility**: Minecraft 1.21.4+
- **Cross-Platform Scheduling**: Optimized for both traditional and regionized servers

### Visual Features
- **Boss Bar Progress**: Shows voting progress with customizable colors and styles
- **Title Messages**: Beautiful title animations when night is skipped
- **Sound Effects**: Configurable sound effects for votes and skips
- **Particle Effects**: Eye-catching particle effects at voting locations
- **Action Bar Updates**: Real-time voting status in action bar

### Advanced Features
- **Database Integration**: SQLite and MySQL support for statistics
- **PlaceholderAPI Support**: Rich placeholders for other plugins
- **WorldGuard Integration**: Region-based permissions (planned)
- **Essentials Integration**: AFK player detection (planned)
- **Developer API**: Comprehensive API for other plugins

## 🚀 Installation

1. Download the latest release from [GitHub Releases](https://github.com/k1ritoz/Vote2Sleep/releases)
2. Place the jar file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin using `/sleep enable` in desired worlds

## 📋 Commands

| Command | Permission          | Description                     |
|---------|---------------------|---------------------------------|
| `/sleep` | `vote2sleep.use`    | Vote to skip night/storm        |
| `/sleep vote` | `vote2sleep.use`    | Vote to skip night/storm        |
| `/sleep status` | `vote2sleep.use`    | Check current vote status       |
| `/sleep help` | `vote2sleep.use`    | Show help message               |
| `/sleep enable` | `vote2sleep.admin`  | Enable plugin in current world  |
| `/sleep disable` | `vote2sleep.admin`  | Disable plugin in current world |
| `/sleep reload` | `vote2sleep.admin`  | Reload configuration            |
| `/sleep stats` | `vote2sleep.stats`  | View plugin statistics          |
| `/sleep cancel` | `vote2sleep.cancel` | Cancel current votes            |
| `/sleep force` | `vote2sleep.force`  | Force skip night/storm          |
| `/sleep config` | `vote2sleep.config` | Modify settings in-game         |
| `/sleep language` | `vote2sleep.admin`  | Change Language for messages    |

### Command Aliases
- `/v2s` - Alternative command name
- `/vote2sleep` - Full plugin name command
- `/sleepvote` - Alternative for `/sleep`

## 🔧 Configuration

### Main Configuration (config.yml)

```yaml
settings:

  language: "en"                # Language for messages (en/pt_br)
  vote-percentage: 0.5          # 50% of players required
  minimum-players: 1            # At least 1 player required
  maximum-players: -1           # No maximum limit
  skip-delay-seconds: 3         # 3 second countdown
  vote-timeout-seconds: 60      # Votes expire after 60 seconds
  allow-storm-skip: true        # Allow storm skipping
  allow-night-skip: true        # Allow night skipping
  reset-statistics: true        # Reset phantom statistics
  heal-players: false           # Heal players on skip
  feed-players: false           # Feed players on skip
  clear-weather: true           # Clear weather on skip

display:
  boss-bar:
    enabled: true
    color: "BLUE"
    style: "SOLID"
  titles:
    enabled: true
  sounds:
    enabled: true
  actionbar:
    enabled: true

advanced:
  database:
    enabled: false              # Enable for statistics tracking
  update-checker: true          # Check for updates
  debug-mode: false             # Debug logging
```

### World Configuration
Worlds are managed through commands or the `worlds.yml` file:
```yaml
worlds:
  world:
    enabled: true
    vote-percentage: 0.5        # World-specific percentage
  world_nether:
    enabled: false
```

## 🎨 Customization

### Messages
All messages are customizable in `messages.yml` with full color code support:
```yaml
vote-cast: "&a{current}&7/&a{required} &7players voted for sleep!"
night-skipped: "&aThe night has been skipped! Sweet dreams! &f✨"
boss-bar-title: "Sleep Vote: {current}/{required}"
```

### Sounds and Effects
- Customizable sound effects for votes and skips
- Particle effects at bed locations
- Configurable boss bar colors and styles
- Title animation timing

## 🔌 API Usage

Vote2Sleep provides a comprehensive API for developers:

```java
// Get the API instance
Vote2SleepAPI api = Vote2Sleep.getInstance().getApi();

// Check if a player has voted
boolean hasVoted = api.hasPlayerVoted(player);

// Get current vote status
int currentVotes = api.getCurrentVotes(world);
int requiredVotes = api.getRequiredVotes(world);

// Force start a vote
api.startSleepVote(player);

// Listen to events
@EventHandler
public void onSleepVote(SleepVoteEvent event) {
    Player player = event.getPlayer();
    World world = event.getWorld();
    // Handle the vote
}
```

## 📊 PlaceholderAPI Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%vote2sleep_current_votes%` | Current number of votes |
| `%vote2sleep_required_votes%` | Required number of votes |
| `%vote2sleep_has_voted%` | Whether the player has voted |
| `%vote2sleep_world_enabled%` | Whether plugin is enabled in world |
| `%vote2sleep_can_sleep%` | Whether sleeping is currently possible |
| `%vote2sleep_progress_percentage%` | Vote progress as percentage |

## 🛡️ Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `vote2sleep.*` | op | All permissions |
| `vote2sleep.use` | true | Basic usage |
| `vote2sleep.admin` | op | Admin commands |
| `vote2sleep.force` | op | Force skip night/storm |
| `vote2sleep.cancel` | op | Cancel votes |
| `vote2sleep.stats` | op | View statistics |
| `vote2sleep.config` | op | Modify config in-game |
| `vote2sleep.exempt` | false | Exempt from voting requirements |

## 🌍 Multi-Platform Support

Vote2Sleep is designed to work across different server platforms:

### Bukkit/Spigot
- Full compatibility with standard Bukkit API
- Traditional scheduling system
- Compatible with most Bukkit plugins

### Paper
- Enhanced performance optimizations
- Access to Paper-specific APIs
- Improved TPS reporting

### Folia
- Region-based scheduling for optimal performance
- Thread-safe operations
- Designed for high-performance regionized servers

## 📈 Statistics & Database

When database integration is enabled, Vote2Sleep tracks:
- Total night skips per world
- Individual player voting statistics
- Vote timing and patterns
- Most active voting times

Database support:
- **SQLite**: Local file-based storage (default)
- **MySQL**: Remote database support for networks

## 🔄 Events API

Vote2Sleep fires several custom events that other plugins can listen to:

```java
// Before a player votes (cancellable)
@EventHandler
public void onPreSleepVote(PreSleepVoteEvent event) {
    if (someCondition) {
        event.setCancelled(true);
    }
}

// After a player votes
@EventHandler
public void onSleepVote(SleepVoteEvent event) {
    Player player = event.getPlayer();
    World world = event.getWorld();
    int totalVotes = event.getTotalVotes();
}

// When a vote is removed
@EventHandler
public void onVoteRemoved(SleepVoteRemovedEvent event) {
    // Handle vote removal
}

// Before night is skipped (cancellable)
@EventHandler
public void onPreNightSkip(PreNightSkipEvent event) {
    List<SleepVote> votes = event.getVotes();
    // Can cancel the skip
}

// After night is skipped
@EventHandler
public void onNightSkip(NightSkipEvent event) {
    World world = event.getWorld();
    List<SleepVote> votes = event.getVotes();
}
```

## 🏗️ Building from Source

### Prerequisites
- Java 21 or higher
- Gradle 8.9 or higher

### Build Steps
```bash
# Clone the repository
git clone https://github.com/k1ritoz/Vote2Sleep.git
cd Vote2Sleep

# Build the plugin
./gradlew build

# The compiled jar will be in build/libs/
```

### Development Setup
```bash
# Run a test server with the plugin
./gradlew runServer

# Build with debug information
./gradlew build -Pdebug=true

# Run tests
./gradlew test
```

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Development Guidelines
- Follow Java conventions and code style
- Add JavaDoc comments for public methods
- Include unit tests for new features
- Update documentation for user-facing changes

## 🐛 Bug Reports & Feature Requests

Please use the [GitHub Issues](https://github.com/k1ritoz/Vote2Sleep/issues) page to:
- Report bugs with detailed reproduction steps
- Request new features with use cases
- Ask questions about usage or configuration

When reporting bugs, please include:
- Server version and platform (Paper, Spigot, etc.)
- Plugin version
- Relevant configuration files
- Console errors or logs
- Steps to reproduce the issue

## 📝 Changelog

### Version 1.0.0
- Initial release
- Multi-platform support (Bukkit, Spigot, Paper, Folia)
- Configurable sleep voting system
- Storm and night skipping
- Boss bar and visual effects
- Database integration
- PlaceholderAPI support
- Comprehensive API for developers
- Update checker with GitHub releases

## 📄 License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.

## 📞 Support

- **Documentation**: [GitHub Wiki](https://github.com/k1ritoz/Vote2Sleep/wiki)
- **Issues**: [GitHub Issues](https://github.com/k1ritoz/Vote2Sleep/issues)

---

⭐ If you find Vote2Sleep useful, please consider giving it a star on GitHub!