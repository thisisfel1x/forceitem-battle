package de.thisisfel1x.forceitembattle.teams;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.utils.FoundItemData;
import de.thisisfel1x.forceitembattle.utils.ItemRegistry;
import de.thisisfel1x.forceitembattle.utils.Items;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ForceItemBattleTeam {

    public static final int MAX_JOKER_STACK = 64;

    private final int teamId;
    private final String teamName;
    private final TextColor teamColor;
    private final List<GamePlayer> teamMembers;
    private final int maxTeamSize;

    private List<ItemStack> itemsToFind;
    private int currentItemIndex;
    private final List<FoundItemData> foundItems;

    private final BossBar bossBar;
    private final Inventory backpack;

    public ForceItemBattleTeam(int teamId, String teamName, TextColor teamColor, int maxTeamSize) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamColor = teamColor;
        this.maxTeamSize = maxTeamSize;
        this.teamMembers = new ArrayList<>();

        this.foundItems = new ArrayList<>();

        // Shared by all members - translatable, so every client sees its own language
        this.bossBar = BossBar.bossBar(Message.LOBBY_BOSSBAR.translatable(), 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        this.backpack = Bukkit.createInventory(null, InventoryType.CHEST, Message.BACKPACK_TITLE.translatable());
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

    /**
     * @return {@code true} while there is still an item left that the team can search for
     */
    public boolean hasItemsLeft() {
        return itemsToFind != null && currentItemIndex < itemsToFind.size();
    }

    public void advanceToNextItem(boolean hasUsedJoker) {
        if (!this.hasItemsLeft()) return;

        ItemStack foundItem = getCurrentItem();
        this.foundItems.add(new FoundItemData(foundItem, System.currentTimeMillis(), hasUsedJoker));
        currentItemIndex++;
    }

    /**
     * Timestamp of the most recently found item, used as tie-breaker in the ranking
     * (the team that reached its score earlier wins)
     */
    public long getLastFoundTimestamp() {
        return this.foundItems.isEmpty() ? Long.MAX_VALUE : this.foundItems.getLast().timestamp();
    }

    public List<Player> getOnlineMembers() {
        return this.teamMembers.stream()
                .map(GamePlayer::getPlayer)
                .filter(Objects::nonNull)
                .toList();
    }

    public Audience getTeamAudience() {
        return Audience.audience(this.getOnlineMembers());
    }

    /**
     * @return {@code true} if the player is now a member of this team
     */
    public boolean addPlayer(GamePlayer gamePlayer) {
        if (this.teamMembers.contains(gamePlayer)) {
            gamePlayer.sendMessage(Message.TEAM_ALREADY_MEMBER);
            return false;
        }

        if (this.isFull()) {
            gamePlayer.sendMessage(Message.TEAM_FULL);
            return false;
        }

        this.teamMembers.add(gamePlayer);
        gamePlayer.setTeam(this);

        this.broadcastTeamMessage(Message.TEAM_MEMBER_JOINED,
                Placeholder.component("player", Component.text(gamePlayer.getName(), teamColor)));

        gamePlayer.ifOnline(player -> player.showBossBar(this.bossBar));

        ForceItemBattle.getInstance().getForceItemBattleScoreboardManager().addPlayerToTeam(gamePlayer);
        return true;
    }

    public void removePlayer(GamePlayer gamePlayer) {
        if (this.teamMembers.remove(gamePlayer)) {
            gamePlayer.setTeam(null);

            this.broadcastTeamMessage(Message.TEAM_MEMBER_LEFT,
                    Placeholder.component("player", Component.text(gamePlayer.getName(), teamColor)));

            gamePlayer.ifOnline(player -> player.hideBossBar(this.bossBar));

            ForceItemBattle.getInstance().getForceItemBattleScoreboardManager().removePlayerFromTeam(gamePlayer);
        }
    }

    /**
     * Sends the message to every online member in its language
     */
    public void broadcastTeamMessage(Message message, TagResolver... placeholders) {
        ForceItemBattle.getInstance().getLanguageManager().send(this.getOnlineMembers(), message, placeholders);
    }

    /**
     * The team name in the team color, e.g. for placeholders
     */
    public Component getDisplayName() {
        return Component.text(this.teamName, this.teamColor);
    }

    public void updateBossBar() {
        ItemStack currentItem = this.getCurrentItem();
        Component icon = ForceItemBattle.getInstance().getIconManager().getIconWithSpacing(currentItem.getType());

        this.bossBar.name(icon.append(Component.translatable(currentItem.translationKey())));
    }

    public void clearBossBar() {
        this.getOnlineMembers().forEach(player -> player.hideBossBar(this.bossBar));
    }

    /**
     * Updates the item shown above the head of every online team member
     */
    public void updateArmorstands() {
        this.teamMembers.forEach(GamePlayer::updateArmorstandItem);
    }

    /**
     * Hands out the jokers for this game
     *
     * @param amount  joker amount from /start (0 - {@value #MAX_JOKER_STACK})
     * @param perTeam if {@code true} the amount is given once per team, otherwise to every member
     */
    public void addJokersToInventory(int amount, boolean perTeam) {
        int clampedAmount = Math.max(0, Math.min(MAX_JOKER_STACK, amount));
        if (clampedAmount == 0) return;

        List<Player> receivers = this.getOnlineMembers();
        if (receivers.isEmpty()) return;

        if (perTeam) {
            // Spread the team's jokers as evenly as possible across its members
            int base = clampedAmount / receivers.size();
            int remainder = clampedAmount % receivers.size();
            for (int i = 0; i < receivers.size(); i++) {
                int share = base + (i < remainder ? 1 : 0);
                if (share > 0) {
                    receivers.get(i).getInventory().addItem(createJoker(receivers.get(i), share));
                }
            }
        } else {
            receivers.forEach(player -> player.getInventory().addItem(createJoker(player, clampedAmount)));
        }
    }

    private static ItemStack createJoker(Player receiver, int amount) {
        // Jokers are recognized by their material, the name is only for the player
        ItemStack joker = Items.named(Material.BARRIER,
                ForceItemBattle.getInstance().getLanguageManager().render(receiver, Message.JOKER_NAME));
        joker.setAmount(amount);
        return joker;
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
                ", teamMembers=" + teamMembers.stream().map(GamePlayer::getName).toList() +
                ", maxTeamSize=" + maxTeamSize +
                '}';
    }
}
