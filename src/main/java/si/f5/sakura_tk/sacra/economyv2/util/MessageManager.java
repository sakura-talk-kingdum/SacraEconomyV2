package si.f5.sakura_tk.sacra.economyv2.util;

import org.bukkit.ChatColor;

public final class MessageManager {
    public String get(String key) {
        return switch (key) {
            case "shop.forbidden-item" -> "&cエンチャント、NBTタグ、または使用感のあるアイテムは売買できません。";
            case "shop.inventory-full" -> "&c購入する数量をインベントリに収容できません。";
            case "shop.not-enough-money" -> "&c所持金が足りません。";
            case "shop.unavailable" -> "&cその出品はすでに購入されています。";
            case "shop.transaction-failed" -> "&c取引に失敗しました。";
            case "land.overlap" -> "&cその範囲は既存の保護エリアと重複しています。";
            case "land.purchased" -> "&a土地を購入しました。";
            default -> "&c不明なメッセージです。";
        };
    }

    public String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
