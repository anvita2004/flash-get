import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Downloads a specific byte range of a file using HTTP Range requests.
 */
public class DownloadWorker implements Runnable {

    private static final int BUFFER_SIZE = 8192;

    private final URL fileUrl;
    private final long startByte;
    private final long endByte;
    private final File outputFile;

    public DownloadWorker(URL fileUrl, long startByte, long endByte, File outputFile) {
        this.fileUrl = fileUrl;
        this.startByte = startByte;
        this.endByte = endByte;
        this.outputFile = outputFile;
    }

    @Override
    public void run() {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) fileUrl.openConnection();
            connection.setRequestProperty(
                    "Range",
                    "bytes=" + startByte + "-" + endByte
            );

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_PARTIAL
                    && responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Server does not support byte ranges");
            }

            try (InputStream inputStream =
                         new BufferedInputStream(connection.getInputStream());
                 RandomAccessFile raf =
                         new RandomAccessFile(outputFile, "rw")) {

                raf.seek(startByte);

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    raf.write(buffer, 0, bytesRead);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed downloading bytes " + startByte + " - " + endByte,
                    e
            );
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
