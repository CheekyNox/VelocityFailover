package pl.blixy.velocityFailover.command;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import pl.blixy.velocityFailover.VelocityFailover;

/** {@code /failoverreload}: re-reads config.yml and restarts the failover with it. */
public final class ReloadCommand implements SimpleCommand {

    public static final String PERMISSION = "velocityfailover.reload";

    private static final Component RELOADED = MiniMessage.miniMessage().deserialize("<green>[Failover] Configuration reloaded successfully.");

    private final VelocityFailover plugin;

    public ReloadCommand(VelocityFailover plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        plugin.reload();
        invocation.source().sendMessage(RELOADED);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION);
    }
}
