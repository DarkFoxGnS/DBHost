package javawebrest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
/**
 * @author Tibor Péter Szabó
 */
/**
 * Serves the clients.
 */
public class ServingManager implements Runnable {

    Thread t;
    Socket client;
    InputStream is;
    OutputStream os;

    /**
     * Creates a new instance of the ServingManager class.
     *
     * @param client - Socket of the connection.
     */
    public ServingManager(Socket client) {
        this.client = client;
    }

    /**
     * Starts the thread.
     */
    public void start() {
        t = new Thread(this);
        t.start();
    }

    /**
     * Main method of the thread.
     */
    @Override
    public void run() {
        try {
            is = client.getInputStream();
            os = client.getOutputStream();
            byte[] buffer;
            String request = "";
            String result = "";
            HashMap<String, String> parsedRequest = null;
            int timer = 0;
            while (is.available() == 0 && timer < 100) {
                timer++;
                Thread.sleep(10);
            }
            buffer = new byte[is.available()];
            is.read(buffer);
            request = new String(buffer, "UTF-8");
            parsedRequest = parseTokens(request);
            if (parsedRequest.get("Command") != null) {
                result = GET(parsedRequest);
            }

            os.write(result.getBytes());
            os.flush();
            Thread.sleep(10);
            client.close();
            os = null;
            is = null;
            System.out.println("________________________________________________");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    /**
     * Handles the GET requests from the client.
     * @param parsedRequest - Parsed HashMap of the request.
     * @return String - Response text.
     */
    public String GET(HashMap<String, String> parsedRequest) {
        String response = "";
        String url = parsedRequest.get("Path");
        String sessionID = getCookie("sessionId", parsedRequest.get("Cookie"));
        int uid = DBManager.getUserId(sessionID);
        System.out.println("----------REQUEST-----------");
        System.out.println("GET " + url);
        System.out.println("----------RESPONSE----------");
        switch (url) {
            case "/":
                if (uid == 0) {
                    response += "HTTP/1.1 302 Found\n"
                            + "Location: /login";
                    System.out.println("No session");
                    System.out.println("Reroute to login");
                    break;
                }
                response += createOKHeader("text/html");

                if (getCookie("command", parsedRequest.get("Cookie")) != null) {
                    response += parseWebsite(readFromFile("table.html"), uid);
                    System.out.println("table.html");
                } else {
                    response += parseWebsite(readFromFile("main.html"), uid);
                    System.out.println("main.html");
                }

                break;
            case "/command":
                if (uid == 0) {
                    response += "HTTP/1.1 302 Found\n"
                            + "Location: /login";
                    break;
                }
                String commandID = getCookie("command", parsedRequest.get("Cookie"));
                if (!DBManager.canAccess(commandID, uid)) {
                    response += "HTTP/1.1 403 Forbidden\n"
                            + "Date: " + new Date().toString();
                    break;
                }
                response += createOKHeader("text/plain");
                String commandResult = commandResult = DBManager.executeCommand(Integer.parseInt(commandID));

                System.out.println("Command: " + commandID + "=" + commandResult);
                response += commandResult;
                break;
            case "/commands":
                if (uid == 0) {
                    response += "HTTP/1.1 302 Found\n"
                            + "Location: /login";
                    break;
                }
                response += createOKHeader("text/plain");
                String commands = DBManager.getCommands(uid);
                System.out.println(commands);
                response += commands;
                break;
            case "/login":
                response += createOKHeader("text/html");
                response += readFromFile("login.html");
                System.out.println("login.html");
                break;
            case "/logout":
                DBManager.removeSessionId(uid);
                response += "HTTP/1.1 302 Found\n"
                        + "Location: /login\n"
                        + "Set-Cookie: sessionId=;HttpOnly;Max-Age=-1";
                System.out.println(DBManager.getUserNick(uid) + " logged out");
                break;
            case "/auth":
                String auth = getCookie("Auth", parsedRequest.get("Cookie"));
                System.out.println("Credentials: " + auth);
                if (auth == null) {
                    response += "HTTP/1.1 403 Forbidden\n"
                            + "Date: " + new Date().toString();
                    System.out.println("No Credentials");
                    break;
                }
                String username = "";
                String password = "";
                try {
                    username = auth.split(" ")[0];
                    password = auth.split(" ")[1];
                } catch (Exception e) {
                    return null;
                }
                uid = DBManager.getUserId(username, password);

                if (uid == 0) {
                    response += "HTTP/1.1 403 Forbidden\n"
                            + "Date: " + new Date().toString();
                    System.out.println("Failed login");
                    break;
                }

                response += createOKHeader("text/plain", "Set-Cookie: sessionId=" + DBManager.createSessionId(uid) + ";HttpOnly;Max-Age=3600");
                response += "/";
                System.out.println(DBManager.getUserNick(uid) + " logged in");
                break;
            case "/favicon.ico":
                response += "HTTP/1.1 200 Not Found";
                break;
            case "/crypto.js":
                response += createOKHeader("text/javascript");
                response += readFromFile("crypto.js");
                System.out.println("crypto.js sent");
                break;
            default:
                System.out.println("error");
                System.out.println(parsedRequest.get("Request"));
                response += "HTTP/1.1 404 Not Found";
        }
        return response;
    }
    /**
     * Replaces the content of the website before displaying it to the user.
     * @param website - String representation of the website.
     * @param uid - ID of the user.
     * @return String - parsed content of the website.
     */
    public static String parseWebsite(String website, int uid) {
        String accessLevel = DBManager.getUserAccess(uid);
        String userNick = DBManager.getUserNick(uid);
        String userName = DBManager.getUserName(uid);

        website = website.replace("$userNick$", userNick);
        website = website.replace("$userName$", userName);
        website = website.replace("$accessLevel$", accessLevel);

        return website;
    }
    /**
     * Gets the cookie from the cookie list string.
     * @param cookieToFind - Cookie to find in the string.
     * @param cookies - A string of cookies.
     * @return String - Content of the cookie.
     */
    public static String getCookie(String cookieToFind, String cookies) {
        if (cookies == null) {
            return null;
        }
        for (String c : cookies.split(";")) {
            c = c.trim();
            if (c.split("=")[0].equals(cookieToFind)) {
                try {
                    return c.split("=")[1];
                } catch (Exception e) {
                    return "";
                }
            }
        }
        return null;
    }
    /**
     * Reads from a file.
     * @param URL - URL of the file.
     * @return String - Content of the file.
     */
    public static String readFromFile(String URL) {
        String result = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(URL));

            String line = "";
            while ((line = br.readLine()) != null) {
                result += line + "\n";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    /**
     * Parses the HTTP header.
     * @param request - Request.
     * @return HashMap - Parsed data.
     */
    public static HashMap<String, String> parseTokens(String request) {
        HashMap<String, String> tokens = new HashMap<>();
        tokens.put("Request", request);
        for (int i = 0; i < request.split("\n").length; i++) {
            String line = request.split("\n")[i];

            if (i == 0 && line.split(" ").length > 1) {
                tokens.put("Command", line.split(" ")[0]);
                if (line.contains("?")) {
                    tokens.put("Params", line.split(" ")[1].split("\\?")[1]);
                }
                tokens.put("Path", line.split(" ")[1].split("\\?")[0]);
                tokens.put("Protocol", line.split(" ")[2]);
                continue;
            }
            try {
                tokens.put(line.split(":")[0].trim(), line.split(":")[1].trim());
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
        return tokens;
    }
    /**
     * Creates a 200 OK header.
     * @param contentType - Type of the content.
     * @param aditional - Additional information of the header.
     * @return String - Assembled header.
     */
    public static String createOKHeader(String contentType, String... aditional) {
        String result = "";
        result += "HTTP/1.1 200 OK\n";
        result += "Date: " + new Date().toString() + "\n";
        result += "Content-type: " + contentType + "\n";

        for (int i = 0; i < aditional.length; i++) {
            result += aditional[i] + "\n";
        }

        result += "\n";
        return result;
    }
}
