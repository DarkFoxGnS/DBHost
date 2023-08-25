package javawebrest;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Tibor Péter Szabó
 */
/**
 * Main class of the program.
 */
public class DBHost {

    /**
     * @param args
     */
    /**
     * The starting point of the program.
     */
    public static void main(String[] args) {
        //Connect to the sqlite3 database using JDBC
        if (!DBManager.init()) {
            //stop program if database cannot be connected
            System.err.println("Error occured starting the Database");
            System.exit(1);
        }

        System.out.print("Starting webserver on port 80....");
        //Handle connections
        try {
            ServerSocket ss = new ServerSocket(80);//create new server on HTTP (80) port
            System.out.println("Done");
            System.out.println("Waiting for connections...");
            while (true) {
                Socket client = ss.accept(); // wait till a connection is requested
                System.out.println("Connection from: " + client.getInetAddress()); //display ip of connected device
                new ServingManager(client).start();//create new thread to handle the connection, so it does not blocks the main thread.
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error occured during handling of the clients");
            System.exit(1);
        }

    }
}
