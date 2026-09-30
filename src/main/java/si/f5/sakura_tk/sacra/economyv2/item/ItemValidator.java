package si.f5.sakura_tk.sacra.economyv2.item;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

public final class ItemValidator {
    private ItemValidator() {}

    public static boolean isForbiddenItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return true;
        if (!item.getEnchantments().isEmpty()) return true;

        if (item.getItemMeta() instanceof Damageable damageable && damageable.hasDamage()) {
            return true;
        }

        if (item.hasItemMeta()) {
            var current = item.getItemMeta();
            var defaults = item.getType().getDefaultItemStack().getItemMeta();
            if (current != null && !current.equals(defaults)) return true;
        }
        return false;
    }
}
