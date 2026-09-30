package si.f5.sakura_tk.sacra.economyv2;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import si.f5.sakura_tk.sacra.economyv2.command.ShopCommand;
import si.f5.sakura_tk.sacra.economyv2.command.TochiCommand;
import si.f5.sakura_tk.sacra.economyv2.database.DatabaseManager;
import si.f5.sakura_tk.sacra.economyv2.land.LandRepository;
import si.f5.sakura_tk.sacra.economyv2.shop.ShopRepository;
import si.f5.sakura_tk.sacra.economyv2.shop.ShopService;
import si.f5.sakura_tk.sacra.economyv2.util.MessageManager;

public final class SacraEconomyV2 extends JavaPlugin {
    private DatabaseManager database;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("lang/ja.yml", false);

        RegisteredServiceProvider<Economy> provider =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null || provider.getProvider() == null) {
            getLogger().severe("Vault Economy provider が見つかりません。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        database = new DatabaseManager(this);
        database.initialize();

        MessageManager messages = new MessageManager();
        ShopRepository shops = new ShopRepository(database);
        LandRepository lands = new LandRepository(database);

        getCommand("shop").setExecutor(
                new ShopCommand(new ShopService(this, shops, provider.getProvider(), messages)));
        getCommand("tochi").setExecutor(
                new TochiCommand(this, lands, messages));

        getLogger().info("SacraEconomyV2 enabled.");
    }

    @Override
    public void onDisable() {
        if (database != null) database.close();
    }
}
