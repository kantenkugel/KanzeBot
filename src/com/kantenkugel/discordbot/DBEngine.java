package com.kantenkugel.discordbot;

import com.kantenkugel.discordbot.listener.MessageEvent;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;

import java.sql.*;
import java.sql.Date;
import java.time.OffsetDateTime;
import java.util.*;

public class DbEngine {
    private static boolean initialized = false;
    private static Connection conn;
    private static PreparedStatement messageInsert, messageUpdate, messageDelete;
    private static PreparedStatement userUpdate;
    private static PreparedStatement banAdd, banLookup;

    public static synchronized void init() {
        if(initialized)
            return;
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            conn = DriverManager.getConnection("jdbc:hsqldb:file:db/kanzedb", "SA", "");
            if(createTables())
                createStatements();
            initialized = true;
        } catch(SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void handleMessage(MessageEvent e) {
        if(!initialized || e.isPrivate())
            return;
        if(e.isEdit()) {
            try {
                messageUpdate.setString(1, e.getMessage().getId());
                messageUpdate.setString(2, e.getMessage().getContent());
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
                messageInsert.setString(6, e.getMessage().getContent());
                messageInsert.setDate(7, new Date(e.getMessage().getTime().toEpochSecond() * 1000));
                messageInsert.executeUpdate();
            } catch(SQLException ex) {
                ex.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
                    boolean exists = Arrays.stream(aliases.split(" ")).anyMatch(a -> a.equals(user.getUsername()));
                    if(!exists) {
                        aliases = aliases + " " + user.getUsername();
                        if(aliases.length() > 1000)
                            aliases = aliases.substring(aliases.indexOf(" ", aliases.length() - 1000) + 1);
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
            e.printStackTrace();
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
        String[] buff;
        List<String[]> responses = new LinkedList<>();
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
                    buff[i - 1] = rs.getObject(i).toString();
                    maxLength[i - 1] = Math.max(maxLength[i - 1], buff[i - 1].length() + 1);
                }
                responses.add(buff);
            }
            StringBuilder fb = new StringBuilder();
            for(int i : maxLength) {
                fb.append("%-").append(i).append('s');
            }
            String format = fb.append('\n').toString();
            StringBuilder out = new StringBuilder();
            for(String[] response : responses) {
                out.append(String.format(format, response));
            }
            out.setLength(out.length() - 1);
            return out.toString();
        } catch(SQLException e) {
            e.printStackTrace();
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
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean createTables() {
        try {
            conn.setAutoCommit(false);
            conn.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS messages(" +
                    "  id VARCHAR(30) NOT NULL PRIMARY KEY," +
                    "  guildId VARCHAR(30) NOT NULL," +
                    "  channelId VARCHAR(30) NOT NULL," +
                    "  authorId VARCHAR(30) NOT NULL," +
                    "  authorName VARCHAR(32) NOT NULL,\n" +
                    "  content VARCHAR(2000) NOT NULL,\n" +
                    "  created DATETIME NOT NULL,\n" +
                    "  deleted BIT(1) DEFAULT 0 NOT NULL\n" +
                    ");");
            conn.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS message_edits(\n" +
                    "  id INT IDENTITY PRIMARY KEY,\n" +
                    "  messageId VARCHAR(30) NOT NULL REFERENCES messages(id) ON DELETE CASCADE,\n" +
                    "  content VARCHAR(2000) NOT NULL,\n" +
                    "  editTime DATETIME NOT NULL\n" +
                    ");");
            conn.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS bans(\n" +
                    "  id INT IDENTITY PRIMARY KEY,\n" +
                    "  guildId VARCHAR(30) NOT NULL,\n" +
                    "  bannedId VARCHAR(30) NOT NULL,\n" +
                    "  bannedName VARCHAR(32) NOT NULL,\n" +
                    "  executorId VARCHAR(30) NOT NULL,\n" +
                    "  executorName VARCHAR(32) NOT NULL,\n" +
                    "  reason VARCHAR(250) NOT NULL,\n" +
                    "  createTime DATETIME NOT NULL\n" +
                    ");");
            conn.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS users(\n" +
                    "  id VARCHAR(30) NOT NULL PRIMARY KEY,\n" +
                    "  username VARCHAR(32) NOT NULL,\n" +
                    "  aliases VARCHAR(1000) NOT NULL,\n" +
                    ");");
            conn.commit();
            return true;
        } catch(SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch(SQLException e1) {
                e1.printStackTrace();
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
        try {
            messageInsert.close();
            messageDelete.close();
            messageUpdate.close();
            userUpdate.close();
            banAdd.close();
            banLookup.close();
        } catch(SQLException e) {
            e.printStackTrace();
        }
        try {
            conn.createStatement().execute("SHUTDOWN COMPACT;");
        } catch(SQLException e) {
            e.printStackTrace();
        }
        try {
            conn.close();
        } catch(SQLException e) {
            e.printStackTrace();
        }
        conn = null;
        initialized = false;
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
}
