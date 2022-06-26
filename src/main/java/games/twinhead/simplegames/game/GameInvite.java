package games.twinhead.simplegames.game;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GameInvite {

    private final GameType gameType;
    private final Player sender;
    private final Player receiver;
    private final UUID inviteId;

    private InviteState state = InviteState.WAITING;

    public GameInvite(GameType game, Player sender, Player receiver){
        this.inviteId = UUID.randomUUID();
        this.gameType = game;
        this.sender = sender;
        this.receiver = receiver;

        sendInviteMessage();
    }

    public InviteState getState() {
        return state;
    }

    public void setState(InviteState state) {
        this.state = state;
    }

    public GameType getGameType() {
        return gameType;
    }

    public Player getSender() {
        return sender;
    }

    public Player getReceiver() {
        return receiver;
    }

    public UUID getInviteId() {
        return inviteId;
    }

    public void sendInviteMessage() {
        TextComponent content = new TextComponent(sender.getDisplayName() + " has challenged you to a game of " + gameType.getDisplayName());
        TextComponent accept = new TextComponent("[ /Accept ]");
        TextComponent decline = new TextComponent("[ /Decline ]");
        TextComponent spacer = new TextComponent(" ");

        accept.setColor(ChatColor.BLUE);
        decline.setColor(ChatColor.RED);

        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accept " + this.getInviteId().toString()));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to Accept")));
        decline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/decline " + this.getInviteId().toString()));
        decline.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to Decline")));

        BaseComponent[] component = new ComponentBuilder().append("     ").append(accept).append("     ").append(decline).create();

        receiver.spigot().sendMessage(spacer);
        receiver.spigot().sendMessage(content);
        receiver.spigot().sendMessage(component);
        receiver.spigot().sendMessage(spacer);
    }
}
enum InviteState{
    ACCEPTED,
    DECLINED,
    WAITING;
}
