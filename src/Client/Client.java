package Client;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
    private static int port = 2121;

    public static void main(String[] args) {
        Client client = new Client();

        // Client attempts initial connection to server.
        Socket socket = null;
        try {
            System.out.println("Connecting to server...");
            socket = new Socket("localhost", port);
            System.out.println("Connected.");
        } catch (IOException e) {
            System.out.println("Connection failed. Please try again.");
        }

        client.waitForInput(socket);
    }

    private void waitForInput(Socket socket) {
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

                } else {
                    // Sets up output stream to socket.
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    // Writes operation to socket.
                    dataOutputStream.writeUTF(operation);
                    // Sends message from socket.
                    dataOutputStream.flush();

                    // Sets up input stream from socket.
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

                    switch (operation) {
                        case "QUIT":
                            socket.close();
                            System.out.println("Connection to server terminated.");
                            break;
                        case "LIST":
                            list(dataInputStream);
                            break;
                        case "DELF":
                            delete(reader, dataOutputStream, dataInputStream);
                            break;
                        case "UPLD":
                            upload(reader, dataOutputStream, dataInputStream);
                            break;
                        case "DWLD":
                            download(reader, dataOutputStream, dataInputStream);
                            break;
                        default:
                            System.out.println("Not a valid command.");
                            break;
                    }
                }
            } catch (SocketException | EOFException e) {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException exception) {
                    System.out.print("Exception: ");
                    exception.printStackTrace();
                }
                System.out.println("Server has disconnected..");
                System.out.println("Please reconnect using the 'CONN' operation.");
            } catch (UnknownHostException e) {
                System.out.print("UnknownHostException: ");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.print("IOException: ");
                e.printStackTrace();
            }
        }
    }

    private void list(DataInputStream inputStream) throws IOException {
        int size = inputStream.readInt();
        System.out.println(size + " files / directories:");
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            files.add(inputStream.readUTF());
        }
        System.out.println(files.toString());
    }

    private void delete(Scanner reader, DataOutputStream dataOutputStream, DataInputStream dataInputStream) throws IOException {
        System.out.print("\nEnter filename to delete: ");
        String filename = reader.nextLine();
        short length = (short) filename.length();
        dataOutputStream.writeShort(length);
        dataOutputStream.writeUTF(filename);
        boolean exists = dataInputStream.readInt() == 1;
        if (exists) {
            String check = "Are you sure you want to delete the file '" + filename + "'? (Yes/No)";
            System.out.println(check);
            String input = reader.nextLine().toLowerCase();
            while (!input.equals("yes") && !input.equals("y") && !input.equals("no") && !input.equals("n")) {
                System.out.println(check);
                input = reader.nextLine().toLowerCase();
            }
            if (input.equals("yes") || input.equals("y")) {
                dataOutputStream.writeUTF("Yes");
                dataOutputStream.flush();
                System.out.println(dataInputStream.readUTF());
            } else {
                System.out.println("Delete of file '" + filename + "' abandoned.");
                dataOutputStream.writeUTF("No");
                dataOutputStream.flush();
            }
        } else {
            System.out.println("File " + filename + " does not exist on the server.");
        }
    }

    private void upload(Scanner reader, DataOutputStream dataOutputStream, DataInputStream dataInputStream) throws IOException {
        System.out.print("\nEnter Filename: ");
        String filename = reader.nextLine();
        try {
            FileInputStream in = new FileInputStream("./src/Client/" + filename);
            short length = (short) filename.length();
            dataOutputStream.writeShort(length);
            dataOutputStream.writeUTF(filename);
            dataOutputStream.flush();
            ArrayList<Integer> bytes = new ArrayList<>();
            int b;
            while ((b = in.read()) != -1) {
                bytes.add(b);
            }
            in.close();
            boolean ready = dataInputStream.readUTF().equals("READY");
            if (ready) {
                System.out.println("Uploading...");
                int file_size = bytes.size();
                dataOutputStream.writeInt(file_size);
                dataOutputStream.flush();
                for (int send : bytes) {
                    dataOutputStream.writeInt(send);
                }
                dataOutputStream.flush();
                String response = dataInputStream.readUTF();
                System.out.println("Server says: " + response);
            } else {
                System.out.print("Error");
            }

        } catch (FileNotFoundException e) {
            System.out.print("Could not find file " + filename + "\n");
        }
    }

    private void download(Scanner reader, DataOutputStream dataOutputStream, DataInputStream dataInputStream) throws IOException {
        System.out.print("\nEnter Filename: ");
        String filename = reader.nextLine();
        short length = (short) filename.length();
        dataOutputStream.writeShort(length);
        dataOutputStream.writeUTF(filename);
        dataOutputStream.flush();
        int file_size = dataInputStream.readInt();
        if (file_size == -1) {
            System.out.println("File " + filename + " does not exist on the server.\n");
        } else {
            System.out.println("Downloading...");

            ArrayList<Integer> bytes = new ArrayList<>();
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < file_size; i++) {
                bytes.add(dataInputStream.readInt());
            }
            long endTime = System.currentTimeMillis();

            long duration = (endTime - startTime);
            float time = (float) (duration / 1000.0);
            String results = file_size + " bytes received in " + time + " seconds.";
            System.out.println(results);

            FileOutputStream out = new FileOutputStream("./src/Client/" + filename);
            for (int num : bytes) {
                out.write(num);
            }
            out.close();

        }
    }
}
