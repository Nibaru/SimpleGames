package games.twinhead.monitorbridge.data;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CollectingCommandSender implements CommandSender {

    private final List<String> messages = new ArrayList<>();
    private final Spigot spigot = new Spigot() {};

    @Override
    public void sendMessage(@NotNull String message) {
        messages.add(message);
    }

    @Override
    public void sendMessage(@NotNull String... messages) {
        for (String msg : messages) {
            this.messages.add(msg);
        }
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String message) {
        messages.add(message);
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String... messages) {
        for (String msg : messages) {
            this.messages.add(msg);
        }
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        messages.add(PlainTextComponentSerializer.plainText().serialize(message));
    }

    @Override
    public @NotNull Component name() {
        return Component.text("ServerMonitor");
    }

    @Override
    public @NotNull String getName() {
        return "ServerMonitor";
    }

    @Override
    public @NotNull Server getServer() {
        return Bukkit.getServer();
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        return true;
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        return true;
    }

    @Override
    public boolean hasPermission(@NotNull String name) {
        return true;
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        return true;
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAttachment(@NotNull PermissionAttachment attachment) {
    }

    @Override
    public void recalculatePermissions() {
    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return Set.of();
    }

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public void setOp(boolean value) {
    }

    @Override
    public @NotNull Spigot spigot() {
        return spigot;
    }

    public String getOutput() {
        if (messages.isEmpty()) return "(no output)";
        return String.join("\n", messages);
    }
}
