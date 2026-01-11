/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher. All rights reserved.
 */

package net.tinkstav.brecher_dim.dimension;

import net.minecraft.resources.ResourceLocation;
import net.tinkstav.brecher_dim.config.BrecherConfig;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
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
        String strategy = BrecherConfig.getSeedStrategy();
        
        // Check for debug seed first
        long debugSeed = BrecherConfig.getDebugSeed();
        if (debugSeed != -1) {
            return debugSeed;
        }
        
        // Generate based on strategy
        switch (strategy.toLowerCase()) {
            case "date-based":
            case "date":
                return generateDateBasedSeed(LocalDate.now(), dimension.toString());
                
            case "weekly":
                DayOfWeek resetDay = DayOfWeek.MONDAY; // default
                try {
                    String configDay = BrecherConfig.getWeeklyResetDay();
                    resetDay = DayOfWeek.valueOf(configDay.toUpperCase());
                } catch (Exception e) {
                    // Use default MONDAY if parsing fails
                }
                return generateWeeklySeed(LocalDate.now(), dimension.toString(), resetDay);
                
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
     * Generates a seed based on the week and dimension name
     * This ensures the same seed is used for the entire week starting from the reset day
     * @param date The current date
     * @param dimensionName The name of the dimension
     * @param resetDay The day of week when the seed should reset
     * @return A seed based on the week and dimension
     */
    public static long generateWeeklySeed(LocalDate date, String dimensionName, DayOfWeek resetDay) {
        // Find the most recent occurrence of the reset day (including today if it matches)
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(resetDay));
        
        // Use the week start date's epoch day to generate consistent seed for the week
        long epochDay = weekStart.toEpochDay();
        
        // Hash the dimension name
        int dimensionHash = dimensionName.hashCode();
        
        // Combine epoch day and dimension hash to create a unique seed
        long seed = (epochDay << 32) | (dimensionHash & 0xFFFFFFFFL);
        
        // Apply the same mixing function as daily for good randomness
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
    
    /**
     * Calculates the time remaining until the next seed reset based on the configured strategy.
     * @return A Duration representing the time until the next reset, or null if the strategy doesn't have a reset period (e.g., random)
     */
    public static Duration getTimeUntilSeedReset() {
        String strategy = BrecherConfig.getSeedStrategy();
        LocalDateTime now = LocalDateTime.now();
        
        switch (strategy.toLowerCase()) {
            case "date-based":
            case "date":
                // Resets daily at midnight
                LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
                return Duration.between(now, nextMidnight);
                
            case "weekly":
                // Resets weekly on the configured day
                DayOfWeek resetDay = DayOfWeek.MONDAY; // default
                try {
                    String configDay = BrecherConfig.getWeeklyResetDay();
                    resetDay = DayOfWeek.valueOf(configDay.toUpperCase());
                } catch (Exception e) {
                    // Use default MONDAY if parsing fails
                }
                
                // Find the next occurrence of the reset day
                LocalDate nextResetDate = now.toLocalDate().with(TemporalAdjusters.next(resetDay));
                // If today is the reset day and we haven't passed midnight yet, the next reset is next week
                if (now.getDayOfWeek() == resetDay) {
                    nextResetDate = now.toLocalDate().plusWeeks(1);
                }
                LocalDateTime nextResetDateTime = nextResetDate.atStartOfDay();
                return Duration.between(now, nextResetDateTime);
                
            case "random":
            default:
                // Random strategy doesn't have a fixed reset period
                return null;
        }
    }
    
    /**
     * Formats a duration into a human-readable string.
     * @param duration The duration to format
     * @return A formatted string like "2 days, 3 hours, 15 minutes"
     */
    public static String formatDuration(Duration duration) {
        if (duration == null) {
            return "No scheduled reset";
        }
        
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        StringBuilder result = new StringBuilder();
        
        if (days > 0) {
            result.append(days).append(" day").append(days != 1 ? "s" : "");
        }
        
        if (hours > 0) {
            if (result.length() > 0) result.append(", ");
            result.append(hours).append(" hour").append(hours != 1 ? "s" : "");
        }
        
        if (minutes > 0 || result.length() == 0) {
            if (result.length() > 0) result.append(", ");
            result.append(minutes).append(" minute").append(minutes != 1 ? "s" : "");
        }
        
        return result.toString();
    }
}