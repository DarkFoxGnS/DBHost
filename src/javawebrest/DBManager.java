package javawebrest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

/**
 * @author Tibor Péter Szabó
 */
/**
 * Handles the Database connection and requests.<br>
 * <br>
 * Database table formalities:<br>
 * <br>
 * users - stores data about the profiles that can log in {<br>
 * <ul>
 * <li>id - Integer - Unique id for the user.<br>
 * <li>user - String - Username of the user.<br>
 * <li>pass - String - Password of the user in sha256 encoding.<br>
 * <li>access - String - Access level of the user.<br>
 * <li>sessionId - String - A base64 representation of the valid session key,
 * <li>expected as sha256 hash from the frontend.<br>
 * <li>nick - String - Display name for the user.<br>
 * </ul>
 * }<br>
 *
 * roles - stores roles of the employees {<br>
 * <ul>
 * <li>id - Integer - Unique id for the role. 0 is reserved for undefined;.<br>
 * <li>role - String - Name of the role.<br>
 * </ul>
 * }<br>
 *
 * employees - stores employee data {<br>
 * <ul>
 * <li>id - Integer - Unique id for the employee. 0 is reserved for no employee.<br>
 * <li>name - String - Name of the employee.<br>
 * <li>role - Integer - Number of the role of the employee.<br>
 * <li>wgroup - Integer - Number of the workgroup the employee is in.<br>
 * <li>location - Integer - Number of the location the employee is at.<br>
 * <li>status - Integer - Acts as boolean (1:Working, 0:Not working) to represent
 * <li>the state of the employee.<br>
 * </ul>
 * }<br>
 *
 * workgroups - stores existing workgroups {<br>
 * <ul>
 * <li>id - Integer - Unique id of the workgroup. 0 is reserved for no
 * <li>workgroup.<br>
 * <li>name - String - Designation of the workgroup.<br>
 * <li>leader - Integer - Id number of the leader employee.<br>
 * </ul>
 * }<br>
 *
 * location - represents locations in the area {<br>
 * <ul>
 * <li>id - Integer - Unique id of the location.<br>
 * <li>name - String - Designation of the location in text.<br>
 * </ul>
 * }<br>
 *
 * machines - table that stores existing machines {<br>
 * <ul>
 * <li>id - Integer - Unique id of the machine.<br>
 * <li>name - String - Designation of the machine.<br>
 * <li>status - Integer - Current status of the machine.<br>
 * <li>location - Integer - Location of the machine.<br>
 * <li>user - Integer - User of the machine.<br>
 * </ul>
 * }<br>
 *
 * status - stores possile statuses for machines {<br>
 * <ul>
 * <li>id - Integer - Unique id for the status.<br>
 * <li>status - String - Designation of the status in text.<br>
 * </ul>
 * }<br>
 *
 * commands - stores executable commands {<br>
 * <ul>
 * <li>id - Integer - Unique id of the command.<br>
 * <li>name - String - Name of the command.<br>
 * <li>return - String - List of the returning values headers, separated by ','
 * <li>character.<br>
 * <li>sql - String - SQL command that will be executed.<br>
 * <li>accessList - String - List of access levels that can use this command
 * <li>separated by ',' character.<br>
 * </ul>
 * }<br>
 */
public class DBManager {

    private static Connection connection;
    private static Statement statement;
    private static boolean accessible = true;

    private DBManager() {
    }

