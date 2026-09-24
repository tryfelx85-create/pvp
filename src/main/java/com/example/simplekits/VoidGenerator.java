package com.example.simplekits;

import org.bukkit.generator.ChunkGenerator;

/**
 * A chunk generator that produces nothing at all: no terrain, no caves,
 * no decorations, no structures, no mobs. Used to create the arena world
 * so it is pure void aside from the platform we build ourselves.
 */
public final class VoidGenerator extends ChunkGenerator {

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }
}
