/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.container.implementations;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.AEApi;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.guiobjects.INetworkTool;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketMEInventoryUpdate;
import appeng.me.cache.GridStorageCache;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

public class ContainerNetworkStatus extends AEBaseContainer {

    @GuiSync(0)
    public long avgAddition;

    @GuiSync(1)
    public long powerUsage;

    @GuiSync(2)
    public long currentPower;

    @GuiSync(3)
    public long maxPower;

    @GuiSync(4)
    public long itemBytesTotal;

    @GuiSync(5)
    public long itemBytesUsed;

    @GuiSync(6)
    public long fluidBytesTotal;

    @GuiSync(7)
    public long fluidBytesUsed;

    @GuiSync(8)
    public long essentiaBytesTotal;

    @GuiSync(9)
    public long essentiaBytesUsed;

    private IGrid network;
    private int delay = 40;

    public ContainerNetworkStatus(final InventoryPlayer ip, final INetworkTool te) {
        super(ip, null, null);
        final IGridHost host = te.getGridHost();

        if (host != null) {
            this.findNode(host, ForgeDirection.UNKNOWN);
            for (final ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                this.findNode(host, d);
            }
        }

        if (this.network == null && Platform.isServer()) {
            this.setValidContainer(false);
        }
    }

    private void findNode(final IGridHost host, final ForgeDirection d) {
        if (this.network == null) {
            final IGridNode node = host.getGridNode(d);
            if (node != null) {
                this.network = node.getGrid();
            }
        }
    }

    @Override
    public void detectAndSendChanges() {
        this.delay++;
        if (Platform.isServer() && this.delay > 15 && this.network != null) {
            this.delay = 0;

            final IEnergyGrid eg = this.network.getCache(IEnergyGrid.class);
            if (eg != null) {
                this.setAverageAddition((long) (100.0 * eg.getAvgPowerInjection()));
                this.setPowerUsage((long) (100.0 * eg.getAvgPowerUsage()));
                this.setCurrentPower((long) (100.0 * eg.getStoredPower()));
                this.setMaxPower((long) (100.0 * eg.getMaxStoredPower()));
            }

            try {
                final PacketMEInventoryUpdate piu = new PacketMEInventoryUpdate();

                for (final Class<? extends IGridHost> machineClass : this.network.getMachinesClasses()) {
                    final IItemList<IAEItemStack> list = AEApi.instance().storage().createItemList();
                    for (final IGridNode machine : this.network.getMachines(machineClass)) {
                        final IGridBlock blk = machine.getGridBlock();
                        final ItemStack is = blk.getMachineRepresentation();
                        if (is != null && is.getItem() != null) {
                            final IAEItemStack ais = AEItemStack.create(is);
                            ais.setStackSize(1);
                            ais.setCountRequestable(
                                    (long) PowerMultiplier.CONFIG.multiply(blk.getIdlePowerUsage() * 100.0));
                            list.add(ais);
                        }
                    }

                    for (final IAEItemStack ais : list) {
                        piu.appendItem(ais);
                    }
                }

                for (final Object c : this.crafters) {
                    if (c instanceof EntityPlayer) {
                        NetworkHandler.instance.sendTo(piu, (EntityPlayerMP) c);
                    }
                }
            } catch (final IOException e) {
                // :P
            }
            final GridStorageCache sg = this.network.getCache(IStorageGrid.class);
            if (sg != null) {
                this.setItemBytesTotal(sg.getItemBytesTotal());
                this.setItemBytesUsed(sg.getItemBytesUsed());
                this.setFluidBytesTotal(sg.getFluidBytesTotal());
                this.setFluidBytesUsed(sg.getFluidBytesUsed());
                this.setEssentiaBytesTotal(sg.getEssentiaBytesTotal());
                this.setEssentiaBytesUsed(sg.getEssentiaBytesUsed());
            }
        }
        super.detectAndSendChanges();
    }

    public long getCurrentPower() {
        return this.currentPower;
    }

    private void setCurrentPower(final long currentPower) {
        this.currentPower = currentPower;
    }

    public long getMaxPower() {
        return this.maxPower;
    }

    private void setMaxPower(final long maxPower) {
        this.maxPower = maxPower;
    }

    public long getAverageAddition() {
        return this.avgAddition;
    }

    private void setAverageAddition(final long avgAddition) {
        this.avgAddition = avgAddition;
    }

    public long getPowerUsage() {
        return this.powerUsage;
    }

    private void setPowerUsage(final long powerUsage) {
        this.powerUsage = powerUsage;
    }

    public long getItemBytesTotal() {
        return this.itemBytesTotal;
    }

    public long getItemBytesUsed() {
        return this.itemBytesUsed;
    }

    public void setItemBytesTotal(final long itemBytesTotal) {
        this.itemBytesTotal = itemBytesTotal;
    }

    public void setItemBytesUsed(final long itemBytesUsed) {
        this.itemBytesUsed = itemBytesUsed;
    }

    public long getFluidBytesTotal() {
        return this.fluidBytesTotal;
    }

    public long getFluidBytesUsed() {
        return this.fluidBytesUsed;
    }

    public void setFluidBytesTotal(final long fluidBytesTotal) {
        this.fluidBytesTotal = fluidBytesTotal;
    }

    public void setFluidBytesUsed(final long fluidBytesUsed) {
        this.fluidBytesUsed = fluidBytesUsed;
    }

    public long getEssentiaBytesTotal() {
        return essentiaBytesTotal;
    }

    public long getEssentiaBytesUsed() {
        return essentiaBytesUsed;
    }

    public void setEssentiaBytesTotal(long essentiaBytesTotal) {
        this.essentiaBytesTotal = essentiaBytesTotal;
    }

    public void setEssentiaBytesUsed(long essentiaBytesUsed) {
        this.essentiaBytesUsed = essentiaBytesUsed;
    }
}
