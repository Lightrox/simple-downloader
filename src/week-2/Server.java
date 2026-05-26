import java.net.*;
import java.io.*;
import java.security.*;
import java.util.Arrays;

public class Server {
    public static void main(String[] args) throws Exception {
        System.out.println("Server started, waiting for connection...");

        ServerSocket serverSocket = new ServerSocket(6881);
        Socket socket = serverSocket.accept();
        System.out.println("Client connected! Receiving pieces...");

        InputStream in = socket.getInputStream();
        DataInputStream dataIn = new DataInputStream(in);

        File outputDir = new File("c:\\Users\\Harsh\\Documents\\bit-torrent-client\\received");
        outputDir.mkdirs();
        RandomAccessFile raf = new RandomAccessFile("c:\\Users\\Harsh\\Documents\\bit-torrent-client\\received\\output.pdf", "rw");

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int PIECE_SIZE = 512 * 1024;
        int totalPieces = dataIn.readInt();
        int verified = 0;
        int failed = 0;
        long startTime = System.currentTimeMillis();
        long totalBytesReceived = 0;

        while (true) {
            int pieceIndex = dataIn.readInt();

            if (pieceIndex == -1) {
                System.out.println("\nTransfer complete!");
                break;
            }

            int pieceSize = dataIn.readInt();
            byte[] expectedHash = new byte[32];
            dataIn.readFully(expectedHash);
            byte[] pieceData = new byte[pieceSize];
            dataIn.readFully(pieceData);

            byte[] actualHash = digest.digest(pieceData);

            if (Arrays.equals(expectedHash, actualHash)) {
                long offset = (long) pieceIndex * PIECE_SIZE;
                raf.seek(offset);
                raf.write(pieceData);
                verified++;
                totalBytesReceived += pieceSize;
            } else {
                failed++;
                System.out.println("\nPiece " + pieceIndex + " FAILED verification ✗");
            }

            long elapsed = System.currentTimeMillis() - startTime;
            double seconds = elapsed / 1000.0;
            double speedMBps = seconds > 0 ? (totalBytesReceived / (1024.0 * 1024.0)) / seconds : 0;
            int progress = (int) ((verified + failed) * 100.0 / totalPieces);
            int filled = progress / 5;
            String bar = "█".repeat(filled) + "░".repeat(20 - filled);

            System.out.print("\r[" + bar + "] " + progress + "% | "
                    + verified + "/" + totalPieces + " pieces | "
                    + String.format("%.2f", speedMBps) + " MB/s");
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("Verified: " + verified + " Failed: " + failed);
        System.out.println("Total time: " + elapsed / 1000.0 + "s");
        System.out.println("Saved to: received/output.pdf");

        raf.close();
        socket.close();
        serverSocket.close();
    }
}