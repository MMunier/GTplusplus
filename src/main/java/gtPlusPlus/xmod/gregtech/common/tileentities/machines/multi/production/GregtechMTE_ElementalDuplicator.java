package gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.util.GT_StructureUtility.ofHatchAdder;
import static gtPlusPlus.core.util.data.ArrayUtils.removeNulls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Energy;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Input;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_InputBus;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Maintenance;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Output;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_OutputBus;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_TieredMachineBlock;
import gregtech.api.objects.GT_RenderedTexture;
import gregtech.api.util.GTPP_Recipe;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gtPlusPlus.api.objects.Logger;
import gtPlusPlus.api.objects.data.AutoMap;
import gtPlusPlus.api.objects.data.Triplet;
import gtPlusPlus.core.item.chemistry.general.ItemGenericChemBase;
import gtPlusPlus.core.lib.CORE;
import gtPlusPlus.core.recipe.common.CI;
import gtPlusPlus.core.util.math.MathUtils;
import gtPlusPlus.core.util.minecraft.ItemUtils;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_ElementalDataOrbHolder;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.GregtechMeta_MultiBlockBase;
import gtPlusPlus.xmod.gregtech.common.blocks.textures.TexturesGtBlock;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

public class GregtechMTE_ElementalDuplicator extends GregtechMeta_MultiBlockBase {

	private int mSolidCasingTier = 0;
	private int mMachineCasingTier = 0;
	private int mPipeCasingTier = 0;
	private int mCoilTier = 0;
	private int checkCoil;
	private int[] checkCasing = new int[8];
	private int checkMachine;
	private int checkPipe;
	private int maxTierOfHatch;
	private int mCasing;
	private IStructureDefinition<GregtechMTE_ElementalDuplicator> STRUCTURE_DEFINITION = null;

	private ArrayList<GT_MetaTileEntity_Hatch_ElementalDataOrbHolder> mReplicatorDataOrbHatches = new ArrayList<GT_MetaTileEntity_Hatch_ElementalDataOrbHolder>();

	private static final HashMap<Integer, Triplet<Block, Integer, Integer>> mTieredBlockRegistry = new HashMap<Integer, Triplet<Block, Integer, Integer>>();

	public GregtechMTE_ElementalDuplicator(final int aID, final String aName, final String aNameRegional) {
		super(aID, aName, aNameRegional);
	}

	public GregtechMTE_ElementalDuplicator(final String aName) {
		super(aName);
	}

	public static boolean registerMachineCasingForTier(int aTier, Block aBlock, int aMeta, int aCasingTextureID) {
		int aSize = mTieredBlockRegistry.size();
		int aSize2 = aSize;
		Triplet<Block, Integer, Integer> aCasingData = new Triplet<Block, Integer, Integer>(aBlock, aMeta, aCasingTextureID);
		if (mTieredBlockRegistry.containsKey(aTier)) {
			CORE.crash("Tried to register a Machine casing for tier "+aTier+" to the Chemical Plant, however this tier already contains one.");
		}
		mTieredBlockRegistry.put(aTier, aCasingData);
		aSize = mTieredBlockRegistry.size();
		return aSize > aSize2;
	}

	private static int getCasingTextureIdForTier(int aTier) {
		if (!mTieredBlockRegistry.containsKey(aTier)) {
			return 10;
		}
		int aCasingID = mTieredBlockRegistry.get(aTier).getValue_3();
		//Logger.INFO("Found casing texture ID "+aCasingID+" for tier "+aTier);
		return aCasingID;
	}

	@Override
	public IMetaTileEntity newMetaEntity(final IGregTechTileEntity aTileEntity) {
		return new GregtechMTE_ElementalDuplicator(this.mName);
	}

	@Override
	public String getMachineType() {
		return "Replicator";
	}

