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
                serverSocket.close();
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
                                delete(dataInputStream, dataOutputStream);
                                break;
                            case "UPLD":
                                upload(dataInputStream, dataOutputStream);
                                break;
                            case "DWLD":
                                download(dataInputStream, dataOutputStream);
                                break;
                            default:
                                System.out.println("Command not recognised.");
                                dataOutputStream.writeUTF("Operation \"" + str + "\" not recognised.");
                                dataOutputStream.flush();
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
        short filename_size = inputStream.readShort();
        StringBuilder filename = new StringBuilder();
        for (int i = 0; i < filename_size; i++) {
            filename.append(inputStream.readChar());
        }
        File file = new File("./src/Server/" + filename);
        if (file.exists()) {
            System.out.println("File " + filename + " exists.");
            outputStream.writeInt(1);
            outputStream.flush();
            boolean confirm = inputStream.readUTF().equals("Yes");
            if (confirm) {
                String response;
                if(file.delete()) {
                    response = "File deleted successfully.";
                } else {
                    response = "Failed to delete the file.";
                }
                System.out.println(response);
                outputStream.writeUTF(response);
                outputStream.flush();
            } else {
                System.out.println("Delete abandoned by client.");
            }
        } else {
            System.out.println("File " + filename + " doesn't exist.");
            outputStream.writeInt(-1);
            outputStream.flush();
        }
    }

    private void list(DataOutputStream outputStream) throws IOException {
        File folder = new File("./src/Server/");
        File[] listOfFiles = folder.listFiles();
        assert listOfFiles != null;

        int size = listOfFiles.length;
        outputStream.writeInt(size);

        System.out.println(size + " files/directories found.");
        for (File file : listOfFiles) {
            System.out.println(file.getName());
            String name = file.getName();
            outputStream.writeUTF(name);
        }
        outputStream.flush();
    }

    private void upload(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        short filename_size = inputStream.readShort();
        StringBuilder filename = new StringBuilder();
        for (int i = 0; i < filename_size; i++) {
            filename.append(inputStream.readChar());
        }
        outputStream.writeUTF("READY");
        outputStream.flush();
        System.out.println("Receiving " + filename + "...");
        int number_of_bytes = inputStream.readInt();
        ArrayList<Integer> bytes = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < number_of_bytes; i++) {
            bytes.add(inputStream.readInt());
        }
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        float time = (float) (duration / 1000.0);
        String results = number_of_bytes + " bytes received in " + time + " seconds.";
        System.out.println(results);
        outputStream.writeUTF(results);
        outputStream.flush();

        FileOutputStream out = new FileOutputStream("./src/Server/" + filename);
        for (int num : bytes) {
            out.write(num);
        }
        out.close();
    }

    private void download(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        short filename_size = inputStream.readShort();
        StringBuilder filename = new StringBuilder();
        for (int i = 0; i < filename_size; i++) {
            filename.append(inputStream.readChar());
        }
        try {
            FileInputStream in = new FileInputStream("./src/Server/" + filename);
            System.out.println("Sending File: " + filename);
            ArrayList<Integer> bytes = new ArrayList<>();
            int b;
            while ((b = in.read()) != -1) {
                bytes.add(b);
            }
            in.close();
            int file_size = bytes.size();
            outputStream.writeInt(file_size);
            outputStream.flush();
            for (int send : bytes) {
                outputStream.writeInt(send);
            }
            outputStream.flush();
        } catch (FileNotFoundException e) {
            System.out.print("Could not find file " + filename + "\n");
            outputStream.writeInt(-1);
            outputStream.flush();
        }
    }
}