    /**
     * This method connects and creates the required connections to the
     * database, so it can be used.
     *
     * @return boolean - Returns true if the connection was successful, false
     * otherwise.
     */
    public static boolean init() {
        try {
            System.out.print("Connecting to Database...");
            connection = DriverManager.getConnection("jdbc:sqlite:./sql/database.db");
            System.out.println("Done");
            statement = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Attempt to grab and lock the database, so only this thread can use it.
     *
     * @return boolean - Returns if the database is ready to use.
     */
    public static boolean grab() {
        if (accessible) {
            accessible = false;
            return true;
        }

        return false;
    }

    /**
     * Releases the database, so other threads can use it as well.
     */
    public static void release() {
        accessible = true;
    }

    /**
     *
     * @param query - SQL code to query.
     * @return ResultSet - returns the result of the database query.
     * @throws SQLException
     */
    public static ResultSet query(String query) throws SQLException {
        return statement.executeQuery(query);
    }

    /**
     *
     * @param executable - SQL code to execute;
     * @return boolean - Returns success of the execution.
     * @throws SQLException
     */
    public static boolean exec(String executable) throws SQLException {
        return statement.execute(executable);
    }

    /**
     * Get the available commands for a user.
     *
     * @param uid - User id of the user.
     * @return String - Returns a String representation of the command list.
     */
    public static String getCommands(int uid) {

        int accessLevel = getUserAccessInt(uid);
        String result = "";
        try {
            while (!grab()) {
                Thread.sleep(10);
            }
            ResultSet rs = query("select * from commands;");
            while (rs.next()) {
                String accessListString = rs.getString("accessList");
                String[] accessList = accessListString.split(",");
                for (String access : accessList) {
                    if (access.equals(accessLevel + "")) {
                        result += rs.getInt("id") + ":" + rs.getString("name") + ";";
                        break;
                    }
                }
            }
            if (result.length() != 0) {
                result = result.substring(0, result.length() - 1);
            }
            rs.close();
            release();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            release();
            result = getCommands(uid);
        }
        return "";
    }

    /**
     * Executes a given command based on it's id.
     *
     * @param commandID - ID of the command to be executed.
     * @return String - Result of the executed code.
     */
    public static String executeCommand(int commandID) {
        String result = "";
        String command = "";
        try {
            while (!grab()) {
                Thread.sleep(10);
            }
            ResultSet rs = query("select return,sql from commands where id=" + commandID + ";");

            result += rs.getString("return") + ";";
            command = rs.getString("sql");
            rs = query(command);

            while (rs.next()) {
                String line = "";
                try {
                    int i = 1;
                    while (true) {
                        String word = rs.getString(i);
                        word = word.replaceAll(",", "%u44");
                        word = word.replaceAll(";", "%u59");
                        line += word + ",";
                        i++;
                    }
                } catch (Exception e) {
                }
                result += line.substring(0, line.length() - 1);
                result += ";";
            }
            result = result.substring(0, result.length() - 1);
            rs.close();
            release();
        } catch (Exception e) {
            release();
            e.printStackTrace();
            result = executeCommand(commandID);
        }

        return result;
    }

    /**
     * Checks if the user can access a certain command.
     *
     * @param commandID - ID of the command to be checked.
     * @param uid - ID of the user, who attempts to use the command;
     * @return boolean - If the user can access the command.
     */
    public static boolean canAccess(String commandID, int uid) {
        int accessLevel = getUserAccessInt(uid);
        try {
            while (!grab()) {
                Thread.sleep(10);
            }
            ResultSet rs = query("select accessList from commands where id=" + commandID + ";");
            String accessList = rs.getString("accessList");
            String[] accessListArray = accessList.split(",");
            for (String access : accessListArray) {
                if (access.equals(accessLevel + "")) {
                    rs.close();
                    release();
                    return true;
                }
            }
            rs.close();
            release();
        } catch (Exception e) {
            release();
            return canAccess(commandID, uid);
        }
        return false;
    }

    /**
     * Gets the name of the user.
     *
     * @param uid - ID of the user.
     * @return String - Name of the user.
     */
    public static String getUserName(int uid) {
        try {
            while (!grab()) {
                Thread.sleep(10);
            }
            ResultSet rs = query("Select user from users where"
                    + " id=" + uid + ";");
            String user = rs.getString("user");
            rs.close();
            release();
            return user;

        } catch (Exception e) {
            release();
            return getUserName(uid);
        }
    }

    /**
     * Gets the display name of the user.
     *
     * @param uid - ID of the user.
     * @return String - Display name of the user.
     */
    public static String getUserNick(int uid) {
        try {
            while (!grab()) {
                Thread.sleep(10);
            }
            ResultSet rs = query("Select nick from users where"
                    + " id=" + uid + ";");
            String nick = rs.getString("nick");
            rs.close();
            release();
            return nick;

        } catch (Exception e) {
            release();
            return getUserNick(uid);
        }
    }

    /**
     * Gets the users id.
     *
     * @param user - username of the user.
     * @param pass - password of the user.
     * @return Integer - The ID of the user.
     */
    public static int getUserId(String user, String pass) {
        try {
            while (!grab()) {
                Thread.sleep(10);
            }
            ResultSet rs = query("Select id from users where"
                    + " user=\'" + user + "\' and pass=\'" + pass + "\';");

            int id = rs.getInt("id");
            rs.close();
            release();
            return id;
        } catch (Exception e) {
            release();
            return getUserId(user, pass);
        }
    }

    /**
     * Gets the users ID.
     *
     * @param sessionId - Session ID.
     * @return Integer - ID of the user.
     */
    public static int getUserId(String sessionId) {
        int id = 0;
        try {
            while (!grab()) {
                Thread.sleep(10);
            }
            ResultSet rs = query("select id,sessionId from users");
            while (rs.next()) {
                String dbSessionId = rs.getString("sessionId");
                if (Utils.toSHA256(dbSessionId).equals(sessionId)) {
                    if (Calendar.getInstance().getTimeInMillis() - Long.parseLong(Utils.Base64ToText(dbSessionId).split(" ")[2]) > 0) {
                        break;
                    }
                    id = rs.getInt("id");
                    break;
                }
            }
            release();
            rs.close();
        } catch (Exception e) {
            release();
            id = getUserId(sessionId);
        }

        return id;
    }

    /**
     * Creates a new session ID.
     *
     * @param id - ID of the user.
     * @return String - The new session ID;
     */
    public static String createSessionId(int id) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1);
        String user = null;
        String plainSessionId = null;
        String base64SessionId = null;
        String hassedSessionId = null;

        try {
            while (!grab()) {
                Thread.sleep(10);
            }
            ResultSet rs = query("select user from users where id=" + id);
            user = rs.getString("user");
            rs.close();
            plainSessionId = user + " " + (int) (Math.random() * 100000) + " " + c.getTimeInMillis();
            base64SessionId = Utils.toBase64(plainSessionId);
            hassedSessionId = Utils.toSHA256(base64SessionId);
            exec("update users set sessionID=\'" + base64SessionId + "\' where id=" + id);

        } catch (Exception e) {
            e.printStackTrace();
        }
        release();

        return hassedSessionId;
    }

