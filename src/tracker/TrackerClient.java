package tracker;

import java.io.*;
import java.net.*;
import java.util.*;

public class TrackerClient {

    private String announceUrl;
    private String infoHash;
    private String peerId;
    private int port;

    public TrackerClient(String announceUrl, String infoHash, int port) {
        this.announceUrl = announceUrl;
        this.infoHash = infoHash;
        this.port = port;
        this.peerId = generatePeerId();
    }

    private String generatePeerId() {
        return "-BT0001-" + String.format("%012d", new Random().nextInt(1000000000));
    }

    public List<PeerInfo> getPeers() throws Exception {
        String urlString = announceUrl
                + "?info_hash=" + infoHash
                + "&peer_id=" + URLEncoder.encode(peerId, "UTF-8")
                + "&port=" + port
                + "&uploaded=0"
                + "&downloaded=0"
                + "&left=999999999"
                + "&compact=1"
                + "&numwant=50"
                + "&event=started";

        System.out.println("Contacting tracker: " + announceUrl);

        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        System.out.println("Tracker response code: " + responseCode);

        InputStream in = connection.getInputStream();
        byte[] response = in.readAllBytes();
        in.close();

        System.out.println("Tracker responded with " + response.length + " bytes");

        // debug — print raw response as text
        System.out.println("Raw response: " + new String(response));

        return parsePeers(response);
    }

    private List<PeerInfo> parsePeers(byte[] response) {
        List<PeerInfo> peers = new ArrayList<>();

        String responseStr = new String(response, 0, response.length);

        int peersIndex = responseStr.indexOf("5:peers");
        if (peersIndex == -1) {
            System.out.println("No peers found in response");
            return peers;
        }

        int i = peersIndex + 7;

        StringBuilder lengthStr = new StringBuilder();
        while (i < response.length && response[i] != ':') {
            lengthStr.append((char) response[i]);
            i++;
        }
        i++;

        int peerDataLength;
        try {
            peerDataLength = Integer.parseInt(lengthStr.toString());
        } catch (NumberFormatException e) {
            System.out.println("Could not parse peer data length");
            return peers;
        }

        System.out.println("Peer data length: " + peerDataLength + " bytes = "
                + (peerDataLength / 6) + " peers");

        for (int j = 0; j < peerDataLength - 5; j += 6) {
            if (i + j + 5 >= response.length) break;

            int b1 = response[i + j] & 0xFF;
            int b2 = response[i + j + 1] & 0xFF;
            int b3 = response[i + j + 2] & 0xFF;
            int b4 = response[i + j + 3] & 0xFF;
            int portHigh = response[i + j + 4] & 0xFF;
            int portLow = response[i + j + 5] & 0xFF;

            String ip = b1 + "." + b2 + "." + b3 + "." + b4;
            int peerPort = (portHigh << 8) | portLow;

            if (peerPort > 0 && peerPort < 65535) {
                peers.add(new PeerInfo(ip, peerPort));
            }
        }

        return peers;
    }
}