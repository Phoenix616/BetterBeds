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

public class NotificationMessage {

    private final NotificationType type;

    private final NotificationLocation location;
    private final String key;

    /**
     * Notification message object
     * @param type The type e.g. who should get the message
     * @param location
     * @param key The text of the message
     */
    public NotificationMessage(NotificationType type, NotificationLocation location, String key) {
        this.type = type;
        this.location = location;
        this.key = key;
    }

    /**
     * Get the type of the notification message, specifies who gets the message
     * @return NotificationType - The type
     */
    public NotificationType getType() {
        return this.type;
    }

    /**
     * Get the notification location
     * @return NotificationLocation - The notification location
     */
    public NotificationLocation getLocation() {
        return location;
    }

    /**
     * Get the language key of the notification message
     * @return String - The language key
     */
    public String getKey() {
        return this.key;
    }
}
