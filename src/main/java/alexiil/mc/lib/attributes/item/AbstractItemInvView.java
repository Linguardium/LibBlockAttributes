package alexiil.mc.lib.attributes.item;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv.ModifiableFixedItemInv;

/** Base interface for {@link FixedItemInvView} and {@link GroupedItemInvView}. */
public interface AbstractItemInvView {

    /** @return A value that indicates whether an inventory might have changed if it differs from the last value
     *         returned. This number doesn't have to start at 0, but it must change every time that the inventory is
     *         changed, although it can change even without any observable changes to this inventory.
     *         <p>
     *         Inventories that don't support this might increment the change number every time that this is called - so
     *         you should never write a loop that depends on the returned value settling down on a particular value.
     *         <p>
     *         Inventories that support {@link #addListener(InvMarkDirtyListener, ListenerRemovalToken) listeners}
     *         <em>highly encouraged</em> to support this - by definition if an inventory knows when it changed then it
     *         should be able to count the number of changes. It is also implied that any changes to this value will
     *         also invoke every registered {@link InvMarkDirtyListener}. */
    int getChangeValue();

    /** Adds the given listener to this inventory, such that
     * {@link InvMarkDirtyListener#onMarkDirty(AbstractItemInvView)} will be called every time that any stored stack is
     * changed (either from {@link FixedItemInv#setInvStack(int, ItemStack, Simulation)} or
     * {@link ModifiableFixedItemInv#markDirty()}).
     * <p>
     * If the listener is registered (and thus this returns null) then it implies that {@link #getChangeValue()} will
     * change every time that the given listener is invoked, always just-before it is invoked.
     * 
     * @param removalToken A token that will be called whenever the given listener is removed from this inventory (or if
     *            this inventory itself is unloaded or otherwise invalidated).
     * @return A token that represents the listener, or null if the listener could not be added. */
    @Nullable
    ListenerToken addListener(InvMarkDirtyListener listener, ListenerRemovalToken removalToken);
}
