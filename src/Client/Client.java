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
                    // Attempts connection to server, if not already connected.
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
                    // These lines are printed if the client is not connected to a server, and a command other than 'CONN' is entered.
                    System.out.println("Cannot perform operation " + operation + ".");
                    System.out.println("Please connect to a server using the 'CONN' operation.");
                } else if (operation.equals("")) {
                    // Do nothing if the operation is empty.
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
                // These exceptions are thrown if the connection is dropped by the server.
                try {
                    // Ensure the socket is closed, so that a new connection can be established.
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
        // Client receives the size of the directory listing..
        int size = inputStream.readInt();
        System.out.println(size + " files / directories:");

        // ..and goes into a loop to read directory listing.
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            files.add(inputStream.readUTF());
        }
        System.out.println(files.toString());
    }

    private void delete(Scanner reader, DataOutputStream dataOutputStream, DataInputStream dataInputStream) throws IOException {
        // Prompts user for a filename.
        System.out.print("\nEnter filename to delete: ");
        String filename = reader.nextLine();

        // Client sends the length of the file name (short int) followed by the file name (character string).
        short length = (short) filename.length();
        dataOutputStream.writeShort(length);
        dataOutputStream.writeChars(filename);

        // Client receives the confirm from the server.
        boolean exists = dataInputStream.readInt() == 1;
        if (exists) {
            // If the confirm is positive, the must confirm if they wants to delete the file.
            String check = "Are you sure you want to delete the file '" + filename + "'? (Yes/No)";
            System.out.println(check);
            String input = reader.nextLine().toLowerCase();
            while (!input.equals("yes") && !input.equals("y") && !input.equals("no") && !input.equals("n")) {
                System.out.println(check);
                input = reader.nextLine().toLowerCase();
            }
            if (input.equals("yes") || input.equals("y")) {
                // If the user's confirm is "Yes"..
                dataOutputStream.writeUTF("Yes");
                dataOutputStream.flush();

                // ..the client waits for the server to send the confirm of file deletion.
                System.out.println(dataInputStream.readUTF());
            } else {
                // If the user's confirm is "No", the client prints out "Delete abandoned by the user!"..
                System.out.println("Delete of file '" + filename + "' abandoned.");
                dataOutputStream.writeUTF("No");
                dataOutputStream.flush();

                // ..and returns to "prompt user for operation" state.
            }
        } else {
            // If the confirm is negative, print out “The file does not exist on server”..
            System.out.println("File " + filename + " does not exist on the server.");

            // ..and return to the "prompt user for operation" state.
        }
    }

    private void upload(Scanner reader, DataOutputStream dataOutputStream, DataInputStream dataInputStream) throws IOException {
        // Prompts user for a filename.
        System.out.print("\nEnter Filename: ");
        String filename = reader.nextLine();

        try {
            // This throws and exception if the file does not exist.
            FileInputStream in = new FileInputStream("./src/Client/" + filename);

            // Client sends the length of the file name which will be sent (short int)..
            short length = (short) filename.length();
            dataOutputStream.writeShort(length);

            // ..followed by the file_name (character string).
            dataOutputStream.writeChars(filename);
            dataOutputStream.flush();

            // Store each byte from the file in an array list.
            ArrayList<Integer> bytes = new ArrayList<>();
            int b;
            while ((b = in.read()) != -1) {
                bytes.add(b);
            }
            in.close();

            // When the server acknowledges it is ready..
            boolean ready = dataInputStream.readUTF().equals("READY");
            if (ready) {
                System.out.println("Uploading...");

                // .. the client sends the size of the file.
                int file_size = bytes.size();
                dataOutputStream.writeInt(file_size);
                dataOutputStream.flush();

                // Client sends file to server.
                for (int send : bytes) {
                    dataOutputStream.writeInt(send);
                }
                dataOutputStream.flush();

                // Processing information is used by the client to inform the user that the transfer was successful.
                String response = dataInputStream.readUTF();
                System.out.println("Server says: " + response);

                // Client returns to the "prompt user for operation" state.
            } else {
                System.out.print("Error");
            }

        } catch (FileNotFoundException e) {
            // This catches the exception thrown if the file does not exist.
            System.out.print("Could not find file " + filename + "\n");
        }
    }

    private void download(Scanner reader, DataOutputStream dataOutputStream, DataInputStream dataInputStream) throws IOException {
        // Prompts user for a filename.
        System.out.print("\nEnter Filename: ");
        String filename = reader.nextLine();

        // Client sends the length of the file name (short int) followed by the file name (character string).
        short length = (short) filename.length();
        dataOutputStream.writeShort(length);
        dataOutputStream.writeChars(filename);
        dataOutputStream.flush();

        // Client receives 32-bit file length from server.
        int file_size = dataInputStream.readInt();

        if (file_size == -1) {
            // If the value is negative 1, the user should be informed that the file does not exist on the server.
            System.out.println("File " + filename + " does not exist on the server.\n");

            // Client returns to "prompt user for operation" state.
        } else {
            System.out.println("Downloading...");

            // Client reads "file size" bytes from server, and times the download.
            ArrayList<Integer> bytes = new ArrayList<>();
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < file_size; i++) {
                bytes.add(dataInputStream.readInt());
            }
            long endTime = System.currentTimeMillis();

            // Once the transfer completes successfully, display the transfer processing information.
            long duration = (endTime - startTime);
            float time = (float) (duration / 1000.0);
            String results = file_size + " bytes received in " + time + " seconds.";
            System.out.println(results);

            // The client saves the file to disk as "file name".
            FileOutputStream out = new FileOutputStream("./src/Client/" + filename);
            for (int num : bytes) {
                out.write(num);
            }
            out.close();

        }
    }
}
