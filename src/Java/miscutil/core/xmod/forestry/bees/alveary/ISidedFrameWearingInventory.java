package miscutil.core.xmod.forestry.bees.alveary;

import net.minecraft.inventory.ISidedInventory;
import forestry.api.apiculture.IBeeHousing;

public abstract interface ISidedFrameWearingInventory
  extends ISidedInventory
{
  public abstract void wearOutFrames(IBeeHousing paramIBeeHousing, int paramInt);
}