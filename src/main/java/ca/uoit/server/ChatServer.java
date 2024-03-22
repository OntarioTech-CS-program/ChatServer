package ca.uoit.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class ChatServer {
    protected Socket clientSocket = null;
    protected ServerSocket serverSocket = null;

    // an array of threads to handle client connections
    protected ChatServerThread[] threads = null;
    // the number of currently connected clients
    protected int numClients = 0;
    // a vector to store client messages
    protected Vector messages = new Vector();

    public static int SERVER_PORT = 16789;
    public static int MAX_CLIENTS = 25;

    public ChatServer() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            threads = new ChatServerThread[MAX_CLIENTS];

            System.out.println("Listening on port " + SERVER_PORT);
            System.out.println("Waiting for clients to connect: " + MAX_CLIENTS + " clients in total can connect.");

            while(true) {
                clientSocket = serverSocket.accept();
                System.out.println("Client #" + (numClients + 1) + " connected.");
                threads[numClients] = new ChatServerThread(clientSocket, messages);
                threads[numClients].start();
                numClients++;
            }
        } catch (IOException e) {
            System.err.println("IO Exception while creating server connection. Total client connections so far: " + numClients);
        }
    }

    public static void main(String[] args) {
        ChatServer app = new ChatServer();
    }

}
