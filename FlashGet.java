import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Main class for Flash-Get downloader.
 */
public class FlashGet {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Usage:");
            System.out.println("java -jar flashget.jar <FILE_URL> <THREAD_COUNT>");
            return;
        }

        try {
            URL fileUrl = new URL(args[0]);
            int threadCount = Integer.parseInt(args[1]);

            long fileSize = getFileSize(fileUrl);
            if (fileSize <= 0) {
                throw new RuntimeException("Cannot determine file size.");
            }

            // Handle very small files safely
            threadCount = (int) Math.min(threadCount, fileSize);

            String fileName = getFileName(fileUrl);
            File outputFile = new File(fileName);

            // Pre-create output file with full size
            try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
                raf.setLength(fileSize);
            }

            ExecutorService executor =
                    Executors.newFixedThreadPool(threadCount);

            List<Future<?>> futures = new ArrayList<>();

            long chunkSize = fileSize / threadCount;
            long start = 0;

            for (int i = 0; i < threadCount; i++) {
                long end = (i == threadCount - 1)
                        ? fileSize - 1
                        : start + chunkSize - 1;

                DownloadWorker worker =
                        new DownloadWorker(fileUrl, start, end, outputFile);

                futures.add(executor.submit(worker));
                start = end + 1;
            }

            // Wait for all threads to finish
            for (Future<?> future : futures) {
                future.get();
            }

            executor.shutdown();
            System.out.println("Download completed successfully.");

        } catch (Exception e) {
            System.out.println("Download failed:");
            e.printStackTrace();
        }
    }

    /**
     * Gets file size using HEAD request.
     * Falls back to Content-Range if needed.
     */
    private static long getFileSize(URL url) throws Exception {

        // Try HEAD request
        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        long size = connection.getContentLengthLong();
        connection.disconnect();

        if (size > 0) {
            return size;
        }

        // Fallback: GET first byte and read Content-Range
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Range", "bytes=0-0");
        connection.connect();

        String contentRange = connection.getHeaderField("Content-Range");
        connection.disconnect();

        if (contentRange == null) {
            throw new RuntimeException("Server does not provide file size.");
        }

        // Example: bytes 0-0/104857600
        return Long.parseLong(
                contentRange.substring(contentRange.lastIndexOf('/') + 1)
        );
    }

    private static String getFileName(URL url) {
        String path = url.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1);
        return name.isEmpty() ? "downloaded.file" : name;
    }
}