	@Override
	protected GT_Multiblock_Tooltip_Builder createTooltip() {
		GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
		tt.addMachineType(getMachineType())
				.addInfo("Controller Block for the Industrial Replication Machine")
				.addInfo("Now replication is less painful")
				.addInfo("Please read to user manual for more information on construction & usage")
				.addSeparator()
				.addController("Bottom Center")
				.addStructureHint("Catalyst Housing", 1)
				.addInputBus("Bottom Casing", 1)
				.addOutputBus("Bottom Casing", 1)
				.addInputHatch("Bottom Casing", 1)
				.addOutputHatch("Bottom Casing", 1)
				.addEnergyHatch("Bottom Casing", 1)
				.addMaintenanceHatch("Bottom Casing", 1)
				.toolTipFinisher(CORE.GT_Tooltip_Builder);
		return tt;
	}

	public void setMachineMeta(int meta) {
		checkMachine = meta;
	}

	public int getMachineMeta() {
		return checkMachine;
	}

	public void setPipeMeta(int meta) {
		checkPipe = meta;
	}

	public int getPipeMeta() {
		return checkPipe;
	}

	public void setCoilMeta(int meta) {
		checkCoil = meta;
	}

	public int getCoilMeta() {
		return checkCoil;
	}

	public int coilTier(int meta) {
		switch (meta) {
			case 0: return 1;
			case 1: return 2;
			case 2: return 3;
			case 3: return 4;
			case 4: return 5;
			case 5: return 7;
			case 6: return 8;
			case 7: return 10;
			case 8: return 11;
			case 9: return 6;
			case 10: return 9;
		}
		return 0;
	}

	@Override
	public IStructureDefinition<GregtechMTE_ElementalDuplicator> getStructureDefinition() {
		if (STRUCTURE_DEFINITION == null) {
			STRUCTURE_DEFINITION = StructureDefinition.<GregtechMTE_ElementalDuplicator>builder()
					.addShape(mName, transpose(new String[][]{
							{"XXXXXXX", "XXXXXXX", "XXXXXXX", "XXXXXXX", "XXXXXXX", "XXXXXXX", "XXXXXXX"},
							{"X     X", " MMMMM ", " MHHHM ", " MHHHM ", " MHHHM ", " MMMMM ", "X     X"},
							{"X     X", "       ", "  PPP  ", "  PPP  ", "  PPP  ", "       ", "X     X"},
							{"X     X", "       ", "  HHH  ", "  HHH  ", "  HHH  ", "       ", "X     X"},
							{"X     X", "       ", "  PPP  ", "  PPP  ", "  PPP  ", "       ", "X     X"},
							{"X     X", " MMMMM ", " MHHHM ", " MHHHM ", " MHHHM ", " MMMMM ", "X     X"},
							{"CCC~CCC", "CMMMMMC", "CMMMMMC", "CMMMMMC", "CMMMMMC", "CMMMMMC", "CCCCCCC"},
					}))
					.addElement(
							'C',
							ofChain(
									ofHatchAdder(
											GregtechMTE_ElementalDuplicator::addChemicalPlantList, getCasingTextureID(), 1
									),
									onElementPass(
											x -> {++x.checkCasing[0]; ++x.mCasing;},
											ofSolidCasing(0)
									),
									onElementPass(
											x -> {++x.checkCasing[1]; ++x.mCasing;},
											ofSolidCasing(1)
									),
									onElementPass(
											x -> {++x.checkCasing[2]; ++x.mCasing;},
											ofSolidCasing(2)
									),
									onElementPass(
											x -> {++x.checkCasing[3]; ++x.mCasing;},
											ofSolidCasing(3)
									),
									onElementPass(
											x -> {++x.checkCasing[4]; ++x.mCasing;},
											ofSolidCasing(4)
									),
									onElementPass(
											x -> {++x.checkCasing[5]; ++x.mCasing;},
											ofSolidCasing(5)
									),
									onElementPass(
											x -> {++x.checkCasing[6]; ++x.mCasing;},
											ofSolidCasing(6)
									),
									onElementPass(
											x -> {++x.checkCasing[7]; ++x.mCasing;},
											ofSolidCasing(7)
									)
							)
					)
					.addElement(
							'X',
							ofChain(
									onElementPass(
											x -> {++x.checkCasing[0]; ++x.mCasing;},
											ofSolidCasing(0)
									),
									onElementPass(
											x -> {++x.checkCasing[1]; ++x.mCasing;},
											ofSolidCasing(1)
									),
									onElementPass(
											x -> {++x.checkCasing[2]; ++x.mCasing;},
											ofSolidCasing(2)
									),
									onElementPass(
											x -> {++x.checkCasing[3]; ++x.mCasing;},
											ofSolidCasing(3)
									),
									onElementPass(
											x -> {++x.checkCasing[4]; ++x.mCasing;},
											ofSolidCasing(4)
									),
									onElementPass(
											x -> {++x.checkCasing[5]; ++x.mCasing;},
											ofSolidCasing(5)
									),
									onElementPass(
											x -> {++x.checkCasing[6]; ++x.mCasing;},
											ofSolidCasing(6)
									),
									onElementPass(
											x -> {++x.checkCasing[7]; ++x.mCasing;},
											ofSolidCasing(7)
									)
							)
					)
					.addElement(
							'M',
							addTieredBlock(
									GregTech_API.sBlockCasings1, GregtechMTE_ElementalDuplicator::setMachineMeta, GregtechMTE_ElementalDuplicator::getMachineMeta, 10
							)
					)
					.addElement(
							'H',
							addTieredBlock(
									GregTech_API.sBlockCasings5, GregtechMTE_ElementalDuplicator::setCoilMeta, GregtechMTE_ElementalDuplicator::getCoilMeta, 11
							)
					)
					.addElement(
							'P',
							addTieredBlock(
									GregTech_API.sBlockCasings2, GregtechMTE_ElementalDuplicator::setPipeMeta, GregtechMTE_ElementalDuplicator::getPipeMeta, 12, 16
							)
					)
					.build();
		}
		return STRUCTURE_DEFINITION;
	}

