package Server;

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
                break;
            } catch (IOException e) {
                e.printStackTrace();
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
        File file = new File("./src/Server/" + filename);
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
        File folder = new File("./src/Server/");
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
        ArrayList<Integer> bytes = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < number_of_bytes; i++) {
            bytes.add(inputStream.readInt());
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
        FileOutputStream out = new FileOutputStream("./src/Server/" + filename);
        for (int num : bytes) {
            out.write(num);
        }
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
            FileInputStream in = new FileInputStream("./src/Server/" + filename);

            System.out.println("Sending File: " + filename);

            // Read the file, and store the bytes in an array.
            ArrayList<Integer> bytes = new ArrayList<>();
            int b;
            while ((b = in.read()) != -1) {
                bytes.add(b);
            }
            in.close();

            // Server returns the size of the file to the client as a 32-bit int.
            int file_size = bytes.size();
            outputStream.writeInt(file_size);
            outputStream.flush();

            // Server sends the file to client.
            for (int send : bytes) {
                outputStream.writeInt(send);
            }
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
