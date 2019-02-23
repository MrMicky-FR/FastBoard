# FastBoard
[![JitPack](https://jitpack.io/v/fr.mrmicky/FastBoard.svg)](https://jitpack.io/#fr.mrmicky/FastBoard)
[![Discord](https://img.shields.io/discord/390919659874156560.svg?colorB=7289da&label=discord&logo=discord&logoColor=white)](https://discord.gg/q9UwaBT)

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
* No characters limit on 1.13 !

## How to use

### Add FastBoard in your plugin
**Maven**
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
            <version>1.0.0</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>
```

**Manual**

Just copy `FastBoard.java` and `FastReflection.java` in your plugin

### Crate a scoreboard
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
* Deploy to an other maven repo