	public static <T> IStructureElement<T> ofSolidCasing(int aIndex) {
		return new IStructureElement<T>() {
			@Override
			public boolean check(T t, World world, int x, int y, int z) {
				Block block = world.getBlock(x, y, z);
				int meta = world.getBlockMetadata(x, y, z);
				Block target = mTieredBlockRegistry.get(aIndex).getValue_1();
				int targetMeta = mTieredBlockRegistry.get(aIndex).getValue_2();
				return target.equals(block) && meta == targetMeta;
			}

			int getIndex(int size) {
				if (size > 8) size = 8;
				return size - 1;
			}

			@Override
			public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
				StructureLibAPI.hintParticle(world, x, y, z, mTieredBlockRegistry.get(getIndex(trigger.stackSize)).getValue_1(), mTieredBlockRegistry.get(getIndex(trigger.stackSize)).getValue_2());
				return true;
			}

			@Override
			public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
				return world.setBlock(x, y, z, mTieredBlockRegistry.get(getIndex(trigger.stackSize)).getValue_1(), mTieredBlockRegistry.get(getIndex(trigger.stackSize)).getValue_2(), 3);
			}
		};
	}

	@Override
	public void construct(ItemStack stackSize, boolean hintsOnly) {
		buildPiece(mName , stackSize, hintsOnly, 3, 6, 0);
	}

	@Override
	public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
		mCasing = 0;
		for (int i = 0; i < 8; i++) {
			checkCasing[i] = 0;
		}
		checkCoil = 0;
		checkPipe = 0;
		checkMachine = 0;
		mSolidCasingTier = 0;
		mMachineCasingTier = 0;
		mPipeCasingTier = 0;
		mCoilTier = 0;
		mReplicatorDataOrbHatches.clear();
		if (checkPiece(mName, 3, 6, 0) && mCasing >= 80) {
			for (int i = 0; i < 8; i++) {
				if (checkCasing[i] == mCasing) {
					mSolidCasingTier = i;
				}
				else if (checkCasing[i] > 0)
					return false;
			}
			mMachineCasingTier = checkMachine - 1;
			mPipeCasingTier = checkPipe - 12;
			mCoilTier = coilTier(checkCoil - 1);
			updateHatchTexture();
			return mMachineCasingTier >= maxTierOfHatch;
		}
		return false;
	}

	public void updateHatchTexture() {
		for (GT_MetaTileEntity_Hatch h : mReplicatorDataOrbHatches) h.updateTexture(getCasingTextureID());
		for (GT_MetaTileEntity_Hatch h : mInputBusses) h.updateTexture(getCasingTextureID());
		for (GT_MetaTileEntity_Hatch h : mMaintenanceHatches) h.updateTexture(getCasingTextureID());
		for (GT_MetaTileEntity_Hatch h : mEnergyHatches) h.updateTexture(getCasingTextureID());
		for (GT_MetaTileEntity_Hatch h : mOutputBusses) h.updateTexture(getCasingTextureID());
		for (GT_MetaTileEntity_Hatch h : mInputHatches) h.updateTexture(getCasingTextureID());
		for (GT_MetaTileEntity_Hatch h : mOutputHatches) h.updateTexture(getCasingTextureID());
	}

	public final boolean addChemicalPlantList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
		if (aTileEntity == null) {
			return false;
		} else {
			IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
			if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_ElementalDataOrbHolder){
				return addToMachineList(aTileEntity, aBaseCasingIndex);
			} else if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_InputBus){
				maxTierOfHatch = Math.max(maxTierOfHatch, ((GT_MetaTileEntity_Hatch_InputBus) aMetaTileEntity).mTier);
				return addToMachineList(aTileEntity, aBaseCasingIndex);
			} else if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Maintenance){
				return addToMachineList(aTileEntity, aBaseCasingIndex);
			} else if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Energy){
				maxTierOfHatch = Math.max(maxTierOfHatch, ((GT_MetaTileEntity_Hatch_Energy) aMetaTileEntity).mTier);
				return addToMachineList(aTileEntity, aBaseCasingIndex);
			} else if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_OutputBus) {
				maxTierOfHatch = Math.max(maxTierOfHatch, ((GT_MetaTileEntity_Hatch_OutputBus) aMetaTileEntity).mTier);
				return addToMachineList(aTileEntity, aBaseCasingIndex);
			} else if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Input) {
				maxTierOfHatch = Math.max(maxTierOfHatch, ((GT_MetaTileEntity_Hatch_Input) aMetaTileEntity).mTier);
				return addToMachineList(aTileEntity, aBaseCasingIndex);
			} else if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Output) {
				maxTierOfHatch = Math.max(maxTierOfHatch, ((GT_MetaTileEntity_Hatch_Output) aMetaTileEntity).mTier);
				return addToMachineList(aTileEntity, aBaseCasingIndex);
			}
		}
		return false;
	}

	@Override
	public String getSound() {
		return GregTech_API.sSoundList.get(Integer.valueOf(207));
	}

	@Override
	public ITexture[] getTexture(final IGregTechTileEntity aBaseMetaTileEntity, final byte aSide, final byte aFacing, final byte aColorIndex, final boolean aActive, final boolean aRedstone) {

		ITexture aOriginalTexture;

		// Check things exist client side (The worst code ever)
		if (aBaseMetaTileEntity.getWorld() != null) {

		}
		int aCasingID = getCasingTextureID();
		aOriginalTexture = Textures.BlockIcons.getCasingTextureForId(aCasingID);

		if (aSide == aFacing) {
			return new ITexture[]{aOriginalTexture, new GT_RenderedTexture(aActive ? TexturesGtBlock.Overlay_Machine_Controller_Advanced_Active : TexturesGtBlock.Overlay_Machine_Controller_Advanced)};
		}
		return new ITexture[]{aOriginalTexture};
	}

	@Override
	public boolean hasSlotInGUI() {
		return true;
	}

	@Override
	public GT_Recipe.GT_Recipe_Map getRecipeMap() {
		if (GTPP_Recipe.GTPP_Recipe_Map.sChemicalPlant_GT.mRecipeList.size() == 0) {
			generateRecipes();
		}
		return GTPP_Recipe.GTPP_Recipe_Map.sChemicalPlant_GT;
	}

	public static void generateRecipes() {
		for (GT_Recipe i : GTPP_Recipe.GTPP_Recipe_Map.sChemicalPlantRecipes.mRecipeList) {
			GTPP_Recipe.GTPP_Recipe_Map.sChemicalPlant_GT.add(i);
		}
	}

	@Override
	public int getMaxParallelRecipes() {
		return 2 * getPipeCasingTier();
	}

	@Override
	public int getEuDiscountForParallelism() {
		return 100;
	}

	private int getSolidCasingTier() {
		return this.mSolidCasingTier;
	}

	private int getMachineCasingTier() {
		return mMachineCasingTier;
	}

	private int getPipeCasingTier() {
		return mPipeCasingTier;
	}

	private int getCasingTextureID() {
		// Check the Tier Client Side
		int aTier = mSolidCasingTier;
		int aCasingID = getCasingTextureIdForTier(aTier);
		return aCasingID;
	}

	public boolean addToMachineList(IGregTechTileEntity aTileEntity) {		
		int aMaxTier = getMachineCasingTier();		
		final IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();		
		if (aMetaTileEntity instanceof GT_MetaTileEntity_TieredMachineBlock) {
			GT_MetaTileEntity_TieredMachineBlock aMachineBlock = (GT_MetaTileEntity_TieredMachineBlock) aMetaTileEntity;
			int aTileTier = aMachineBlock.mTier;
			if (aTileTier > aMaxTier) {
				log("Hatch tier too high.");
				return false;
			}
			else {
				return addToMachineList(aTileEntity, getCasingTextureID());
			}
		}
		else {
			log("Bad Tile Entity being added to hatch map."); // Shouldn't ever happen, but.. ya know..
			return false;
		}		
	}

	@Override
	public void saveNBTData(NBTTagCompound aNBT) {
		super.saveNBTData(aNBT);
		aNBT.setInteger("mSolidCasingTier", this.mSolidCasingTier);
		aNBT.setInteger("mMachineCasingTier", this.mMachineCasingTier);
		aNBT.setInteger("mPipeCasingTier", this.mPipeCasingTier);
		aNBT.setInteger("mCoilTier", this.mCoilTier);
	}

	@Override
	public void loadNBTData(NBTTagCompound aNBT) {
		super.loadNBTData(aNBT);
		mSolidCasingTier = aNBT.getInteger("mSolidCasingTier");
		mMachineCasingTier = aNBT.getInteger("mMachineCasingTier");
		mPipeCasingTier = aNBT.getInteger("mPipeCasingTier");
		mCoilTier = aNBT.getInteger("mCoilTier");
	}

	@Override
	public boolean addToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
		final IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
		if (aMetaTileEntity == null) {
			return false;
		}
		if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_ElementalDataOrbHolder) {
			log("Found GT_MetaTileEntity_Hatch_ElementalDataOrbHolder");
	        ((GT_MetaTileEntity_Hatch_ElementalDataOrbHolder) aTileEntity).mRecipeMap = getRecipeMap();	        
			return addToMachineListInternal(mReplicatorDataOrbHatches, aMetaTileEntity, aBaseCasingIndex);
		}		
		return super.addToMachineList(aTileEntity, aBaseCasingIndex);
	}

	@Override
	public int getMaxEfficiency(final ItemStack aStack) {
		return 10000;
	}

	@Override
	public int getPollutionPerTick(final ItemStack aStack) {
		return 100;
	}

	@Override
	public int getAmountOfOutputs() {
		return 1;
	}

	@Override
	public boolean explodesOnComponentBreak(final ItemStack aStack) {
		return false;
	}

	@Override
	public String getCustomGUIResourceName() {
		return null;
	}

	// Same speed bonus as pyro oven
	public int getSpeedBonus() {
		return 50 * (this.mCoilTier - 2);
	}

	@Override
	public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
		if (aBaseMetaTileEntity.isServerSide()) {
			if (this.mUpdate == 1 || this.mStartUpCheck == 1) {
				this.mReplicatorDataOrbHatches.clear();
			}
		}
		super.onPostTick(aBaseMetaTileEntity, aTick);
	}

	@Override
	public void onPreTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
		super.onPreTick(aBaseMetaTileEntity, aTick);
	}

	@Override
	public boolean checkRecipe(final ItemStack aStack) {		
		return checkRecipeGeneric(getMaxParallelRecipes(), getEuDiscountForParallelism(), getSpeedBonus());
	}


	@Override
	public boolean checkRecipeGeneric(
			ItemStack[] aItemInputs, FluidStack[] aFluidInputs,
			int aMaxParallelRecipes, int aEUPercent,
			int aSpeedBonusPercent, int aOutputChanceRoll, GT_Recipe aRecipe, boolean isOC) {
		return false;
	}

	private static final HashMap<Long, AutoMap<GT_Recipe>> mTieredRecipeMap = new HashMap<Long, AutoMap<GT_Recipe>>();
	private static final AutoMap<GT_Recipe> aTier0Recipes = new AutoMap<GT_Recipe>();
	private static final AutoMap<GT_Recipe> aTier1Recipes = new AutoMap<GT_Recipe>();
	private static final AutoMap<GT_Recipe> aTier2Recipes = new AutoMap<GT_Recipe>();
	private static final AutoMap<GT_Recipe> aTier3Recipes = new AutoMap<GT_Recipe>();
	private static final AutoMap<GT_Recipe> aTier4Recipes = new AutoMap<GT_Recipe>();
	private static final AutoMap<GT_Recipe> aTier5Recipes = new AutoMap<GT_Recipe>();
	private static final AutoMap<GT_Recipe> aTier6Recipes = new AutoMap<GT_Recipe>();
	private static final AutoMap<GT_Recipe> aTier7Recipes = new AutoMap<GT_Recipe>();
	private static boolean mInitRecipeCache = false;

	private static void initRecipeCaches() {
		if (!mInitRecipeCache) {		
			mTieredRecipeMap.put((long) 0, aTier0Recipes);
			mTieredRecipeMap.put((long) 1, aTier1Recipes);
			mTieredRecipeMap.put((long) 2, aTier2Recipes);
			mTieredRecipeMap.put((long) 3, aTier3Recipes);
			mTieredRecipeMap.put((long) 4, aTier4Recipes);
			mTieredRecipeMap.put((long) 5, aTier5Recipes);
			mTieredRecipeMap.put((long) 6, aTier6Recipes);
			mTieredRecipeMap.put((long) 7, aTier7Recipes);
			for (GT_Recipe aRecipe : GTPP_Recipe.GTPP_Recipe_Map.sChemicalPlant_GT.mRecipeList) {
				if (aRecipe != null) {					
					switch (aRecipe.mSpecialValue) {
						case 0:
							aTier0Recipes.add(aRecipe);
							continue;
						case 1:
							aTier1Recipes.add(aRecipe);
							continue;
						case 2:
							aTier2Recipes.add(aRecipe);
							continue;
						case 3:
							aTier3Recipes.add(aRecipe);
							continue;
						case 4:
							aTier4Recipes.add(aRecipe);
							continue;
						case 5:
							aTier5Recipes.add(aRecipe);
							continue;
						case 6:
							aTier6Recipes.add(aRecipe);
							continue;
						case 7:
							aTier7Recipes.add(aRecipe);
							continue;
					}					
				}
			}
			mInitRecipeCache = true;			
		}
	}

	private static boolean areInputsEqual(GT_Recipe aComparator, ItemStack[] aInputs, FluidStack[] aFluids) {
		int aInputCount = aComparator.mInputs.length;
		if (aInputCount > 0) {
			//Logger.INFO("Looking for recipe with "+aInputCount+" Items");
			int aMatchingInputs = 0;
			recipe : for (ItemStack a : aComparator.mInputs) {
				for (ItemStack b : aInputs) {
					if (a.getItem() == b.getItem()) {
						if (a.getItemDamage() == b.getItemDamage()) {
							//Logger.INFO("Found matching Item Input - "+b.getUnlocalizedName());
							aMatchingInputs++;
							continue recipe;
						}
					}
				}
			}
			if (aMatchingInputs != aInputCount) {
				return false;
			}
		}
		int aFluidInputCount = aComparator.mFluidInputs.length;
		if (aFluidInputCount > 0) {
			//Logger.INFO("Looking for recipe with "+aFluidInputCount+" Fluids");
			int aMatchingFluidInputs = 0;
			recipe : for (FluidStack b : aComparator.mFluidInputs) {
				//Logger.INFO("Checking for fluid "+b.getLocalizedName());
				for (FluidStack a : aFluids) {
					if (GT_Utility.areFluidsEqual(a, b)) {
						//Logger.INFO("Found matching Fluid Input - "+b.getLocalizedName());
						aMatchingFluidInputs++;
						continue recipe;
					}
					else {
						//Logger.INFO("Found fluid which did not match - "+a.getLocalizedName());
					}
				}
			}
			if (aMatchingFluidInputs != aFluidInputCount) {
				return false;
			}
		}
		Logger.INFO("Recipes Match!");
		return true;
	}

	public GT_Recipe findRecipe(final GT_Recipe aRecipe, final long aVoltage, final long aSpecialValue, ItemStack[] aInputs, final FluidStack[] aFluids) {
		if (!mInitRecipeCache) {		
			initRecipeCaches();		
		}
		if (this.getRecipeMap().mRecipeList.isEmpty()) {
			log("No Recipes in Map to search through.");
			return null;
		}
		else {			
			log("Checking tier "+aSpecialValue+" recipes and below. Using Input Voltage of "+aVoltage+"V.");
			log("We have "+aInputs.length+" Items and "+aFluids.length+" Fluids.");
			// Try check the cached recipe first
			if (aRecipe != null) {
				if (areInputsEqual(aRecipe, aInputs, aFluids)) {
					if (aRecipe.mEUt <= aVoltage) {
						Logger.INFO("Using cached recipe.");
						return aRecipe;
					}
				}
			}

			// Get all recipes for the tier
			AutoMap<AutoMap<GT_Recipe>> aMasterMap = new AutoMap<AutoMap<GT_Recipe>>();
			for (long i=0;i<=aSpecialValue;i++) {
				aMasterMap.add(mTieredRecipeMap.get(i));
			}
			GT_Recipe aFoundRecipe = null;

			// Iterate the tiers recipes until we find the one with all inputs matching
			master : for (AutoMap<GT_Recipe> aTieredMap : aMasterMap) {
				for (GT_Recipe aRecipeToCheck : aTieredMap) {
					if (areInputsEqual(aRecipeToCheck, aInputs, aFluids)) {
						log("Found recipe with matching inputs!");
						if (aRecipeToCheck.mSpecialValue <= aSpecialValue) {
							if (aRecipeToCheck.mEUt <= aVoltage) {
								aFoundRecipe = aRecipeToCheck;
								break master;
							}
						}					
					}
				}
			}

			// If we found a recipe, return it
			if (aFoundRecipe != null) {
				log("Found valid recipe.");
				return aFoundRecipe;
			}
		}
		log("Did not find valid recipe.");
		return null;
	}

	/*
	 *  Catalyst Handling
	 */

	@Override
	public ArrayList<ItemStack> getStoredInputs() {
		ArrayList<ItemStack> tItems = super.getStoredInputs();
		for (GT_MetaTileEntity_Hatch_ElementalDataOrbHolder tHatch : mReplicatorDataOrbHatches) {
			tHatch.mRecipeMap = getRecipeMap();
			if (isValidMetaTileEntity(tHatch)) {                
				tItems.addAll(tHatch.getInventory());            	
			}
		}		
		return tItems;
	}

}
