package net.wesjd.anvilgui;

import net.wesjd.anvilgui.version.Version;
import net.wesjd.anvilgui.version.VersionWrapper;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/**
 * An anvil gui, used for gathering a user's input
 * @author Wesley Smith
 * @since 1.0
 */
public class AnvilGUI {
    private static final String GITHUB_LINK = "https://github.com/WesJD/AnvilGUI";

    /**
     * The player who has the GUI open
     */
    private final Player holder;
    /**
     * The ItemStack that is in the {@link Slot#INPUT_LEFT} slot.
     */
    private final ItemStack insert;
    /**
     * Called when the player clicks the {@link Slot#OUTPUT} slot
     */
    private final ClickHandler clickHandler;

    /**
     * The {@link VersionWrapper} for this server
     */
    private final VersionWrapper wrapper;
    /**
     * The container id of the inventory, used for NMS methods
     */
    private final int containerId;
    /**
     * The inventory that is used on the Bukkit side of things
     */
    private final Inventory inventory;
    /**
     * The listener holder class
     */
    private final ListenUp listener = new ListenUp();

    /**
     * Represents the state of the inventory being open
     */
    private boolean open = false;

    /**
     * Create an AnvilGUI and open it for the player.
     * @param plugin A {@link org.bukkit.plugin.java.JavaPlugin} instance
     * @param holder The {@link Player} to open the inventory for
     * @param insert What to have the text already set to
     * @param clickHandler A {@link ClickHandler} that is called when the player clicks the {@link Slot#OUTPUT} slot
     * @throws NullPointerException If the server version isn't supported
     */
    public AnvilGUI(Plugin plugin, Player holder, String insert, ClickHandler clickHandler) {
        this.holder = holder;
        this.clickHandler = clickHandler;

        final ItemStack paper = new ItemStack(Material.PAPER);
        final ItemMeta paperMeta = paper.getItemMeta();
        paperMeta.setDisplayName(insert);
        paper.setItemMeta(paperMeta);
        this.insert = paper;

        final String strVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        final Version version = Version.of(strVersion);
        //Validate.notNull(version, "Your server version isn't supported in AnvilGUI!");
        if (version != null)
            wrapper = version.getWrapper();
        else {
            plugin.getLogger().warning("[AnvilGUI] Using fallback wrapper, please ask the developers to implement \"" + strVersion + "\" too (" + GITHUB_LINK + ")");
            wrapper = Version.getFallback();
        }

        wrapper.handleInventoryCloseEvent(holder);
        wrapper.setActiveContainerDefault(holder);

        Bukkit.getPluginManager().registerEvents(listener, plugin);

        final Object container = wrapper.newContainerAnvil(holder);

        inventory = wrapper.toBukkitInventory(container);
        inventory.setItem(Slot.INPUT_LEFT, this.insert);

        containerId = wrapper.getNextContainerId(holder);
        wrapper.sendPacketOpenWindow(holder, containerId);
        wrapper.setActiveContainer(holder, container);
        wrapper.setActiveContainerId(container, containerId);
        wrapper.addActiveContainerSlotListener(container, holder);

        open = true;
    }

    /**
     * Closes the inventory if it's open.
     * @throws IllegalArgumentException If the inventory isn't open
     */
    public void closeInventory() {
        Validate.isTrue(open, "You can't close an inventory that isn't open!");
        open = false;

        wrapper.handleInventoryCloseEvent(holder);
        wrapper.setActiveContainerDefault(holder);
        wrapper.sendPacketCloseWindow(holder, containerId);

        HandlerList.unregisterAll(listener);
    }

    /**
     * Simply holds the listeners for the GUI
     */
    private class ListenUp implements Listener {

        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if(e.getInventory().equals(inventory)) {
                e.setCancelled(true);
                final Player clicker = (Player) e.getWhoClicked();
                if(e.getRawSlot() == Slot.OUTPUT) {
                    final ItemStack clicked = inventory.getItem(e.getRawSlot());
                    if(clicked == null || clicked.getType() == Material.AIR) return;
                    final String ret = clickHandler.onClick(clicker, clicked.hasItemMeta() ? clicked.getItemMeta().getDisplayName() : clicked.getType().toString());
                    if(ret != null) {
                        final ItemMeta meta = clicked.getItemMeta();
                        meta.setDisplayName(ret);
                        clicked.setItemMeta(meta);
                        inventory.setItem(Slot.INPUT_LEFT, clicked);
                    } else closeInventory();
                }
            }
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent e) {
            if(e.getInventory().equals(inventory)) {
                if(open)
                    closeInventory();
                e.getInventory().clear();
            }
        }

    }

    /**
     * Handles the click of the output slot
     */
    public interface ClickHandler{

        /**
         * Is called when a {@link Player} clicks on the output in the GUI
         * @param clicker The {@link Player} who clicked the output
         * @param input What the item was renamed to
         * @return What to replace the text with, or null to close the inventory
         */
        String onClick(Player clicker, String input);

    }

    /**
     * Class wrapping the magic constants of slot numbers in an anvil GUI
     */
    public static class Slot {

        /**
         * The slot on the far left, where the first input is inserted. An {@link ItemStack} is always inserted
         * here to be renamed
         */
        public static final int INPUT_LEFT = 0;
        /**
         * Not used, but in a real anvil you are able to put the second item you want to combine here
         */
        public static final int INPUT_RIGHT = 1;
        /**
         * The output slot, where an item is put when two items are combined from {@link #INPUT_LEFT} and
         * {@link #INPUT_RIGHT} or {@link #INPUT_LEFT} is renamed
         */
        public static final int OUTPUT = 2;

    }

}
