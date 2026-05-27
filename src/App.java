import tracker.TrackerClient;
import tracker.PeerInfo;
import java.util.List;

public class App {
    public static void main(String[] args) throws Exception {

        // Ubuntu 22.04.3 LTS desktop amd64
        // announce from the actual .torrent file
        String infoHash = "%66%9E%47%79%D0%3D%1D%CE%B3%5E%FC%C7%05%DD%85%26%45%D9%37%51";

        // using HTTP tracker that supports any torrent
        String announceUrl = "http://tracker.opentrackr.org:1337/announce";

        int port = 6881;

        TrackerClient tracker = new TrackerClient(announceUrl, infoHash, port);

        try {
            List<PeerInfo> peers = tracker.getPeers();
            System.out.println("Found " + peers.size() + " peers:");
            for (PeerInfo peer : peers) {
                System.out.println("  " + peer);
            }
        } catch (Exception e) {
            System.out.println("Tracker failed: " + e.getMessage());
            System.out.println("Try again in a few minutes or use a different tracker.");
        }
    }
}