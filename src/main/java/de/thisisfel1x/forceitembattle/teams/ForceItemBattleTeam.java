package de.thisisfel1x.forceitembattle.teams;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.utils.FoundItemData;
import de.thisisfel1x.forceitembattle.utils.ItemRegistry;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ForceItemBattleTeam {

    private final int teamId;
    private final String teamName;
    private final TextColor teamColor;
    private final List<GamePlayer> teamMembers;
    private final int maxTeamSize = 2;

    private List<ItemStack> itemsToFind;
    private int currentItemIndex;
    private final List<FoundItemData> foundItems;

    private final BossBar bossBar;
    private final Inventory backpack;

    public ForceItemBattleTeam(int teamId, String teamName, TextColor teamColor) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamColor = teamColor;
        this.teamMembers = new ArrayList<>();

        this.foundItems = new ArrayList<>();

        this.bossBar = BossBar.bossBar(Component.text("IDLE"), 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        this.backpack = Bukkit.createInventory(null, InventoryType.CHEST, Component.text("Backpack"));
    }

    public void setupForGame() {
        this.itemsToFind = ItemRegistry.getNewShuffledItemList();
        this.currentItemIndex = 0;
        this.foundItems.clear();
    }

    public ItemStack getCurrentItem() {
        if (itemsToFind == null || currentItemIndex >= itemsToFind.size()) {
            return new ItemStack(Material.BARRIER);
        }
        return itemsToFind.get(currentItemIndex);
    }

    public void advanceToNextItem(boolean hasUsedJoker) {
        ItemStack foundItem = getCurrentItem();
        this.foundItems.add(new FoundItemData(foundItem, System.currentTimeMillis(), hasUsedJoker));
        currentItemIndex++;
    }

    public Audience getTeamAudience() {
        List<Player> onlinePlayers = this.teamMembers.stream()
                .map(GamePlayer::getPlayer)
                //.filter(Objects::nonNull)
                .toList();
        return Audience.audience(onlinePlayers);
    }

    public void addPlayer(GamePlayer gamePlayer) {
        if (this.teamMembers.contains(gamePlayer)) {
            gamePlayer.getPlayer().sendMessage(ForceItemBattle.getInstance().getPrefix().append(
                    Component.text("Du bist bereits in diesem Team", NamedTextColor.RED)
            ));
            return;
        }

        if (this.isFull()) {
            gamePlayer.getPlayer().sendMessage(ForceItemBattle.getInstance().getPrefix().append(
                    Component.text("Dieses Team ist bereits voll", NamedTextColor.RED)
            ));
            return;
        }

        this.teamMembers.add(gamePlayer);
        gamePlayer.setTeam(this);

        Component joinMessage = ForceItemBattle.getInstance().getPrefix().append(
                Component.text(gamePlayer.getPlayer().getName(), teamColor)
                        .append(Component.text(" hat dein Team betreten", NamedTextColor.GRAY))
        );
        this.broadcastTeamMessage(joinMessage);

        gamePlayer.getPlayer().showBossBar(this.bossBar);

        ForceItemBattle.getInstance().getForceItemBattleScoreboardManager().addPlayerToTeam(gamePlayer);
    }

    public void removePlayer(GamePlayer gamePlayer) {
        if (this.teamMembers.remove(gamePlayer)) {
            gamePlayer.setTeam(null);

            Component leaveMessage = ForceItemBattle.getInstance().getPrefix().append(
                    Component.text(gamePlayer.getPlayer().getName(), teamColor)
                            .append(Component.text(" hat dein Team verlassen", NamedTextColor.GRAY))
            );
            this.broadcastTeamMessage(leaveMessage);

            gamePlayer.getPlayer().hideBossBar(this.bossBar);

            ForceItemBattle.getInstance().getForceItemBattleScoreboardManager().removePlayerFromTeam(gamePlayer);
        }
    }

    public void broadcastTeamMessage(Component message) {
        this.getTeamAudience().sendMessage(message);
    }

    public void updateBossBar() {
        if (ForceItemBattle.getInstance().getIconManager().isLoaded()) {
            Component icon = ForceItemBattle.getInstance().getIconManager().getIconSafe("fib_" + this.getCurrentItem().getType().name().toLowerCase());
            this.bossBar.name(
                    icon.append(Component.text(" "))
                            .append(Component.translatable(this.getCurrentItem().translationKey()))
            );
            //System.out.println("fib_" + this.getCurrentItem().getType().name().toLowerCase());
        } else {
            String materialKey = this.getCurrentItem().getType().getKey().toString();

            String texturePath = ForceItemBattle.getInstance().getTextureMap().getOrDefault(materialKey, "item/barrier");

            Component sprite = Component.object(ObjectContents.sprite(Key.key(texturePath)));

            this.bossBar.name(
                    sprite.append(Component.text(" "))
                            .append(Component.translatable(this.getCurrentItem().translationKey()))
            );
        }
    }

    public void clearBossBar() {
        this.teamMembers.forEach(gamePlayer -> gamePlayer.getPlayer().hideBossBar(this.bossBar));
    }

    public boolean isFull() {
        return this.teamMembers.size() >= this.maxTeamSize;
    }

    public List<GamePlayer> getTeamMembers() {
        return teamMembers;
    }

    public String getTeamName() {
        return teamName;
    }

    public TextColor getTeamColor() {
        return teamColor;
    }

    public int getCurrentTeamSize() {
        return this.teamMembers.size();
    }

    public int getMaxTeamSize() {
        return maxTeamSize;
    }

    public List<FoundItemData> getFoundItems() {
        return foundItems;
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public Inventory getBackpack() {
        return backpack;
    }

    public int getTeamId() {
        return teamId;
    }

    @Override
    public String toString() {
        return "Team{" +
                "teamName='" + teamName + '\'' +
                ", teamColor=" + teamColor +
                ", teamMembers=" + teamMembers +
                ", maxTeamSize=" + maxTeamSize +
                '}';
    }

    public void addJokersToInventory() {
        ItemStack joker = ItemBuilder.from(Material.BARRIER)
                .amount(ForceItemBattle.getInstance().getJokerAmount())
                .name(Component.text("Joker", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                .build();

        this.getTeamMembers().forEach(gamePlayer -> gamePlayer.getPlayer().getInventory().addItem(joker));
    }
}
