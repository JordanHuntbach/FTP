package Server;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server {

    public static void main(String[] args){
        Server server = new Server();
        server.waitForConnection();
    }

    private void waitForConnection() {
        // Sets up socket.
        ServerSocket serverSocket;
        while (true) {
            try {
                // "Wait for connection" state.
                serverSocket = new ServerSocket(2121);
                System.out.println("Waiting for connection...");
                Socket socket = serverSocket.accept();
                System.out.println("Connection established.");
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                while (true) {
                    try {
                        System.out.println("\nWaiting for operation from client...");
                        String str = dataInputStream.readUTF();

                        System.out.println("Operation received: " + str);
                        switch (str) {
                            case "QUIT":
                                System.out.println("Client disconnected.\n");
                                serverSocket.close();
                                break;
                            case "LIST":
                                list(dataOutputStream);
                                break;
                            case "DELF":
                                break;
                            case "UPLD":
                                break;
                            case "DWLD":
                                break;
                            default:
                                System.out.println("Command not recognised.");
                                dataOutputStream.writeUTF("Operation \"" + str + "\" not recognised.");
                                dataOutputStream.flush();
                                break;
                        }
                    } catch (EOFException e) {
                        System.out.println("Client disconnected.\n");
                        serverSocket.close();
                        break;
                    }
                }
            } catch (BindException e) {
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void list(DataOutputStream outputStream) throws IOException {
        File folder = new File("./src/Server/");
        File[] listOfFiles = folder.listFiles();
        assert listOfFiles != null;

        int size = listOfFiles.length;
        outputStream.writeInt(size);

        System.out.println(size + " files /directories found.");
        for (File file : listOfFiles) {
            System.out.println(file.getName());
            String name = file.getName();
            outputStream.writeUTF(name);
        }
        outputStream.flush();
    }
}
