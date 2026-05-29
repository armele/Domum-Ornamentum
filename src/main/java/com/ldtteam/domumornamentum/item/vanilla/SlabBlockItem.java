package com.ldtteam.domumornamentum.item.vanilla;

import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlockComponent;
import com.ldtteam.domumornamentum.block.vanilla.SlabBlock;
import com.ldtteam.domumornamentum.client.model.data.MaterialTextureData;
import com.ldtteam.domumornamentum.entity.block.MateriallyTexturedBlockEntity;
import com.ldtteam.domumornamentum.item.interfaces.IDoItem;
import com.ldtteam.domumornamentum.util.BlockUtils;
import com.ldtteam.domumornamentum.util.Constants;
import com.ldtteam.domumornamentum.util.MaterialTextureDataUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SlabBlockItem extends BlockItem implements IDoItem
{
    private final SlabBlock slabBlock;

    public SlabBlockItem(final SlabBlock blockIn, final Properties builder)
    {
        super(blockIn, builder);
        this.slabBlock = blockIn;
    }

    @Override
    public InteractionResult place(final BlockPlaceContext context)
    {
        final BlockPos pos = context.getClickedPos();
        final Level level = context.getLevel();
        final BlockState existingState = level.getBlockState(pos);
        final MaterialTextureData existingTextureData;
        final SlabType existingSlabType;

        if (existingState.is(this.getBlock())
              && existingState.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)
              && existingState.getValue(net.minecraft.world.level.block.SlabBlock.TYPE) != SlabType.DOUBLE
              && level.getBlockEntity(pos) instanceof MateriallyTexturedBlockEntity mtbe)
        {
            existingTextureData = mtbe.getTextureData();
            existingSlabType = existingState.getValue(net.minecraft.world.level.block.SlabBlock.TYPE);
        }
        else
        {
            existingTextureData = MaterialTextureData.EMPTY;
            existingSlabType = null;
        }

        final MaterialTextureData incomingTextureData = MaterialTextureData.deserializeFromNBT(context.getItemInHand().getOrCreateTagElement("textureData"));
        final InteractionResult result = super.place(context);

        if (result.consumesAction()
              && existingSlabType != null
              && level.getBlockState(pos).is(this.getBlock())
              && level.getBlockState(pos).getValue(net.minecraft.world.level.block.SlabBlock.TYPE) == SlabType.DOUBLE
              && level.getBlockEntity(pos) instanceof MateriallyTexturedBlockEntity mtbe)
        {
            if (existingSlabType == SlabType.BOTTOM)
            {
                mtbe.updateSlabTextureDataWith(existingTextureData, incomingTextureData);
            }
            else
            {
                mtbe.updateSlabTextureDataWith(incomingTextureData, existingTextureData);
            }
        }

        return result;
    }

    @Override
    public Component getName(final ItemStack stack)
    {
        final CompoundTag dataNbt = stack.getOrCreateTagElement("textureData");
        final MaterialTextureData textureData = MaterialTextureData.deserializeFromNBT(dataNbt);

        final IMateriallyTexturedBlockComponent coverComponent = slabBlock.getComponents().get(0);
        final Block centerBlock = textureData.getTexturedComponents().getOrDefault(coverComponent.getId(), coverComponent.getDefault());
        final Component centerBlockName = BlockUtils.getHoverName(centerBlock);

        return Component.translatable(Constants.MOD_ID + ".slab.name.format", centerBlockName);
    }

    @Override
    public void appendHoverText(@NotNull final ItemStack stack, @Nullable final Level worldIn, @NotNull final List<Component> tooltip, @NotNull final TooltipFlag flagIn)
    {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(Component.translatable(Constants.MOD_ID + ".origin.tooltip"));

        final CompoundTag dataNbt = stack.getOrCreateTagElement("textureData");
        MaterialTextureData textureData = MaterialTextureData.deserializeFromNBT(dataNbt);
        if (textureData.isEmpty()) {
            textureData = MaterialTextureDataUtil.generateRandomTextureDataFrom(stack);
        }

        final IMateriallyTexturedBlockComponent component = slabBlock.getComponents().get(0);
        final Block block = textureData.getTexturedComponents().getOrDefault(component.getId(), component.getDefault());
        tooltip.add(Component.translatable(Constants.MOD_ID + ".desc.onlyone", Component.translatable(Constants.MOD_ID + ".desc.material", BlockUtils.getHoverName(block))));
    }

    @Override
    public ResourceLocation getGroup()
    {
        return new ResourceLocation(Constants.MOD_ID, "avanilla");
    }
}

