package games.twinhead.simplegames.database;

import games.twinhead.simplegames.SimpleGames;
import games.twinhead.simplegames.settings.PlayerSettings;
import games.twinhead.simplegames.settings.Setting;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.UUID;

public class SqliteImplementation implements Implementation{

    private final SimpleGames plugin;


    public SqliteImplementation(SimpleGames sg){
        this.plugin = sg;
        initDb();
    }


    @Override
    public PlayerSettings getPlayerSettings(UUID uuid) {
        try (Connection conn = getConnection()){
            PlayerSettings settings = new PlayerSettings();

            PreparedStatement findPlayer = conn.prepareStatement("SELECT * FROM players WHERE players.uuid = ?;");
            findPlayer.setString(1, uuid.toString());
            final ResultSet playerResult = findPlayer.executeQuery();


            if(!playerResult.next()) {
                PreparedStatement addPlayer = conn.prepareStatement("INSERT INTO players(uuid, username) VALUES(?, ?);");
                addPlayer.setString(1, uuid.toString());
                addPlayer.setString(2, plugin.getServer().getOfflinePlayer(uuid).getPlayer().getDisplayName());
                addPlayer.execute();
            } else {
                return stringToSettings(playerResult.getString("settings"));
            }

            return settings;
        }catch (java.sql.SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void getOnlinePlayerSettings(){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, ()-> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                plugin.getSettingsManager().addSettings(player.getUniqueId(), getPlayerSettings(player.getUniqueId()));
            }
        });
    }

    public void getPlayerSettingsAsync(UUID uuid, final PlayerSettingsCallBack callback){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, ()-> {
            PlayerSettings settings = getPlayerSettings(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                callback.onQueryDone(settings);
            });
        });
    }

    @Override
    public void savePlayerSettings(UUID uuid, PlayerSettings playerSettings) {
        try (Connection conn = getConnection()){
            String updatePlayerSettingsStatement = "UPDATE players SET settings=? WHERE players.uuid=?;";
            PreparedStatement player = conn.prepareStatement(updatePlayerSettingsStatement);

            player.setString(1, settingsToString(playerSettings));
            player.setString(2, String.valueOf(uuid));

            player.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveAllPlayerSettingsSync(){
        for (Player player: plugin.getServer().getOnlinePlayers())
            savePlayerSettings(player.getUniqueId(), plugin.getSettingsManager().getSettings(player.getUniqueId()));
    }

    @Override
    public void savePlayerSettingsAsync(UUID uuid, PlayerSettings playerSettings) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, ()-> {
           savePlayerSettings(uuid, playerSettings);
        });
    }


    private PlayerSettings stringToSettings(String string){
        PlayerSettings playerSettings = new PlayerSettings();

        if(string == null || string.equals("")) return playerSettings;

        for (String settingAndKey: string.split("\\|")) {
            String[] setting = settingAndKey.split(",");
            playerSettings.setSetting(Setting.valueOf(setting[0]), setting[1]);
        }

        return playerSettings;
    }

    private String settingsToString(PlayerSettings playerSettings){
        StringBuilder string = new StringBuilder();
        for (Setting setting: playerSettings.getStoredSettings()) {
            string.append(setting.toString()).append(",").append(playerSettings.getString(setting)).append("|");
        }
        return string.toString();
    }


    private void initDb(){
        String[] queries = new String[1];
        queries[0] = "CREATE TABLE IF NOT EXISTS players\n" +
                "(\n" +
                "    uuid CHAR(36) NOT NULL PRIMARY KEY,\n" +
                "    username VARCHAR(16),\n" +
                "    settings VARCHAR(255)\n" +
                ");";

        // execute each query to the database.
        for (String query : queries) {
            // If you use the legacy way you have to check for empty queries here.
            if (query.isBlank()) continue;
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                 stmt.execute();
            } catch (SQLException e){
                e.printStackTrace();
            }
        }
        plugin.getLogger().info("§2Database setup complete.");
    }

    private Connection getConnection() throws SQLException{
        return DriverManager.getConnection("jdbc:sqlite:plugins/SimpleGames/data/player_data.db");
    }
}
