package si.f5.sakura_tk.sacra.economyv2.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import si.f5.sakura_tk.sacra.economyv2.shop.ShopService;

public final class ShopCommand implements CommandExecutor {
    private final ShopService shop;

    public ShopCommand(ShopService shop) {
        this.shop = shop;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only.");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("buy")) {
            shop.buy(player, args[1]);
            return true;
        }

        player.sendMessage("/shop buy <MATERIAL>");
        return true;
    }
}
