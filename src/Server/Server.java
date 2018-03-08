package Server;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Server {
    private int port = 2121;

    public static void main(String[] args){
        Server server = new Server();
        server.waitForConnection();
    }

    private void waitForConnection() {
        // Sets up socket.
        ServerSocket serverSocket;
        getPort();
        while (true) {
            try {
                // "Wait for connection" state.
                serverSocket = new ServerSocket(port);
                System.out.println("Waiting for connection...");
                Socket socket = serverSocket.accept();
                System.out.println("Connection established.");

                // Prevents other clients connecting to the server - they would just hang while the server processes the first client.
                serverSocket.close();

                // Sets up input / output streams.
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                // Until the client disconnects, the server will handle its operations in this loop.
                while (true) {
                    try {
                        // "Waiting for operation from client" state.
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
                                delete(dataInputStream, dataOutputStream);
                                break;
                            case "UPLD":
                                upload(dataInputStream, dataOutputStream);
                                break;
                            case "DWLD":
                                download(dataInputStream, dataOutputStream);
                                break;
                            default:
                                System.out.println("Operation \"" + str + "\" not recognised.");
                                break;
                        }
                    } catch (SocketException | EOFException e) {
                        System.out.println("Client disconnected.\n");
                        serverSocket.close();
                        break;
                    }
                }
            } catch (BindException e) {
                System.out.print("There is an error connecting to port: " + port);
                System.out.print("Please restart the server.");
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getPort() {
        while (true) {
            Scanner reader = new Scanner(System.in);
            System.out.print("\nPort to listen on: ");
            String input = reader.nextLine();
            port = Integer.valueOf(input);
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.print("Connected to port: " + port + "\n");
                serverSocket.close();
                return;
            } catch (IOException | IllegalArgumentException e) {
                System.out.print("Cannot bind to port number: " + port);
            }
        }
    }

    private void delete(DataInputStream inputStream, DataOutputStream outputStream) throws IOException{
        // Server decodes what it receives..
        short filename_size = inputStream.readShort();
        StringBuilder filename = new StringBuilder();
        for (int i = 0; i < filename_size; i++) {
            filename.append(inputStream.readChar());
        }

        // ..and checks to see if the file exists in its local directory.
        File file = new File("./src/Server/Files/" + filename);
        if (file.exists()) {
            // If the file exists, the server sends a positive confirm (integer value 1) back to the client.
            System.out.println("File " + filename + " exists.");
            outputStream.writeInt(1);
            outputStream.flush();

            // The server waits for the delete confirm sent by the client.
            boolean confirm = inputStream.readUTF().equals("Yes");
            if (confirm) {
                // If the confirm is "Yes", the server deletes the requested file..
                String response;
                if(file.delete()) {
                    response = "File deleted successfully.";
                } else {
                    response = "Failed to delete the file.";
                }

                // ..and returns an acknowledgement to the client to indicate the success or failure of file deletion operation.
                System.out.println(response);
                outputStream.writeUTF(response);
                outputStream.flush();
            } else {
                // If the confirm is "No", the server returns to "wait for operation from client" state.
                System.out.println("Delete abandoned by client.");
            }
        } else {
            // If the file does not exit, the server sends a negative confirm (integer value -1) back to the client.
            System.out.println("File " + filename + " doesn't exist.");
            outputStream.writeInt(-1);
            outputStream.flush();

            // After that, the server returns to the "wait for operation from client" state.
        }
    }

    private void list(DataOutputStream outputStream) throws IOException {
        // Server obtains listing of it’s directories/files.
        File folder = new File("./src/Server/Files/");
        File[] listOfFiles = folder.listFiles();
        assert listOfFiles != null;

        // Server computes the size of directory listing..
        int size = listOfFiles.length;
        // ..and sends the size to client as a 32 bit int.
        outputStream.writeInt(size);

        System.out.println(size + " files/directories found.");

        // Server sends file names to client.
        for (File file : listOfFiles) {
            System.out.println(file.getName());
            String name = file.getName();
            outputStream.writeUTF(name);
        }
        outputStream.flush();
    }

    private void upload(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {

        // Server receives the information, decodes file name size and file name...
        short filename_size = inputStream.readShort();
        StringBuilder filename = new StringBuilder();
        for (int i = 0; i < filename_size; i++) {
            filename.append(inputStream.readChar());
        }

        // ..and acknowledges that it is ready to receive.
        outputStream.writeUTF("READY");
        outputStream.flush();
        System.out.println("Receiving " + filename + "...");

        // Server receives and decodes the file size.
        int number_of_bytes = inputStream.readInt();

        // Server enters a loop to receive file (timing the duration of the upload).
        byte[] bytes = new byte[number_of_bytes];
        int byteCounter = 0;
        long startTime = System.currentTimeMillis();
        while(byteCounter < number_of_bytes) {
            int numBytesRead = inputStream.read(bytes, byteCounter, number_of_bytes - byteCounter);
            byteCounter += numBytesRead;
        }
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        float time = (float) (duration / 1000.0);

        // Server computes the transfer process result (such as how many bytes received, processing time etc.)
        String results = number_of_bytes + " bytes received in " + time + " seconds.";
        System.out.println(results);

        // The server sends the transfer process results to the client.
        outputStream.writeUTF(results);
        outputStream.flush();

        // Write the file to disk.
        FileOutputStream out = new FileOutputStream("./src/Server/Files/" + filename);
        out.write(bytes);
        out.close();

        // Return to "wait for operation" state.
    }

    private void download(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        //Server decodes what it receives..
        short filename_size = inputStream.readShort();
        StringBuilder filename = new StringBuilder();
        for (int i = 0; i < filename_size; i++) {
            filename.append(inputStream.readChar());
        }
        try {
            // ..and checks to see if the file exists in its local directory.
            File file = new File("./src/Server/Files/" + filename);
            FileInputStream in = new FileInputStream(file);

            System.out.println("Sending File: " + filename);

            // Store each byte from the file in an array.
            int file_size = (int) file.length();
            byte[] bytes = new byte[file_size];
            in.read(bytes);
            in.close();

            // Server returns the size of the file to the client as a 32-bit int.
            outputStream.writeInt(file_size);
            outputStream.flush();

            // Server sends the file to client.
            outputStream.write(bytes);
            outputStream.flush();

        } catch (FileNotFoundException e) {
            // If the file does not exist, server will return a negative 1 (32-bit int).
            System.out.print("Could not find file " + filename + "\n");
            outputStream.writeInt(-1);
            outputStream.flush();

            // After that, the server should return to the "wait for operation from client" state.
        }
    }
}
