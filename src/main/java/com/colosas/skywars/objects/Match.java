package com.colosas.skywars.objects;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.util.EditSessionBuilder;
import com.colosas.skywars.Skywars;
import com.colosas.skywars.tasks.StartCountdown;
import com.colosas.skywars.tasks.TeleportCountdown;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Match {

    private final String joinMessage;
    private final String leaveMessage;
    private final String canceledMessage;
    private final String startedMessage;
    private final String killMessageWithoutAttacker;
    private final String killMessageWithAttacker;
    private final String winnerMessage;

    private final String name;
    private final int maxPlayers;
    private final int minPlayers;
    private Location lobbyPoint;
    private Location spectatorPoint;
    private final List<Location> spawnPoints = new ArrayList<>();
    private final World world;

    private MatchState state = MatchState.WAITING;
    private final List<Player> players = new ArrayList<>();
    private final Set<Player> spectators = new HashSet<>();
    private final MatchMap map;
    private final Set<Chest> openedChests = new HashSet<>();

    private TeleportCountdown teleportCountdown;

    private StartCountdown startCountdown;

    public Match() {
        joinMessage = Skywars.getInstanceConfig().getString("messages.join");
        leaveMessage = Skywars.getInstanceConfig().getString("messages.leave");
        canceledMessage = Skywars.getInstanceConfig().getString("messages.canceled");
        startedMessage = Skywars.getInstanceConfig().getString("messages.started");
        killMessageWithoutAttacker = Skywars.getInstanceConfig().getString("messages.kill.withoutAttacker");
        killMessageWithAttacker = Skywars.getInstanceConfig().getString("messages.kill.withAttacker");
        winnerMessage = Skywars.getInstanceConfig().getString("messages.winner");

        name = Skywars.getInstanceConfig().getString("game.name");
        maxPlayers = Skywars.getInstanceConfig().getInt("game.maxPlayers");
        minPlayers = Skywars.getInstanceConfig().getInt("game.minPlayers");

        if (minPlayers > maxPlayers) Skywars.getInstance().getLogger().severe("The number of minimum players is bigger than the number of maximum players!");

        String worldName = Skywars.getInstanceConfig().getString("game.world");
        if (worldName == null) {
            Skywars.getInstance().getLogger().warning("World not specified, using default 'world'.");
            worldName = "world";
        }

        world = new WorldCreator(worldName).type(WorldType.FLAT).createWorld();

        assert world != null;

        Random random = new Random();

        map = Skywars.getInstance().getMatchMaps().get(random.nextInt(Skywars.getInstance().getMatchMaps().size()));

        Clipboard clipboard = map.getSchematic();

        com.sk89q.worldedit.world.World weWorld = FaweAPI.getWorld(world.getName());

        try (EditSession editSession = new EditSessionBuilder(weWorld).world(weWorld).build()) {
            Operation operation = new ClipboardHolder(clipboard).createPaste(editSession).to(clipboard.getOrigin()).build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            e.printStackTrace();
            // TODO: handle this better
        }

        Match.this.lobbyPoint = new Location(world, map.getLobbyPoint().getBlockX() + .5, map.getLobbyPoint().getBlockY(), map.getLobbyPoint().getBlockZ() + .5);
        Match.this.spectatorPoint = new Location(world, map.getSpectatorPoint().getBlockX() + .5, map.getSpectatorPoint().getBlockY(), map.getSpectatorPoint().getBlockZ() + .5);

        for (BlockVector3 vector : map.getSpawnPoints()) {
            spawnPoints.add(new Location(world, vector.getBlockX() + .5, vector.getBlockY(), vector.getBlockZ() + .5));
        }

        if (spawnPoints.size() < maxPlayers) {
            if (spawnPoints.size() < minPlayers) Skywars.getInstance().getLogger().severe("There are less spawn points than the minimum amount of players!");
            else Skywars.getInstance().getLogger().severe("There are less spawn points than the maximum amount of players!");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            handleJoin(player);
        }

        world.setGameRule(GameRule.FALL_DAMAGE, false);

        String[] response = new String[6];

        response[0] = Skywars.getInstance().getServerName();
        response[1] = name;
        response[2] = Boolean.toString(isState(Match.MatchState.WAITING) || isState(Match.MatchState.STARTING));
        response[3] = Integer.toString(getPlayers().size());
        response[4] = map.getName();

        Skywars.getInstance().getSocketClient().sendMessage("Info", response);
    }

    public void handleJoin(Player player) {
        if (isState(MatchState.WAITING) || isState(MatchState.STARTING)) {
            processAsPlayer(player);
            Skywars.getInstance().getSocketClient().sendMessage("Join");

            Component joinMessageComponent = LegacyComponentSerializer.legacy('&').deserialize(joinMessage)
                    .replaceText(TextReplacementConfig.builder().match("%player%").replacement(player.displayName()).build())
                    .replaceText(TextReplacementConfig.builder().match("%playerCount%").replacement(Integer.toString(getPlayers().size())).build())
                    .replaceText(TextReplacementConfig.builder().match("%maxPlayers%").replacement(Integer.toString(maxPlayers)).build());

            broadcast(joinMessageComponent);
            if (getPlayers().size() >= minPlayers && isState(MatchState.WAITING)) {
                setTeleportCountdown(new TeleportCountdown(this));
                getTeleportCountdown().runTaskTimer(Skywars.getInstance(), 0, 20);
            }
        } else {
            processAsSpectator(player, false);
        }
    }

    public void handleDeath(Player victim, Player attacker) {
        if (isState(MatchState.SHUTTING_DOWN)) return;
        if (!hasPlayer(victim) || (attacker != null && !hasPlayer(attacker))) return;
        processAsSpectator(victim, true);
        Skywars.getInstance().getSocketClient().sendMessage("Quit");

        Component killMessage;
        if (attacker == null) {
            killMessage = LegacyComponentSerializer.legacy('&').deserialize(killMessageWithoutAttacker).replaceText(TextReplacementConfig.builder().match("%victim%").replacement(victim.displayName()).build());
        } else {
            killMessage = LegacyComponentSerializer.legacy('&').deserialize(killMessageWithAttacker).replaceText(TextReplacementConfig.builder().match("%victim%").replacement(victim.displayName()).build()).replaceText(TextReplacementConfig.builder().match("%attacker%").replacement(attacker.displayName()).build());
        }
        broadcast(killMessage);
        broadcastSpectators(killMessage);

        if ((isState(MatchState.ACTIVE) || isState(MatchState.DEATHMATCH)) && getPlayers().size() <= 1) handleEnd();
    }

    public void handleLeave(Player player) {
        if (isState(MatchState.SHUTTING_DOWN)) return;
        if (hasSpectator(player)) {
            getSpectators().remove(player);
            return;
        }
        if (!hasPlayer(player)) return;
        Skywars.getInstance().getSocketClient().sendMessage("Quit");

        getPlayers().remove(player);
        if (isState(MatchState.WAITING) || isState(MatchState.STARTING)) {

            Component leaveMessageComponent = LegacyComponentSerializer.legacy('&').deserialize(leaveMessage)
                    .replaceText(TextReplacementConfig.builder().match("%player%").replacement(player.displayName()).build())
                    .replaceText(TextReplacementConfig.builder().match("%playerCount%").replacement(Integer.toString(getPlayers().size())).build())
                    .replaceText(TextReplacementConfig.builder().match("%maxPlayers%").replacement(Integer.toString(maxPlayers)).build());

            broadcast(leaveMessageComponent);
            if (getPlayers().size() < minPlayers && isState(MatchState.STARTING)) {
                cancelCountdown();
            }
        }
        if ((isState(MatchState.ACTIVE) || isState(MatchState.DEATHMATCH)) && getPlayers().size() <= 1) handleEnd();
    }

    public void cancelCountdown() {
        getTeleportCountdown().cancel();
        teleportCountdown = null;
        setState(MatchState.WAITING);
        broadcast(LegacyComponentSerializer.legacy('&').deserialize(canceledMessage));
    }

    public void teleportPlayers() {
        Set<Location> usedSpawnPoints = new HashSet<>();
        Random random = new Random();
        for (Player player : getPlayers()) {
            if (getPlayers().size() > getSpawnPoints().size()) {
                Skywars.getInstance().getLogger().severe("There are too many players for the amount of spawn points!");
                return;
            }

            Location spawnPoint = getSpawnPoints().get(random.nextInt(getSpawnPoints().size()));
            while (usedSpawnPoints.contains(spawnPoint)) {
                spawnPoint = getSpawnPoints().get(random.nextInt(getSpawnPoints().size()));
            }
            usedSpawnPoints.add(spawnPoint);

            player.teleport(spawnPoint);
        }
    }

    public void processAsPlayer(Player player) {
        getPlayers().add(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.getInventory().clear();
        player.teleport(lobbyPoint);
    }

    public void processAsSpectator(Player player, boolean playerDead) {
        getPlayers().remove(player);
        getSpectators().add(player);
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().clear();
        if (!playerDead) player.teleport(spectatorPoint);
    }

    public void handleStart() {
        setState(MatchState.ACTIVE);
        for (Player player : getPlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            player.setFoodLevel(20);
            player.setSaturation(5);
        }
        for (Location spawnPoint : spawnPoints) {
            Location underneath = spawnPoint.subtract(0, 1, 0);
            spawnPoint.getWorld().getBlockAt(underneath).setType(Material.AIR);
        }
        Bukkit.getScheduler().runTaskLater(Skywars.getInstance(), () -> world.setGameRule(GameRule.FALL_DAMAGE, true), 40);
        broadcast(LegacyComponentSerializer.legacy('&').deserialize(startedMessage));
    }

    public void clearMap() {
        if (getTeleportCountdown() != null && !getTeleportCountdown().isCancelled()) getTeleportCountdown().cancel();
        if (getStartCountdown() != null && !getStartCountdown().isCancelled()) getStartCountdown().cancel();
        com.sk89q.worldedit.world.World weWorld = FaweAPI.getWorld(world.getName());
        Clipboard schem = map.getSchematic();
        Region region = schem.getRegion();

        try (EditSession session = new EditSessionBuilder(weWorld).build()) {
            session.setBlocks(region, BlockTypes.AIR.getDefaultState());
        } catch (MaxChangedBlocksException e) {
            e.printStackTrace();
        }
    }

    public void handleEnd() {
        setState(MatchState.ENDING);
        if (getPlayers().size() > 0 || getSpectators().size() > 0) {
            Player player = null;
            if (getPlayers().size() > 0) {
                player = getPlayers().get(0);
                Component winnerMessageComponent = LegacyComponentSerializer.legacy('&').deserialize(winnerMessage).replaceText(TextReplacementConfig.builder().match("%player%").replacement(player.displayName()).build());
                broadcast(winnerMessageComponent);
                broadcastSpectators(winnerMessageComponent);
            }

            final Player finalPlayer = player;
            new BukkitRunnable() {
                @Override
                public void run() {
                    setState(MatchState.SHUTTING_DOWN);
                    if (finalPlayer != null && finalPlayer.isOnline()) {
                        finalPlayer.kick(Component.text("Match's over"));
                    }
                    for (Player player : getSpectators()) {
                        if (player.isOnline()) player.kick(Component.text("Match's over"));
                    }
                    getPlayers().clear();
                    getSpectators().clear();
                    Skywars.getInstance().reload();
                }
            }.runTaskLater(Skywars.getInstance(), 300);
        } else {
            setState(MatchState.SHUTTING_DOWN);
            Skywars.getInstance().reload();
        }
    }

    public void broadcast(Component message) {
        for (Player player : getPlayers()) {
            player.sendMessage(message);
        }
    }

    public void broadcastSpectators(Component message) {
        for (Player player : getSpectators()) {
            player.sendMessage(message);
        }
    }

    public void clearActionBars() {
        for (Player player : getPlayers()) {
            player.sendActionBar(Component.text(" "));
        }
    }

    public boolean hasPlayer(Player player) {
        for (Player gamePlayer : getPlayers()) {
            if (gamePlayer == player) return true;
        }
        return false;
    }

    public boolean hasSpectator(Player player) {
        for (Player spectator : getSpectators()) {
            if (spectator == player) return true;
        }
        return false;
    }

    public boolean isState(MatchState state) {
        return getState() == state;
    }

    public MatchState getState() {
        return state;
    }

    public void setState(MatchState state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Set<Player> getSpectators() {
        return spectators;
    }

    public Location getLobbyPoint() {
        return lobbyPoint;
    }

    public Location getSpectatorPoint() {
        return spectatorPoint;
    }

    public List<Location> getSpawnPoints() {
        return spawnPoints;
    }

    public MatchMap getMap() {
        return map;
    }

    public Set<Chest> getOpenedChests() {
        return openedChests;
    }

    public TeleportCountdown getTeleportCountdown() {
        return teleportCountdown;
    }

    public void setTeleportCountdown(TeleportCountdown teleportCountdown) {
        this.teleportCountdown = teleportCountdown;
    }

    public StartCountdown getStartCountdown() {
        return startCountdown;
    }

    public void setStartCountdown(StartCountdown startCountdown) {
        this.startCountdown = startCountdown;
    }

    public enum MatchState {
        WAITING,
        STARTING,
        PREPARATION,
        ACTIVE,
        DEATHMATCH,
        ENDING,
        SHUTTING_DOWN
    }

}
