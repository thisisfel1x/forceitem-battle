package de.thisisfel1x.forceitembattle.player;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.i18n.LanguageManager;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.teams.ForceItemBattleTeam;
import de.thisisfel1x.forceitembattle.utils.Items;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

public class GamePlayer {

    private static final double FROZEN_SPEED = 0.00001;
    private static final double DEFAULT_SPEED = 0.1;

    private final String name;
    private final UUID uniqueId;

    private ForceItemBattleTeam forceItemBattleTeam;

    private boolean spectator = false;

    private Entity lastPassenger;

    public GamePlayer(Player player) {
        this.name = player.getName();
        this.uniqueId = player.getUniqueId();

        this.forceItemBattleTeam = null;
    }

    /**
     * Sends the message in the player's language if it is online
     */
    public void sendMessage(Message message, TagResolver... placeholders) {
        this.ifOnline(player -> ForceItemBattle.getInstance().getLanguageManager().send(player, message, placeholders));
    }

    public String getName() {
        return name;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    /**
     * @return the Bukkit player or {@code null} if the player is currently offline
     */
    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(this.uniqueId);
    }

    public boolean isOnline() {
        return this.getPlayer() != null;
    }

    /**
     * Runs the action only if the player is currently online
     */
    public void ifOnline(Consumer<Player> action) {
        Player player = this.getPlayer();
        if (player != null) {
            action.accept(player);
        }
    }

    public ForceItemBattleTeam getTeam() {
        return forceItemBattleTeam;
    }

    public void setTeam(ForceItemBattleTeam forceItemBattleTeam) {
        this.forceItemBattleTeam = forceItemBattleTeam;
    }

    public boolean isInTeam() {
        return forceItemBattleTeam != null;
    }

    public boolean isPlayer() {
        return !this.isSpectator();
    }

    public boolean isActive() {
        return this.isOnline();
    }

    public boolean isSpectator() {
        return spectator;
    }

    public void setSpectator(boolean spectator) {
        this.spectator = spectator;
    }

    public void cleanOnJoin() {
        this.ifOnline(player -> {
            player.closeInventory();
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);

            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(10);
            player.setExp(0);
            player.setLevel(0);
        });
    }

    public void setLobbyInventory() {
        this.ifOnline(player -> {
            player.getInventory().clear();

            LanguageManager languageManager = ForceItemBattle.getInstance().getLanguageManager();
            ItemStack teamSelector = Items.named(Material.AMETHYST_SHARD,
                    languageManager.render(player, Message.LOBBY_ITEM_TEAM_SELECTOR), true);
            player.getInventory().setItem(0, teamSelector);

            if (player.hasPermission("forceitembattle.settings")) {
                ItemStack settingsItem = Items.named(Material.PAPER,
                        languageManager.render(player, Message.LOBBY_ITEM_SETTINGS), true);
                settingsItem.editMeta(itemMeta -> itemMeta.setItemModel(NamespacedKey.fromString("fib:win")));
                player.getInventory().setItem(1, settingsItem);
            }
        });
    }

    public void setSpectatorInventory() {
        this.ifOnline(player -> {
            player.getInventory().clear();

            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);

            ForceItemBattle plugin = ForceItemBattle.getInstance();
            for (GamePlayer gamePlayer : plugin.getTeamManager().getGamePlayers().values()) {
                if (!gamePlayer.isSpectator()) {
                    gamePlayer.ifOnline(other -> other.hidePlayer(plugin, player));
                }
            }

            ItemStack playerCompass = Items.named(Material.COMPASS,
                    plugin.getLanguageManager().render(player, Message.SPECTATOR_ITEM_COMPASS));
            player.getInventory().setItem(0, playerCompass);
        });
    }

    public void addEffectsForGame() {
        this.ifOnline(player -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20 * 30, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 0));
        });
    }

    public void freezePlayer() {
        if (this.isSpectator()) return;
        this.setMovementSpeed(FROZEN_SPEED);
    }

    public void unfreezePlayer() {
        if (this.isSpectator()) return;
        this.setMovementSpeed(DEFAULT_SPEED);
    }

    private void setMovementSpeed(double speed) {
        this.ifOnline(player -> {
            AttributeInstance attributeInstance = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (attributeInstance != null) {
                attributeInstance.setBaseValue(speed);
            }
        });
    }

    /**
     * (Re-)spawns the armor stand above the player's head showing the team's current item
     */
    public void updateArmorstandItem() {
        if (this.isSpectator() || !this.isInTeam()) return;

        this.removeArmorstand();

        this.ifOnline(player -> player.addPassenger(player.getWorld().spawnEntity(player.getLocation(),
                EntityType.ARMOR_STAND, CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
                    ArmorStand armorStand = (ArmorStand) entity;

                    armorStand.setInvisible(true);
                    armorStand.setInvulnerable(true);
                    // Never save the stand to disk, it is recreated whenever needed
                    armorStand.setPersistent(false);
                    armorStand.setItem(EquipmentSlot.HEAD, this.getTeam().getCurrentItem());

                    this.lastPassenger = armorStand;
                })));
    }

    /**
     * Removes the item armor stand, e.g. when the player quits or the game ends
     */
    public void removeArmorstand() {
        this.ifOnline(player -> player.getPassengers().forEach(passenger -> {
            player.removePassenger(passenger);
            if (passenger instanceof ArmorStand) {
                passenger.remove();
            }
        }));

        if (this.lastPassenger != null) {
            this.lastPassenger.remove();
            this.lastPassenger = null;
        }
    }

}
