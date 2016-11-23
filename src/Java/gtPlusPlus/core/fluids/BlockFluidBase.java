package gtPlusPlus.core.fluids;

import gtPlusPlus.core.creative.AddToCreativeTab;
import gtPlusPlus.core.lib.CORE;
import gtPlusPlus.core.material.Material;
import gtPlusPlus.core.util.Utils;
import gtPlusPlus.core.util.math.MathUtils;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockFluidBase extends BlockFluidClassic {

    @SideOnly(Side.CLIENT)
    protected IIcon stillIcon;
    @SideOnly(Side.CLIENT)
    protected IIcon flowingIcon;
    
    protected int colour;
    protected Material fluidMaterial;
    final String displayName;
    
    @SuppressWarnings("deprecation")
	public BlockFluidBase(Fluid fluid, Material material) {
            super(fluid, net.minecraft.block.material.Material.lava);
            short[] tempColour = material.getRGBA(); 
            this.colour = Utils.rgbtoHexValue(tempColour[0], tempColour[1], tempColour[2]);
            this.fluidMaterial = material;
            setCreativeTab(AddToCreativeTab.tabOther);
            this.displayName = material.getLocalizedName();
            LanguageRegistry.addName(this, "Molten "+displayName+" ["+MathUtils.celsiusToKelvin(fluidMaterial.getBoilingPointC())+"K]");
    		this.setBlockName(GetProperName());
    }
    
    @SuppressWarnings("deprecation")
	public BlockFluidBase(String fluidName, Fluid fluid, short[] colour) {
            super(fluid, net.minecraft.block.material.Material.lava);
            short[] tempColour = colour; 
            this.colour = Utils.rgbtoHexValue(tempColour[0], tempColour[1], tempColour[2]);
            setCreativeTab(AddToCreativeTab.tabOther);
            this.displayName = fluidName;
            LanguageRegistry.addName(this, "Molten "+displayName);
    		this.setBlockName(GetProperName());
    }
    
    @Override
    public IIcon getIcon(int side, int meta) {
            return (side == 0 || side == 1)? stillIcon : flowingIcon;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister register) {
            stillIcon = register.registerIcon(CORE.MODID+":fluids/fluid.molten.autogenerated");
            flowingIcon = register.registerIcon(CORE.MODID+":fluids/fluid.molten.autogenerated");
    }
    
    @Override
    public boolean canDisplace(IBlockAccess world, int x, int y, int z) {
            if (world.getBlock(x,  y,  z).getMaterial().isLiquid()) return false;
            return super.canDisplace(world, x, y, z);
    }
    
    @Override
    public boolean displaceIfPossible(World world, int x, int y, int z) {
            if (world.getBlock(x,  y,  z).getMaterial().isLiquid()) return false;
            return super.displaceIfPossible(world, x, y, z);
    }

	@Override
	public int colorMultiplier(IBlockAccess par1IBlockAccess, int par2, int par3, int par4){		
		
		if (this.colour == 0){
			return MathUtils.generateSingularRandomHexValue();
		}
		
		return this.colour;
	}
	
    @Override
	public int getRenderColor(int aMeta) {
    	if (this.colour == 0){
			return MathUtils.generateSingularRandomHexValue();
		}
		
		return this.colour;
    }
    
    public String GetProperName() {
		String tempIngot;	

		tempIngot = "Molten "+displayName;

		return tempIngot;
	}
    
    public Material getFluidMaterial(){
    	return fluidMaterial;
    }
    
}		