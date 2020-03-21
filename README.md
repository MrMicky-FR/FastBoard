# FastBoard
[![JitPack](https://jitpack.io/v/fr.mrmicky/FastBoard.svg)](https://jitpack.io/#fr.mrmicky/FastBoard)
[![Discord](https://img.shields.io/discord/390919659874156560.svg?colorB=7289da&label=discord&logo=discord&logoColor=white)](https://discord.gg/q9UwaBT)

A Scoreboard API for Bukkit with 1.15 support

## Features

* No flickering (and without using a buffer !)
* Works with 1.15 !
* Really small (around 500 lines with everything) and don't use any dependency
* Easy to use
* Dynamic scoreboard size: you don't need to add/remove lines, you can just give String list (or array) to change all the lines
* Everything is at packet level so it works with other plugins using scoreboard and/or teams
* Can be use in an async thread (but is not thread safe yet)
* No characters limit!
* ProtocolLib dependency

## How to use

### Add FastBoard in your plugin
**Maven**
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
            <groupId>com.github.Ytnoos</groupId>
            <artifactId>FastBoard</artifactId>
            <version>1.2.0</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>
```

**Manual**

Just copy `FastBoard.java` in your plugin

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

### Example

Just a small example plugin with a scoreboard that refresh every seconds
```java
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
        Player p = e.getPlayer();

        FastBoard board = new FastBoard(p);

        board.updateTitle(ChatColor.RED + "FastBoard");

        boards.put(p.getUniqueId(), board);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();

        FastBoard board = boards.remove(p.getUniqueId());

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
* Deploy to an other maven repo
