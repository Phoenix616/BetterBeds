package de.themoep.BetterBeds;

/**
 * Created by Phoenix616 on 28.01.2015.
 */
public class NotificationMessage {

    private NotificationType type;
    private String text;

    /**
     * Notification message object
     * @param type The type e.g. who should get the message
     * @param text The text of the message
     */
    public NotificationMessage(NotificationType type, String text) {
        this.type = type;
        this.text = text;
    }

    /**
     * Get the type of the notification message, specifies who gets the message
     * @return NotificationType - The type
     */
    public NotificationType getType() {
        return this.type;
    }

    /**
     * Get the text of the notification message
     * @return String - The text
     */
    public String getText() {
        return this.text;
    }
}
