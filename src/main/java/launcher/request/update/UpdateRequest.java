package launcher.request.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SignatureException;
import java.time.Duration;
import java.util.Objects;

import launcher.Launcher.Config;
import launcher.LauncherAPI;
import launcher.hasher.FileNameMatcher;
import launcher.hasher.HashedDir;
import launcher.helper.IOHelper;
import launcher.request.Request;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;
import launcher.serialize.signed.SignedObjectHolder;
import launcher.request.update.UpdateRequest.State.Callback;

public final class UpdateRequest extends Request<SignedObjectHolder<HashedDir>> {
    @LauncherAPI public static final int MAX_QUEUE_SIZE = 128;

    // Instance
    private final String dirName;
    private final Path dir;
    private final FileNameMatcher matcher;
    private final boolean digest;

    @LauncherAPI
    public UpdateRequest(Config config, String dirName, Path dir, FileNameMatcher matcher, boolean digest) {
        super(config);
        this.dirName = IOHelper.verifyFileName(dirName);
        this.dir = Objects.requireNonNull(dir, "dir");
        this.matcher = matcher;
        this.digest = digest;
    }

    @LauncherAPI
    public UpdateRequest(String dirName, Path dir, FileNameMatcher matcher, boolean digest) {
        this(null, dirName, dir, matcher, digest);
    }

    @Override
    public Type getType() {
        return Type.UPDATE;
    }

    @Override
    public SignedObjectHolder<HashedDir> request() throws Throwable {
        Files.createDirectories(dir);
        return super.request();
    }

    @LauncherAPI
    public void setStateCallback(Callback callback) {}

    @Override
    protected SignedObjectHolder<HashedDir> requestDo(HInput input, HOutput output) throws IOException, SignatureException {
        output.writeString(this.dirName, 255);
        output.flush();
        this.readError(input);
        final SignedObjectHolder<HashedDir> remoteHDirHolder = new SignedObjectHolder<HashedDir>(input, this.config.publicKey, HashedDir::new);
        return remoteHDirHolder;
    }

    public static final class State {
        @LauncherAPI public final long fileDownloaded;
        @LauncherAPI public final long fileSize;
        @LauncherAPI public final long totalDownloaded;
        @LauncherAPI public final long totalSize;
        @LauncherAPI public final String filePath;
        @LauncherAPI public final Duration duration;

        public State(String filePath, long fileDownloaded, long fileSize, long totalDownloaded, long totalSize, Duration duration) {
            this.filePath = filePath;
            this.fileDownloaded = fileDownloaded;
            this.fileSize = fileSize;
            this.totalDownloaded = totalDownloaded;
            this.totalSize = totalSize;

            // Also store time of creation
            this.duration = duration;
        }

        @LauncherAPI
        public double getBps() {
            long seconds = duration.getSeconds();
            if (seconds == 0) {
                return -1.0D; // Otherwise will throw /0 exception
            }
            return totalDownloaded / (double) seconds;
        }

        @LauncherAPI
        public Duration getEstimatedTime() {
            double bps = getBps();
            if (bps <= 0.0D) {
                return null; // Otherwise will throw /0 exception
            }
            return Duration.ofSeconds((long) (getTotalRemaining() / bps));
        }

        @LauncherAPI
        public double getFileDownloadedKiB() {
            return fileDownloaded / 1024.0D;
        }

        @LauncherAPI
        public double getFileDownloadedMiB() {
            return getFileDownloadedKiB() / 1024.0D;
        }

        @LauncherAPI
        public double getFileDownloadedPart() {
            if (fileSize == 0) {
                return 0.0D;
            }
            return (double) fileDownloaded / fileSize;
        }

        @LauncherAPI
        public long getFileRemaining() {
            return fileSize - fileDownloaded;
        }

        @LauncherAPI
        public double getFileRemainingKiB() {
            return getFileRemaining() / 1024.0D;
        }

        @LauncherAPI
        public double getFileRemainingMiB() {
            return getFileRemainingKiB() / 1024.0D;
        }

        @LauncherAPI
        public double getFileSizeKiB() {
            return fileSize / 1024.0D;
        }

        @LauncherAPI
        public double getFileSizeMiB() {
            return getFileSizeKiB() / 1024.0D;
        }

        @LauncherAPI
        public double getTotalDownloadedKiB() {
            return totalDownloaded / 1024.0D;
        }

        @LauncherAPI
        public double getTotalDownloadedMiB() {
            return getTotalDownloadedKiB() / 1024.0D;
        }

        @LauncherAPI
        public double getTotalDownloadedPart() {
            if (totalSize == 0) {
                return 0.0D;
            }
            return (double) totalDownloaded / totalSize;
        }

        @LauncherAPI
        public long getTotalRemaining() {
            return totalSize - totalDownloaded;
        }

        @LauncherAPI
        public double getTotalRemainingKiB() {
            return getTotalRemaining() / 1024.0D;
        }

        @LauncherAPI
        public double getTotalRemainingMiB() {
            return getTotalRemainingKiB() / 1024.0D;
        }

        @LauncherAPI
        public double getTotalSizeKiB() {
            return totalSize / 1024.0D;
        }

        @LauncherAPI
        public double getTotalSizeMiB() {
            return getTotalSizeKiB() / 1024.0D;
        }

        @FunctionalInterface
        public interface Callback {
            void call(State state);
        }
    }
}
