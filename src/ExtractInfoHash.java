import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.net.*;

public class ExtractInfoHash {
    public static void main(String[] args) throws Exception {
        // read the .torrent file
        File torrentFile = new File(
            "c:\\Users\\Harsh\\Documents\\bit-torrent-client\\torrents\\forza.torrent"
        );
        
        byte[] torrentBytes = Files.readAllBytes(torrentFile.toPath());
        
        // find the info dictionary in the bencode
        // it starts with "4:info" and ends at the matching 'e'
        String torrentStr = new String(torrentBytes, "ISO-8859-1");
        int infoStart = torrentStr.indexOf("4:info") + 6;
        
        // find the end of the info dictionary
        // we need to count nested d...e pairs
        int depth = 0;
        int infoEnd = infoStart;
        for (int i = infoStart; i < torrentBytes.length; i++) {
            char c = (char) torrentBytes[i];
            if (c == 'd' || c == 'l') depth++;
            else if (c == 'e') {
                depth--;
                if (depth == 0) {
                    infoEnd = i + 1;
                    break;
                }
            }
        }
        
        // extract info bytes and hash them
        byte[] infoBytes = new byte[infoEnd - infoStart];
        System.arraycopy(torrentBytes, infoStart, infoBytes, 0, infoBytes.length);
        
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(infoBytes);
        
        // print as URL encoded string
        StringBuilder urlEncoded = new StringBuilder();
        for (byte b : hash) {
            urlEncoded.append(String.format("%%%02X", b & 0xFF));
        }
        
        // print as hex for reference
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b & 0xFF));
        }
        
        System.out.println("Info hash (URL encoded): " + urlEncoded);
        System.out.println("Info hash (hex): " + hex);
        
        // also extract announce URL
        int announceStart = torrentStr.indexOf("8:announce") + 10;
        int colonIndex = torrentStr.indexOf(":", announceStart);
        int announceLength = Integer.parseInt(torrentStr.substring(announceStart, colonIndex));
        String announceUrl = torrentStr.substring(colonIndex + 1, colonIndex + 1 + announceLength);
        System.out.println("Announce URL: " + announceUrl);
    }
}