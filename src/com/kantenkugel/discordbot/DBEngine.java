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
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.utils.SimpleLog;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.sql.*;
import java.sql.Date;
import java.time.OffsetDateTime;
import java.util.*;

public class DbEngine {
    private static final SimpleLog LOG = SimpleLog.getLog("DB");

    private static boolean initialized = false;
    private static Connection conn;
    private static PreparedStatement messageInsert, messageUpdate, messageDelete;
    private static PreparedStatement userUpdate;
    private static PreparedStatement banAdd, banLookup;

    public static synchronized boolean init() {
        if(initialized)
            return true;
        try {
            open();
            if(createTables())
                createStatements();
            initialized = true;
        } catch(SQLException | ClassNotFoundException e) {
            LOG.log(e);
        } catch(LoginException e) {
            LOG.info("Did not establish DB-Connection due to missing config-entries");
        }
        return initialized;
    }

    private static void open() throws ClassNotFoundException, SQLException, LoginException {
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
        Class.forName("com.mysql.jdbc.Driver");
        conn = DriverManager.getConnection("jdbc:mysql://"+host+'/'+database, user, password);
        LOG.info("Successfully opened Database-connection");
    }

    public static void handleMessage(MessageEvent e) {
        if(!initialized || e.isPrivate())
            return;
        if(e.isEdit()) {
            try {
                messageUpdate.setString(1, e.getMessage().getId());
                messageUpdate.setString(2, e.getMessage().getRawContent());
                messageUpdate.setDate(3, new Date(e.getMessage().getEditedTimestamp().toEpochSecond() * 1000));
                messageUpdate.executeUpdate();
            } catch(SQLException ignored) {
                //message is older than db of that channel
            }
        } else {
            try {
                updateUser(e.getAuthor());
                messageInsert.setString(1, e.getMessage().getId());
                messageInsert.setString(2, e.getGuild().getId());
                messageInsert.setString(3, e.getTextChannel().getId());
                messageInsert.setString(4, e.getAuthor().getId());
                messageInsert.setString(5, e.getAuthor().getUsername());
                messageInsert.setString(6, e.getMessage().getRawContent());
                messageInsert.setDate(7, new Date(e.getMessage().getTime().toEpochSecond() * 1000));
                messageInsert.executeUpdate();
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
            banAdd.setString(3, banned.getUsername());
            banAdd.setString(4, executor.getId());
            banAdd.setString(5, executor.getUsername());
            banAdd.setString(6, reason);
            banAdd.setDate(7, new Date(OffsetDateTime.now().toEpochSecond() * 1000));
            banAdd.executeUpdate();
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
                    String aliases = rs.getString("aliases");
                    boolean exists = Arrays.stream(aliases.split("\n")).anyMatch(a -> a.equals(user.getUsername()));
                    if(!exists) {
                        aliases = aliases + '\n' + user.getUsername();
                        if(aliases.length() > 1000)
                            aliases = aliases.substring(aliases.indexOf('\n', aliases.length() - 1000) + 1);
                        rs.updateString("aliases", aliases);
                    }
                    rs.updateRow();
                }
            } else {
                rs.moveToInsertRow();
                rs.updateString("id", user.getId());
                rs.updateString("username", user.getUsername());
                rs.updateString("aliases", user.getUsername());
                rs.insertRow();
            }
            rs.close();
        } catch(SQLException e) {
            LOG.log(e);
        }
    }

    public static ResultSet query(String query) throws SQLException {
        if(!initialized)
            return null;
        Statement statement = conn.createStatement();
        statement.closeOnCompletion();
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
            messageInsert = conn.prepareStatement("INSERT INTO messages(id, guildId, channelId, authorId, authorName, content, created)" +
                    " VALUES (?, ?, ?, ?, ?, ?, ?);");
            messageUpdate = conn.prepareStatement("INSERT INTO message_edits(messageId, content, editTime)" +
                    " VALUES (?, ?, ?);");
            messageDelete = conn.prepareStatement("UPDATE messages SET deleted=1 WHERE id=?;");

            banAdd = conn.prepareStatement("INSERT INTO bans(guildId, bannedId, bannedName, executorId, executorName, reason, createTime)" +
                    " VALUES (?, ?, ?, ?, ?, ?, ?);");
            banLookup = conn.prepareStatement("SELECT * FROM bans WHERE guildId=?;");

            userUpdate = conn.prepareStatement("SELECT * FROM users WHERE id=?;", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            LOG.info("Created statements");
        } catch(SQLException e) {
            LOG.log(e);
        }
    }

    private static boolean createTables() {
        try {
            conn.setAutoCommit(false);
            Statement statement = conn.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS users(" +
                    "  id VARCHAR(20) NOT NULL PRIMARY KEY," +
                    "  username VARCHAR(32) NOT NULL," +
                    "  aliases VARCHAR(1000) NOT NULL" +
                    ");");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS messages(" +
                        "  id VARCHAR(20) NOT NULL PRIMARY KEY," +
                        "  guildId VARCHAR(20) NOT NULL," +
                        "  channelId VARCHAR(20) NOT NULL," +
                        "  authorId VARCHAR(20) NOT NULL REFERENCES users(id) ON DELETE NO ACTION," +
                        "  authorName VARCHAR(32) NOT NULL," +
                        "  content VARCHAR(2000) NOT NULL," +
                        "  created DATETIME NOT NULL," +
                        "  deleted BIT(1) DEFAULT 0 NOT NULL" +
                        ");");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS message_edits(" +
                        "  id INT AUTO_INCREMENT PRIMARY KEY," +
                        "  messageId VARCHAR(20) NOT NULL REFERENCES messages(id) ON DELETE CASCADE," +
                        "  content VARCHAR(2000) NOT NULL," +
                        "  editTime DATETIME NOT NULL" +
                        ");");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bans(" +
                        "  id INT AUTO_INCREMENT PRIMARY KEY," +
                        "  guildId VARCHAR(20) NOT NULL," +
                        "  bannedId VARCHAR(20) NOT NULL REFERENCES users(id) ON DELETE NO ACTION," +
                        "  bannedName VARCHAR(32) NOT NULL," +
                        "  executorId VARCHAR(20) NOT NULL REFERENCES users(id) ON DELETE NO ACTION," +
                        "  executorName VARCHAR(32) NOT NULL," +
                        "  reason VARCHAR(250) NOT NULL," +
                        "  createTime DATETIME NOT NULL" +
                        ");");
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

    public static void close() {
        if(!initialized)
            return;
        try {
            messageInsert.close();
            messageDelete.close();
            messageUpdate.close();
            userUpdate.close();
            banAdd.close();
            banLookup.close();
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
            for(String table : tables) {
                statement.executeUpdate("DROP TABLE " + table + ";");
            }
            statement.close();
            conn.commit();
            LOG.info("All tables dropped!");
        } catch(ClassNotFoundException | SQLException | LoginException e) {
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
                } catch(SQLException e) {
                    LOG.log(e);
                }
            }
        }
        close();
    }
}
