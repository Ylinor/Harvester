package com.ylinor.harvester.data.dao;

import com.ylinor.harvester.Harvester;
import com.ylinor.harvester.data.beans.RespawningBlockBean;
import com.ylinor.harvester.data.handlers.DatabaseHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RespawningBlockDao {
    /**
     * Generate database tables if they do not exist
     */
    public static void createTableIfNotExist() {
        String query = "CREATE TABLE IF NOT EXISTS respawning_block (id INT AUTO_INCREMENT, x INT, y INT, z INT, block_type VARCHAR(50), respawn_time INT)";
        try {
            Connection connection = DatabaseHandler.getDatasource().getConnection();
            PreparedStatement statement = connection.prepareStatement(query);
            statement.execute();
            connection.close();
        } catch (SQLException e) {
            Harvester.getLogger().error("Error while creating respawning blocks table : " + e.getMessage());
        }
    }

    /**
     * Fetch database to query every block that need a respawn
     * @return List of respawning blocks
     */
    public static List<RespawningBlockBean> getRespawningBlocks() {
        String query = "SELECT id, x, y, z, block_type, respawn_time AS datediff FROM respawning_block WHERE DATEDIFF('s', '1970-01-01', CURRENT_TIMESTAMP)-2*60*60 > respawn_time";
        List<RespawningBlockBean> respawningBlocks = new ArrayList<>();
        try {
            Connection connection = DatabaseHandler.getDatasource().getConnection();
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                respawningBlocks.add(new RespawningBlockBean(results.getInt("id"), results.getInt("x"),
                        results.getInt("y"), results.getInt("z"), results.getString("block_type"),
                        results.getInt("respawn_time")));
            }
            connection.close();
        } catch (SQLException e) {
            Harvester.getLogger().error("Error while fetching respawning blocks : " + e.getMessage());
        }
        return respawningBlocks;
    }

    /**
     * Add a block to be respawn later into database
     * @param block Block to respawn later
     */
    public static void addRespawningBlock(RespawningBlockBean block) {
        String query = "INSERT INTO respawning_block (x, y, z, block_type, respawn_time) VALUES (?, ?, ?, ?, ?)";
        try {
            Connection connection = DatabaseHandler.getDatasource().getConnection();
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, block.getX());
            statement.setInt(2, block.getY());
            statement.setInt(3, block.getZ());
            statement.setString(4, block.getBlockType());
            statement.setInt(5, block.getRespawnTime());
            statement.execute();
            connection.close();
        } catch (SQLException e) {
            Harvester.getLogger().error("Error while inserting respawning block : " + e.getMessage());
        }
    }

    /**
     * Remove list of blocks from database
     * @param blocks List of blocks to remove
     */
    public static void removeRespawningBlocks(List<RespawningBlockBean> blocks) {
        String query = "DELETE FROM respawning_block WHERE id = ?";
        try {
            Connection connection = DatabaseHandler.getDatasource().getConnection();
            for (RespawningBlockBean block : blocks) {
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setInt(1, block.getId());
                statement.execute();
            }
            connection.close();
        } catch (SQLException e) {
            Harvester.getLogger().error("Error while deleting respawning block : " + e.getMessage());
        }
    }
}