<p align="center"><a href="https://kitsunetechnologies.org/work"><img src="https://raw.githubusercontent.com/KitsuneTech1/.github/main/assets/kitsune-banner.svg" alt="Built by Kitsune Technologies" width="760"></a></p>

# UpgradableSpawners

Right-click a spawner to upgrade its radius, speed, and mob quantity with vanilla items.

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21-brightgreen.svg)
![Spigot](https://img.shields.io/badge/Spigot%2FPaper-API-orange.svg)
![Java](https://img.shields.io/badge/Java-21-blue.svg)

## What it does

UpgradableSpawners lets players right-click any mob spawner (with an empty hand) to open an upgrade GUI. Three independent upgrade tracks (radius, speed, quantity) each go up to level 5, paid for with iron blocks, emerald blocks, and diamonds. Upgrades are saved per-spawner and survive breaking and re-placing, since a broken upgraded spawner drops a custom item that restores its levels when placed again.

## Features

- Right-click GUI to view and purchase spawner upgrades
- **Radius** - increases spawn activation range, cost paid in iron blocks
- **Speed** - reduces spawn delay by a percentage, cost paid in emerald blocks
- **Quantity** - adds extra mobs per spawn cycle, cost paid in diamonds
- Upgrade data is tied to the spawner and persists through break/place via a custom drop item
- `/uspawner` diagnostic command shows a spawner's current levels and live spawn settings
- A per-spawner cap on simultaneously alive mobs, to prevent lag

## Commands

| Command | Description | Permission |
|---|---|---|
| `/uspawner` | Look at a spawner to view its upgrade levels and spawn settings | `upgradablespawners.info` |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `upgradablespawners.upgrade` | true | Upgrade spawners via the right-click GUI |
| `upgradablespawners.info` | true | View spawner info |
| `upgradablespawners.admin` | op | Reload the plugin configuration and use admin functions |

## Config

Key options in `config.yml`:

- `max-levels.radius` / `max-levels.speed` / `max-levels.quantity` - level cap per upgrade track (default 5 each)
- `radius-costs` / `speed-costs` / `quantity-costs` - iron blocks, emerald blocks, and diamonds required per level
- `radius-effects` - spawn activation radius in chunks per level
- `speed-effects` - percentage speed increase per level
- `quantity-effects` - extra mobs spawned per cycle per level
- `max-mobs-per-spawner` - hard cap on mobs alive at once from a single spawner
- `messages.*` - player-facing message strings, with color codes

## Install

### What you need

- A Spigot or Paper server on Minecraft 1.21. Get Paper at https://papermc.io/downloads . This plugin will not work on vanilla Minecraft or on Realms.
- Java 21. Get it at https://adoptium.net . Check what you have installed by running `java -version` in a terminal.

### Step by step

**Option A: download the jar**

1. Go to the [v1.1.0 release page](https://github.com/KitsuneTech1/UpgradableSpawners/releases/tag/v1.1.0) and download `UpgradableSpawners-1.1.0.jar`, or grab it directly from [this link](https://github.com/KitsuneTech1/UpgradableSpawners/releases/download/v1.1.0/UpgradableSpawners-1.1.0.jar).

**Option B: build from source**

1. Install Java 21 (the JDK, not just the JRE) from https://adoptium.net .
2. Clone the repo: `git clone https://github.com/KitsuneTech1/UpgradableSpawners.git`. If you don't use git, click the green "Code" button on GitHub and choose "Download ZIP" instead, then unzip it.
3. Open a terminal in the project folder: `cd UpgradableSpawners`.
4. Run `mvn clean package`.
5. The built jar lands at `target/UpgradableSpawners-1.1.0.jar`.

**Then, for either option:**

1. Drop the jar into your server's `plugins/` folder.
2. Restart the server (a full restart, not `/reload`). `config.yml` is generated on first run.
3. Verify it worked: in-game or in console, run `/uspawner`. If the plugin loaded, you'll get a response back (either spawner info or a message telling you to look at a spawner), not "Unknown command." Check the console log too, there should be no errors mentioning UpgradableSpawners on startup.

It worked if `/uspawner` responds and the console shows the plugin enabled with no errors.

**Troubleshooting**

- `UnsupportedClassVersionError` in the console: your server is running an older Java than 21. Install Java 21 from https://adoptium.net and make sure your server startup script points to it.
- Console warning about an unsupported or unknown API version, or the plugin fails to load: your server isn't on Minecraft 1.21. Update to a 1.21 Paper or Spigot build.
- `/uspawner` says "Unknown command": the jar isn't in `plugins/`, or the server wasn't fully restarted after adding it. Confirm the file is there and restart again.

## License

MIT. See [LICENSE](LICENSE).
