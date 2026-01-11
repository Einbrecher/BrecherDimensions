/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.tinkstav.brecher_dim.generation;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

/**
 * Generates chunk positions in an outward spiral pattern from a center point.
 * Supports saving/restoring state for resumable generation.
 *
 * <p>Spiral direction: Starts at center, first step is north (-Z), then turns
 * right (clockwise when viewed from above): N -> E -> S -> W -> N...
 *
 * <p>The {@link #next()} method returns the current position BEFORE advancing,
 * meaning the first call returns the center chunk.
 *
 * <p>Internal state variables (stepsInCurrentDirection, sidesCompleted, etc.) use int
 * which is safe even for very large radii since they grow linearly with radius, not quadratically.
 * Only {@link #getTotalChunks()} returns long to handle the quadratic growth of total chunk count.
 *
 * <p><b>Maximum Radius:</b> The theoretical maximum supported radius is approximately
 * 1,073,741,823 chunks (Integer.MAX_VALUE / 2), but practical limits are much lower
 * due to memory and time constraints. For reference:
 * <ul>
 *   <li>radius=100 = 40,401 chunks (small server spawn area)</li>
 *   <li>radius=1,000 = 4,004,001 chunks (medium exploration area)</li>
 *   <li>radius=10,000 = 400,040,001 chunks (large world border - will take hours/days)</li>
 *   <li>radius=100,000 = ~40 billion chunks (impractical - will take weeks/months)</li>
 * </ul>
 * A warning is logged for radius values greater than 100,000 chunks.
 */
public class SpiralIterator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int LARGE_RADIUS_WARNING_THRESHOLD = 100_000;
    private int centerX;
    private int centerZ;
    private int maxRadius;
    
    // Spiral state
    private int currentX;
    private int currentZ;
    private int dx = 0;
    private int dz = -1;
    private int stepsInCurrentDirection = 0;
    private int stepsInCurrentSide = 1;
    private int sidesCompleted = 0;
    
    /**
     * Create a new spiral iterator starting from the center
     *
     * @param centerX center X coordinate in chunk coordinates
     * @param centerZ center Z coordinate in chunk coordinates
     * @param maxRadius maximum radius in chunks (must be non-negative)
     * @throws IllegalArgumentException if maxRadius is negative
     */
    public SpiralIterator(int centerX, int centerZ, int maxRadius) {
        if (maxRadius < 0) {
            throw new IllegalArgumentException("maxRadius cannot be negative: " + maxRadius);
        }
        if (maxRadius > LARGE_RADIUS_WARNING_THRESHOLD) {
            LOGGER.warn("Very large radius {} requested - this will generate approximately {} chunks",
                maxRadius, getTotalChunksForRadius(maxRadius));
        }
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.maxRadius = maxRadius;
        this.currentX = 0;
        this.currentZ = 0;
    }

    /**
     * Calculate total chunks for a given radius (static helper for validation).
     */
    private static long getTotalChunksForRadius(int radius) {
        long diameter = 2L * radius + 1;
        return diameter * diameter;
    }
    
    /**
     * Restore a spiral iterator from saved state
     */
    public SpiralIterator(int centerX, int centerZ, int maxRadius, 
                         int currentX, int currentZ, int dx, int dz,
                         int stepsInCurrentDirection, int stepsInCurrentSide, int sidesCompleted) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.maxRadius = maxRadius;
        this.currentX = currentX;
        this.currentZ = currentZ;
        this.dx = dx;
        this.dz = dz;
        this.stepsInCurrentDirection = stepsInCurrentDirection;
        this.stepsInCurrentSide = stepsInCurrentSide;
        this.sidesCompleted = sidesCompleted;
    }
    
    /**
     * Check if there are more chunks to generate
     */
    public boolean hasNext() {
        int distX = Math.abs(currentX);
        int distZ = Math.abs(currentZ);
        int currentRadius = Math.max(distX, distZ);
        return currentRadius <= maxRadius;
    }
    
    /**
     * Get the next chunk position in the spiral
     */
    public ChunkPos next() {
        if (!hasNext()) {
            return null;
        }
        
        // Save current position to return
        ChunkPos result = new ChunkPos(centerX + currentX, centerZ + currentZ);
        
        // Move to next position in spiral
        currentX += dx;
        currentZ += dz;
        stepsInCurrentDirection++;
        
        // Check if we need to turn
        if (stepsInCurrentDirection >= stepsInCurrentSide) {
            stepsInCurrentDirection = 0;
            
            // Turn right: (dx, dz) -> (-dz, dx)
            int temp = dx;
            dx = -dz;
            dz = temp;
            
            sidesCompleted++;
            
            // Every 2 sides, increase the side length
            if (sidesCompleted % 2 == 0) {
                stepsInCurrentSide++;
            }
        }
        
        return result;
    }
    
    /**
     * Get the current position in chunk coordinates
     */
    public ChunkPos getCurrentPos() {
        return new ChunkPos(centerX + currentX, centerZ + currentZ);
    }
    
    /**
     * Calculate total chunks in the generation area.
     * Returns long to prevent integer overflow for large radii.
     * For radius > 23,170, the total exceeds Integer.MAX_VALUE (2.1B).
     *
     * @return total number of chunks in the generation area (side^2)
     */
    public long getTotalChunks() {
        // Area of a square with side length (2 * maxRadius + 1)
        // Use 2L to force long arithmetic and prevent overflow
        long side = 2L * maxRadius + 1;
        return side * side;
    }

    /**
     * Calculate approximate progress percentage.
     *
     * @param chunksVisited number of chunks visited so far
     * @return progress percentage (0-100)
     */
    public int getProgressPercent(long chunksVisited) {
        long total = getTotalChunks();
        if (total == 0) return 100;
        // Use long arithmetic to prevent overflow
        return (int) Math.min(100L, (chunksVisited * 100L) / total);
    }
    
    // State getters for persistence
    public int getCenterX() { return centerX; }
    public int getCenterZ() { return centerZ; }
    public int getMaxRadius() { return maxRadius; }
    public int getCurrentX() { return currentX; }
    public int getCurrentZ() { return currentZ; }
    public int getDx() { return dx; }
    public int getDz() { return dz; }
    public int getStepsInCurrentDirection() { return stepsInCurrentDirection; }
    public int getStepsInCurrentSide() { return stepsInCurrentSide; }
    public int getSidesCompleted() { return sidesCompleted; }
}