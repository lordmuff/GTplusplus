package gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.*;
import static gregtech.api.enums.GT_HatchElement.*;
import static gregtech.api.util.GT_OreDictUnificator.getAssociation;
import static gregtech.api.util.GT_StructureUtility.buildHatchAdder;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.*;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TAE;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Energy;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Input;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_InputBus;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Muffler;
import gregtech.api.util.*;
import gtPlusPlus.api.objects.data.AutoMap;
import gtPlusPlus.core.block.ModBlocks;
import gtPlusPlus.core.item.chemistry.general.ItemGenericChemBase;
import gtPlusPlus.core.lib.CORE;
import gtPlusPlus.core.material.ELEMENT;
import gtPlusPlus.core.util.minecraft.ItemUtils;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.GregtechMeta_MultiBlockBase;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.nbthandlers.GT_MetaTileEntity_Hatch_Catalysts;
import gtPlusPlus.xmod.gregtech.common.blocks.textures.TexturesGtBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.tuple.Pair;

public class GregtechMetaTileEntity_QuantumForceTransformer
        extends GregtechMeta_MultiBlockBase<GregtechMetaTileEntity_QuantumForceTransformer>
        implements ISurvivalConstructable {

    private int mCasing;
    protected int mChamberTier = 0;
    protected int mFabCoilTier = 0;
    protected int mGlassTier = 0;
    protected int mMinimumMufflerTier = 0;
    private boolean mSeparateInputBusses = false;
    private boolean mFluidMode = false, doFermium = false;
    private Fluid mNeptunium = ELEMENT.getInstance().NEPTUNIUM.getPlasma();
    private Fluid mFermium = ELEMENT.getInstance().FERMIUM.getPlasma();
    private GT_MetaTileEntity_Hatch_Input mNeptuniumHatch;
    private GT_MetaTileEntity_Hatch_Input mFermiumHatch;
    private IStructureDefinition<GregtechMetaTileEntity_QuantumForceTransformer> STRUCTURE_DEFINITION = null;

    private final ArrayList<GT_MetaTileEntity_Hatch_Catalysts> mCatalystBuses =
            new ArrayList<GT_MetaTileEntity_Hatch_Catalysts>();

    public GregtechMetaTileEntity_QuantumForceTransformer(
            final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GregtechMetaTileEntity_QuantumForceTransformer(final String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(final IGregTechTileEntity aTileEntity) {
        return new GregtechMetaTileEntity_QuantumForceTransformer(this.mName);
    }

    @Override
    public String getMachineType() {
        return "Quantum Force Transformer";
    }

    @Override
    protected GT_Multiblock_Tooltip_Builder createTooltip() {
        GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
        tt.addMachineType(getMachineType())
                .addInfo("Controller Block for the Quantum Force Transformer")
                .addInfo("Allows Complex chemical lines to be performed instantly")
                .addInfo("Requires 1 Catalyst Housing, all recipes need a catalyst")
                .addInfo("All inputs go on the bottom, all outputs go on the top")
                .addInfo("Accepts TecTech Energy and Laser Hatches")
                .addInfo("Put a circuit in the controller to specify the focused output.")
                .addInfo("Uses FocusTier*4*sqrt(parallels) Neptunium Plasma for focusing")
                .addInfo("Can use FocusTier*4*sqrt(parallels) Fermium Plasma for additional chance output")
                .addInfo("This multi gives bonuses when all casings of some types are upgraded")
                .addInfo("Casing functions:")
                .addInfo("Neutron Pulse Manipulators: Recipe Tier Allowed")
                .addInfo("Neutron Shielding Cores: Focusing Tier")
                .addPollutionAmount(getPollutionPerSecond(null))
                .addSeparator()
                .beginStructureBlock(15, 21, 15, true) // @Steelux TODO
                .addController("Bottom Center")
                .addCasingInfo("Bulk Production Frame", 96)
                .addCasingInfo("Quantum Force Conductor", 177)
                .addCasingInfo("Particle Containment Casing", 224)
                .addCasingInfo("Neutron Shielding Cores", 234)
                .addCasingInfo("Neutron Pulse Manipulators", 142)
                .addInputBus("Bottom Layer", 4)
                .addInputHatch("Bottom Layer", 4)
                .addOutputHatch("Top Layer", 5)
                .addOutputBus("Top Layer", 5)
                .addEnergyHatch("Bottom Layer", 4)
                .addMaintenanceHatch("Bottom Layer", 4)
                .addStructureHint("Catalyst Housing", 4)
                .addMufflerHatch("Top Layer (except edges), x21", 5)
                .toolTipFinisher(CORE.GT_Tooltip_Builder);
        return tt;
    }

    @Override
    public IStructureDefinition<GregtechMetaTileEntity_QuantumForceTransformer> getStructureDefinition() {
        if (this.STRUCTURE_DEFINITION == null) {
            this.STRUCTURE_DEFINITION = StructureDefinition.<GregtechMetaTileEntity_QuantumForceTransformer>builder()
                    .addShape(this.mName, new String[][] { // A - 142, B - 234, C - 177, D - 96, E - 224, H - 36, M - 21
                        {
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "      ABA      ",
                            "   AAAABAAAA   ",
                            "   ABBBBBBBA   ",
                            "   ABAABAABA   ",
                            "   AB     BA   ",
                            "    B     B    ",
                            "    B     B    ",
                            "    B     B    "
                        },
                        {
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "      ABA      ",
                            "   BBBAAABBB   ",
                            "  ABBBBBBBBBA  ",
                            "  A         A  ",
                            "  B         B  ",
                            "  B         B  ",
                            "               ",
                            "               ",
                            "               "
                        },
                        {
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "      ABA      ",
                            "    BB   BB    ",
                            "  BB       BB  ",
                            " ABB       BBA ",
                            " A           A ",
                            " B           B ",
                            " B           B ",
                            "               ",
                            "               ",
                            "               "
                        },
                        {
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "     ABBBA     ",
                            "   BB     BB   ",
                            " BB         BB ",
                            "ABB         BBA",
                            "A             A",
                            "B             B",
                            "B             B",
                            "B             B",
                            "B             B",
                            "B             B"
                        },
                        {
                            "      TTT      ",
                            "      EEE      ",
                            "      EEE      ",
                            "      EEE      ",
                            "      DDD      ",
                            "      EEE      ",
                            "      DDD      ",
                            "      EEE      ",
                            "      EEE      ",
                            "      EEE      ",
                            "      DDD      ",
                            "    ABEEEBA    ",
                            "  BB  EEE  BB  ",
                            " B    EEE    B ",
                            "AB    DDD    BA",
                            "A     EEE     A",
                            "A     DDD     A",
                            "      EEE      ",
                            "      EEE      ",
                            "      EEE      ",
                            "      H~H      "
                        },
                        {
                            "     TTTTT     ",
                            "     ECCCE     ",
                            "     ECCCE     ",
                            "     ECCCE     ",
                            "     D   D     ",
                            "     ECCCE     ",
                            "     D   D     ",
                            "     ECCCE     ",
                            "     ECCCE     ",
                            "     ECCCE     ",
                            "     D   D     ",
                            "   ABECCCEBA   ",
                            "  B  ECCCE  B  ",
                            " B   ECCCE   B ",
                            "AB   D   D   BA",
                            "A    ECCCE    A",
                            "A    D   D    A",
                            "A    ECCCE    A",
                            "     ECCCE     ",
                            "     ECCCE     ",
                            "     HHHHH     "
                        },
                        {
                            "    TTTTTTT    ",
                            "    ECCCCCE    ",
                            "    EC   CE    ",
                            "    EC   CE    ",
                            "    D     D    ",
                            "    EC   CE    ",
                            "    D     D    ",
                            "    EC   CE    ",
                            "    EC   CE    ",
                            "    EC   CE    ",
                            "    D     D    ",
                            "  ABEC   CEBA  ",
                            " A  EC   CE  A ",
                            "AA  EC   CE  AA",
                            "AB  D     D  BA",
                            "B   EC   CE   B",
                            "B   D     D   B",
                            "B   EC   CE   B",
                            "    EC   CE    ",
                            "    EC   CE    ",
                            "    HHHHHHH    "
                        },
                        {
                            "    TTTTTTT    ",
                            "    ECCCCCE    ",
                            "    EC   CE    ",
                            "    EC   CE    ",
                            "    D     D    ",
                            "    EC   CE    ",
                            "    D     D    ",
                            "    EC   CE    ",
                            "    EC   CE    ",
                            "    EC   CE    ",
                            "    D     D    ",
                            "  BBEC   CEBB  ",
                            " B  EC   CE  B ",
                            "BA  EC   CE  AB",
                            "BB  D     D  BB",
                            "B   EC   CE   B",
                            "B   D     D   B",
                            "    EC   CE    ",
                            "    EC   CE    ",
                            "    EC   CE    ",
                            "    HHHHHHH    "
                        },
                        {
                            "    TTTTTTT    ",
                            "    ECCCCCE    ",
                            "    EC   CE    ",
                            "    EC   CE    ",
                            "    D     D    ",
                            "    EC   CE    ",
                            "    D     D    ",
                            "    EC   CE    ",
                            "    EC   CE    ",
                            "    EC   CE    ",
                            "    D     D    ",
                            "  ABEC   CEBA  ",
                            " A  EC   CE  A ",
                            "AA  EC   CE  AA",
                            "AB  D     D  BA",
                            "B   EC   CE   B",
                            "B   D     D   B",
                            "B   EC   CE   B",
                            "    EC   CE    ",
                            "    EC   CE    ",
                            "    HHHHHHH    "
                        },
                        {
                            "     TTTTT     ",
                            "     ECCCE     ",
                            "     ECCCE     ",
                            "     ECCCE     ",
                            "     D   D     ",
                            "     ECCCE     ",
                            "     D   D     ",
                            "     ECCCE     ",
                            "     ECCCE     ",
                            "     ECCCE     ",
                            "     D   D     ",
                            "   ABECCCEBA   ",
                            "  B  ECCCE  B  ",
                            " B   ECCCE   B ",
                            "AB   D   D   BA",
                            "A    ECCCE    A",
                            "A    D   D    A",
                            "A    ECCCE    A",
                            "     ECCCE     ",
                            "     ECCCE     ",
                            "     HHHHH     "
                        },
                        {
                            "      TTT      ",
                            "      EEE      ",
                            "      EEE      ",
                            "      EEE      ",
                            "      DDD      ",
                            "      EEE      ",
                            "      DDD      ",
                            "      EEE      ",
                            "      EEE      ",
                            "      EEE      ",
                            "      DDD      ",
                            "    ABEEEBA    ",
                            "  BB  EEE  BB  ",
                            " B    EEE    B ",
                            "AB    DDD    BA",
                            "A     EEE     A",
                            "A     DDD     A",
                            "      EEE      ",
                            "      EEE      ",
                            "      EEE      ",
                            "      ZHX      "
                        },
                        {
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "     ABBBA     ",
                            "   BB     BB   ",
                            " BB         BB ",
                            "ABB          BA",
                            "A             A",
                            "B             B",
                            "B             B",
                            "B             B",
                            "B             B",
                            "B             B"
                        },
                        {
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "      ABA      ",
                            "    BB   BB    ",
                            "  BB       BB  ",
                            " ABB        BA ",
                            " A           A ",
                            " B           B ",
                            " B           B ",
                            "               ",
                            "               ",
                            "               "
                        },
                        {
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "      ABA      ",
                            "   BBBAAABBB   ",
                            "  ABBBBBBBBBA  ",
                            "  A         A  ",
                            "  B         B  ",
                            "  B         B  ",
                            "               ",
                            "               ",
                            "               "
                        },
                        {
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "               ",
                            "      ABA      ",
                            "   AAAABAAAA   ",
                            "   AAABBBAAA   ",
                            "   BAABBBAAB   ",
                            "   B AB BA B   ",
                            "      B B      ",
                            "      B B      ",
                            "      B B      "
                        },
                    })
                    .addElement(
                            'A',
                            StructureUtility.ofBlocksTiered(
                                    chamberTierConverter(),
                                    getAllChamberTiers(),
                                    0,
                                    GregtechMetaTileEntity_QuantumForceTransformer::setChamberTier,
                                    GregtechMetaTileEntity_QuantumForceTransformer::getChamberTier))
                    .addElement(
                            'B',
                            StructureUtility.ofBlocksTiered(
                                    fabCoilTierConverter(),
                                    getAllFabCoilTiers(),
                                    0,
                                    GregtechMetaTileEntity_QuantumForceTransformer::setFabCoilTier,
                                    GregtechMetaTileEntity_QuantumForceTransformer::getFabCoilTier))
                    .addElement('C', ofBlock(ModBlocks.blockCasings4Misc, 4))
                    .addElement('D', ofBlock(ModBlocks.blockCasings2Misc, 12))
                    .addElement(
                            'E',
                            StructureUtility.ofBlocksTiered(
                                    glassTierConverter(),
                                    getAllGlassTiers(),
                                    0,
                                    GregtechMetaTileEntity_QuantumForceTransformer::setGlassTier,
                                    GregtechMetaTileEntity_QuantumForceTransformer::getGlassTier))
                    .addElement(
                            'H',
                            ofChain(
                                    buildHatchAdder(GregtechMetaTileEntity_QuantumForceTransformer.class)
                                            .hatchClass(GT_MetaTileEntity_Hatch_Catalysts.class)
                                            .adder(GregtechMetaTileEntity_QuantumForceTransformer::addCatalystHousing)
                                            .casingIndex(TAE.getIndexFromPage(0, 10))
                                            .dot(4)
                                            .build(),
                                    buildHatchAdder(GregtechMetaTileEntity_QuantumForceTransformer.class)
                                            .atLeast(InputBus, InputHatch, Maintenance, Energy, ExoticEnergy)
                                            .casingIndex(TAE.getIndexFromPage(0, 10))
                                            .dot(4)
                                            .buildAndChain(onElementPass(
                                                    x -> ++x.mCasing, ofBlock(ModBlocks.blockCasings2Misc, 12)))))
                    .addElement(
                            'T',
                            buildHatchAdder(GregtechMetaTileEntity_QuantumForceTransformer.class)
                                    .atLeast(OutputBus, OutputHatch, Maintenance)
                                    .casingIndex(TAE.getIndexFromPage(0, 10))
                                    .dot(5)
                                    .buildAndChain(
                                            onElementPass(x -> ++x.mCasing, ofBlock(ModBlocks.blockCasings2Misc, 12))))
                    .addElement(
                            'Z',
                            buildHatchAdder(GregtechMetaTileEntity_QuantumForceTransformer.class)
                                    .hatchClass(GT_MetaTileEntity_Hatch_Input.class)
                                    .adder(GregtechMetaTileEntity_QuantumForceTransformer::addNeptuniumHatch)
                                    .casingIndex(TAE.getIndexFromPage(0, 10))
                                    .dot(5)
                                    .buildAndChain(
                                            onElementPass(x -> ++x.mCasing, ofBlock(ModBlocks.blockCasings2Misc, 12))))
                    .addElement(
                            'X',
                            buildHatchAdder(GregtechMetaTileEntity_QuantumForceTransformer.class)
                                    .hatchClass(GT_MetaTileEntity_Hatch_Input.class)
                                    .adder(GregtechMetaTileEntity_QuantumForceTransformer::addFermiumHatch)
                                    .casingIndex(TAE.getIndexFromPage(0, 10))
                                    .dot(5)
                                    .buildAndChain(
                                            onElementPass(x -> ++x.mCasing, ofBlock(ModBlocks.blockCasings2Misc, 12))))
                    .build();
        }
        return this.STRUCTURE_DEFINITION;
    }

    public final boolean addCatalystHousing(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) {
            return false;
        } else {
            IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
            if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Catalysts) {
                return addToMachineList(aTileEntity, aBaseCasingIndex);
            }
        }
        return false;
    }

    @Override
    public boolean addToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        final IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) {
            return false;
        }
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Catalysts) {
            log("Found GT_MetaTileEntity_Hatch_Catalysts");
            return addToMachineListInternal(mCatalystBuses, aMetaTileEntity, aBaseCasingIndex);
        }
        return super.addToMachineList(aTileEntity, aBaseCasingIndex);
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        this.mCasing = 0;
        this.mMinimumMufflerTier = 0;
        mCatalystBuses.clear();
        if (checkPiece(this.mName, 7, 20, 4)
                && checkHatch()
                && (mTecTechEnergyHatches.size() >= 1 || mEnergyHatches.size() >= 1)) {
            return true;
        }
        return false;
    }

    @Override // The lowest tier muffler of the total 21 will decide the muffler tier in the multi
    public boolean addMufflerToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) {
            return false;
        } else {
            IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
            if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Muffler) {
                if (mMinimumMufflerTier == 0
                        || ((GT_MetaTileEntity_Hatch_Muffler) aMetaTileEntity).mTier < mMinimumMufflerTier) {
                    mMinimumMufflerTier = ((GT_MetaTileEntity_Hatch_Muffler) aMetaTileEntity).mTier;
                }
                return addToMachineList(aTileEntity, aBaseCasingIndex);
            }
        }
        return false;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(this.mName, stackSize, hintsOnly, 7, 20, 4);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, IItemSource source, EntityPlayerMP actor) {
        if (this.mMachine) return -1;
        return survivialBuildPiece(this.mName, stackSize, 7, 20, 4, elementBudget, source, actor, false, true);
    }

    public static List<Pair<Block, Integer>> getAllChamberTiers() {
        return new ArrayList<Pair<Block, Integer>>() {
            {
                add(Pair.of(ModBlocks.blockCasings5Misc, 7));
                add(Pair.of(ModBlocks.blockCasings5Misc, 8));
                add(Pair.of(ModBlocks.blockCasings5Misc, 9));
                add(Pair.of(ModBlocks.blockCasings5Misc, 10));
            }
        };
    }

    public static List<Pair<Block, Integer>> getAllFabCoilTiers() {
        return new ArrayList<Pair<Block, Integer>>() {
            {
                add(Pair.of(ModBlocks.blockCasings5Misc, 11));
                add(Pair.of(ModBlocks.blockCasings5Misc, 12));
                add(Pair.of(ModBlocks.blockCasings5Misc, 13));
                add(Pair.of(ModBlocks.blockCasings5Misc, 14));
            }
        };
    }

    public static List<Pair<Block, Integer>> getAllGlassTiers() {
        return new ArrayList<Pair<Block, Integer>>() {
            {
                add(Pair.of(ModBlocks.blockCasings5Misc, 15));
            }
        };
    }

    public static ITierConverter<Integer> chamberTierConverter() {
        return (block, meta) -> {
            if (block == null) {
                return -1;
            } else if (block == ModBlocks.blockCasings5Misc) { // Resonance Chambers
                switch (meta) {
                    case 7:
                        return 1;
                    case 8:
                        return 2;
                    case 9:
                        return 3;
                    case 10:
                        return 4;
                }
            }
            return -1;
        };
    }

    public static ITierConverter<Integer> fabCoilTierConverter() {
        return (block, meta) -> {
            if (block == null) {
                return -1;
            } else if (block == ModBlocks.blockCasings5Misc) { // Generation Coils
                switch (meta) {
                    case 11:
                        return 1;
                    case 12:
                        return 2;
                    case 13:
                        return 3;
                    case 14:
                        return 4;
                }
            }
            return -1;
        };
    }

    public static ITierConverter<Integer> glassTierConverter() {
        return (block, meta) -> {
            if (block == null) {
                return -1;
            } else if (block == ModBlocks.blockCasings5Misc) { // Glass
                return 1;
            }
            return -1;
        };
    }

    private void setChamberTier(int tier) {
        mChamberTier = tier;
    }

    private void setFabCoilTier(int tier) {
        mFabCoilTier = tier;
    }

    private void setGlassTier(int tier) {
        mGlassTier = tier;
    }

    private int getChamberTier() {
        return mChamberTier;
    }

    private int getFabCoilTier() {
        return mFabCoilTier;
    }

    private int getGlassTier() {
        return mGlassTier;
    }

    @Override
    public String getSound() {
        return GregTech_API.sSoundList.get(Integer.valueOf(208));
    }

    @Override
    protected IIconContainer getActiveOverlay() {
        return TexturesGtBlock.Overlay_Machine_Controller_Advanced_Active;
    }

    @Override
    protected IIconContainer getInactiveOverlay() {
        return TexturesGtBlock.Overlay_Machine_Controller_Advanced;
    }

    @Override
    protected int getCasingTextureId() {
        return 70;
    }

    @Override
    public boolean hasSlotInGUI() {
        return true;
    }

    @Override
    public String getCustomGUIResourceName() {
        return null;
    }

    @Override
    public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        return GTPP_Recipe.GTPP_Recipe_Map.sQuantumForceTransformerRecipes;
    }

    @Override
    public boolean isCorrectMachinePart(final ItemStack aStack) {
        return true;
    }

    @Override
    public long getMaxInputVoltage() {
        long rVoltage = 0;
        for (GT_MetaTileEntity_Hatch_Energy tHatch : mEnergyHatches)
            if (isValidMetaTileEntity(tHatch)) {
                rVoltage += tHatch.getBaseMetaTileEntity().getInputVoltage()
                        * tHatch.getBaseMetaTileEntity().getInputAmperage();
            }
        return rVoltage;
    }

    private int mMaxParallel = 64;
    private int mCurrentParallel = 0;

    @Override
    public boolean checkRecipe(final ItemStack aStack) {
        int mCurrentParallel = 0;
        this.mEUt = 0;
        this.mMaxProgresstime = 0;
        this.mOutputItems = null;
        this.mOutputFluids = null;
        FluidStack[] tFluidList = getCompactedFluids();
        ItemStack[] tCatalysts = mCatalystBuses.get(0).mInventory;
        if (mSeparateInputBusses) {
            ArrayList<ItemStack> tInputList = new ArrayList<ItemStack>();
            for (GT_MetaTileEntity_Hatch_InputBus tBus : mInputBusses) {
                for (int i = tBus.getSizeInventory() - 1; i >= 0; i--) {
                    if (tBus.getStackInSlot(i) != null) {
                        tInputList.add(tBus.getStackInSlot(i));
                        if (ItemUtils.isCatalyst(tBus.getStackInSlot(i))) {
                            if (mCurrentParallel < mMaxParallel) {
                                mCurrentParallel += tBus.getStackInSlot(i).stackSize;
                                if (mCurrentParallel >= mMaxParallel) {
                                    mCurrentParallel = mMaxParallel;
                                }
                            }
                        }
                    }
                }
                ItemStack[] tInputs = tInputList.toArray(new ItemStack[0]);
                if (processRecipe(tInputs, tFluidList, getRecipeMap(), aStack)) return true;
                else tInputList.clear();
            }
        } else {
            ItemStack[] tInputList = getCompactedInputs();
            return processRecipe(tInputList, tFluidList, getRecipeMap(), aStack);
        }

        return false;
    }

    private boolean processRecipe(
            ItemStack[] aItemInputs, FluidStack[] aFluidInputs, GT_Recipe.GT_Recipe_Map aRecipeMap, ItemStack aStack) {
        long tVoltage = GT_ExoticEnergyInputHelper.getMaxInputVoltageMulti(mAllEnergyHatches);
        byte tTier = (byte) Math.max(1, GT_Utility.getTier(tVoltage));
        GT_Recipe tRecipe = aRecipeMap
                .findRecipe(
                        getBaseMetaTileEntity(),
                        false,
                        gregtech.api.enums.GT_Values.V[tTier],
                        aFluidInputs,
                        aItemInputs)
                .copy();

        if (tRecipe != null && tRecipe.mSpecialValue <= getChamberTier()) {
            ItemStack aRecipeCatalyst = null;
            for (ItemStack tItem : tRecipe.mInputs) {
                if (ItemUtils.isCatalyst(tItem)) {
                    aRecipeCatalyst = tItem;
                    break;
                }
            }

            if (aRecipeCatalyst == null) {
                return false;
            }

            int mCurrentMaxParallel = 0;
            for (ItemStack tItem : aItemInputs) {
                if (ItemUtils.isCatalyst(tItem) && tItem.getItem() == aRecipeCatalyst.getItem()) {
                    mCurrentMaxParallel += tItem.stackSize;
                }

                if (mCurrentMaxParallel >= mMaxParallel) {
                    mCurrentMaxParallel = mMaxParallel;
                    break;
                }
            }

            if (mFermiumHatch != null) {
                if (mFermiumHatch.getFluid().getFluid() == mFermium) {
                    doFermium = true;
                }
            }

            mCurrentParallel = 0;
            while (mCurrentParallel <= mCurrentMaxParallel
                    && tRecipe.isRecipeInputEqual(true, aFluidInputs, aItemInputs)) {
                mCurrentParallel++;
            }

            this.mEfficiency = (10000 - (getIdealStatus() - getRepairStatus()) * 1000);
            this.mEfficiencyIncrease = 10000;

            calculateOverclockedNessMulti(
                    tRecipe.mEUt * mCurrentParallel, tRecipe.mDuration, mCurrentParallel, tVoltage);

            if (mMaxProgresstime == Integer.MAX_VALUE - 1 && mEUt == Integer.MAX_VALUE - 1) return false;

            if (this.mEUt > 0) {
                this.mEUt = (-this.mEUt);
            }

            int[] tChances;
            if (aStack == null
                    || aStack.getItemDamage() == 0
                    || mNeptuniumHatch.getFluid().getFluid() != mNeptunium) {
                tChances = new int[tRecipe.mOutputs.length];
                Arrays.fill(tChances, 10000 / tChances.length);
            } else {
                tChances = GetChanceOutputs(tRecipe, aStack.getItemDamage() - 1);
            }

            if (mFluidMode) {
                FluidStack[] tFluidOutputs = new FluidStack[tRecipe.mOutputs.length];
                for (int i = 0; i < tRecipe.mOutputs.length; i++) {
                    if (tRecipe.mOutputs[i] != null) {
                        Materials mat = getAssociation(tRecipe.mOutputs[i]).mMaterial.mMaterial;
                        tFluidOutputs[i] = mat.getMolten(0L);
                        if (getBaseMetaTileEntity().getRandomNumber(10000) < tChances[i])
                            tFluidOutputs[i].amount +=
                                    mat.getMolten(144L * tRecipe.mOutputs[i].stackSize).amount * mCurrentParallel;
                    }
                }

                this.mOutputFluids = tFluidOutputs;
            } else {
                ItemStack[] tOutputs = new ItemStack[tRecipe.mOutputs.length];
                for (int i = 0; i < tRecipe.mOutputs.length; i++) {
                    if (tRecipe.mOutputs[i] != null) {
                        tOutputs[i] = tRecipe.mOutputs[i].copy();
                        tOutputs[i].stackSize = 0;
                        if (getBaseMetaTileEntity().getRandomNumber(10000) < tChances[i])
                            tOutputs[i].stackSize += tRecipe.getOutput(i).stackSize * mCurrentParallel;
                    }
                }

                this.mOutputItems = tOutputs;
            }

            this.mMaxProgresstime = Math.max(1, this.mMaxProgresstime);
            updateSlots();

            return true;
        }
        return false;
    }

    private byte runningTick = 0;

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        if (!super.onRunningTick(aStack)) {
            return false;
        }

        if (runningTick % 20 == 0) {
            if (doFermium) {
                if (!depleteInput(
                        new FluidStack(mFermium, (int) (getFabCoilTier() * 4 * Math.sqrt(mCurrentParallel))))) {
                    criticalStopMachine();
                    return false;
                }
            }

            if (!depleteInput(new FluidStack(mNeptunium, (int) (getFabCoilTier() * 4 * Math.sqrt(mCurrentParallel))))) {
                criticalStopMachine();
                return false;
            }

            runningTick = 0;
        } else {
            runningTick++;
        }

        return true;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aBaseMetaTileEntity.isServerSide()) {
            if (this.mUpdate == 1 || this.mStartUpCheck == 1) {
                this.mCatalystBuses.clear();
            }
        }
        super.onPostTick(aBaseMetaTileEntity, aTick);
    }

    @Override
    public int getMaxParallelRecipes() {
        if (mGlassTier == 2) { // 4x bonus to parallel amount with upgraded Containment Chambers (glass)
            return 256;
        }
        return 64;
    }

    @Override
    public int getEuDiscountForParallelism() {
        return 0;
    }

    @Override
    public int getMaxEfficiency(final ItemStack aStack) {
        return 10000;
    }

    @Override
    public int getPollutionPerSecond(final ItemStack aStack) {
        return 0;
    }

    @Override
    public int getDamageToComponent(final ItemStack aStack) {
        return 0;
    }

    @Override
    public int getAmountOfOutputs() {
        return 2;
    }

    public int getMaxCatalystDurability() {
        return 50;
    }

    private int getDamage(ItemStack aStack) {
        return ItemGenericChemBase.getCatalystDamage(aStack);
    }

    private void setDamage(ItemStack aStack, int aAmount) {
        ItemGenericChemBase.setCatalystDamage(aStack, aAmount);
    }

    @Override
    public boolean explodesOnComponentBreak(final ItemStack aStack) {
        return false;
    }

    private int getCatalysts(
            ItemStack[] aItemInputs, ItemStack aRecipeCatalyst, int aMaxParallel, ArrayList<ItemStack> aOutPut) {
        int allowedParallel = 0;
        for (final ItemStack aInput : aItemInputs) {
            if (aRecipeCatalyst.isItemEqual(aInput)) {
                int aDurabilityRemaining = getMaxCatalystDurability() - getDamage(aInput);
                return Math.min(aMaxParallel, aDurabilityRemaining);
            }
        }
        return allowedParallel;
    }

    private ItemStack findCatalyst(ItemStack[] aItemInputs, ItemStack[] aRecipeInputs) {
        if (aItemInputs != null) {
            for (final ItemStack aInput : aItemInputs) {
                if (aInput != null) {
                    if (ItemUtils.isCatalyst(aInput)) {
                        for (ItemStack aRecipeInput : aRecipeInputs) {
                            if (GT_Utility.areStacksEqual(aRecipeInput, aInput, true)) {
                                return aInput;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void damageCatalyst(ItemStack aStack, int parallelRecipes) {
        // For now, catalysts don't get damaged
        // The current intended method of using catalysts in recipes is to
        // require 1 very expensive catalyst, but never damage it
    }

    @Override
    public ArrayList<ItemStack> getStoredInputs() {
        ArrayList<ItemStack> tItems = super.getStoredInputs();
        if (this.hasSlotInGUI() && this.getGUIItemStack() != null) {
            tItems.add(this.getGUIItemStack());
        }
        for (GT_MetaTileEntity_Hatch_Catalysts tHatch : mCatalystBuses) {
            tHatch.mRecipeMap = getRecipeMap();
            if (isValidMetaTileEntity(tHatch)) {
                AutoMap<ItemStack> aHatchContent = tHatch.getContentUsageSlots();
                if (!aHatchContent.isEmpty()) {
                    tItems.addAll(aHatchContent);
                }
            }
        }
        return tItems;
    }

    private int[] GetChanceOutputs(GT_Recipe tRecipe, int aChanceIncreased) {
        int difference = getFabCoilTier() - tRecipe.mSpecialValue;
        int aChancePerOutput = 10000 / tRecipe.mOutputs.length;
        int aOutputsAmount = tRecipe.mOutputs.length;
        int[] tChances = new int[aOutputsAmount];
        Arrays.fill(tChances, 10000 / aOutputsAmount);

        switch (difference) {
            case 0:
                for (int i = 0; i < tChances.length; i++) {
                    if (i == aChanceIncreased) {
                        tChances[i] = aChancePerOutput / 2 * (aOutputsAmount - 1);
                    } else {
                        tChances[i] /= 2;
                    }
                    if (doFermium) {
                        tChances[i] += (10000 - tChances[i]) / 4;
                    }
                }
                break;
            case 1:
                for (int i = 0; i < tChances.length; i++) {
                    if (i == aChanceIncreased) {
                        tChances[i] = aChancePerOutput * 3 / 4 * (aOutputsAmount - 1);
                    } else {
                        tChances[i] /= 4;
                    }
                    if (doFermium) {
                        tChances[i] += (10000 - tChances[i]) / 3;
                    }
                }
                break;
            case 2:
            case 3:
                for (int i = 0; i < tChances.length; i++) {
                    if (i == aChanceIncreased) {
                        tChances[i] = 10000;
                    } else {
                        tChances[i] = 0;
                    }
                    if (doFermium) {
                        tChances[i] += (10000 - tChances[i]) / 2;
                    }
                }
                break;
        }
        return tChances;
    }

    @Override
    public boolean onWireCutterRightClick(
            byte aSide, byte aWrenchingSide, EntityPlayer aPlayer, float aX, float aY, float aZ) {
        mSeparateInputBusses = !mSeparateInputBusses;
        GT_Utility.sendChatToPlayer(
                aPlayer, StatCollector.translateToLocal("GT5U.machines.separatebus") + " " + mSeparateInputBusses);
        return true;
    }

    public void onModeChangeByScrewdriver(byte aSide, EntityPlayer aPlayer, float aX, float aY, float aZ) {
        mFluidMode = !mFluidMode;
        GT_Utility.sendChatToPlayer(
                aPlayer, StatCollector.translateToLocal("miscutils.machines.QFTFluidMode") + " " + mFluidMode);
    }

    public boolean addNeptuniumHatch(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Input) {
            ((GT_MetaTileEntity_Hatch) aMetaTileEntity).updateTexture(aBaseCasingIndex);
            ((GT_MetaTileEntity_Hatch_Input) aMetaTileEntity).mRecipeMap = getRecipeMap();
            mNeptuniumHatch = (GT_MetaTileEntity_Hatch_Input) aMetaTileEntity;
            return true;
        }
        return false;
    }

    public boolean addFermiumHatch(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Input) {
            ((GT_MetaTileEntity_Hatch) aMetaTileEntity).updateTexture(aBaseCasingIndex);
            ((GT_MetaTileEntity_Hatch_Input) aMetaTileEntity).mRecipeMap = getRecipeMap();
            mFermiumHatch = (GT_MetaTileEntity_Hatch_Input) aMetaTileEntity;
            return true;
        }
        return false;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setBoolean("mSeparateInputBusses", mSeparateInputBusses);
        aNBT.setBoolean("mFluidMode", mFluidMode);
        aNBT.setBoolean("doFermium", doFermium);
        super.saveNBTData(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        this.mSeparateInputBusses = aNBT.getBoolean("mSeparateInputBusses");
        this.mFluidMode = aNBT.getBoolean("mFluidMode");
        this.doFermium = aNBT.getBoolean("doFermium");
        super.loadNBTData(aNBT);
    }
}