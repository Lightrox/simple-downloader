package download;

import java.io.*;

public class FileWriter {

    private final RandomAccessFile raf;
    private final int pieceSize;

    public FileWriter(String outputPath, long fileSize, int pieceSize) throws Exception {
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();

        this.raf = new RandomAccessFile(outputFile, "rw");
        this.pieceSize = pieceSize;

        raf.setLength(fileSize);
        System.out.println("Output file created: " + outputPath);
        System.out.println("File size: " + fileSize + " bytes");
    }

    public synchronized void writePiece(int pieceIndex, byte[] pieceData) throws Exception {
        long offset = (long) pieceIndex * pieceSize;
        raf.seek(offset);
        raf.write(pieceData);
    }

    public void close() throws Exception {
        raf.close();
        System.out.println("File closed successfully");
    }
}
