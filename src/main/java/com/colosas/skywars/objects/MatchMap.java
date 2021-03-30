package com.colosas.skywars.objects;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.HashSet;
import java.util.Set;

public class MatchMap {

    private String name;
    private Clipboard schematic;

    private BlockVector3 lobbyPoint;
    private BlockVector3 spectatorPoint;
    private final Set<BlockVector3> spawnPoints = new HashSet<>();

    public MatchMap(String name, Clipboard schematic) {
        this.name = name;
        this.schematic = schematic;
    }

    public String getName() {
        return name;
    }

    public Clipboard getSchematic() {
        return schematic;
    }

    public BlockVector3 getLobbyPoint() {
        return lobbyPoint;
    }

    public BlockVector3 getSpectatorPoint() {
        return spectatorPoint;
    }

    public Set<BlockVector3> getSpawnPoints() {
        return spawnPoints;
    }

    public void setSchematic(Clipboard schematic) {
        this.schematic = schematic;
    }

    public void setLobbyPoint(BlockVector3 lobbyPoint) {
        this.lobbyPoint = lobbyPoint;
    }

    public void setSpectatorPoint(BlockVector3 spectatorPoint) {
        this.spectatorPoint = spectatorPoint;
    }
}
