package tracker;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Tracker {

    // stores all announced peers: infoHash -> list of peers
    private static Map<String, List<PeerInfo>> swarm = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/announce", new AnnounceHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Tracker running on port " + port);
        System.out.println("Announce URL: http://localhost:8080/announce");
    }

    static class AnnounceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // parse query parameters
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);

            String infoHash = params.getOrDefault("info_hash", "");
            String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
            int port = Integer.parseInt(params.getOrDefault("port", "6881"));

            // register this peer in the swarm
            swarm.putIfAbsent(infoHash, new CopyOnWriteArrayList<>());
            List<PeerInfo> peers = swarm.get(infoHash);

            // add peer if not already in list
            PeerInfo newPeer = new PeerInfo(ip, port);
            boolean exists = peers.stream()
                .anyMatch(p -> p.getIp().equals(ip) && p.getPort() == port);
            if (!exists) {
                peers.add(newPeer);
                System.out.println("New peer announced: " + newPeer 
                    + " for hash: " + infoHash.substring(0, 8) + "...");
            }

            System.out.println("Swarm size for this torrent: " + peers.size() + " peers");

            // build bencoded response with peer list
            // exclude the requesting peer from the list
            List<PeerInfo> otherPeers = new ArrayList<>();
            for (PeerInfo p : peers) {
                if (!(p.getIp().equals(ip) && p.getPort() == port)) {
                    otherPeers.add(p);
                }
            }

            byte[] response = buildResponse(otherPeers);

            exchange.sendResponseHeaders(200, response.length);
            OutputStream out = exchange.getResponseBody();
            out.write(response);
            out.close();
        }

        private byte[] buildResponse(List<PeerInfo> peers) throws IOException {
            // build compact peer bytes — 6 bytes per peer
            ByteArrayOutputStream peerBytes = new ByteArrayOutputStream();
            for (PeerInfo peer : peers) {
                // convert IP to 4 bytes
                String[] parts = peer.getIp().split("\\.");
                for (String part : parts) {
                    peerBytes.write(Integer.parseInt(part));
                }
                // convert port to 2 bytes
                int port = peer.getPort();
                peerBytes.write((port >> 8) & 0xFF);
                peerBytes.write(port & 0xFF);
            }

            byte[] peerData = peerBytes.toByteArray();

            // build bencoded response
            // d8:intervali1800e5:peersX:...e
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            String header = "d8:intervali1800e5:peers" + peerData.length + ":";
            response.write(header.getBytes("UTF-8"));
            response.write(peerData);
            response.write("e".getBytes("UTF-8"));

            return response.toByteArray();
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> params = new HashMap<>();
            if (query == null) return params;
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2) {
                    try {
                        params.put(
                            URLDecoder.decode(pair[0], "UTF-8"),
                            URLDecoder.decode(pair[1], "UTF-8")
                        );
                    } catch (Exception e) {
                        // skip malformed params
                    }
                }
            }
            return params;
        }
    }
}