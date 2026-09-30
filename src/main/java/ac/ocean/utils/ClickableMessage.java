package ac.ocean.utils;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class ClickableMessage {

    public static void send(Player player, String prefix, String clickText, String hoverText, String command) {
        TextComponent message = new TextComponent(TextComponent.fromLegacyText(colorize(prefix)));

        TextComponent clickable = new TextComponent(TextComponent.fromLegacyText(colorize(clickText)));
        clickable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        clickable.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(colorize(hoverText)).create()));

        message.addExtra(clickable);
        player.spigot().sendMessage(message);
    }

    public static void sendDouble(Player player, String prefix,
                                  String clickText1, String command1, String hoverText1,
                                  String clickText2, String command2, String hoverText2) {
        TextComponent message = new TextComponent(TextComponent.fromLegacyText(colorize(prefix)));

        TextComponent click1 = new TextComponent(TextComponent.fromLegacyText(colorize(clickText1)));
        click1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command1));
        click1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(colorize(hoverText1)).create()));

        TextComponent space = new TextComponent(" ");

        TextComponent click2 = new TextComponent(TextComponent.fromLegacyText(colorize(clickText2)));
        click2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command2));
        click2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(colorize(hoverText2)).create()));

        message.addExtra(click1);
        message.addExtra(space);
        message.addExtra(click2);
        player.spigot().sendMessage(message);
    }

    private static String colorize(String message) {
        return message.replace("&", "\u00a7");
    }
}
