package de.themoep.BetterBeds;

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

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Level;

public class BetterBedsListener implements Listener {
    private final BetterBeds plugin;

    public BetterBedsListener(BetterBeds plugin) {
        this.plugin = plugin;
    }

    /**
     * Calculate if number of sleeping players is enough to fast forward the night.
     * @param event PlayerBedEnterEvent
     */
    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.isCancelled()
                || (!plugin.ignoredHelp() && event.getPlayer().hasPermission("betterbeds.ignore"))
                || !event.getPlayer().hasPermission("betterbeds.sleep"))
            return;

        World world = event.getBed().getWorld();
        WorldInfo worldInfo = plugin.getInfo(world);

        for (Player p : world.getPlayers()) {
            if (p != event.getPlayer() && !p.isSleeping() && p.hasPermission("betterbeds.ghost") && !p.hasPermission("betterbeds.ghost.buster")) {
                event.setCancelled(true);
                plugin.sendMessage(event.getPlayer(), "ghost");
                plugin.getLogger().log(Level.INFO, "There is a ghost online, players can't sleep now!");
                return;
            }
        }

        int requiredPlayers = plugin.getRequiredPlayers(world, false);

        worldInfo.setAsleep(event.getPlayer());
        worldInfo.setLastPlayerToEnterBed(event.getPlayer());

        if (worldInfo.isTransitioning()) {
            plugin.notifyPlayers(world, "sleep", plugin.getReplacements(world, event.getPlayer().getName(), worldInfo.getAsleep().size(), plugin.getRequiredPlayers(world, false)));
        } else {
            plugin.getLogger().log(Level.INFO, event.getPlayer().getName() + " sleeps now. " + worldInfo.getAsleep().size() + "/" + requiredPlayers + " players are asleep in world " + world.getName());

            if (!plugin.checkPlayers(world, false))
                plugin.notifyPlayers(world, "sleep", plugin.getReplacements(world, event.getPlayer().getName(), worldInfo.getAsleep().size(), plugin.getRequiredPlayers(world, false)));
        }
    }

    /**
     * Recalculates the number of sleeping players if a player leaves his bed
     * @param event PlayerBedLeaveEvent
     */
    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        plugin.calculateBedLeave(event.getPlayer(), event.getBed().getWorld(), false);
    }

    /**
     * Recalculates the number of sleeping players if a player quits the game between 12500 and 100 time ticks
     * @param event PlayerQuitEvent
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.calculateBedLeave(event.getPlayer(), event.getPlayer().getWorld(), true))
            plugin.checkPlayers(event.getPlayer().getWorld(), true);
    }

    /**
     * Recalculates the number of sleeping players if a player changes from a normal world between 12500 and 100 time ticks
     * @param event PlayerChangedWorldEvent
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!plugin.calculateBedLeave(event.getPlayer(), event.getFrom(), false))
            plugin.checkPlayers(event.getFrom(), false);
    }
}
