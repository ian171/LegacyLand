package net.chen.legacyLand.item;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ItemBuilder {
    private final ItemStack itemStack;
    public ItemBuilder(@NotNull Material material){
        this.itemStack = new ItemStack(material);
    }
    public ItemBuilder name(String name){
        itemStack.getItemMeta().setDisplayName(name);
        return this;
    }
    public ItemBuilder lore(List<String> list){
        itemStack.getItemMeta().setLore(list);
        return this;
    }
    public ItemBuilder enhanced(@NotNull Enchantment enchantment, int level, boolean a){
        itemStack.getItemMeta().addEnchant(enchantment,level,a);
        return this;
    }
    public ItemMeta getItemMeta(){
        return this.itemStack.getItemMeta();
    }
}
