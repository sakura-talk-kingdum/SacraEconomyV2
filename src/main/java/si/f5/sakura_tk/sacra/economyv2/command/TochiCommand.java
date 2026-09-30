package si.f5.sakura_tk.sacra.economyv2.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import si.f5.sakura_tk.sacra.economyv2.land.LandRepository;
import si.f5.sakura_tk.sacra.economyv2.util.MessageManager;

public final class TochiCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final LandRepository lands;
    private final MessageManager messages;

    public TochiCommand(JavaPlugin plugin, LandRepository lands,
                        MessageManager messages) {
        this.plugin = plugin;
        this.lands = lands;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only.");
            return true;
        }

        if (args.length >= 2
                && args[0].equalsIgnoreCase("buy")
                && args[1].equalsIgnoreCase("chunk")) {

            int minX = (player.getLocation().getBlockX() >> 4) << 4;
            int minZ = (player.getLocation().getBlockZ() >> 4) << 4;
            int maxX = minX + 15;
            int maxZ = minZ + 15;

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    boolean created = lands.create(
                        player.getUniqueId(),
                        player.getWorld().getName(),
                        minX, maxX, minZ, maxZ,
                        false
                    );

                    Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(messages.color(
                            created
                                ? messages.get("land.purchased")
                                : messages.get("land.overlap"))));
                } catch (Exception e) {
                    plugin.getLogger().warning(
                        "Land transaction failed: " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(messages.color(
                            "&c土地購入処理に失敗しました。")));
                }
            });
            return true;
        }

        player.sendMessage("/tochi buy chunk");
        return true;
    }
}
