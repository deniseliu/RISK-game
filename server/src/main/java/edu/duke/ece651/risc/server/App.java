/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package edu.duke.ece651.risc.server;

import java.io.IOException;
import java.net.ServerSocket;

public class App {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ServerSocket serverSock = new ServerSocket(12345);
        SocketServer<String> server = new SocketServer<String>(serverSock, "BasicPlayer");
        server.runServer();
    }
}