    /**
     * Creates a new expired session ID.
     *
     * @param id - id of the user.
     */
    public static void removeSessionId(int id) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, -1); //set validity to negative to always fail validity check.
        String user = null;
        String plainSessionId = null;
        String base64SessionId = null;

        try {
            while (!grab()) {
                Thread.sleep(10);
            }
            ResultSet rs = query("select user from users where id=" + id);
            user = rs.getString("user");
            rs.close();
            plainSessionId = user + " " + (int) (Math.random() * 100000) + " " + c.getTimeInMillis();
            base64SessionId = Utils.toBase64(plainSessionId);
            exec("update users set sessionID=\'" + base64SessionId + "\' where id=" + id);

        } catch (Exception e) {
            e.printStackTrace();
        }
        release();
    }

    /**
     * Gets the users access level.
     *
     * @param id - ID of the user;
     * @return Integer - Access level the user.
     */
    public static int getUserAccessInt(int id) {
        int accessInt = 0;
        try {
            while (!grab()) {
                Thread.sleep(10);
            }
            ResultSet rs = query("Select access from users where id=" + id + ";");
            accessInt = rs.getInt("access");
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        release();
        return accessInt;
    }

    /**
     * Gets the users access level.
     *
     * @param id - ID of the user.
     * @return String - Access level of the user.
     */
    public static String getUserAccess(int id) {
        try {
            while (!grab()) {
                Thread.sleep(10);
            }
            ResultSet rs = query("Select role from users join roles where access=roles.id and users.id=" + id + ";");
            String role = rs.getString("role");
            rs.close();
            release();
            return role;
        } catch (Exception e) {
            release();
        }
        return null;
    }

}
