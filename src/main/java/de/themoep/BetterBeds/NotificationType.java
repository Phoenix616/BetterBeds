package de.themoep.BetterBeds;

/**
 * Created by Phoenix616 on 28.01.2015.
 */

/**
 * Enum to specify who should get a notification message.
 * NOONE - Don't display the message to anyone
 * SLEEPING - Only players who lye in a bed
 * WORLD - Every player who is in the same world
 * SERVER - Every player on the server
 */
public enum NotificationType {
        NOONE,
        SLEEPING,
        WORLD,
        SERVER;
}
