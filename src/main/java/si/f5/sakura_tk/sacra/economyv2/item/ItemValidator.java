package si.f5.sakura_tk.sacra.economyv2.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemValidator {

    private ItemValidator() {
    }

    public static boolean isForbiddenItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return true;
        }

        if (item.getAmount() <= 0) {
            return true;
        }

        // エンチャント禁止
        if (!item.getEnchantments().isEmpty()) {
            return true;
        }

        // ダメージ禁止
        if (item.getType().getMaxDurability() > 0) {
            if (item.getDurability() != 0) {
                return true;
            }
        }

        Material material = item.getType();

        // 完全な素のItemStackを生成
        ItemStack pristine = new ItemStack(material, 1);

        ItemMeta actualMeta = item.getItemMeta();
        ItemMeta pristineMeta = pristine.getItemMeta();

        // ItemMetaが改変されている場合は禁止
        if (actualMeta != null && !actualMeta.equals(pristineMeta)) {
            return true;
        }

        return false;
    }

    public static boolean isValid(ItemStack item) {
        return !isForbiddenItem(item);
    }
}
