# ForceItemBattle

This project brings the "ForceItemBattle" challenge, inspired by the popular YouTube videos from German content creator **BastiGHG**, to your Minecraft server.

The game principle is simple: Players are divided into teams and are tasked with finding a sequence of randomly assigned items. When an item is found, crafted, or picked up, the team is given a new item to find. The team that has found the most items when the timer runs out wins\!

### Features

* **Dynamic Team System:** Players can join teams via a GUI. Team colors and prefixes are shown in the tab list.
* **Live Scoreboard & Suffixes:** The tab list shows each team's currently targeted item as a suffix. The sidebar shows the lobby status and - if enabled in the config - the live team ranking during the game.
* **Joker Item:** Players can use a "Joker" item to skip a difficult item.
* **Rejoin:** Players who lose their connection during a running game can rejoin and continue with their team.
* **End-Game Showcase:** An animated title sequence at the end of the game reveals all teams and their found items, from last place up to the winner.
* **Custom Resource Pack:** Item icons in the boss bar and the scoreboard title are provided by a resource pack that is sent to players on join. The pack is built together with the plugin and attached to every release.
* **Smaller Biomes:** Optionally generates the world with smaller biomes, so biome specific items are not hundreds of blocks apart (see [Biome Size](#biome-size)).
* **Game Settings:** Backpack, hunger, damage and starter tools are switched in the lobby with the settings item.
* **Multiple Languages:** Every player gets the texts in its client language. English and German are included, more languages are just another file (see [Languages](#languages)).

-----

## Commands & Permissions

| Command                                       | Description                                                   | Permission              |
|:----------------------------------------------|:--------------------------------------------------------------|:------------------------|
| `/start <duration_in_min> <amount_of_jokers>` | Starts the game (also from the console). Jokers: 0 - 64.      | `forceitembattle.start` |
| `/itemrecipe`                                 | Displays the crafting recipe for the current item.            | (None)                  |
| `/backpack` or `/bp`                          | Opens the team backpack                                       | (None)                  |
| `/spawn`                                      | Teleports player to world spawn location                      | (None)                  |
| `/top`                                        | Teleports player to the highest block at the current location | (None)                  |

The lobby settings item is only available with the permission `forceitembattle.settings`. Both permissions default to operators.

### Game Settings

Switched in the lobby with the settings item, they apply to the next game:

| Setting       | Default | Description                                                                                 |
|:--------------|:--------|:--------------------------------------------------------------------------------------------|
| Backpack      | on      | Every team has a shared backpack (`/backpack`)                                              |
| No hunger     | on      | The food bar stays full the whole game                                                      |
| No damage     | off     | Players lose no hearts during the game (the void and `/kill` still kill)                    |
| Starter tools | off     | Every player gets an unbreakable diamond pickaxe and axe with Efficiency I when the game starts |

Developers: every setting is a constant of `SettingType` (`settings/SettingType.java`) - no string keys.

### Requirements

* [Paper](https://papermc.io/) **1.21.11, 26.1.1, 26.1.2, 26.2 or 26.3** - one jar for all of them
* Java **21** (1.21.11) or **25** (26.x) - whatever your Paper version needs

-----

## Getting Started

### Building from Source

If you want to compile the project yourself, follow these steps.

**Prerequisites:**

* Git
* Java Development Kit (JDK) 25 (downloaded automatically by Gradle if it is missing)

**Cloning and Building:**

1.  Clone the repository to your local machine:

    ```sh
    git clone https://github.com/thisisfel1x/forceitem-battle.git
    ```

2.  Navigate into the project directory:

    ```sh
    cd forceitem-battle
    ```

3.  Compile the project using the Gradle Wrapper:

    ```sh
    ./gradlew build
    ```

    *(On Windows use `gradlew build`)*

4.  The plugin jar (including its relocated dependencies) is located at `build/libs/forceitem-battle-<version>.jar`,
    the resource pack at `build/libs/forceitem-battle-pack-<version>.zip`.

Useful while developing:

| Command                                                  | Description                                               |
|:---------------------------------------------------------|:----------------------------------------------------------|
| `./gradlew runServer -PtestVersion=26.1.2`               | Local test server of any supported version (default: newest) |
| `./gradlew compileJava -PcompileVersion=26.2`            | Compile against the API of another supported version      |
| `./gradlew test`                                         | Unit tests (language files, biome size data pack)         |
| `python3 tools/smoke-test/smoke_test.py --minecraft 1.21.11 --plugin build/libs/forceitem-battle-1.2.0.jar` | Start a Paper server with the jar and check the log |
| `python3 tools/smoke-test/e2e_test.py --plugin build/libs/forceitem-battle-1.2.0.jar --pack build/libs/forceitem-battle-pack-1.2.0.zip` | Server + two real game clients play a game, checks the boss bar icon (opens two game windows) |

The scripts accept the [Minecraft EULA](https://aka.ms/MinecraftEULA) for their test servers.

-----

## Supported Minecraft Versions

All versions are listed in `gradle.properties`:

```properties
supportedVersions=1.21.11,26.1.1,26.1.2,26.2,26.3
resourcePackMinFormat=75
resourcePackMaxFormat=97
dataPackMinFormat=94
dataPackMaxFormat=121
```

* The jar is compiled against the **oldest** version (its API, `api-version` and Java bytecode level) - Paper loads
  plugins with an older `api-version`, so the same jar runs on every newer version.
* The item pool is read from the running server, every version gets exactly its items.
* One resource pack serves all versions (format range in `pack.mcmeta`). Items are never removed from the game, so the
  icons of the newest version cover the older ones.
* CI compiles against the API of every version and starts a real Paper server of every version with the jar
  (smoke test). Two real clients play a game on the newest version (end-to-end test).

### New Minecraft versions

The workflow **New Minecraft version** checks daily for a new Minecraft version with a stable Paper build. If there is
one, it adds the version to `supportedVersions`, raises `resourcePackMaxFormat` and `dataPackMaxFormat`, bumps the patch version, renders the
icons with the new client (as soon as Fabric supports it) and opens a pull request `Support Minecraft <version>`.
The same run verifies the branch (compile, smoke test, end-to-end test). **Merging the pull request publishes the
release to GitHub and Modrinth.** It can also be started manually with explicit versions.

Dropping an old version: remove it from `supportedVersions` and set `resourcePackMinFormat` / `dataPackMinFormat` to
the formats of the new oldest version (`pack_version.resource_major` / `data_major` in the `version.json` of its
client jar).

-----

## Configuration

On first start the plugin creates `plugins/forceitem-battle/config.yml`:

| Option                               | Default | Description                                                                                         |
|:-------------------------------------|:--------|:----------------------------------------------------------------------------------------------------|
| `language.default`                   | `en`    | Language of the console and of players whose client language has no file                           |
| `language.per-player`                | `true`  | Every player gets its client language. `false`: everybody gets `language.default`                   |
| `world.biome-size`                   | `SMALL` | `NORMAL`, `SMALL` (half as wide) or `TINY` (a quarter as wide), see [Biome Size](#biome-size)       |
| `resource-pack.enabled`              | `true`  | Send the resource pack on join                                                                      |
| `resource-pack.url`                  | `""`    | Empty: pack of the GitHub release of this plugin version. Otherwise a direct download link          |
| `resource-pack.sha1`                 | `""`    | SHA-1 of the pack zip, only needed with an own `url` (empty url: the built-in hash is used)         |
| `resource-pack.required`             | `true`  | Kick players that decline the pack                                                                  |
| `teams.amount`                       | `14`    | Number of teams (max. 18)                                                                           |
| `teams.size`                         | `2`     | Players per team                                                                                    |
| `jokers.per-team`                    | `true`  | `true`: joker amount is given once per team, `false`: every team member gets the full amount        |
| `scoreboard.show-ranking-ingame`     | `false` | Show the live ranking in the sidebar during the game                                                |
| `results.shutdown-server`            | `true`  | Stop the server after the results                                                                   |
| `results.shutdown-delay-seconds`     | `20`    | Countdown before the server stops                                                                   |
| `items.additional-banned`            | `[]`    | Additional material names that are never chosen                                                     |

### Languages

The texts are in `plugins/forceitem-battle/lang/<language>.yml` - `en.yml` and `de.yml` are created on first start
and can be edited. Formatting uses [MiniMessage](https://docs.advntr.dev/minimessage/format.html)
(colors, gradients, ...), the placeholders are noted in the files.

* **New language:** copy `en.yml` to e.g. `fr.yml` or `pt_br.yml` (the [Minecraft language code](https://minecraft.wiki/w/Language))
  and translate it. It is detected on the next start and used for every player with that client language.
* **Missing messages** fall back to the bundled version of the language, then to `language.default`, then to English -
  a file only needs the messages it changes, and new messages of plugin updates show up automatically.
* Item names are translated by the game client itself.

### Biome Size

In a vanilla world a single biome is often several hundred blocks wide - looking for cocoa beans, a cactus or
terracotta can take a long walk. `world.biome-size` makes biomes smaller:

| Value    | Biome width                    |
|:---------|:-------------------------------|
| `NORMAL` | vanilla                        |
| `SMALL`  | half (a quarter of the area)   |
| `TINY`   | a quarter (1/16 of the area)   |

Three noises of the overworld get a higher frequency: temperature and humidity, which decide nothing but the biome,
and continentalness, which decides where ocean and land are. So oceans and continents shrink along with the biomes and
oceans do not take over the map. Mountains and rivers are generated like in vanilla.
Technically the plugin registers a data pack before the world is loaded (Paper plugin bootstrap).

* The option is read on server start and applies to **newly generated chunks** only. After changing it use a new world
  (delete the world folders) - otherwise old and new chunks meet at hard biome edges.
* A config of an older plugin version (without the option) keeps `NORMAL`, so existing worlds are not affected.
* `level-type=large_biomes` uses its own noise and is not affected.

-----

## Resource Pack & Item Icons

The item icons are shown with sprite object components (`fib:icon/<item id>` in the `gui` atlas) - no unicode mapping
to maintain, the item id is the sprite name.

```
resourcepack/
├── static/       hand made assets (scoreboard title glyphs, win model, ...)
├── icons/        <item id>.png, 64x64 - generated, not in the repository
└── icons.json    manifest of the export (Minecraft version, pack format) - generated, not in the repository
```

The icons are renders of Mojang's textures, so they are not committed: CI renders them before every build and they
only end up in the resource pack of the build (and of the release).

`./gradlew build` downscales the icons (`resourcePackIconSize` in `gradle.properties`, default 32 px), zips the pack
reproducibly and bakes its SHA-1 and the release download URL into the plugin.

### Generating icons

The icons are rendered by the real game client, so 3D items (beds, chests, stairs, banners, ...) look exactly like in
the inventory. `tools/icon-exporter` is a small Fabric client gametest; Gradle downloads Minecraft itself, no
installation or account is needed. Exports are deterministic (animated textures are frozen on their first frame).

The build workflow renders them headless before every build, the artifact `icon-preview` contains boss bar screenshots
of the result. The Minecraft version of the client is set in `tools/icon-exporter/gradle.properties`
(`minecraft_version`, `loader_version`, `fabric_api_version` - current values: <https://fabricmc.net/develop>); the
**New Minecraft version** workflow updates it automatically.

Locally (opens a game window for about a minute, needed for a local build with icons):
`./gradlew -p tools/icon-exporter runClientGameTest`

## License

Distributed under CC BY-NC-SA 4.0. Click [here](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.en) for more information.