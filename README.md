# FastBoard

A Scoreboard API for Bukkit with 1.7-1.13 support

## Features

* No flickering (and without using a buffer !)
* Works with all version from 1.7.10 to 1.13.2 !
* Really small (around 500 lines with everything) and don't use any dependency
* Easy to use
* Dynamic scoreboard size: you don't need to add/remove lines, you can just give String list (or array) to change all the lines
* Everything is at packet level so it works with other plugins using scoreboard and/or teams
* Can be use in an async thread (but is not thread safe yet)
* Support up to 30 characters per line on 1.7-1.12
* No characters limit on 1.13

## How to use

Just create a new `FastBoard` and update the title and the lines

```java
FastBoard board = new FastBoard(player);

// Set the title
board.updateTitle(ChatColor.GOLD + "FastBoard");

// Change the lines
board.updateLines(
        null, // Empty line
        "One line",
        "", // Empty line too
        "Second line"
);
```

## TODO
* Deploy to a maven repo
