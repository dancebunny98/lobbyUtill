package ru.dancebunny98.lobbyUtill;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

public class BowListener implements Listener {

    private final LobbyUtill plugin;

    public BowListener(LobbyUtill plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getInventory().setItem(plugin.getConfig().getInt("settings.bow.inventory-slot", 0), plugin.getTeleportBow());
        player.getInventory().setItem(plugin.getConfig().getInt("settings.arrow.inventory-slot", 40), plugin.getArrow());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;

        // Check if the clicked item is the teleport bow or arrow and prevent movement
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == org.bukkit.event.inventory.InventoryType.PLAYER) {
            ItemStack clickedItem = event.getCurrentItem();

            boolean isBow = clickedItem.isSimilar(plugin.getTeleportBow());
            boolean isArrow = clickedItem.isSimilar(plugin.getArrow());

            if (isBow && plugin.getConfig().getBoolean("settings.bow.prevent-movement", true)) {
                event.setCancelled(true);
            }

            if (isArrow && plugin.getConfig().getBoolean("settings.arrow.prevent-movement", true)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (droppedItem.isSimilar(plugin.getTeleportBow()) && plugin.getConfig().getBoolean("settings.bow.prevent-drop", true)) {
            event.setCancelled(true);
        }
        if (droppedItem.isSimilar(plugin.getArrow()) && plugin.getConfig().getBoolean("settings.arrow.prevent-drop", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!plugin.getConfig().getBoolean("settings.teleport.enabled", true)) return;

        if (event.getBow().isSimilar(plugin.getTeleportBow())) {
            long cooldownTime = plugin.getConfig().getLong("settings.teleport.cooldown", 5);

            if (cooldownTime > 0) {
                long cooldownEnd = plugin.getCooldowns().getOrDefault(player.getUniqueId(), 0L);

                if (System.currentTimeMillis() < cooldownEnd) {
                    // Player is on cooldown. The action bar message is now handled by a repeating task,
                    // so we only need to cancel the event.
                    event.setCancelled(true);
                    return;
                }

                // Player can shoot, set cooldown and handle arrow visibility
                plugin.getCooldowns().put(player.getUniqueId(), System.currentTimeMillis() + (cooldownTime * 1000));
                final int arrowSlot = plugin.getConfig().getInt("settings.arrow.inventory-slot", 40);

                // Using a 1-tick delay to remove the arrow, allowing the shot to fire first
                plugin.getServer().getScheduler().runTask(plugin, () -> player.getInventory().clear(arrowSlot));

                // Schedule arrow to be returned after cooldown
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.getInventory().setItem(arrowSlot, plugin.getArrow());
                    }
                }, cooldownTime * 20L); // Convert seconds to server ticks
            }

            event.getProjectile().setMetadata("teleport_arrow", new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        Arrow arrow = (Arrow) event.getEntity();
        if (!arrow.hasMetadata("teleport_arrow")) return;
        if (!(arrow.getShooter() instanceof Player)) return;

        Player player = (Player) arrow.getShooter();
        Location loc = arrow.getLocation();
        arrow.remove();

        // Arrow is now returned via a delayed task in onEntityShootBow to handle the cooldown
        if (plugin.getConfig().getBoolean("settings.teleport.preserve-look-direction", true)) {
            loc.setPitch(player.getLocation().getPitch());
            loc.setYaw(player.getLocation().getYaw());
        }

        player.teleport(loc);
    }

    @EventHandler
    public void onPlayerPickupArrow(PlayerPickupArrowEvent event) {
        // Prevent picking up any arrows to avoid clutter and exploits
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // A small delay might be needed for some servers
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.getInventory().setItem(plugin.getConfig().getInt("settings.bow.inventory-slot", 0), plugin.getTeleportBow());
            player.getInventory().setItem(plugin.getConfig().getInt("settings.arrow.inventory-slot", 40), plugin.getArrow());
        }, 1L);
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (event.getItem().isSimilar(plugin.getTeleportBow())) {
            if (plugin.getConfig().getBoolean("settings.bow.unbreakable", true)) {
                event.setCancelled(true);
            }
        }
    }
} 