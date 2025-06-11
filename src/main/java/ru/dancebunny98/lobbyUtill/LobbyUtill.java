package ru.dancebunny98.lobbyUtill;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LobbyUtill extends JavaPlugin {

    private ItemStack teleportBow;
    private ItemStack arrow;
    private MessageHandler messageHandler;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private BukkitTask cooldownActionBarTask;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        this.messageHandler = new MessageHandler(this);
        reloadPlugin();

        getServer().getPluginManager().registerEvents(new BowListener(this), this);

        getLogger().info("LobbyUtill has been enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (cooldownActionBarTask != null && !cooldownActionBarTask.isCancelled()) {
            cooldownActionBarTask.cancel();
        }
        getLogger().info("LobbyUtill has been disabled!");
    }

    public void reloadPlugin() {
        reloadConfig();
        messageHandler.loadMessages();
        loadItems();

        if (cooldownActionBarTask != null && !cooldownActionBarTask.isCancelled()) {
            cooldownActionBarTask.cancel();
        }
        startCooldownActionBarTask();
    }

    private void startCooldownActionBarTask() {
        cooldownActionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    long cooldownEnd = getCooldowns().getOrDefault(player.getUniqueId(), 0L);

                    if (System.currentTimeMillis() < cooldownEnd) {
                        // Player is on cooldown, check if holding bow
                        if (getTeleportBow().isSimilar(player.getInventory().getItemInMainHand())) {
                            long timeLeft = (cooldownEnd - System.currentTimeMillis()) / 1000;
                            String message = getMessageHandler().getString("cooldown.message", "&cYou are on cooldown for %cooldown% seconds.")
                                    .replace("%cooldown%", String.valueOf(timeLeft + 1));
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L); // Check every second
    }

    private void loadItems() {
        FileConfiguration config = getConfig();

        // Load Bow
        String bowName = messageHandler.getString("bow.name", "&aTeleport Bow");
        List<String> bowLore = messageHandler.getStringList("bow.lore");

        teleportBow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = teleportBow.getItemMeta();
        bowMeta.setDisplayName(bowName);
        bowMeta.setLore(bowLore);
        if (config.getBoolean("settings.bow.unbreakable", true)) {
            bowMeta.setUnbreakable(true);
            bowMeta.addEnchant(Enchantment.INFINITY, 1, true);
            bowMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            bowMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        teleportBow.setItemMeta(bowMeta);


        // Load Arrow
        arrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.setDisplayName(messageHandler.getString("arrow.name", "&fArrow"));
        arrow.setItemMeta(arrowMeta);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("lobbyutill")) {
            if (!sender.hasPermission("lobbyutill.reload")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            reloadPlugin();
            sender.sendMessage(ChatColor.GREEN + "LobbyUtill configuration reloaded.");
            return true;
        }
        return false;
    }

    public ItemStack getTeleportBow() {
        return teleportBow;
    }

    public ItemStack getArrow() {
        return arrow;
    }

    public Map<UUID, Long> getCooldowns() {
        return cooldowns;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }
}
