# FastBoard

[![Java CI](https://github.com/MrMicky-FR/FastBoard/actions/workflows/build.yml/badge.svg)](https://github.com/MrMicky-FR/FastBoard/actions/workflows/build.yml)
[![Language grade](https://img.shields.io/lgtm/grade/java/github/MrMicky-FR/FastBoard.svg?logo=lgtm&logoWidth=18&label=code%20quality)](https://lgtm.com/projects/g/MrMicky-FR/FastBoard/context:java)
[![Maven Central](https://img.shields.io/maven-central/v/fr.mrmicky/fastboard.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22fr.mrmicky%22%20AND%20a:%22fastboard%22)
[![Discord](https://img.shields.io/discord/390919659874156560.svg?colorB=5865f2&label=Discord&logo=discord&logoColor=white)](https://discord.gg/q9UwaBT)

Lightweight packet-based scoreboard API for Bukkit plugins, with 1.7.10 to 1.18 support.

⚠️ To use FastBoard on a 1.8 server, the server must be on 1.8.8.

## Features

* No flickering (without using a buffer)
* Works with all versions from 1.7.10 to 1.18
* Very small (around 600 lines of code with the JavaDoc) and no dependencies
* Easy to use
* Dynamic scoreboard size: you don't need to add/remove lines, you can just give a string list (or array) to change all
  the lines
* Everything is at packet level, so it works with other plugins using scoreboard and/or teams
* Can be used asynchronously
* Supports up to 30 characters per line on 1.12.2 and below
* No character limit on 1.13 and higher
* Supports hex colors on 1.16 and higher

## Installation

### Maven

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-shade-plugin</artifactId>
      <version>3.2.4</version>
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
    <version>1.2.1</version>
  </dependency>
</dependencies>
```

### Gradle

```groovy
plugins {
  id 'com.github.johnrengelman.shadow' version '6.1.0'
}

repositories {
  mavenCentral()
}

dependencies {
  implementation 'fr.mrmicky:fastboard:1.2.1'
}

shadowJar {
  // Replace 'com.yourpackage' with the package of your plugin 
  relocate 'fr.mrmicky.fastboard', 'com.yourpackage.fastboard'
}
```

### Manual

Copy `FastBoard.java`, `FastReflection.java` and `FastBoardRegistry.java` in your plugin.

## Usage

### Creating a scoreboard

Just create a new `FastBoard` and update the title and the lines:

```java
final class Test {
  void test() {
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
  }
}
```

### Example-1

#### Manual Registry

Just a small example plugin with a scoreboard that refreshes every second:

```java
package fr.mrmicky.fastboard.example;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.plugin.java.JavaPlugin;
import fr.mrmicky.fastboard.FastBoardRegistry;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ExamplePlugin extends JavaPlugin implements Listener {

  private final Map<UUID, FastBoard> boards = new HashMap<>();

  @Override
  public void onEnable() {
    this.getServer().getPluginManager().registerEvents(this, this);
    this.getServer().getScheduler().runTaskTimer(this, () -> {
      for (final FastBoard board : this.boards.values()) {
        this.updateBoard(board);
      }
    }, 0, 20);
  }

  @EventHandler
  public void onJoin(final PlayerJoinEvent event) {
    final Player player = event.getPlayer();
    final FastBoard board = new FastBoard(player);
    board.updateTitle(ChatColor.RED + "FastBoard");
    this.boards.put(player.getUniqueId(), board);
  }

  @EventHandler
  public void onQuit(final PlayerQuitEvent event) {
    final Player player = event.getPlayer();
    final FastBoard board = this.boards.remove(player.getUniqueId());
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

### Example-2

#### Auto Registry

Just a small example plugin with a scoreboard that refreshes every second:

```java
package fr.mrmicky.fastboard.example;

import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import fr.mrmicky.fastboard.FastBoardRegistry;

public final class ExamplePlugin extends JavaPlugin implements Listener {

  private final FastBoardRegistry registry = new FastBoardRegistry(this)
    .setPeriodDelay(0L)
    .setPeriodInterval(20L)
    .setOnRegister(board -> {
      board.updateTitle(ChatColor.RED + "FastBoard");
    })
    .setOnPeriod(board -> {
      board.updateLines(
        "",
        "Players: " + this.getServer().getOnlinePlayers().size(),
        "",
        "Kills: " + board.getPlayer().getStatistic(Statistic.PLAYER_KILLS),
        ""
      );
    })
    .setOnUnregister(board -> {
    });

  @Override
  public void onEnable() {
    this.registry.initiate();
  }
}
```
