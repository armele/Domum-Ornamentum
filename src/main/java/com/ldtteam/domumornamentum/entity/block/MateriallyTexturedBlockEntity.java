package com.ldtteam.domumornamentum.entity.block;

import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlock;
import com.ldtteam.domumornamentum.client.model.data.MaterialTextureData;
import com.ldtteam.domumornamentum.client.model.properties.ModProperties;
import com.ldtteam.domumornamentum.util.MaterialTextureDataUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.ldtteam.domumornamentum.entity.block.ModBlockEntityTypes.MATERIALLY_TEXTURED;
import static net.minecraft.world.level.block.SlabBlock.TYPE;
public class MateriallyTexturedBlockEntity extends AbstractMateriallyTexturedBlockEntity
{

    private MaterialTextureData textureData = MaterialTextureData.EMPTY;
    private MaterialTextureData slabBottomTextureData = MaterialTextureData.EMPTY;
    private MaterialTextureData slabTopTextureData = MaterialTextureData.EMPTY;

    public MateriallyTexturedBlockEntity(BlockPos pos, BlockState state)
    {
        super(MATERIALLY_TEXTURED.get(), pos, state);
    }

    @Override
    public void updateTextureDataWith(final MaterialTextureData materialTextureData)
    {
        this.textureData = materialTextureData;
        if (this.textureData.isEmpty()) {
            this.textureData = MaterialTextureDataUtil.generateRandomTextureDataFrom(this.getBlockState().getBlock());
        }

        this.slabBottomTextureData = MaterialTextureData.EMPTY;
        this.slabTopTextureData = MaterialTextureData.EMPTY;
        this.requestModelDataUpdate();
    }

    /**
     * Allow recording of separate top and bottom slab textures.
     * @param bottomTextureData texture to apply to the bottom slab
     * @param topTextureData texture to apply to the top slab
     */
    public void updateSlabTextureDataWith(final MaterialTextureData bottomTextureData, final MaterialTextureData topTextureData)
    {
        this.slabBottomTextureData = normalizeTextureData(bottomTextureData);
        this.slabTopTextureData = normalizeTextureData(topTextureData);
        this.textureData = this.slabBottomTextureData;

        this.setChanged();
        this.requestModelDataUpdate();
    }

    @Override
    public @NotNull CompoundTag getUpdateTag()
    {
        return this.saveWithId();
    }

    @Override
    public void onDataPacket(final Connection net, final ClientboundBlockEntityDataPacket packet)
    {
        this.load(Objects.requireNonNull(packet.getTag()));
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag)
    {
        this.load(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Save additional tag information to the block entity.
     * Updated to support stacked slabs with separate texture data.
     */
    @Override
    public void saveAdditional(@NotNull final CompoundTag compound)
    {
        super.saveAdditional(compound);
        compound.put("textureData", textureData.serializeNBT());
        if (!slabBottomTextureData.isEmpty())
        {
            compound.put("slabBottomTextureData", slabBottomTextureData.serializeNBT());
        }
        if (!slabTopTextureData.isEmpty())
        {
            compound.put("slabTopTextureData", slabTopTextureData.serializeNBT());
        }
    }

    /**
     * Load additional tag information for the block entity.
     * Updated to support stacked slabs with separate texture data.
     */
    @Override
    public void load(@NotNull final CompoundTag nbt)
    {
        super.load(nbt);

        this.textureData = new MaterialTextureData();
        this.slabBottomTextureData = MaterialTextureData.EMPTY;
        this.slabTopTextureData = MaterialTextureData.EMPTY;
        if (nbt.contains("textureData", Tag.TAG_COMPOUND))
        {
            this.textureData.deserializeNBT(nbt.getCompound("textureData"));
            if (getBlockState().getBlock() instanceof IMateriallyTexturedBlock materiallyTexturedBlock)
            {
                final List<ResourceLocation> validKeys = new ArrayList<>();
                materiallyTexturedBlock.getComponents().forEach(key -> validKeys.add(key.getId()));
                final Map<ResourceLocation, Block> textureMap = new HashMap<>();
                for (Map.Entry<ResourceLocation, Block> entry : this.textureData.getTexturedComponents().entrySet())
                {
                    if (validKeys.contains(entry.getKey()))
                    {
                        textureMap.put(entry.getKey(), entry.getValue());
                    }
                }
                this.textureData = new MaterialTextureData(textureMap);
            }
        }
        if (nbt.contains("slabBottomTextureData", Tag.TAG_COMPOUND))
        {
            this.slabBottomTextureData = MaterialTextureData.deserializeFromNBT(nbt.getCompound("slabBottomTextureData"));
        }
        if (nbt.contains("slabTopTextureData", Tag.TAG_COMPOUND))
        {
            this.slabTopTextureData = MaterialTextureData.deserializeFromNBT(nbt.getCompound("slabTopTextureData"));
        }

        this.requestModelDataUpdate();
    }

    /**
     * Retrieves model data.domum_ornamentum
     * Updated to support stacked slabs with separate texture data.
     */
    @NotNull
    @Override
    public ModelData getModelData()
    {
        final ModelData.Builder builder = ModelData.builder()
          .with(ModProperties.MATERIAL_TEXTURE_PROPERTY, this.textureData);

        if (this.getBlockState().hasProperty(TYPE) && this.getBlockState().getValue(TYPE) == SlabType.DOUBLE)
        {
            builder.with(ModProperties.SLAB_BOTTOM_TEXTURE_PROPERTY, getSlabBottomTextureData())
              .with(ModProperties.SLAB_TOP_TEXTURE_PROPERTY, getSlabTopTextureData());
        }

        return builder.build();
    }

    @Override
    @NotNull
    public MaterialTextureData getTextureData()
    {
        return textureData;
    }

    /**
     * Returns bottom slab-specific texture data.
     * @return
     */
    @NotNull
    public MaterialTextureData getSlabBottomTextureData()
    {
        return slabBottomTextureData.isEmpty() ? textureData : slabBottomTextureData;
    }

    /**
     * Returns top slab-specific texture data.
     * @return
     */
    @NotNull
    public MaterialTextureData getSlabTopTextureData()
    {
        return slabTopTextureData.isEmpty() ? textureData : slabTopTextureData;
    }

    /**
     * Protect against empty materialTextureData by providing a fallback.
     * Strategy replicated from similar use in many other Domum block items.
     * @param materialTextureData
     * @return
     */
    private MaterialTextureData normalizeTextureData(final MaterialTextureData materialTextureData)
    {
        if (!materialTextureData.isEmpty())
        {
            return materialTextureData;
        }

        return MaterialTextureDataUtil.generateRandomTextureDataFrom(this.getBlockState().getBlock());
    }
}
