package Client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static int port = 2121;

    public static void main(String[] args) {

        // Client attempts connection to server.
        Socket socket = null;
        try {
            socket = new Socket("localhost", port);
        } catch (IOException e) {
            System.out.print("Could not connect to server.");
            e.printStackTrace();
        }

        while (true) {
            try {
                // "Prompt user for operation" state.
                Scanner reader = new Scanner(System.in);
                System.out.print("\nEnter Operation: ");
                String operation = reader.nextLine();


                if (operation.equals("CONN")) {
                    if (socket == null || socket.isClosed()) {
                        System.out.println("Connecting to server...");
                        try {
                            socket = new Socket("localhost", port);
                            System.out.println("Connected.");
                        } catch (ConnectException e) {
                            System.out.println("Connection failed. Please try again.");
                        }
                    } else {
                        System.out.println("Already connected to a server.");
                    }
                } else if (socket == null || socket.isClosed()) {
                    System.out.println("Cannot perform operation " + operation + ".");
                    System.out.println("Please connect to a server using the 'CONN' operation.");
                } else if (operation.equals("")) {
                    ;
                } else {
                    // Sets up output stream to socket.
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    // Writes operation to socket.
                    dataOutputStream.writeUTF(operation);
                    // Sends message from socket.
                    dataOutputStream.flush();
                    // Closes output stream.
                    dataOutputStream.close();

                    switch (operation) {
                        case "QUIT":
                            socket.close();
                            System.out.println("Connection to server terminated.");
                            break;
                        case "LIST":
                            break;
                        case "DELF":
                            break;
                        case "UPLD":
                            break;
                        case "DWLD":
                            break;
                        default:
                            System.out.println("Not a valid command.");
                            break;
                    }

//                    // Gets result back from server.
//                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
//                    String response = dataInputStream.readUTF();
//                    System.out.println(response);

                }
            } catch (UnknownHostException e) {
                System.out.print("UnknownHostException: ");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.print("IOException: ");
                e.printStackTrace();
            }
        }
    }

}
