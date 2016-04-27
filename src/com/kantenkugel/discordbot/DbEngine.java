/*
 * Copyright 2016 Michael Ritter (Kantenkugel)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kantenkugel.discordbot;

import com.kantenkugel.discordbot.config.BotConfig;
import com.kantenkugel.discordbot.listener.MessageEvent;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.utils.SimpleLog;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.sql.*;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoField;
import java.util.*;

public class DbEngine {
    private static final SimpleLog LOG = SimpleLog.getLog("DB");

    private static boolean initialized = false;
    private static Connection conn;
    private static PreparedStatement guildUpdate, guildUpdate2, channelUpdate;
    private static PreparedStatement messageInsert, messageUpdate, messageDelete;
    private static PreparedStatement userUpdate, userAliasUpdate;
    private static PreparedStatement banAdd, banLookup;
    private static PreparedStatement historyCreate;

    public static synchronized boolean init() {
        if(initialized)
            return true;
        try {
            open();
            if(!createTables()) {
                LOG.fatal("Could not create tables! Closing Db!");
                close();
                return false;
            }
            createStatements();
            initialized = true;
        } catch(LoginException e) {
            LOG.info("Did not establish DB-Connection due to missing config-entries");
        }
        return initialized;
    }

    private static void open() throws LoginException {
        JSONObject config = BotConfig.get("db");
        if(config == null)
            throw new LoginException("Config is missing db-section!");
        String host = config.has("host") ? config.getString("host") : null;
        String database = config.has("database") ? config.getString("database") : null;
        String user = config.has("user") ? config.getString("user") : null;
        String password = config.has("password") ? config.getString("password") : null;
        if(host == null || host.trim().isEmpty() || database == null || database.trim().isEmpty()
                || user == null || user.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new LoginException("one of the db-configs values was empty or non-present");
        }
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://" + host + '/' + database + "?useUnicode=true", user, password);
            Statement statement = conn.createStatement();
            statement.executeQuery("SET NAMES 'utf8mb4'");
            statement.close();
        } catch(Exception ex) {
            throw new LoginException("Failed to connect or login to DB");
        }
        LOG.info("Successfully opened Database-connection");
    }

    public static void handleMessage(MessageEvent e) {
        if(!initialized || e.isPrivate())
            return;
        if(e.isEdit()) {
            try {
                messageUpdate.setString(1, e.getMessage().getRawContent());
                messageUpdate.setTimestamp(2, new Timestamp(e.getMessage().getEditedTimestamp().toEpochSecond() * 1000
                        + e.getMessage().getEditedTimestamp().get(ChronoField.MILLI_OF_SECOND)));
                messageUpdate.setString(3, e.getMessage().getId());
                messageUpdate.executeUpdate();
            } catch(SQLTimeoutException ex) {
                onTimeout();
            } catch(SQLException ex) {
                LOG.log(ex);
            }
        } else {
            try {
                updateUser(e.getAuthor());
                messageInsert.setString(1, e.getMessage().getId());
                messageInsert.setString(2, e.getTextChannel().getId());
                messageInsert.setString(3, e.getAuthor().getId());
                messageInsert.setString(4, e.getMessage().getRawContent());
                messageInsert.setTimestamp(5, new Timestamp(e.getMessage().getTime().toEpochSecond() * 1000
                        + e.getMessage().getTime().get(ChronoField.MILLI_OF_SECOND)));
                messageInsert.executeUpdate();
            } catch(SQLTimeoutException ex) {
                onTimeout();
            } catch(SQLException ex) {
                LOG.log(ex);
            }
        }
    }

    public static void deleteMessage(String id) {
        if(!initialized)
            return;
        try {
            messageDelete.setLong(1, Long.parseLong(id));
            messageDelete.executeUpdate();
        } catch(SQLTimeoutException ex) {
            onTimeout();
        } catch(SQLException e) {
            LOG.log(e);
        }
    }

    public static void handleGuilds(List<Guild> guilds) {
        if(!initialized)
            return;
        LOG.info("Starting DB-CHECK of Guilds...");
        try {
            //get existing guilds...
            ResultSet query = query("SELECT id FROM guilds WHERE last_seen != NULL;");
            Set<String> existing = new HashSet<>();
            while(query.next()) {
                existing.add(query.getString(1));
            }
            query.close();

            //add/update guilds
            for(Guild guild : guilds) {
                LOG.trace("Validating guild "+guild.getId());
                existing.remove(guild.getId());
                updateGuild(guild);
            }

            LOG.debug("Marking unseen dbs");
            //mark as unseen
            for(String unfound : existing) {
                update("UPDATE guilds SET last_seen = CURRENT_DATE WHERE id = ?;", unfound);
            }
        } catch(SQLTimeoutException e) {
            onTimeout();
        } catch(SQLException e) {
            LOG.log(e);
        }
        LOG.info("Finished DB-CHECK of Guilds!");
    }

    public static void updateGuild(Guild g) {
        if(!initialized)
            return;
        try {
            guildUpdate.setString(1, g.getId());
            ResultSet rs = guildUpdate.executeQuery();
            if(rs.next()) {
                boolean updated = false;
                if(!rs.getString("name").equals(g.getName())) {
                    rs.updateString("name", g.getName());
                    updated = true;
                }
                if(rs.getDate("last_seen") != null) {
                    rs.updateNull("last_seen");
                    updated = true;
                }
                if(updated)
                    rs.updateRow();
                handleChannels(g);
            } else {
                rs.moveToInsertRow();
                rs.updateString("id", g.getId());
                rs.updateString("name", g.getName());
                rs.insertRow();
                for(TextChannel channel : g.getTextChannels()) {
                    update("INSERT INTO channels(id, name, guildId) VALUES (?, ?, ?);", channel.getId(), channel.getName(), g.getId());
                }
            }
        } catch(SQLTimeoutException e) {
            onTimeout();
        } catch(SQLException e) {
            LOG.log(e);
        }

    }

    public static void deleteGuild(Guild g) {
        if(!initialized)
            return;
        try {
            update("UPDATE guilds SET last_seen = CURRENT_DATE WHERE id = ?;", g.getId());
        } catch(SQLTimeoutException e) {
            onTimeout();
        } catch(SQLException e) {
            LOG.log(e);
        }
    }

    public static void updateChannel(TextChannel channel) {
        if(!initialized)
            return;
        try {
            channelUpdate.setString(1, channel.getId());
            ResultSet rs = channelUpdate.executeQuery();
            if(rs.next()) {
                if(!rs.getString("name").equals(channel.getName())) {
                    rs.updateString("name", channel.getName());
                    rs.updateBoolean("deleted", rs.getBoolean("deleted"));
                    rs.updateRow();
                }
            } else {
                rs.moveToInsertRow();
                rs.updateString("id", channel.getId());
                rs.updateString("name", channel.getName());
                rs.updateString("guildId", channel.getGuild().getId());
                rs.insertRow();
            }
            rs.close();
        } catch(SQLTimeoutException e) {
            onTimeout();
        } catch(SQLException e) {
            LOG.log(e);
        }
    }

    private static void handleChannels(Guild g) {
        Map<String, TextChannel> channels = new HashMap<>();
        g.getTextChannels().forEach(c -> channels.put(c.getId(), c));
        try {
            guildUpdate2.setString(1, g.getId());
            ResultSet rs = guildUpdate2.executeQuery();
            while(rs.next()) {
                String channelId = rs.getString("id");
                if(channels.containsKey(channelId)) {
                    TextChannel tc = channels.get(channelId);
                    if(!rs.getString("name").equals(tc.getName())) {
                        rs.updateString("name", tc.getName());
                        rs.updateBoolean("deleted", rs.getBoolean("deleted"));
                        rs.updateRow();
                    }
                    channels.remove(channelId);
                } else {
                    rs.updateBoolean("deleted", true);
                    rs.updateRow();
                }
            }
            for(TextChannel newChannel : channels.values()) {
                rs.moveToInsertRow();
                rs.updateString("id", newChannel.getId());
                rs.updateString("name", newChannel.getName());
                rs.updateString("guildId", g.getId());
                rs.insertRow();
            }
            rs.close();
        } catch(SQLTimeoutException e) {
            onTimeout();
        } catch(SQLException e) {
            LOG.log(e);
        }
    }

    public static void deleteChannel(TextChannel channel) {
        if(!initialized)
            return;
        try {
            update("UPDATE channels SET deleted = 1 WHERE id = ?;", channel.getId());
        } catch(SQLTimeoutException e) {
            onTimeout();
        } catch(SQLException e) {
            LOG.log(e);
        }
    }

    public static List<Ban> getBans(Guild guild) {
        List<Ban> bans = new LinkedList<>();
        if(!initialized)
            return bans;
        try {
            banLookup.setString(1, guild.getId());
            ResultSet resultSet = banLookup.executeQuery();
            while(!resultSet.next()) {
                bans.add(new Ban(resultSet.getString("reason"), resultSet.getString("bannedId"), resultSet.getString("bannedName")
                        , resultSet.getString("executorId"), resultSet.getString("executorName"), resultSet.getInt("created")));
            }
            resultSet.close();
        } catch(SQLTimeoutException ex) {
            onTimeout();
        } catch(SQLException e) {
            LOG.log(e);
        }
        return bans;
    }

    public static void addBan(Guild guild, User banned, User executor, String reason) {
        if(!initialized)
            return;
        if(reason.length() > 250) {
            reason = reason.substring(0, 247) + "...";
        }
        try {
            updateUser(banned);
            banAdd.setString(1, guild.getId());
            banAdd.setString(2, banned.getId());
            banAdd.setString(3, executor.getId());
            banAdd.setString(4, reason);
            banAdd.setTimestamp(5, new Timestamp(OffsetDateTime.now().toEpochSecond() * 1000));
            banAdd.executeUpdate();
        } catch(SQLTimeoutException ex) {
            onTimeout();
        } catch(SQLException e) {
            LOG.log(e);
        }
    }

    public static void updateUser(User user) {
        if(!initialized)
            return;
        try {
            userUpdate.setString(1, user.getId());
            ResultSet rs = userUpdate.executeQuery();
            if(rs.next()) {
                if(!rs.getString("username").equals(user.getUsername())) {
                    rs.updateString("username", user.getUsername());
                    rs.updateRow();
                    userAliasUpdate.setString(1, user.getId());
                    userAliasUpdate.setString(2, user.getUsername());
                    ResultSet resultSet = userAliasUpdate.executeQuery();
                    if(!resultSet.next()) {
                        resultSet.moveToInsertRow();
                        resultSet.updateString("userId", user.getId());
                        resultSet.updateString("alias", user.getUsername());
                        resultSet.insertRow();
                    }
                    resultSet.close();
                }
            } else {
                rs.moveToInsertRow();
                rs.updateString("id", user.getId());
                rs.updateString("username", user.getUsername());
                rs.insertRow();
                update("INSERT INTO user_aliases(userId, alias) VALUES (?, ?);", user.getId(), user.getUsername());
            }
            rs.close();
        } catch(SQLTimeoutException ex) {
            onTimeout();
        } catch(SQLException e) {
            LOG.log(e);
        }
    }

    public static long createHistory(User user, TextChannel channel) {
        if(!initialized)
            return -1;
        try {
            historyCreate.setString(1, user.getId());
            historyCreate.setString(2, channel.getId());
            historyCreate.executeUpdate();
            ResultSet generatedKeys = historyCreate.getGeneratedKeys();
            if(generatedKeys.next())
                return generatedKeys.getLong(1);
        } catch(SQLTimeoutException e) {
            onTimeout();
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static void update(String update, Object... objects) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(update);
        statement.setQueryTimeout(10);
        int index = 1;
        for(Object object : objects) {
            if(object == null)
                statement.setNull(index++, Types.VARCHAR);
            else if(object.getClass() == String.class)
                statement.setString(index++, ((String) object));
            else if(object.getClass() == int.class)
                statement.setInt(index++, (int) object);
            else if(object.getClass() == Integer.class)
                statement.setInt(index++, (Integer) object);
            else if(object.getClass() == long.class)
                statement.setLong(index++, (long) object);
            else if(object.getClass() == Long.class)
                statement.setLong(index++, (Long) object);
            else if(object.getClass() == boolean.class)
                statement.setBoolean(index++, (boolean) object);
            else if(object.getClass() == Boolean.class)
                statement.setBoolean(index++, (Boolean) object);
            else {
                LOG.warn("Unknown parameter type for update()... Got " + object.getClass().getName() + "... Skipping update!");
                statement.close();
                return;
            }
        }
        statement.executeUpdate();
        statement.close();
    }

    public static ResultSet query(String query) throws SQLException {
        if(!initialized)
            return null;
        Statement statement = conn.createStatement();
        statement.closeOnCompletion();
        statement.setQueryTimeout(10);
        return statement.executeQuery(query);
    }

    //UGLY AF but it works!
    public static String stringify(ResultSet rs) {
        if(rs == null)
            return "DB not available!";
        int columncount;
        int[] maxLength;
        Object[] buff;
        List<Object[]> responses = new LinkedList<>();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            columncount = metaData.getColumnCount();
            buff = new String[columncount];
            maxLength = new int[columncount];
            for(int i = 1; i <= columncount; i++) {
                maxLength[i - 1] = metaData.getColumnLabel(i).length() + 1;
                buff[i - 1] = metaData.getColumnLabel(i);
            }
            responses.add(buff);
            while(rs.next()) {
                buff = new String[columncount];
                for(int i = 1; i <= columncount; i++) {
                    buff[i - 1] = rs.getObject(i) == null ? "null" : rs.getObject(i).toString();
                    maxLength[i - 1] = Math.max(maxLength[i - 1], buff[i - 1].toString().length() + 1);
                }
                responses.add(buff);
            }
            StringBuilder fb = new StringBuilder();
            for(int i : maxLength) {
                fb.append("%-").append(i).append('s');
            }
            String format = fb.append('\n').toString();
            StringBuilder out = new StringBuilder();
            for(Object[] response : responses) {
                out.append(String.format(format, response));
            }
            out.setLength(out.length() - 1);
            return out.toString();
        } catch(SQLException e) {
            LOG.log(e);
        } finally {
            try {
                rs.close();
            } catch(SQLException ignored) {}
        }
        return null;
    }

    private static void createStatements() {
        try {
            //Guild+Channel
            guildUpdate = conn.prepareStatement("SELECT * FROM guilds WHERE id = ?;", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            guildUpdate.setQueryTimeout(10);
            guildUpdate2 = conn.prepareStatement("SELECT * FROM channels WHERE guildId = ?;", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            guildUpdate2.setQueryTimeout(10);
            channelUpdate = conn.prepareStatement("SELECT * FROM channels WHERE id = ?;", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            channelUpdate.setQueryTimeout(10);

            //Messages
            messageInsert = conn.prepareStatement("INSERT INTO messages(id, channelId, authorId, content, created)" +
                    " VALUES (?, ?, ?, ?, ?);");
            messageInsert.setQueryTimeout(10);
            messageUpdate = conn.prepareStatement("INSERT INTO message_edits (messageId, content, edited) SELECT id, ?, ? FROM messages WHERE id = ?;");
            messageUpdate.setQueryTimeout(10);
            messageDelete = conn.prepareStatement("UPDATE messages SET deleted=1 WHERE id=?;");
            messageDelete.setQueryTimeout(10);

            //Bans
            banAdd = conn.prepareStatement("INSERT INTO bans(guildId, bannedId, executorId, reason, created)" +
                    " VALUES (?, ?, ?, ?, ?);");
            banAdd.setQueryTimeout(10);
            banLookup = conn.prepareStatement("SELECT bans.*, u1.username AS bannedName, u2.username AS executorName " +
                    "FROM bans JOIN users AS u1 ON bans.bannedId = u1.id JOIN users AS u2 ON bans.executorId = u2.id " +
                    "WHERE guildId=?;");
            banLookup.setQueryTimeout(10);

            //User-Update
            userUpdate = conn.prepareStatement("SELECT * FROM users WHERE id=?;", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            userUpdate.setQueryTimeout(10);
            userAliasUpdate = conn.prepareStatement("SELECT * FROM user_aliases WHERE userId=? AND alias=?;"
                    , ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            userAliasUpdate.setQueryTimeout(10);

            //History
            historyCreate = conn.prepareStatement("INSERT INTO histories(userId, channelId) VALUES (?, ?);",
                    Statement.RETURN_GENERATED_KEYS);
            historyCreate.setQueryTimeout(10);

            LOG.info("Created statements");
        } catch(SQLException e) {
            LOG.log(e);
        }
    }

    private static boolean createTables() {
        try {
            conn.setAutoCommit(false);
            Statement statement = conn.createStatement();
            statement.setQueryTimeout(10);
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS guilds(" +
                    " id VARCHAR(20) NOT NULL PRIMARY KEY," +
                    " name VARCHAR(100) NOT NULL," +
                    " last_seen DATE DEFAULT NULL" +
                    ") COLLATE utf8mb4_unicode_ci;");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS channels(" +
                    " id VARCHAR(20) NOT NULL PRIMARY KEY," +
                    " name VARCHAR(100) NOT NULL," +
                    " guildId VARCHAR(20) NOT NULL," +
                    " deleted BIT(1) DEFAULT 0 NOT NULL," +
                    " FOREIGN KEY (guildId) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ") COLLATE utf8mb4_unicode_ci;");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS users(" +
                    " id VARCHAR(20) NOT NULL PRIMARY KEY," +
                    " username VARCHAR(32) NOT NULL" +
                    ") COLLATE utf8mb4_unicode_ci;");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS user_aliases(" +
                    " id INT AUTO_INCREMENT PRIMARY KEY," +
                    " userId VARCHAR(20) NOT NULL," +
                    " alias VARCHAR(32) NOT NULL," +
                    " FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE" +
                    ") COLLATE utf8mb4_unicode_ci;");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS messages(" +
                    " id VARCHAR(20) NOT NULL PRIMARY KEY," +
                    " channelId VARCHAR(20) NOT NULL," +
                    " authorId VARCHAR(20) NOT NULL," +
                    " content VARCHAR(2000) NOT NULL," +
                    " created DATETIME(3) NOT NULL," +
                    " deleted BIT(1) DEFAULT 0 NOT NULL," +
                    " FOREIGN KEY (authorId) REFERENCES users(id) ON DELETE NO ACTION," +
                    " FOREIGN KEY (channelId) REFERENCES channels(id) ON DELETE CASCADE" +
                    ") COLLATE utf8mb4_unicode_ci;");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS message_edits(" +
                    " id INT AUTO_INCREMENT PRIMARY KEY," +
                    " messageId VARCHAR(20) NOT NULL," +
                    " content VARCHAR(2000) NOT NULL," +
                    " edited DATETIME(3) NOT NULL," +
                    " FOREIGN KEY (messageId) REFERENCES messages(id) ON DELETE CASCADE" +
                    ") COLLATE utf8mb4_unicode_ci;");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bans(" +
                    " id INT AUTO_INCREMENT PRIMARY KEY," +
                    " guildId VARCHAR(20) NOT NULL," +
                    " bannedId VARCHAR(20) NOT NULL," +
                    " executorId VARCHAR(20) NOT NULL," +
                    " reason VARCHAR(250) NOT NULL," +
                    " created DATETIME NOT NULL," +
                    " FOREIGN KEY (bannedId) REFERENCES users(id) ON DELETE NO ACTION," +
                    " FOREIGN KEY (executorId) REFERENCES users(id) ON DELETE NO ACTION," +
                    " FOREIGN KEY (guildId) REFERENCES guilds(id) ON DELETE CASCADE" +
                    ") COLLATE utf8mb4_unicode_ci;");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS histories(" +
                    " id INT AUTO_INCREMENT PRIMARY KEY," +
                    " userId VARCHAR(20) NOT NULL," +
                    " channelId VARCHAR(20) NOT NULL," +
                    " created DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL," +
                    " FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE," +
                    " FOREIGN KEY (channelId) REFERENCES channels(id) ON DELETE CASCADE" +
                    ") COLLATE utf8mb4_unicode_ci;");
            statement.close();
            conn.commit();
            LOG.info("Tables checked/created");
            return true;
        } catch(SQLException e) {
            LOG.log(e);
            try {
                conn.rollback();
            } catch(SQLException e1) {
                LOG.log(e1);
            }
            close();
        } finally {
            if(conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch(SQLException ignored) {
                }
            }
        }
        return false;
    }

    private static void onTimeout() {
        LOG.fatal("SQL-Query timed out during execution! Closing DB...");
        close();
    }

    public static void close() {
        if(!initialized)
            return;
        try {
            guildUpdate.close();
            guildUpdate2.close();
            channelUpdate.close();
            messageInsert.close();
            messageDelete.close();
            messageUpdate.close();
            userUpdate.close();
            userAliasUpdate.close();
            banAdd.close();
            banLookup.close();
            historyCreate.close();
        } catch(SQLException e) {
            LOG.log(e);
        }
        try {
            conn.close();
        } catch(SQLException e) {
            LOG.log(e);
        }
        conn = null;
        initialized = false;
        LOG.info("Database successfully closed");
    }

    public static class Ban {
        public final String reason;
        public final String bannedId, bannedName;
        public final String executorId, getExecutorName;
        public final int timestampS;

        public Ban(String reason, String bannedId, String bannedName, String executorId, String getExecutorName, int timestampS) {
            this.reason = reason;
            this.bannedId = bannedId;
            this.bannedName = bannedName;
            this.executorId = executorId;
            this.getExecutorName = getExecutorName;
            this.timestampS = timestampS;
        }
    }

    public static void drop() {
        try {
            open();
            LOG.info("Dropping all tables...");
            conn.setAutoCommit(false);
            Set<String> tables = new HashSet<>();
            ResultSet tableRows = conn.getMetaData().getTables(null, null, null, new String[]{"TABLE"});
            while(tableRows.next()) {
                tables.add(tableRows.getString("TABLE_NAME").toLowerCase());
            }
            tableRows.close();
            Statement statement = conn.createStatement();
            statement.setQueryTimeout(10);
            statement.addBatch("SET FOREIGN_KEY_CHECKS = 0;");
            for(String table : tables) {
                statement.addBatch("DROP TABLE " + table + ";");
            }
            statement.addBatch("SET FOREIGN_KEY_CHECKS  = 1;");
            statement.executeBatch();
            statement.close();
            conn.commit();
            LOG.info("All tables dropped!");
        } catch(SQLException | LoginException e) {
            try {
                conn.rollback();
            } catch(SQLException e1) {
                LOG.log(e1);
            }
            LOG.log(e);
        } finally {
            if(conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch(SQLException e) {
                    LOG.log(e);
                }
                conn = null;
            }
        }
    }

    public static void main(String[] args) {
        BotConfig.load();
        drop();
        if(init()) {
            close();
        }
    }
}
