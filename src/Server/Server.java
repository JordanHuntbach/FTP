package Server;

import java.io.*;
import java.net.*;

public class Server {

    public static void main(String[] args){
        Server server = new Server();
        server.waitForConnection();
    }

    private void waitForConnection() {
        try {
            // Sets up socket.
            ServerSocket serverSocket;

            while (true) {
                // "Wait for connection" state.
                serverSocket = new ServerSocket(2121);
                System.out.println("Waiting for connection...");
                Socket socket = serverSocket.accept();
                System.out.println("Connection established.");
                while (true) {
                    try {
                        System.out.println("Waiting for operation from client...");
                        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                        // Gets string representation of the data.
                        String str = dataInputStream.readUTF();

                        System.out.println("Operation received: " + str);

                        if (str.equals("QUIT")) {
                            System.out.println("Client disconnected.\n");
                            serverSocket.close();
                            break;
                        } else {
                            // Returns messages to the client.
                            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                            dataOutputStream.writeUTF("Operation received: " + str);
                            dataOutputStream.flush();

                            // Closes socket.
                            dataOutputStream.close();
                            System.out.println("Connection Closed\n\n");
                        }
                    } catch (EOFException e) {
                        System.out.println("Client disconnected.\n");
                        serverSocket.close();
                        break;
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
