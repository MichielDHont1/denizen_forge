package com.denizenscript.denizen.nms.interfaces;

import com.denizenscript.denizen.nms.util.PlayerProfile;
import com.denizenscript.denizen.nms.util.jnbt.CompoundTag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.List;

public interface BlockHelper {


    void applyPhysics(Location location);

    PlayerProfile getPlayerProfile(Skull skull);

    void setPlayerProfile(Skull skull, PlayerProfile playerProfile);

    CompoundTag getNbtData(Block block);

    void setNbtData(Block block, CompoundTag compoundTag);

    boolean hasBlock(Material material);

    boolean setBlockResistance(Material material, float resistance);

    float getBlockResistance(Material material);

    default BlockState generateBlockState(Material mat) {
        return null;
    }

    List<Location> getBlocksList(PortalCreateEvent event);

    default String getPushReaction(Material mat) {
        throw new UnsupportedOperationException();
    }

    default void setPushReaction(Material mat, String reaction) {
        throw new UnsupportedOperationException();
    }

    default float getBlockStength(Material mat) {
        throw new UnsupportedOperationException();
    }

    default void setBlockStrength(Material mat, float strength) {
        throw new UnsupportedOperationException();
    }

    static String getMaterialNameFromBlockData(String text) {
        int openBracket = text.indexOf('[');
        String material = text;
        if (openBracket > 0) {
            material = text.substring(0, openBracket);
        }
        if (material.startsWith("minecraft:")) {
            material = material.substring("minecraft:".length());
        }
        return material;
    }

    default BlockData parseBlockData(String text) {
        int openBracket = text.indexOf('[');
        String material = text;
        String otherData = null;
        if (openBracket > 0) {
            material = text.substring(0, openBracket);
            otherData = text.substring(openBracket);
        }
        if (material.startsWith("minecraft:")) {
            material = material.substring("minecraft:".length());
        }
        return parseBlockData(Material.getMaterial(material.toUpperCase()), otherData);
    }

    default BlockData parseBlockData(Material material, String otherData) {
        throw new UnsupportedOperationException();
    }

    default void makeBlockStateRaw(BlockState state) {
        throw new UnsupportedOperationException();
    }

    default void doRandomTick(Location location) {
        throw new UnsupportedOperationException();
    }
}
