package net.tinkstav.brecher_dim.dimension;

import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.config.BrecherConfig;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Random;

/**
 * Manages seed generation for exploration dimensions
 * Supports multiple strategies: random, date-based, or debug (fixed)
 */
public class SimpleSeedManager {
    private static final Random RANDOM = new Random();
    
    /**
     * Generates a seed for a dimension based on the configured strategy
     * @param dimension The dimension to generate a seed for
     * @return The generated seed
     */
    public static long generateDailySeed(ResourceLocation dimension) {
        String strategy = BrecherConfig.seedStrategy.get();
        
        // Check for debug seed first
        Long debugSeed = BrecherConfig.debugSeed.get();
        if (debugSeed != null && debugSeed != -1) {
            return debugSeed;
        }
        
        // Generate based on strategy
        switch (strategy.toLowerCase()) {
            case "date-based":
            case "date":
                return generateDateBasedSeed(LocalDate.now(), dimension.toString());
                
            case "random":
            default:
                return generateRandomSeed();
        }
    }
    
    /**
     * Generates a completely random seed
     * @return A random long value
     */
    public static long generateRandomSeed() {
        return RANDOM.nextLong();
    }
    
    /**
     * Generates a seed based on the current date and dimension name
     * This ensures the same seed is used for the entire day
     * @param date The date to generate the seed for
     * @param dimensionName The name of the dimension
     * @return A seed based on the date and dimension
     */
    public static long generateDateBasedSeed(LocalDate date, String dimensionName) {
        // Convert date to epoch day (days since 1970-01-01)
        long epochDay = date.toEpochDay();
        
        // Hash the dimension name
        int dimensionHash = dimensionName.hashCode();
        
        // Combine epoch day and dimension hash to create a unique seed
        // Use bit shifting to ensure good distribution
        long seed = (epochDay << 32) | (dimensionHash & 0xFFFFFFFFL);
        
        // Mix the bits for better randomness
        seed ^= (seed >>> 33);
        seed *= 0xff51afd7ed558ccdL;
        seed ^= (seed >>> 33);
        seed *= 0xc4ceb9fe1a85ec53L;
        seed ^= (seed >>> 33);
        
        return seed;
    }
    
    /**
     * Gets a seed for a specific date (useful for testing or special events)
     * @param year The year
     * @param month The month (1-12)
     * @param day The day of month
     * @param dimensionName The dimension name
     * @return The seed for that specific date
     */
    public static long getSpecificDateSeed(int year, int month, int day, String dimensionName) {
        LocalDate date = LocalDate.of(year, month, day);
        return generateDateBasedSeed(date, dimensionName);
    }
    
    /**
     * Generates a seed based on server uptime (for variety within a day)
     * @param serverStartTime The time the server started in milliseconds
     * @param dimensionName The dimension name
     * @return A seed based on server uptime
     */
    public static long generateUptimeSeed(long serverStartTime, String dimensionName) {
        // Use server start time as base
        long seed = serverStartTime;
        
        // Mix with dimension name
        seed ^= dimensionName.hashCode();
        
        // Apply mixing function
        seed ^= (seed >>> 33);
        seed *= 0xff51afd7ed558ccdL;
        seed ^= (seed >>> 33);
        
        return seed;
    }
}