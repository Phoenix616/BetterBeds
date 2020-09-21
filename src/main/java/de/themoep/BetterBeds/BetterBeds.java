package de.themoep.BetterBeds;

import de.themoep.minedown.MineDown;
import de.themoep.utils.lang.bukkit.LanguageManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/*
 * BetterBeds
 * Copyright (c) 2019 Max Lee aka Phoenix616 (mail@moep.tv)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class BetterBeds extends JavaPlugin implements Listener {

    private int minPlayers = 2;
    private double sleepPercentage = 0.5;
    private int nightSpeed = 0;
    private boolean ignoredHelp = true;
    private boolean resetPhantomsForAll = false;

    private Map<String, NotificationMessage> notificationMessages = new HashMap<>();

    private final Map<UUID, WorldInfo> infoMap = new HashMap<>();

    private LanguageManager lang;

    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        getServer().getPluginManager().registerEvents(new BetterBedsListener(this), this);
    }

    /**
     * Reload command's method
     */
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) throws NumberFormatException {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "BetterBeds v" + getDescription().getVersion());
        } else if ("reload".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ChatColor.YELLOW + "[BetterBeds] Reloading Config");
            loadConfig();
            return true;
        }
        return false;
    }

    /**
     * Loads the options from the config file into the plugins variables
     */
    private void loadConfig() {
        reloadConfig();
        lang = new LanguageManager(this, getConfig().getString("default-language"));
        minPlayers = getConfig().getInt("minPlayers");
        try {
            sleepPercentage = Double.parseDouble(this.getConfig().getString("sleepPercentage").replaceAll(" ", "").replace("%", ""));
        } catch (NumberFormatException e) {
            getLogger().log(Level.WARNING, "You have an Error in your config at the sleepPercentage-node! Using the default now: " + this.sleepPercentage);
        }
        if (sleepPercentage > 1) {
            sleepPercentage = sleepPercentage / 100;
        }
        nightSpeed = getConfig().getInt("nightSpeed");
        ignoredHelp = getConfig().getBoolean("ignoredHelp");
        resetPhantomsForAll = getConfig().getBoolean("resetPhantomsForAll");

        notificationMessages.clear();
        ConfigurationSection notificationsSection = getConfig().getConfigurationSection("notifications");
        for (String key : notificationsSection.getKeys(true)) {
            try {
                notificationMessages.put(key.toLowerCase(), new NotificationMessage(
                        NotificationType.valueOf(notificationsSection.getString(key + ".type", "server").toUpperCase()),
                        NotificationLocation.valueOf(notificationsSection.getString(key + ".location", "chat").toUpperCase()),
                        key
                ));
            } catch (IllegalArgumentException e) {
                getLogger().log(Level.WARNING, "Error while loading notification config of " + key + ": " + e.getMessage());
            }
        }
    }

    public WorldInfo getInfo(World world) {
        return infoMap.computeIfAbsent(world.getUID(), id -> new WorldInfo());
    }

    public String getText(CommandSender sender, String key, Map<String, String> replacements) {
        return TextComponent.toLegacyText(getMessage(sender, key, replacements));
    }

    public BaseComponent[] getMessage(CommandSender sender, String key, Map<String, String> replacements) {
        return new MineDown(lang.getConfig(sender).get(key)).placeholderPrefix("{").placeholderSuffix("}").replace(replacements).toComponent();
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, Collections.emptyMap());
    }

    public void sendMessage(CommandSender sender, String key, Map<String, String> replacements) {
        if (sender instanceof Player) {
            NotificationMessage notInfo = getNotification(key);
            switch (notInfo.getLocation()) {
                case CHAT:
                    sender.spigot().sendMessage(getMessage(sender, key, replacements));
                    break;
                case TITLE:
                    String[] msg = getText(sender, key, replacements).split("\n");
                    ((Player) sender).sendTitle(msg[0], msg.length > 1 ? msg[1] : "", 10, 70, 20);
                    if (msg.length > 2) {
                        ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg[3]));
                    }
                    break;
                case ACTIONBAR:
                    ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, getMessage(sender, key, replacements));
                    break;
            }
        } else {
            sender.sendMessage(getText(sender, key, replacements));
        }
    }

    private NotificationMessage getNotification(String key) {
        return notificationMessages.computeIfAbsent(key.toLowerCase(), k -> new NotificationMessage(NotificationType.SERVER, NotificationLocation.CHAT, k));
    }

    /**
     * Check if enough players are asleep.
     * @param world         The world to calculate with
     * @param playerQuit    Whether this was called by a player quit or not (substracts one from the count)
     */
    public boolean isPlayerRequirementSatisfied(World world, boolean playerQuit) {
        if (!infoMap.containsKey(world.getUID()))
            return false;

        for (Player p : world.getPlayers()) {
            if (!p.isSleeping() && p.hasPermission("betterbeds.ghost") && !p.hasPermission("betterbeds.ghost.buster"))
                return false;
        }

        return !getInfo(world).getAsleep().isEmpty() && getInfo(world).getAsleep().size() >= getRequiredPlayers(world, playerQuit);
    }

    /**
     * Get the amount of players required to sleep to advance the night
     * @param world         The world to check
     * @param playerQuit    Whether this was called by a player quit or not (substracts one from the count)
     * @return
     */
    public int getRequiredPlayers(World world, boolean playerQuit) {
        int eligible = 0;
        for (Player p : world.getPlayers()) {
            if (!p.isSleepingIgnored() && p.hasPermission("betterbeds.sleep") && !p.hasPermission("betterbeds.ignore") && !isPlayerAFK(p) )
                eligible++;
        }

        if (playerQuit && eligible > 0) {
            eligible--;
        }

        int required = (int) Math.ceil(eligible * sleepPercentage);

        if (required < minPlayers) {
            return Math.min(minPlayers, eligible);
        }
        return required;
    }

    /**
     * Check if enough players are asleep and fast forward if so.
     * @param world      The world to calculate with
     * @param onQuit
     */
    public boolean checkPlayers(final World world, boolean onQuit) {
        if (isPlayerRequirementSatisfied(world, onQuit)) {
            WorldInfo worldInfo = getInfo(world);
            if (nightSpeed == 0) {
                getLogger().log(Level.INFO, "Set time to dawn in world " + world.getName());
                notifyPlayers(world, (worldInfo.getAsleep().size() > 1) ? "notify" : "notifyOnSingle", getReplacements(world, onQuit));
                setWorldToMorning(world);
            } else {
                if (worldInfo.isTransitioning())
                    return false;

                notifyPlayers(world, (worldInfo.getAsleep().size() > 1) ? "notify" : "notifyOnSingle", getReplacements(world, onQuit));

                getLogger().log(Level.INFO, "Timelapsing " + nightSpeed + "x until dawn in world " + world.getName());
                worldInfo.setTransitionTask(getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
                    if (!isPlayerRequirementSatisfied(world, false)) {
                        getServer().getScheduler().cancelTask(worldInfo.getTransitionTask());
                        worldInfo.setTransitionTask(0);
                        return;
                    }
                    long currentTime = world.getTime();
                    long newTime = currentTime + nightSpeed;
                    if (newTime >= getWakeupTime(world)) {
                        getServer().getScheduler().cancelTask(worldInfo.getTransitionTask());
                        worldInfo.setTransitionTask(0);
                        setWorldToMorning(world);
                    } else {
                        world.setTime(currentTime + nightSpeed);
                    }
                }, 1L, 1L));
            }
            return true;
        }
        return false;
    }

    /**
     * Notifies all the players within a world of skipping the night
     * @param world         The world
     * @param key           The key of the message
     * @param replacements  The replacements
     */
    public void notifyPlayers(World world, String key, Map<String, String> replacements) {
        NotificationMessage notification = getNotification(key);
        Set<Player> pl = new HashSet<>();
        if (notification.getType() != NotificationType.NOONE) {
            switch (notification.getType()) {
                case WORLD:
                    pl.addAll(world.getPlayers());
                    break;
                case SERVER:
                    pl.addAll(getServer().getOnlinePlayers());
                    break;
                case SLEEPING:
                    WorldInfo worldInfo = getInfo(world);
                    for (Player p : world.getPlayers())
                        if (worldInfo.isAsleep(p))
                            pl.add(p);

            }
        }

        for (Player p : getServer().getOnlinePlayers()) {
            if (p.hasPermission("betterbeds.allnotifications"))
                pl.add(p);
        }

        for (Player p : pl) {
            sendMessage(p, key, replacements);
        }
    }

    /**
     * Resets the world's climate and the list of sleeping players.
     * @param world The world to change the time for
     */
    public void setWorldToMorning(World world) {
        if (world.getTime() < getWakeupTime(world)) {
            world.setTime(getWakeupTime(world));
        }
        if (world.hasStorm())
            world.setStorm(false);

        if (world.isThundering())
            world.setThundering(false);

        notifyPlayers(world, "wake", getReplacements(world, false));

        WorldInfo worldInfo = getInfo(world);
        for (Player player : world.getPlayers()) {
            if (resetPhantomsForAll || worldInfo.isAsleep(player)) {
                player.setStatistic(Statistic.TIME_SINCE_REST, 0);
            }
        }

        worldInfo.clearAsleep();
    }

    private long getWakeupTime(World world) {
        return world.hasStorm() ? 23992 : 23460;
    }

    /**
     * Calculates what happens when a player leaves the bed.
     * @param player The player who left the bed
     * @param world  The world the bed was in (because it's possible the player isn't there anymore when he existed it)
     * @param onQuit Whether or not this is called on quit
     * @return boolean - True if we don't need to check the players anymore, False if didn't get checked if we should fast forward
     */
    public boolean calculateBedLeave(Player player, World world, boolean onQuit) {

        if (world.getEnvironment() != Environment.NORMAL)
            return true;

        if ((world.getWeatherDuration() > 0 && (world.getTime() < 12010 || world.getTime() > 23992))
                || (world.getWeatherDuration() == 0 && (world.getTime() < 12542 && world.getTime() > 23460)))
            return true;

        WorldInfo worldInfo = getInfo(world);
        if (worldInfo.isAsleep(player)) {
            int requiredPlayers = getRequiredPlayers(world, onQuit);

            worldInfo.setAwake(player);

            getLogger().log(Level.INFO, player.getName() + " is not sleeping anymore. " + worldInfo.getAsleep().size() + "/" + requiredPlayers + " players are asleep in world " + world.getName());

            notifyPlayers(world, "leave", getReplacements(world, player.getName(), worldInfo.getAsleep().size(), requiredPlayers));

            checkPlayers(world, false);
            return true;
        }
        return false;
    }

    /**
     * Check if a player is AFK
     * Currently this only works with the WhosAFK plugin
     * TODO: Add support for checking with more methods
     * TODO: Add a config option to decide whether AFK players should be counted
     * @param p the player
     * @return boolean - True if Player is currently AFK
     */
    private boolean isPlayerAFK(Player p) {
        ClassLoader classLoader = BetterBeds.class.getClassLoader();

        // Check if the player is AFK, according to WhosAFK
        try {
            // Load the WhosAFK class and it's playerIsAFK(Player) method
            Class<?> WhosAFK = classLoader.loadClass("whosafk.WhosAFK");
            Method whosafkPlayerIsAFK = WhosAFK.getMethod("playerIsAFK", Player.class);

            // Get the instance of WhosAFK being used by spigot
            @SuppressWarnings({"unchecked", "rawtypes"})
            JavaPlugin whosafk = JavaPlugin.getPlugin((Class) WhosAFK);

            // Finally, check if WhosAFK thinks the player is AFK
            if (whosafk.isEnabled() && (Boolean) whosafkPlayerIsAFK.invoke(whosafk, p))
                return true;
        } catch (ClassNotFoundException e) {
            // WhosAFK is not installed, no need to panic
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Default to not AFK
        return false;
    }

    private Map<String, String> getReplacements(World world, boolean onQuit) {
        WorldInfo worldInfo = getInfo(world);
        return getReplacements(world, worldInfo.getLastPlayerToEnterBed(), worldInfo.getAsleep().size(), getRequiredPlayers(world, onQuit));
    }

    /**
     * Converts eventual parameters in a message into a replacement map
     * TODO: Make it so that not every parameter is required!
     * @param world         The World to notify for
     * @param playername    String of the playername to insert in the message
     * @param sleeping      Integer of sleeping players
     * @param required      Integer of players required to sleep in the world
     * @return Replacement map
     */
    public Map<String, String> getReplacements(World world, String playername, int sleeping, int required) {
        Map<String, String> replacements = new LinkedHashMap<>();

        replacements.put("world", world.getName());
        replacements.put("player", playername != null ? playername : "Someone");

        replacements.put("sleeping", String.valueOf(sleeping));
        replacements.put("required", String.valueOf(required));

        float percentage = (float) Math.round(((double) sleeping / required * 100 * 100) / 100);
        replacements.put("percentage", String.format("%.2f", percentage));

        int more = required - sleeping;
        replacements.put("more", String.valueOf(more));

        return replacements;
    }

    public boolean ignoredHelp() {
        return ignoredHelp;
    }
}
