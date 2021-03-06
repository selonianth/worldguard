// $Id$
/*
 * WorldGuard
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.sk89q.worldguard.blacklist.loggers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.blacklist.events.BlacklistEvent;
import com.sk89q.worldguard.blacklist.events.BlockBreakBlacklistEvent;
import com.sk89q.worldguard.blacklist.events.BlockPlaceBlacklistEvent;
import com.sk89q.worldguard.blacklist.events.DestroyWithBlacklistEvent;
import com.sk89q.worldguard.blacklist.events.ItemAcquireBlacklistEvent;
import com.sk89q.worldguard.blacklist.events.ItemDropBlacklistEvent;
import com.sk89q.worldguard.blacklist.events.ItemUseBlacklistEvent;

/**
 *
 * @author sk89q
 */
public class DatabaseLoggerHandler implements BlacklistLoggerHandler {
    /**
     * Logger.
     */
    private static final Logger logger = Logger.getLogger("Minecraft.WorldGuard");

    /**
     * DSN.
     */
    private String dsn;
    /**
     * Username.
     */
    private String user;
    /**
     * Password.
     */
    private String pass;
    /**
     * Table.
     */
    private String table;
    /**
     * World name.
     */
    private String worldName;
    /**
     * Database connection.
     */
    private Connection conn;

    /**
     * Construct the object.
     * 
     * @param dsn
     * @param user
     * @param pass
     * @param table 
     * @param worldName 
     */
    public DatabaseLoggerHandler(String dsn, String user, String pass, String table, String worldName) {
        this.dsn = dsn;
        this.user = user;
        this.pass = pass;
        this.table = table;
        this.worldName = worldName;
    }

    /**
     * Gets the database connection.
     * 
     * @return
     * @throws SQLException
     */
    private Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(dsn, user, pass);
        }
        return conn;
    }

    /**
     * Log an event to the database.
     * 
     * @param event
     * @param name
     * @param x
     * @param y
     * @param z
     * @param item
     * @param comment
     */
    private void logEvent(String event, LocalPlayer player, Vector pos, int item,
            String comment) {
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO " + table
                  + "(event, world, player, x, y, z, item, time, comment) VALUES "
                  + "(?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setString(1, event);
            stmt.setString(2, worldName);
            stmt.setString(3, player.getName());
            stmt.setInt(4, pos.getBlockX());
            stmt.setInt(5, pos.getBlockY());
            stmt.setInt(6, pos.getBlockZ());
            stmt.setInt(7, item);
            stmt.setInt(8, (int)(System.currentTimeMillis() / 1000));
            stmt.setString(9, comment);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to log blacklist event to database: "
                    + e.getMessage());
        }
    }
    
    /**
     * Log an event.
     *
     * @param event
     */
    public void logEvent(BlacklistEvent event, String comment) {
        // Block break
        if (event instanceof BlockBreakBlacklistEvent) {
            BlockBreakBlacklistEvent evt = (BlockBreakBlacklistEvent)event;
            logEvent("BREAK", evt.getPlayer(), evt.getPosition(),
                    evt.getType(), comment);
        
        // Block place
        } else if (event instanceof BlockPlaceBlacklistEvent) {
            BlockPlaceBlacklistEvent evt = (BlockPlaceBlacklistEvent)event;
            logEvent("PLACE", evt.getPlayer(), evt.getPosition(),
                    evt.getType(), comment);
            
        // Block interact
        } else if (event instanceof BlockPlaceBlacklistEvent) {
            BlockPlaceBlacklistEvent evt = (BlockPlaceBlacklistEvent)event;
            logEvent("INTERACT", evt.getPlayer(), evt.getPosition(),
                    evt.getType(), comment);
        
        // Destroy with
        } else if (event instanceof DestroyWithBlacklistEvent) {
            DestroyWithBlacklistEvent evt = (DestroyWithBlacklistEvent)event;
            logEvent("DESTROY_WITH", evt.getPlayer(), evt.getPosition(),
                    evt.getType(), comment);
        
        // Acquire
        } else if (event instanceof ItemAcquireBlacklistEvent) {
            ItemAcquireBlacklistEvent evt = (ItemAcquireBlacklistEvent)event;
            logEvent("ACQUIRE", evt.getPlayer(), evt.getPlayer().getPosition(),
                    evt.getType(), comment);
        
        // Drop
        } else if (event instanceof ItemDropBlacklistEvent) {
            ItemDropBlacklistEvent evt = (ItemDropBlacklistEvent)event;
            logEvent("DROP", evt.getPlayer(), evt.getPlayer().getPosition(),
                    evt.getType(), comment);
        
        // Use
        } else if (event instanceof ItemUseBlacklistEvent) {
            ItemUseBlacklistEvent evt = (ItemUseBlacklistEvent)event;
            logEvent("USE", evt.getPlayer(), evt.getPlayer().getPosition(),
                    evt.getType(), comment);
        
        // Unknown
        } else {
            logEvent("UNKNOWN", event.getPlayer(), event.getPlayer().getPosition(),
                    -1, comment);
        }
    }

    /**
     * Close the connection.
     */
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            
        }
    }
}
