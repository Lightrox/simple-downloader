import tracker.TrackerClient;
import tracker.PeerInfo;
import peer.PeerConnection;
import java.util.List;
import java.util.ArrayList;
import java.net.*;
import java.io.*;

public class App {
    public static void main(String[] args) throws Exception {

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 6881;
        System.out.println("Starting client on port: " + port);

        String infoHashEncoded = "%45%5A%D4%8C%E8%CD%08%15%6D%F3%6F%31%5B%42%B1%AC%3A%71%82%45";
        byte[] infoHash = urlDecodeToBytes(infoHashEncoded);
        String peerIdStr = "-BT0001-" + String.format("%012d",
            new java.util.Random().nextInt(1000000000));
        byte[] peerId = peerIdStr.getBytes("UTF-8");

        TrackerClient tracker = new TrackerClient(
            "http://localhost:8080/announce", infoHashEncoded, port);

        // if seeder — announce and listen for incoming connections
        if (port == 6882) {
            System.out.println("Running as seeder — announcing and listening...");

            try {
                tracker.getPeers(); // announce to tracker, ignore response
                System.out.println("Announced to tracker successfully");
            } catch (Exception e) {
                System.out.println("Tracker announce note: " + e.getMessage());
                // continue anyway — we still want to listen
            }

            ServerSocket serverSocket = new ServerSocket(6882);
            System.out.println("Seeder listening on port 6882...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Incoming connection from: "
                        + socket.getInetAddress().getHostAddress());
                new Thread(() -> {
                    try {
                        handleIncomingHandshake(socket, infoHash, peerId);
                    } catch (Exception e) {
                        System.out.println("Handshake error: " + e.getMessage());
                    }
                }).start();
            }
        }

        // if leecher — find peers and connect
        System.out.println("Running as leecher — looking for peers...");
        List<PeerInfo> peers = new ArrayList<>();

        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                peers = tracker.getPeers();
            } catch (Exception e) {
                System.out.println("Tracker error: " + e.getMessage());
            }
            if (!peers.isEmpty()) {
                System.out.println("Got " + peers.size() + " peers on attempt " + attempt);
                break;
            }
            System.out.println("No peers yet, retrying in 2 seconds... (" + attempt + "/5)");
            Thread.sleep(2000);
        }

        if (peers.isEmpty()) {
            System.out.println("No peers found after 5 attempts");
            return;
        }

        for (PeerInfo peer : peers) {
            System.out.println("Trying peer: " + peer);
            PeerConnection connection = new PeerConnection(peer, infoHash, peerId, port);
            boolean success = connection.connect();

            if (success) {
                System.out.println("Successfully handshaked with: " + peer);
                connection.close();
                break;
            } else {
                System.out.println("Peer " + peer + " failed, trying next...");
            }
        }
    }

    private static void handleIncomingHandshake(Socket socket,
            byte[] infoHash, byte[] peerId) throws Exception {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // read incoming handshake
        int pstrLen = in.readByte() & 0xFF;
        byte[] pstr = new byte[pstrLen];
        in.readFully(pstr);
        byte[] reserved = new byte[8];
        in.readFully(reserved);
        byte[] receivedInfoHash = new byte[20];
        in.readFully(receivedInfoHash);
        byte[] receivedPeerId = new byte[20];
        in.readFully(receivedPeerId);

        System.out.println("Received handshake from peer!");

        // send handshake back
        ByteArrayOutputStream handshake = new ByteArrayOutputStream();
        handshake.write(19);
        handshake.write("BitTorrent protocol".getBytes("UTF-8"));
        handshake.write(new byte[8]);
        handshake.write(infoHash);
        handshake.write(peerId);
        out.write(handshake.toByteArray());
        out.flush();

        System.out.println("Handshake completed with leecher!");
        socket.close();
    }

    private static byte[] urlDecodeToBytes(String encoded) throws Exception {
        String[] parts = encoded.split("%");
        byte[] bytes = new byte[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            bytes[i - 1] = (byte) Integer.parseInt(parts[i], 16);
        }
        return bytes;
    }
}