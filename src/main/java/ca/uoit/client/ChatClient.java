package ca.uoit.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class ChatClient {

    private Socket socket = null;

    private BufferedReader inputKeyboard = null;

    private PrintWriter networkOut = null;
    private BufferedReader networkIn = null;


    public  static String SERVER_ADDRESS = "localhost";
    public  static int SERVER_PORT = 16789;

    public ChatClient() {

        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        } catch (UnknownHostException e) {
            System.err.println("Unknown Host Exception at: " + SERVER_ADDRESS);
        } catch (IOException e) {
            System.err.println("IO Exception while connecting to the chat server at: " + SERVER_ADDRESS);
        }

        if (socket == null) {
            System.err.println("Socket is null: Unable to connect to the chat server");
        }


        try {
            networkOut = new PrintWriter(socket.getOutputStream(), true);
            networkIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("IO Exception while opening a read/write connection to the chat server");
        }

        // get input from chat client user
        inputKeyboard = new BufferedReader(new InputStreamReader(System.in));

        // get username and password of chat client user
        boolean ok = login();

        // on unsuccessful login, exit
        if (!ok) {
            System.exit(0);
        }

        //on successful login, process chat client user commands
        //ok = true;
        while(ok) {
            ok = processUserInput();
        }

        // close connection to the chat server once there are no more chat client user commands to process
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected boolean login() {
        String input = null;
        String message = null;
        int errorCode = 0;

        try {
            // read the first two messages sent by the chat server
            message = networkIn.readLine(); // Welcome to chat
            System.out.println(message);
            message = networkIn.readLine(); // 200 Ready for chat
            System.out.println(message);
        } catch (IOException e) {
            System.err.println("Error reading initial greeting from the chat server");
        }


        // keep trying to log in unless user quits
        while(errorCode != 200) {

            // get userID
            System.out.print("Type your username (quit to exit): ");
            try {
                input = inputKeyboard.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // return false if quit, else proceeds
            if (input.equalsIgnoreCase("quit")) {
                return false;
            }

            networkOut.println("UID " + input);
            try {
                message = networkIn.readLine();
            } catch (IOException e) {
                System.err.println("Error reading response to UID.");
            }

            // get password
            System.out.print("Passcode: ");
            try {
                input = inputKeyboard.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            networkOut.println("PWD " + input);

            try {
                message = networkIn.readLine();
            } catch (IOException e) {
                System.err.println("Error reading response to PWD.");
            }
            errorCode = getErrorCode(message);
            if (errorCode != 200) {
                System.out.println("Login unsuccessful: " + getErrorMessage(message));
                return false;
            }
        }
        return true;
    }

    // display possible chat client commands and process them
    protected boolean processUserInput() {
        String input = null;

        // print the possible commands
        System.out.println("Possible Commands: ");
        System.out.println("1 - List all messages");
        System.out.println("2 - Add new message");
        System.out.println("3 - Quit");
        System.out.print("Enter command: ");

        try {
            input = inputKeyboard.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (input.equals("1")) {
            listAllMessages();
        } else if (input.equals("2")) {
            addNewMessage();
        } else if (input.equals("3")) {
            return false;
        }
        return true;
    }

    protected int getErrorCode(String message) {
        StringTokenizer st = new StringTokenizer(message);
        String code = st.nextToken();
        return Integer.parseInt(code);
    }

    protected String getErrorMessage(String message) {
        StringTokenizer st = new StringTokenizer(message);
        String code = st.nextToken();
        String errorMessage = null;
        if (st.hasMoreTokens()) {
            errorMessage = message.substring(code.length() + 1);
        }
        return errorMessage;
    }

    // add new chat client message
    public void addNewMessage() {
        String message = null;
        String input = null;

        System.out.print("Please type your message: ");
        try {
            input = inputKeyboard.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // send ADDMSG + input to the chat server
        networkOut.println("ADDMSG " + input);

        // read and ignore the chat server response
        try{
            message = networkIn.readLine();
        }catch(IOException e){
            System.err.println("Error reading from socket.");

        }

    }

    // list all chat client messages so far
    public void listAllMessages() {
        String message = null;

        // Send a request to the  chat server to get the index of the last message using the "LASTMSG" command
        networkOut.println("LASTMSG");
        int id = -1;
        try {
            message = networkIn.readLine();
        } catch (IOException e) {
            System.err.println("Error reading from socket.");
        }

        // parse chat server response
        // response format: "RESPONSE_CODE: <INT>"
        String strID = message.substring(message.indexOf(':') + 1);
        id = Integer.parseInt(strID.trim());

        // send the GETMSG command for each messages from 0 to id
        for(int i = 0; i <= id; i++){
            networkOut.println("GETMSG " + i);
            try {
                message = networkIn.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int index = message.indexOf(':') + 1;
            String msg = message.substring(index);
            // print chat client message on the console
            System.out.println(msg);
        }

    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
    }

}
