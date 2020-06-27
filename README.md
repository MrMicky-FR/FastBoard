# FastBoard
[![JitPack](https://jitpack.io/v/fr.mrmicky/FastBoard.svg)](https://jitpack.io/#fr.mrmicky/FastBoard)
[![Discord](https://img.shields.io/discord/390919659874156560.svg?colorB=7289da&label=discord&logo=discord&logoColor=white)](https://discord.gg/q9UwaBT)

A Scoreboard API for Bukkit with 1.7-1.16 support.

## Features

* No flickering (without using a buffer)
* Works with all version from 1.7.10 to 1.16.
* Really small (around 500 lines with everything) and don't use any dependency
* Easy to use
* Dynamic scoreboard size: you don't need to add/remove lines, you can just give String list (or array) to change all the lines
* Everything is at packet level, so it works with other plugins using scoreboard and/or teams
* Can be use in an async thread (but is not thread safe yet)
* Support up to 30 characters per line on 1.7-1.12
* No characters limit on 1.13+
* Support 1.16 custom hex colors

## How to use

### Add FastBoard in your plugin

#### Maven
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.2.1</version>
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
                        <!-- Replace with the package of your plugin ! -->
                        <shadedPattern>com.yourpackage.fastboard</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </plugin>
    </plugins>
</build>
```
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
```xml
<dependencies>
    <dependency>
        <groupId>fr.mrmicky</groupId>
        <artifactId>FastBoard</artifactId>
        <version>1.1.0</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

#### Gradle
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```
```groovy
dependencies {
    compile 'fr.mrmicky:FastBoard:1.1.0'
}
```

#### Manual

Just copy `FastBoard.java` and `FastReflection.java` in your plugin

### Create a scoreboard
Just create a new `FastBoard` and update the title and the lines

```java
FastBoard board = new FastBoard(player);

// Set the title
board.updateTitle(ChatColor.GOLD + "FastBoard");

// Change the lines
board.updateLines(
        "", // Empty line
        "One line",
        "", // Empty line
        "Second line"
);
```

## Example

Just a small example plugin with a scoreboard that refresh every second :
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
            for (FastBoard board : boards.values()) {
                updateBoard(board);
            }
        }, 0, 20);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        FastBoard board = new FastBoard(player);

        board.updateTitle(ChatColor.RED + "FastBoard");

        boards.put(player.getUniqueId(), board);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        FastBoard board = boards.remove(player.getUniqueId());

        if (board != null) {
            board.delete();
        }
    }

    private void updateBoard(FastBoard board) {
        board.updateLines(
                "",
                "Online: " + getServer().getOnlinePlayers().size(),
                "",
                "Kills: " + board.getPlayer().getStatistic(Statistic.PLAYER_KILLS),
                ""
        );
    }
}
```

## TODO
* Deploy to another maven repo
