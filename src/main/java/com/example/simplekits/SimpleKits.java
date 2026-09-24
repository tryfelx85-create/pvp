package com.example.simplekits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SimpleKits extends JavaPlugin implements Listener {

    /** Anti-spam delay between kit claims, in milliseconds. Set to 0 to disable. */
    private static final long COOLDOWN_MS = 5_000L;

    private final Map<UUID, Long> lastClaim = new HashMap<>();

    private ArenaManager arenaManager;

    /** Marker holder so we can recognise our menu without relying on the title. */
    private static final class KitMenu implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        arenaManager = new ArenaManager(this);
        getLogger().info("SimpleKits enabled.");
    }

    // ------------------------------------------------------------ command

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "kit" -> onKitCommand(sender, args);
            case "arena" -> onArenaCommand(sender, args);
            default -> false;
        };
    }

    private boolean onKitCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("simplekits.use")) {
            player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            openMenu(player);
            return true;
        }

        int kit;
        try {
            kit = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            kit = 0;
        }
        if (kit < 1 || kit > 3) {
            player.sendMessage(Component.text("Usage: /kit [1|2|3]", NamedTextColor.RED));
            return true;
        }
        claim(player, kit);
        return true;
    }

    private boolean onArenaCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        String sub = args.length == 0 ? "tp" : args[0].toLowerCase();
        switch (sub) {
            case "tp", "join" -> {
                if (!player.hasPermission("simplekits.arena.use")) {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    return true;
                }
                arenaManager.teleport(player);
                player.sendMessage(Component.text("Teleported to the arena.", NamedTextColor.GREEN));
            }
            case "create" -> {
                if (!player.hasPermission("simplekits.arena.admin")) {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    return true;
                }
                arenaManager.getOrCreateArenaWorld();
                player.sendMessage(Component.text("Arena world is ready.", NamedTextColor.GREEN));
            }
            case "reset" -> {
                if (!player.hasPermission("simplekits.arena.admin")) {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    return true;
                }
                arenaManager.resetPlatform();
                player.sendMessage(Component.text("Arena platform reset.", NamedTextColor.GREEN));
            }
            default -> player.sendMessage(Component.text("Usage: /arena [tp|create|reset]", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        return switch (command.getName().toLowerCase()) {
            case "kit" -> List.of("1", "2", "3");
            case "arena" -> List.of("tp", "create", "reset");
            default -> List.of();
        };
    }

    // ------------------------------------------------------------ menu

    private void openMenu(Player player) {
        KitMenu holder = new KitMenu();
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text("Choose a kit"));
        holder.inventory = inv;

        inv.setItem(2, icon(Material.COOKED_BEEF, "Kit 1 - Sword",
                "Diamond armor", "Diamond sword", "6 steaks"));
        inv.setItem(4, icon(Material.SHIELD, "Kit 2 - Axe + Shield",
                "Diamond armor", "Diamond axe", "Diamond sword", "Shield", "6 steaks"));
        inv.setItem(6, icon(Material.SPLASH_POTION, "Kit 3 - Pot",
                "Diamond armor", "Diamond sword", "Inventory full of", "Instant Health II potions"));

        player.openInventory(inv);
    }

    private ItemStack icon(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.Arrays.stream(lore)
                .map(line -> Component.text(line, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))
                .toList());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof KitMenu)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int kit = switch (event.getSlot()) {
            case 2 -> 1;
            case 4 -> 2;
            case 6 -> 3;
            default -> 0;
        };
        if (kit != 0) {
            player.closeInventory();
            claim(player, kit);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof KitMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastClaim.remove(event.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------ kits

    private void claim(Player player, int kit) {
        long now = System.currentTimeMillis();
        Long last = lastClaim.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) {
            long left = (COOLDOWN_MS - (now - last) + 999) / 1000;
            player.sendMessage(Component.text("Wait " + left + "s before taking another kit.",
                    NamedTextColor.RED));
            return;
        }
        lastClaim.put(player.getUniqueId(), now);
        giveKit(player, kit);
        player.sendMessage(Component.text("You received kit " + kit + ".", NamedTextColor.GREEN));
    }

    private void giveKit(Player player, int kit) {
        PlayerInventory inv = player.getInventory();
        inv.clear(); // replaces whatever the player was carrying/wearing

        inv.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        inv.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        inv.setBoots(new ItemStack(Material.DIAMOND_BOOTS));

        switch (kit) {
            case 1 -> {
                inv.setItem(0, new ItemStack(Material.DIAMOND_SWORD));
                inv.setItem(1, new ItemStack(Material.COOKED_BEEF, 6));
            }
            case 2 -> {
                inv.setItem(0, new ItemStack(Material.DIAMOND_AXE));
                inv.setItem(1, new ItemStack(Material.DIAMOND_SWORD));
                inv.setItem(2, new ItemStack(Material.COOKED_BEEF, 6));
                inv.setItemInOffHand(new ItemStack(Material.SHIELD));
            }
            case 3 -> {
                inv.setItem(0, new ItemStack(Material.DIAMOND_SWORD));
                for (int slot = 1; slot < 36; slot++) {
                    inv.setItem(slot, healingPotion());
                }
            }
            default -> { }
        }
        player.updateInventory();
    }

    private ItemStack healingPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(PotionType.STRONG_HEALING); // Instant Health II
        potion.setItemMeta(meta);
        return potion;
    }
}
