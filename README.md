# Flash-Get

Flash-Get is a **multi-threaded Java command-line file downloader** designed to accelerate large file downloads by splitting files into byte-range chunks and downloading them concurrently.

This project focuses on **concurrency, networking, and low-level file I/O**, demonstrating how real-world download managers work under the hood.

---

## Key Features

- Java CLI application (no GUI, no external libraries)
- Concurrent downloads using `ExecutorService`
- HTTP byte-range requests (`Range` headers)
- Parallel disk writes using `RandomAccessFile`
- Pre-allocation of output file to avoid fragmentation
- Fallback handling for servers that do not return `Content-Length`
- Safe resource handling with try-with-resources
- Compatible with Java 11+

---

## How It Works

1. The program accepts a file URL and number of threads via the command line.
2. It determines the file size using:
   - An HTTP `HEAD` request, or
   - A fallback `GET` request with `Range: bytes=0-0` and `Content-Range` parsing.
3. The file is split into non-overlapping byte ranges based on the thread count.
4. Each byte range is downloaded concurrently by a worker thread.
5. Each worker writes directly to its assigned position in the output file using `RandomAccessFile`.
6. The main thread waits for all workers to complete before exiting.

---

## Why Multithreading Improves Download Performance

Single-threaded downloads often underutilize available network bandwidth.  
By downloading multiple byte ranges in parallel, Flash-Get can:

- Better utilize network throughput
- Reduce total download time
- Mimic real-world systems such as multipart downloads used by cloud storage providers

---

## How to Compile

```bash
javac FlashGet.java DownloadWorker.java
