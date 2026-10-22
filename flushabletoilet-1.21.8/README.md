# Flushable Toilet

A Fabric mod for Minecraft 1.21.8 that adds a toilet you can flush.

- **Right-click** to flush: lever click, a whirlpool sound, splash particles, and the bowl water drains and swirls.
- **Items dropped in the bowl** get flushed away (they're deleted).
- The tank refills after about 2.5 seconds. You can't flush again until it's full.
- The bowl water takes the color of the local biome's water.
- Mine it with a pickaxe. It's in the Functional Blocks creative tab.

## Recipe

```
. . Q      Q = Block of Quartz
Q W Q      W = Water Bucket (you get the bucket back)
. Q .
```

## Build

Requires JDK 21.

```
./gradlew build        # jar goes to build/libs/flushabletoilet-1.0.0.jar
./gradlew runClient    # launch a dev client with the mod
```

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for 1.21.8.
2. Put `flushabletoilet-1.0.0.jar` and [Fabric API](https://modrinth.com/mod/fabric-api) (1.21.8) in `%APPDATA%\.minecraft\mods`.
