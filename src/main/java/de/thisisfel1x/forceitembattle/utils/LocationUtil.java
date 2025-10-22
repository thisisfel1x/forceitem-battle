package de.thisisfel1x.forceitembattle.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class LocationUtil {

    /**
     * Calculates a list of evenly spaced locations in a circle around a center point.
     * The calculated locations will have their direction set to face the center of the circle.
     *
     * @param center The center location of the circle.
     * @param pointCount The number of points (spawn locations) to calculate.
     * @param radius The radius of the circle.
     * @return A List of Locations representing the spawn points.
     */
    public static List<Location> calculateCircleSpawns(Location center, int pointCount, double radius) {
        List<Location> locations = new ArrayList<>();
        World world = center.getWorld();

        if (pointCount <= 0) {
            return locations; // Return empty list if no points are requested
        }

        // Calculate the angle between each point in radians
        double angleIncrement = 2 * Math.PI / pointCount;

        for (int i = 0; i < pointCount; i++) {
            double angle = i * angleIncrement;

            // Calculate the X and Z coordinates for the point on the circle
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            // Create the new location with the same Y-coordinate as the center
            Location spawnLocation = new Location(world, x, center.getY(), z);
            Block currentBlock = world.getHighestBlockAt(spawnLocation);
            currentBlock.setType(Material.TINTED_GLASS);
            spawnLocation = currentBlock.getLocation().toCenterLocation().add(0, 0.5, 0);

            locations.add(spawnLocation);
        }

        return locations;
    }
}