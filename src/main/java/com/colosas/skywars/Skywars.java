package com.colosas.skywars;

import com.colosas.skywars.commands.*;
import com.colosas.skywars.listeners.*;
import com.colosas.skywars.objects.Match;
import com.colosas.skywars.objects.MatchMap;
import com.colosas.skywars.socket.SocketClient;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Skywars extends JavaPlugin {

    private static Skywars instance;

    private Match match;

    private final List<MatchMap> matchMaps = new ArrayList<>();
    private final Map<ItemStack, Integer> items = new HashMap<>();
    private String serverName;
    private String lobbyServer;
    private int socketPort;

    private SocketClient socketClient;

    @Override
    public void onEnable() {
        instance = this;

        reloadConfig();
        getConfig().options().copyDefaults(true);
        getConfig().options().copyHeader(true);
        saveDefaultConfig();

        serverName = getConfig().getString("serverName");
        socketClient = new SocketClient(getConfig().getInt("socketPort"));

        loadSchematics();
        loadItems();

        if (getMatchMaps().size() == 0) {
            getLogger().severe("No schematics found.");
            return;
        }

        match = new Match();

        getServer().getPluginManager().registerEvents(new PlayerJoin(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeath(), this);
        getServer().getPluginManager().registerEvents(new PlayerLeave(), this);
        getServer().getPluginManager().registerEvents(new ChestOpen(), this);
        getServer().getPluginManager().registerEvents(new CraftItem(), this);
        getServer().getPluginManager().registerEvents(new BlockBreak(), this);

        getServer().getPluginCommand("toworld").setExecutor(new ToWorld());
        getServer().getPluginCommand("addspawnpoint").setExecutor(new AddSpawnPoint());
        getServer().getPluginCommand("setlobbypoint").setExecutor(new SetLobbyPoint());
        getServer().getPluginCommand("setspectatorpoint").setExecutor(new SetSpectatorPoint());
        getServer().getPluginCommand("test").setExecutor(new Test());
        getServer().getPluginCommand("swrl").setExecutor(new Reload());
    }

    @Override
    public void onDisable() {
        if (!socketClient.isClosed()) socketClient.close();

        match.clearMap();
        matchMaps.clear();
        items.clear();
        instance = null;
        match = null;
    }

    public void reload() {

        reloadConfig();
        getConfig().options().copyDefaults(true);
        getConfig().options().copyHeader(true);
        saveDefaultConfig();

        if (!socketClient.isClosed()) socketClient.close();
        socketClient = new SocketClient(getConfig().getInt("socketPort"));

        match.clearMap();
        matchMaps.clear();
        items.clear();

        loadSchematics();
        loadItems();
        if (getMatchMaps().size() == 0) {
            getLogger().severe("No schematics found.");
            return;
        }
        match = new Match();
    }

    public void loadSchematics() {
        File dataFolder = getDataFolder();

        if (!dataFolder.exists()) dataFolder.mkdir();

        File schemFolder = new File(dataFolder, "schematics");

        if (!schemFolder.exists()) schemFolder.mkdir();

        File[] files = schemFolder.listFiles();

        Set<String> maps = getConfig().getConfigurationSection("maps").getKeys(false);

        for (String map : maps) {
            String BASE = "maps." + map + ".";
            String schemName = getConfig().getString(BASE + "schemFile");

            if (schemName == null) continue;

            File schemFile = null;

            for (File file : files) {
                if (schemName.equals(file.getName())) {
                    schemFile = file;
                    break;
                }
            }

            if (schemFile == null) continue;

            Clipboard clipboard = null;

            ClipboardFormat format = ClipboardFormats.findByFile(schemFile);

            if (format == null) continue;

            try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
                clipboard = reader.read();
            } catch (IOException e) {
                e.printStackTrace();
                // TODO: handle this better
            }

            if (clipboard == null) continue;

            MatchMap matchMap = new MatchMap(map, clipboard);

            double lobbyX = getConfig().getDouble(BASE + "lobbyPoint.x");
            double lobbyY = getConfig().getDouble(BASE + "lobbyPoint.y");
            double lobbyZ = getConfig().getDouble(BASE + "lobbyPoint.z");

            BlockVector3 lobbyPoint = BlockVector3.at(lobbyX, lobbyY, lobbyZ);

            matchMap.setLobbyPoint(lobbyPoint);

            double spectatorX = getConfig().getDouble(BASE + "spectatorPoint.x");
            double spectatorY = getConfig().getDouble(BASE + "spectatorPoint.y");
            double spectatorZ = getConfig().getDouble(BASE + "spectatorPoint.z");

            BlockVector3 spectatorPoint = BlockVector3.at(spectatorX, spectatorY, spectatorZ);

            matchMap.setSpectatorPoint(spectatorPoint);

            List<Map<?, ?>> spawnPoints = getConfig().getMapList(BASE + "spawnPoints");

            for (Map<?, ?> spawnPoint : spawnPoints) {
                double x;
                double y;
                double z;
                if (spawnPoint.get("x") instanceof Integer) {
                    x = (int) spawnPoint.get("x");
                } else x = (double) spawnPoint.get("x");
                if (spawnPoint.get("y") instanceof Integer) {
                    y = (int) spawnPoint.get("y");
                } else y = (double) spawnPoint.get("y");
                if (spawnPoint.get("z") instanceof Integer) {
                    z = (int) spawnPoint.get("z");
                } else z = (double) spawnPoint.get("z");

                matchMap.getSpawnPoints().add(BlockVector3.at(x, y, z));
            }

            getMatchMaps().add(matchMap);
        }
    }

    public void loadItems() {
        List<String> itemList = getConfig().getStringList("game.items");

        for (String item : itemList) {
            String[] itemSplit = item.split(":");
            String itemName = itemSplit[0].toUpperCase(Locale.ENGLISH);
            int itemAmount = Integer.parseInt(itemSplit[1]);
            Material material = Material.getMaterial(itemName);

            int itemProbability = -1;

            if (itemSplit.length >= 3) {
                String probabilityString = itemSplit[2];
                if (probabilityString.endsWith("%")) probabilityString = probabilityString.substring(0, probabilityString.length() - 1);

                itemProbability = Integer.parseInt(probabilityString);
            }

            if (material == null) {
                getLogger().warning("Invalid item '" + itemName + "'.");
                continue;
            }

            ItemStack stack = new ItemStack(material, itemAmount);
            items.put(stack, itemProbability);
        }
    }

    public static FileConfiguration getInstanceConfig() {
        return instance.getConfig();
    }

    public static Skywars getInstance() {
        return instance;
    }

    public String getServerName() {
        return serverName;
    }

    public String getLobbyServer() {
        return lobbyServer;
    }

    public void setLobbyServer(String lobbyServer) {
        this.lobbyServer = lobbyServer;
    }

    public List<MatchMap> getMatchMaps() {
        return matchMaps;
    }

    public Map<ItemStack, Integer> getItems() {
        return items;
    }

    public Match getMatch() {
        return match;
    }

    public SocketClient getSocketClient() {
        return socketClient;
    }

    public int getSocketPort() {
        return socketPort;
    }

    public void setSocketPort(int socketPort) {
        this.socketPort = socketPort;
    }
}
