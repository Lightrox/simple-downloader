package download;

import java.security.MessageDigest;
import java.util.Arrays;

public class PieceVerifier {

    public static boolean verify(byte[] pieceData, byte[] expectedHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] actualHash = digest.digest(pieceData);

            boolean valid = Arrays.equals(actualHash, expectedHash);

            if (!valid) {
                System.out.println("Hash mismatch!");
                System.out.println("Expected: " + bytesToHex(expectedHash));
                System.out.println("Actual:   " + bytesToHex(actualHash));
            }

            return valid;

        } catch (Exception e) {
            System.out.println("Verification error: " + e.getMessage());
            return false;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
