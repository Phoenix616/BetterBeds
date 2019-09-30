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

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WorldInfo {

    private final Set<UUID> asleep = new HashSet<>();
    private String lastPlayerToEnterBed = "";
    private int transitionTask = 0;

    public Set<UUID> getAsleep() {
        return asleep;
    }

    public boolean isAsleep(Player player) {
        return asleep.contains(player.getUniqueId());
    }

    public void setAsleep(Player player) {
        asleep.add(player.getUniqueId());
    }

    public boolean setAwake(Player player) {
        return asleep.remove(player.getUniqueId());
    }

    public void clearAsleep() {
        asleep.clear();
    }

    public String getLastPlayerToEnterBed() {
        return lastPlayerToEnterBed;
    }

    public void setLastPlayerToEnterBed(Player player) {
        lastPlayerToEnterBed = player.getName();
    }

    public int getTransitionTask() {
        return transitionTask;
    }

    public void setTransitionTask(int transitionTask) {
        this.transitionTask = transitionTask;
    }

    public boolean isTransitioning() {
        return getTransitionTask() != 0;
    }
}
