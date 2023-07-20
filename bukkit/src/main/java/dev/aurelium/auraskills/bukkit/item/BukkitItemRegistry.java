package dev.aurelium.auraskills.bukkit.item;

import dev.aurelium.auraskills.api.item.ItemCategory;
import dev.aurelium.auraskills.api.item.ItemFilter;
import dev.aurelium.auraskills.api.item.ItemFilterMeta;
import dev.aurelium.auraskills.api.item.PotionData;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.user.BukkitUser;
import dev.aurelium.auraskills.bukkit.util.ItemUtils;
import dev.aurelium.auraskills.common.item.ItemRegistry;
import dev.aurelium.auraskills.common.message.type.LevelerMessage;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.data.KeyIntPair;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BukkitItemRegistry implements ItemRegistry {

    private final AuraSkills plugin;
    private final Map<NamespacedId, ItemStack> items = new HashMap<>();
    private final BukkitSourceMenuItems sourceMenuItems;

    public BukkitItemRegistry(AuraSkills plugin) {
        this.plugin = plugin;
        this.sourceMenuItems = new BukkitSourceMenuItems(plugin);
    }

    public void register(NamespacedId key, ItemStack item) {
        items.put(key, item.clone());
    }

    public void unregister(NamespacedId key) {
        items.remove(key);
    }

    @Nullable
    public ItemStack getItem(NamespacedId key) {
        ItemStack item = items.get(key);
        if (item != null) {
            return item.clone();
        } else {
            return null;
        }
    }

    @Override
    public boolean containsItem(NamespacedId key) {
        return items.containsKey(key);
    }

    @Override
    public void giveItem(User user, NamespacedId key, int amount) {
        Player player = ((BukkitUser) user).getPlayer();
        ItemStack item = getItem(key);

        if (item == null) {
            return;
        }

        ItemStack leftoverItem = ItemUtils.addItemToInventory(player, item); // Attempt item give
        // Handle items that could not fit in the inventory
        if (leftoverItem != null) {
            // Add unclaimed item key and amount to player data
            user.getUnclaimedItems().add(new KeyIntPair(key.toString(), leftoverItem.getAmount()));
            // Notify player
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    player.sendMessage(plugin.getPrefix(user.getLocale()) + plugin.getMsg(LevelerMessage.UNCLAIMED_ITEM, user.getLocale())), 1);
        }
    }

    @Override
    public int getItemAmount(NamespacedId key) {
        ItemStack item = getItem(key);
        if (item == null) {
            return 0;
        }
        return item.getAmount();
    }

    @Override
    public @Nullable String getEffectiveItemName(NamespacedId key) {
        ItemStack item = getItem(key);
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            return meta.hasDisplayName() ? meta.getDisplayName() : meta.getLocalizedName();
        }
        return null;
    }

    public BukkitSourceMenuItems getSourceMenuItems() {
        return sourceMenuItems;
    }

    public boolean passesFilter(ItemStack item, ItemFilter filter) {
        // Check materials
        String[] materials = filter.materials();
        if (materials != null) {
            if (!TextUtil.contains(materials, item.getType().toString())) {
                return false;
            }
        }
        // Check excluded materials
        String[] excludedMaterials = filter.excludedMaterials();
        if (excludedMaterials != null) {
            for (String excludedMaterial : excludedMaterials) {
                if (excludedMaterial.toUpperCase(Locale.ROOT).equals(item.getType().toString())) {
                    return false;
                }
            }
        }
        // Check ItemCategory
        ItemCategory category = filter.category();
        if (category != null) {
            if (!getItemCategories(item, item.getType()).contains(category)) {
                return false;
            }
        }
        // Check meta
        return passesItemMetaFilter(item, filter);
    }

    private boolean passesItemMetaFilter(ItemStack item, ItemFilter filter) {
        ItemFilterMeta filterMeta = filter.meta();
        if (filterMeta == null) {
            return true;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return true;
        }
        if (filterMeta.displayName() != null) {
            if (!itemMeta.getDisplayName().equals(filterMeta.displayName())) {
                return false;
            }
        }
        List<String> filterLore = filterMeta.lore();
        if (filterLore != null && filterLore.size() > 0) {
            if (!filterLore.equals(itemMeta.getLore())) {
                return false;
            }
        }
        PotionData potionData = filterMeta.potionData();
        if (potionData != null) {
            if (!(itemMeta instanceof PotionMeta potionMeta)) {
                return false;
            }
            org.bukkit.potion.PotionData basePotionData = potionMeta.getBasePotionData();
            String[] types = potionData.types();
            if (types != null) {
                if (!TextUtil.contains(types, basePotionData.getType().toString())) {
                                        return false;
                }
            }
            String[] excludedTypes = potionData.excludedTypes();
            if (excludedTypes != null) {
                if (TextUtil.contains(excludedTypes, basePotionData.getType().toString())) {
                    return false;
                }
            }
            if (potionData.extended() != basePotionData.isExtended()) {
                return false;
            }
            if (potionData.upgraded() != basePotionData.isUpgraded()) {
                return false;
            }
        }
        return true;
    }


    private Set<ItemCategory> getItemCategories(ItemStack item, Material mat) {
        Set<ItemCategory> found = new HashSet<>();
        String name = mat.toString();
        if (name.contains("_SWORD") || mat == Material.BOW || mat == Material.CROSSBOW || mat == Material.TRIDENT) {
            found.add(ItemCategory.WEAPON);
        } else if (name.contains("_HELMET") || name.contains("_CHESTPLATE") || name.contains("_LEGGINGS") || name.contains("_BOOTS") || mat == Material.ELYTRA) {
            found.add(ItemCategory.ARMOR);
        } else if (name.contains("_PICKAXE") || name.contains("_AXE") || name.contains("_SHOVEL") || name.contains("_HOE") ||
                mat == Material.SHEARS || mat == Material.FISHING_ROD || mat == Material.FLINT_AND_STEEL) {
            found.add(ItemCategory.TOOL);
        }
        if (mat == Material.LILY_PAD || mat == Material.BOWL || mat == Material.LEATHER || mat == Material.LEATHER_BOOTS || mat == Material.ROTTEN_FLESH ||
                mat == Material.STICK || mat == Material.STRING || mat == Material.BONE || mat == Material.INK_SAC || mat == Material.TRIPWIRE_HOOK) {
            found.add(ItemCategory.FISHING_JUNK);
        }
        if (mat == Material.FISHING_ROD) {
            if (item.getEnchantments().size() == 0) {
                found.add(ItemCategory.FISHING_JUNK);
            } else {
                found.add(ItemCategory.FISHING_TREASURE);
            }
        }
        // Check water bottle
        if (mat == Material.POTION) {
            if (item.getItemMeta() instanceof PotionMeta potionMeta) {
                if (potionMeta.getBasePotionData().getType() == PotionType.WATER) {
                    found.add(ItemCategory.FISHING_JUNK);
                }
            }
        }
        if (mat == Material.BOW || mat == Material.ENCHANTED_BOOK || mat == Material.NAME_TAG || mat == Material.NAUTILUS_SHELL || mat == Material.SADDLE) {
            found.add(ItemCategory.FISHING_TREASURE);
        }
        return found;
    }

}
