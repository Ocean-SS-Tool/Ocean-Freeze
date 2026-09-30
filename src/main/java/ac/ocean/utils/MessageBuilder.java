package ac.ocean.utils;

import org.bukkit.entity.Player;

public class MessageBuilder {

    private final Player player;
    private final StringBuilder message;

    public MessageBuilder(Player player) {
        this.player = player;
        this.message = new StringBuilder();
    }

    public MessageBuilder text(String text) {
        message.append(text);
        return this;
    }

    public MessageBuilder clickable(String text, String command, String hover) {
        String current = message.toString();
        message.setLength(0);
        ClickableMessage.send(player, current, text, hover, command);
        return this;
    }

    public MessageBuilder newLine() {
        if (message.length() > 0) {
            player.sendMessage(colorize(message.toString()));
            message.setLength(0);
        }
        return this;
    }

    public void send() {
        if (message.length() > 0) {
            player.sendMessage(colorize(message.toString()));
            message.setLength(0);
        }
    }

    private String colorize(String text) {
        return text.replace("&", "\u00a7");
    }

    public static void sendScanResultsWithClickables(Player player, String baseMessage,
                                                     String clickText1, String command1, String hover1,
                                                     String clickText2, String command2, String hover2) {
        ClickableMessage.sendDouble(player, baseMessage, clickText1, command1, hover1, clickText2, command2, hover2);
    }

    public static void sendClickableMessage(Player player, String baseMessage,
                                           String clickText, String command, String hover) {
        ClickableMessage.send(player, baseMessage, clickText, hover, command);
    }
}
