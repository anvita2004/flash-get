# Flash-Get: Multi-Threaded Java Downloader

![Java](https://img.shields.io/badge/Java-11%2B-orange) ![Concurrency](https://img.shields.io/badge/Concurrency-Multi--Threaded-blue) ![License](https://img.shields.io/badge/License-MIT-green)

**Flash-Get** is a high-throughput, command-line file downloader designed to accelerate large file transfers. It mimics the architecture of enterprise download managers (like IDM) by splitting files into logical byte-range chunks and downloading them concurrently using `ExecutorService`.

This project serves as a Proof of Concept for **network optimization**, **concurrency control**, and **low-level disk I/O** in Java without relying on external libraries.

## Key Features

* **Concurrent Architecture:** Utilizes a custom `FixedThreadPool` to manage worker threads, maximizing bandwidth utilization.
* **Resilient Network I/O:** Implements HTTP `Range` headers to fetch partial byte streams.
* **Smart Size Detection:** Automatically falls back to parsing `Content-Range` headers if the server restricts standard `HEAD` requests.
* **Non-Blocking Disk Writes:** Uses `RandomAccessFile` to write to specific disk sectors concurrently, preventing file locking issues and reducing fragmentation.
* **Memory Efficiency:** Streams data directly to disk using buffered I/O, ensuring low memory footprint even for gigabyte-scale downloads.

## How It Works (System Design)

1.  **Initialization:** The program accepts a target URL and a thread count (concurrency limit).
2.  **Metadata Fetching:** It queries the server (via `HEAD` or partial `GET`) to determine the total `Content-Length`.
3.  **Logical Sharding:** The file size is divided by the thread count to calculate `startByte` and `endByte` for each worker.
4.  **Parallel Execution:**
    * Worker threads initiate independent HTTP connections.
    * Data is streamed and written to the pre-allocated file on disk using `seek()` operations.
5.  **Synchronization:** The main thread uses `Future.get()` to act as a barrier, ensuring all chunks are successfully persisted before closing the file.

## Build & Usage

### Prerequisites
* Java Development Kit (JDK) 11 or higher.

### Compilation
```bash
javac FlashGet.java DownloadWorker.java
jar cfe flashget.jar FlashGet *.class
