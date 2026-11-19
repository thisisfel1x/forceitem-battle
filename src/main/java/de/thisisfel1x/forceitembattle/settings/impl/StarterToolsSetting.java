package de.thisisfel1x.forceitembattle.settings.impl;

import de.thisisfel1x.forceitembattle.ForceItemBattle;
import de.thisisfel1x.forceitembattle.i18n.Message;
import de.thisisfel1x.forceitembattle.player.GamePlayer;
import de.thisisfel1x.forceitembattle.settings.BooleanSetting;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Gives every player an unbreakable diamond pickaxe and axe with Efficiency I when the game starts
 */
public class StarterToolsSetting extends BooleanSetting {

    public StarterToolsSetting() {
        super(Message.SETTINGS_STARTER_TOOLS_NAME, Message.SETTINGS_STARTER_TOOLS_DESCRIPTION, Material.DIAMOND_PICKAXE, false);
    }

    @Override
    public void applyEffect(ForceItemBattle plugin) {
        if (!this.value) return;

        for (GamePlayer gamePlayer : plugin.getTeamManager().getGamePlayers().values()) {
            if (gamePlayer.isSpectator() || !gamePlayer.isInTeam()) continue;

            gamePlayer.ifOnline(player -> player.getInventory().addItem(createTools().toArray(ItemStack[]::new)));
        }
    }

    public static List<ItemStack> createTools() {
        return List.of(createTool(Material.DIAMOND_PICKAXE), createTool(Material.DIAMOND_AXE));
    }

    private static ItemStack createTool(Material material) {
        ItemStack tool = new ItemStack(material);
        tool.editMeta(meta -> {
            meta.setUnbreakable(true);
            meta.addEnchant(Enchantment.EFFICIENCY, 1, true);
        });
        return tool;
    }
}
