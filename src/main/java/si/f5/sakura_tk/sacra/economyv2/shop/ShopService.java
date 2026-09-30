package si.f5.sakura_tk.sacra.economyv2.shop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import si.f5.sakura_tk.sacra.economyv2.item.ItemValidator;
import si.f5.sakura_tk.sacra.economyv2.util.MessageManager;

import java.sql.Connection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopService {
    private final JavaPlugin plugin;
    private final ShopRepository repository;
    private final Economy economy;
    private final MessageManager messages;
    private final ConcurrentHashMap<UUID, Boolean> purchasing = new ConcurrentHashMap<>();

    public ShopService(JavaPlugin plugin, ShopRepository repository,
                       Economy economy, MessageManager messages) {
        this.plugin = plugin;
        this.repository = repository;
        this.economy = economy;
        this.messages = messages;
    }

    public void buy(Player player, String materialName) {
        final Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(messages.color("&c不明なMaterialです。"));
            return;
        }

        UUID buyer = player.getUniqueId();
        if (purchasing.putIfAbsent(buyer, Boolean.TRUE) != null) {
            player.sendMessage(messages.color("&c購入処理が進行中です。"));
            return;
        }

        /*
         * 最初にDBをロックするのではなく、最初に購入候補を読む。
         * その後Main Threadで「購入数量を完全に収容できるか」を確認する。
         */
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ShopRepository.Auction preview;

                try (Connection c = repository.database().connection()) {
                    preview = repository.findCheapestLocked(c, material.name());
                }

                if (preview == null) {
                    finish(player, messages.get("shop.unavailable"));
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!canFit(player, material, preview.amount())) {
                        player.sendMessage(messages.color(messages.get("shop.inventory-full")));
                        purchasing.remove(buyer);
                        return;
                    }

                    completePurchase(player, material, buyer);
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Purchase preparation failed: " + e.getMessage());
                finish(player, messages.get("shop.transaction-failed"));
            }
        });
    }

    private boolean canFit(Player player, Material material, int amount) {
        if (amount <= 0) return false;

        ItemStack wanted = new ItemStack(material, amount);
        int remaining = amount;

        // まず既存の同一スタックへ入る量を計算。
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType().isAir()) continue;
            if (!stack.isSimilar(wanted)) continue;

            remaining -= Math.min(
                stack.getMaxStackSize() - stack.getAmount(),
                remaining
            );
            if (remaining <= 0) return true;
        }

        // 次に空きスロットへ入る量を計算。
        int max = wanted.getMaxStackSize();
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                remaining -= Math.min(max, remaining);
                if (remaining <= 0) return true;
            }
        }

        return remaining <= 0;
    }

    private void completePurchase(Player player, Material material, UUID buyer) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection c = repository.database().connection()) {
                c.setAutoCommit(false);

                try {
                    // ここで本番の行ロックを取得する。
                    ShopRepository.Auction auction =
                            repository.findCheapestLocked(c, material.name());

                    if (auction == null) {
                        c.rollback();
                        finish(player, messages.get("shop.unavailable"));
                        return;
                    }

                    if (!economy.has(player, auction.price().doubleValue())) {
                        c.rollback();
                        finish(player, messages.get("shop.not-enough-money"));
                        return;
                    }

                    if (!repository.deactivateAndRecord(c, auction, buyer)) {
                        c.rollback();
                        finish(player, messages.get("shop.transaction-failed"));
                        return;
                    }

                    c.commit();

                    // DB確定後、Main Threadへ戻してアイテムを付与する。
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            ItemStack item = new ItemStack(material, auction.amount());
                            var leftovers = player.getInventory().addItem(item);

                            /*
                             * 事前確認済みなので通常ここには来ない。
                             * 万一残った場合も消失させずワールドへ返す。
                             */
                            leftovers.values().forEach(stack ->
                                player.getWorld().dropItemNaturally(
                                    player.getLocation(), stack));

                            /*
                             * VaultはMySQL transactionの外部なので、ここは別処理。
                             * 実運用ではEconomy provider側のatomic性も確認する。
                             */
                            economy.withdrawPlayer(player, auction.price().doubleValue());
                            economy.depositPlayer(
                                Bukkit.getOfflinePlayer(auction.seller()),
                                auction.price().doubleValue());

                            player.sendMessage(messages.color(
                                "&a" + material.name() + " を " +
                                auction.amount() + "個、" +
                                auction.price().toPlainString() + "円で購入しました。"));
                        } finally {
                            purchasing.remove(buyer);
                        }
                    });
                } catch (Exception e) {
                    c.rollback();
                    throw e;
                }
            } catch (Exception e) {
                plugin.getLogger().warning(
                    "Purchase transaction failed: " + e.getMessage());
                finish(player, messages.get("shop.transaction-failed"));
            }
        });
    }

    public void sell(Player player, ItemStack item, java.math.BigDecimal price) {
        if (ItemValidator.isForbiddenItem(item)) {
            player.sendMessage(messages.color(messages.get("shop.forbidden-item")));
            return;
        }

        ItemStack stored = item.clone();
        UUID seller = player.getUniqueId();
        player.getInventory().removeItem(item);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                repository.createAuction(
                    seller,
                    stored.getType().name(),
                    stored.getAmount(),
                    price
                );

                Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(messages.color(
                        "&a" + stored.getType().name() + " を " +
                        stored.getAmount() + "個、" +
                        price.toPlainString() + "円で出品しました。")));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    var leftovers = player.getInventory().addItem(stored);
                    leftovers.values().forEach(stack ->
                        player.getWorld().dropItemNaturally(
                            player.getLocation(), stack));
                    player.sendMessage(messages.color(
                        messages.get("shop.transaction-failed")));
                });
            }
        });
    }

    private void finish(Player player, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage(messages.color(message));
            purchasing.remove(player.getUniqueId());
        });
    }
}
