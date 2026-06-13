package Utilities;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class AppLog {
    private static final Path LOG_DIR = Path.of("logs");
    private static final Path APP_LOG = LOG_DIR.resolve("app.log");
    private static boolean initialized = false;

    private AppLog() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            Files.createDirectories(LOG_DIR);
            PrintStream logFileStream = new PrintStream(
                    Files.newOutputStream(
                            APP_LOG,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND
                    ),
                    true
            );

            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            System.setOut(new PrintStream(new TeeOutputStream(originalOut, logFileStream), true));
            System.setErr(new PrintStream(new TeeOutputStream(originalErr, logFileStream), true));
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                System.err.println("[UncaughtException] Thread " + thread.getName() + " crashed.");
                throwable.printStackTrace(System.err);
            });
            initialized = true;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize logging", e);
        }
    }

    private static final class TeeOutputStream extends OutputStream {
        private final OutputStream first;
        private final OutputStream second;

        private TeeOutputStream(OutputStream first, OutputStream second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public void write(int b) throws IOException {
            first.write(b);
            second.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            first.write(b, off, len);
            second.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            first.flush();
            second.flush();
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }
}
