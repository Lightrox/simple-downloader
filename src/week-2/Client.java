import java.net.*;
import java.io.*;
import java.util.*;
import java.security.*;

public class Client {
    public static void main(String[] args) throws Exception {
        System.out.println("Connecting to server...");

        Socket socket = new Socket("localhost", 6881);
        OutputStream out = socket.getOutputStream();
        DataOutputStream dataOut = new DataOutputStream(out);

        FileInputStream fileIn = new FileInputStream("c:\\Users\\Harsh\\Documents\\bit-torrent-client\\test-files\\sample.pdf");

        List<byte[]> pieces = new ArrayList<>();
        byte[] buffer = new byte[512 * 1024];
        int bytesRead;

        while ((bytesRead = fileIn.read(buffer)) != -1) {
            byte[] piece = new byte[bytesRead];
            System.arraycopy(buffer, 0, piece, 0, bytesRead);
            pieces.add(piece);
        }

        System.out.println("Total pieces: " + pieces.size());

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        dataOut.writeInt(pieces.size());

        for (int i = 0; i < pieces.size(); i++) {
            byte[] piece = pieces.get(i);
            byte[] hash = digest.digest(piece);

            dataOut.writeInt(i);
            dataOut.writeInt(piece.length);
            dataOut.write(hash);
            dataOut.write(piece);

            System.out.println("Sent piece " + i + "/" + (pieces.size() - 1));
        }

        dataOut.writeInt(-1);
        System.out.println("All pieces sent!");

        fileIn.close();
        socket.close();
    }
}