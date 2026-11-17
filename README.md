# ForceItemBattle

This project brings the "ForceItemBattle" challenge, inspired by the popular YouTube videos from German content creator **BastiGHG**, to your Minecraft server.

The game principle is simple: Players are divided into teams and are tasked with finding a sequence of randomly assigned items. When an item is found, crafted, or picked up, the team is given a new item to find. The team that has found the most items when the timer runs out wins\!

### Features

* **Dynamic Team System:** Players can join teams via a GUI. Team colors and prefixes are shown in the tab list.
* **Live Scoreboard & Suffixes:** The tab list shows each team's currently targeted item as a suffix. The sidebar shows the lobby status and - if enabled in the config - the live team ranking during the game.
* **Joker Item:** Players can use a "Joker" item to skip a difficult item.
* **Rejoin:** Players who lose their connection during a running game can rejoin and continue with their team.
* **End-Game Showcase:** An animated title sequence at the end of the game reveals all teams and their found items, from last place up to the winner.
* **Custom Resource Pack:** Item icons in the boss bar and the scoreboard title are provided by a resource pack that is sent to players on join.

### Requirements

* [Paper](https://papermc.io/) **26.3**
* Java **25**

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

4.  The plugin jar (including its relocated dependencies) is located at `build/libs/forceitem-battle-<version>.jar`.

-----

## Configuration

On first start the plugin creates `plugins/forceitem-battle/config.yml`:

| Option                               | Default | Description                                                                                         |
|:-------------------------------------|:--------|:----------------------------------------------------------------------------------------------------|
| `resource-pack.enabled`              | `true`  | Send the resource pack on join                                                                      |
| `resource-pack.url`                  |         | Direct download link of the pack                                                                    |
| `resource-pack.sha1`                 | `""`    | SHA-1 of the pack zip. Strongly recommended, otherwise clients re-download the pack on every join   |
| `resource-pack.required`             | `true`  | Kick players that decline the pack                                                                  |
| `teams.amount`                       | `14`    | Number of teams (max. 18)                                                                           |
| `teams.size`                         | `2`     | Players per team                                                                                    |
| `jokers.per-team`                    | `true`  | `true`: joker amount is given once per team, `false`: every team member gets the full amount        |
| `scoreboard.show-ranking-ingame`     | `false` | Show the live ranking in the sidebar during the game                                                |
| `results.shutdown-server`            | `true`  | Stop the server after the results                                                                   |
| `results.shutdown-delay-seconds`     | `20`    | Countdown before the server stops                                                                   |
| `items.additional-banned`            | `[]`    | Additional material names that are never chosen                                                     |

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

-----

## Releasing

Every pull request and every commit on `master` is built by GitHub Actions, nothing is published automatically.
To publish a release to GitHub and Modrinth:

1.  Set the new version in `gradle.properties` (e.g. `version=1.1.0`) and merge it to `master`.
2.  Push a matching tag:

    ```sh
    git tag v1.1.0
    git push origin v1.1.0
    ```

Tags with a suffix (e.g. `v1.2.0-beta.1`) are published as pre-release.

-----

## License

Distributed under CC BY-NC-SA 4.0. Click [here](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.en) for more information.