# FastBoard

[![Java CI](https://github.com/MrMicky-FR/FastBoard/actions/workflows/build.yml/badge.svg)](https://github.com/MrMicky-FR/FastBoard/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/fr.mrmicky/fastboard.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/fr.mrmicky/fastboard)
[![Discord](https://img.shields.io/discord/390919659874156560.svg?colorB=5865f2&label=Discord&logo=discord&logoColor=white)](https://discord.gg/q9UwaBT)

Lightweight packet-based scoreboard API for Bukkit plugins, with 1.7.10 to 1.20.x support.

> [!IMPORTANT]
> To use FastBoard on a 1.8 server, the server must be on 1.8.8.

## Features

* No flickering (without using a buffer)
* Works with all versions from 1.7.10 to 1.20
* Small (around 750 lines of code with the JavaDoc) and no dependencies
* Easy to use
* Dynamic scoreboard size: you don't need to add/remove lines, you can directly give a string list (or array) to change all the lines
* Everything is at the packet level, so it works with other plugins using scoreboard and/or teams
* Can be used asynchronously
* Supports up to 30 characters per line on 1.12.2 and below
* No character limit on 1.13 and higher
* Supports hex colors on 1.16 and higher
* Custom scores (including blank) on 1.20.3 and higher
* [Adventure](https://github.com/KyoriPowered/adventure) components support

## Installation

### Maven
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.3.0</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>fr.mrmicky.fastboard</pattern>
                        <!-- Replace 'com.yourpackage' with the package of your plugin ! -->
                        <shadedPattern>com.yourpackage.fastboard</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </plugin>
    </plugins>
</build>

<dependencies>
    <dependency>
        <groupId>fr.mrmicky</groupId>
        <artifactId>fastboard</artifactId>
        <version>2.0.2</version>
    </dependency>
</dependencies>
```

> [!NOTE]
> When using Maven, make sure to build directly with Maven and not with your IDE configuration (on IntelliJ IDEA: in the `Maven` tab on the right, in `Lifecycle`, use `package`).

### Gradle

```groovy
plugins {
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'fr.mrmicky:fastboard:2.0.2'
}

shadowJar {
    // Replace 'com.yourpackage' with the package of your plugin 
    relocate 'fr.mrmicky.fastboard', 'com.yourpackage.fastboard'
}
```

### Manual

Copy `FastBoardBase.java`, `FastBoard.java` and `FastReflection.java` in your plugin.

## Usage

### Creating a scoreboard

Simply create a new `FastBoard` and update the title and the lines:

```java
FastBoard board = new FastBoard(player);

// Set the title
board.updateTitle(ChatColor.GOLD + "FastBoard");

// Change the lines
board.updateLines(
        "", // Empty line
        "One line",
        "",
        "Second line"
);
```

### Example

Small example plugin with a scoreboard that refreshes every second:

```java
package fr.mrmicky.fastboard.example;

import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ExamplePlugin extends JavaPlugin implements Listener {

    private final Map<UUID, FastBoard> boards = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().runTaskTimer(this, () -> {
            for (FastBoard board : this.boards.values()) {
                updateBoard(board);
            }
        }, 0, 20);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        FastBoard board = new FastBoard(player);

        board.updateTitle(ChatColor.RED + "FastBoard");

        this.boards.put(player.getUniqueId(), board);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        FastBoard board = this.boards.remove(player.getUniqueId());

        if (board != null) {
            board.delete();
        }
    }

    private void updateBoard(FastBoard board) {
        board.updateLines(
                "",
                "Players: " + getServer().getOnlinePlayers().size(),
                "",
                "Kills: " + board.getPlayer().getStatistic(Statistic.PLAYER_KILLS),
                ""
        );
    }
}
```

## Adventure support

For servers on modern [PaperMC](https://papermc.io) versions, FastBoard supports
using [Adventure](https://github.com/KyoriPowered/adventure) components instead of strings,
by using the class `fr.mrmicky.fastboard.adventure.FastBoard`.

## RGB colors

When using the non-Adventure version of FastBoard, RGB colors can be added on 1.16 and higher with `ChatColor.of("#RRGGBB")` (`net.md_5.bungee.api.ChatColor` import).

## ViaBackwards compatibility

When using ViaBackwards on a post-1.13 server with pre-1.13 clients, older clients
might get incomplete lines. To solve this issue, you can override the method `hasLinesMaxLength()` and return `true` for older clients.
For example using the ViaVersion API:
```java
FastBoard board = new FastBoard(player) {
    @Override
    public boolean hasLinesMaxLength() {
        return Via.getAPI().getPlayerVersion(getPlayer()) < ProtocolVersion.v1_13.getVersion(); // or just 'return true;'
    }
});
```
