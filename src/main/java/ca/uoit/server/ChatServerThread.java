package ca.uoit.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;
import java.util.StringTokenizer;

public class ChatServerThread extends Thread{

    protected Socket socket = null;


    protected PrintWriter out = null;
    protected BufferedReader in = null;

    // available client logins
    protected String[] strUserIDs   = {"admin", "student"};
    protected String[] strPasswords = {"admin", "student"};

    protected boolean bLoggedIn = false;
    protected String strUserID = null;
    protected String strPassword = null;

    protected Vector messages = null;

    public ChatServerThread(Socket socket, Vector messages) {
        super();
        this.socket = socket;
        this.messages = messages;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("IO Exception while opening a read/write connection to the chat client.");
        }
    }

    public void run() {
        // initialize interaction
        out.println("Welcome to ChatServer Winter 2024!");
        out.println("200 Ready For Chat, Please Log In: ");

        boolean endOfSession = false;
        while(!endOfSession) {
            endOfSession = processCommand();
        }

        try {
            socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    protected boolean processCommand() {
        String message = null;
        try {
            message = in.readLine();
        } catch (IOException e) {
            System.err.println("Error reading command from socket.");
            return true;
        }
        if (message == null) {
            return true;
        }

        // parse the message received from chat client
        // message format: "CMD argument(s)"
        StringTokenizer st = new StringTokenizer(message);
        String command = st.nextToken();
        String args = null;
        if (st.hasMoreTokens()) {
            args = message.substring(command.length() + 1, message.length());
        }
        //process command
        return processCommand(command, args);
    }

    // - UID: receives the username
    // - PWD: checks if sent password matches the user
    // - GETMSG: gets a message at specific id
    // - LASTMSG: returns the id of the last (most recent) message
    // - ADDMSG: adds new message to chat
    // - LOGOUT: ends the client connection
    protected boolean processCommand(String command, String arguments) {

        // treating login
        if (command.equalsIgnoreCase("UID")) {
            // Always accept userIDs
            // This prevents hackers from finding available accounts
            // Store the userID and sk for password
            strUserID = arguments;
            out.println("200 Please Enter Password");
            return false;
        } else if (command.equalsIgnoreCase("PWD")) {
            // Check if the userID and password match stored accounts
            strPassword = arguments;
            boolean loginCorrect = false;
            for (int i = 0; i < strUserIDs.length; i++) {
                if (strUserIDs[i].equalsIgnoreCase(strUserID)) {
                    if (strPasswords[i].equalsIgnoreCase(strPassword)) {
                        loginCorrect = true;
                    }
                    break;
                }
            }
            if (loginCorrect) {
                out.println("200 Login Successful");
            } else {
                out.println("500 Login Unsuccessful");
                strUserID = null;
                strPassword = null;
            }
            return false;
        } else {
            if (strPassword == null) {
                // Unless logged in, no other commands can be processed
                out.println("500 Unauthenticated Client:  Please Log In");
                return false;
            }
        }

        // these are the other possible commands
        if (command.equalsIgnoreCase("LASTMSG")) {
            // send the index of the last (most recent msg) and send response code
            out.println("200 LastMessage: " + (messages.size() - 1));
            return false;
        } else if (command.equalsIgnoreCase("GETMSG")) {
            int id = Integer.parseInt(arguments);
            // send the message at the given index, send response code
            if(id >= 0 && id < messages.size()){
                String msg = (String)messages.elementAt(id);
                out.println("200 Message #" + id + ": " + msg);
            }else{
                out.println("400 Message does not exist.");
            }
            return false;
        } else if (command.equalsIgnoreCase("ADDMSG")) {
            // add a new message to the chat and send response code
            int id = -1;
            // make the critical section thread-safe
            synchronized (this) {
                messages.addElement("[" + strUserID + "]: " + arguments);
                id = messages.size() - 1;
            }
            out.println("200 Message sent: " + id);
            return false;
        } else if (command.equalsIgnoreCase("LOGOUT")) {
            out.println("200 Client Logged Out");
            return true;
        } else {
            out.println("400 Unrecognized Command: " + command);
            return false;
        }
    }

}
