# ForceItemBattle

[](https://opensource.org/licenses/MIT)

This project brings the "ForceItemBattle" challenge, inspired by the popular YouTube videos from German content creator **BastiGHG**, to your Minecraft server.

The game principle is simple: Players are divided into teams and are tasked with finding a sequence of randomly assigned items. When an item is found, crafted, or picked up, the team is given a new item to find. The team that has found the most items when the timer runs out wins\!

### Features

* **Dynamic Team System:** Players can join teams via a GUI. Team colors and prefixes are shown in the tab list.
* **Live Scoreboard & Suffixes:** A dynamic sidebar scoreboard shows game status and team rankings. The tab list is updated to show each team's currently targeted item as a suffix.
* **Joker Item:** Players can use a configurable "Joker" item to skip a difficult item.
* **End-Game Showcase:** An animated title sequence at the end of the game showcases the top 3 teams and their achievements.

-----

## Getting Started

### Building from Source

If you want to compile the project yourself, follow these steps.

**Prerequisites:**

* Git
* Java Development Kit (JDK) 21 or newer
* Gradle

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

    *(On Windows, you might need to use `gradlew build` instead of `./gradlew build`)*

4.  The compiled `.jar` file will be located in the `build/libs/` directory.

-----

## Commands & Permissions

| Command                                       | Description                                                   | Permission              |
|:----------------------------------------------|:--------------------------------------------------------------|:------------------------|
| `/start <duration_in_min> <amount_of_jokers>` | Starts the game with a set duration and the amount of jokers. | `forceitembattle.start` |
| `/itemrecipe`                                 | Displays the crafting recipe for the current item.            | (None)                  |
| `/backpack` or `/bp`                          | Opens the team backpack                                       | (None)                  |

-----

## License

Distributed under CC BY-NC-SA 4.0. Click [here](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.en) for more information.