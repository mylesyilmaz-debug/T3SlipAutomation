package ca.empire.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public class FileOperations {
    private static final Logger logger = LogManager.getLogger(FileOperations.class);

    /**
     * Deletes all files / subdirectories within a directory, and then deletes the directory itself. If symbolic links
     * are found, then they will be left alone and will need to be manually actioned.
     * @param file - The directory that will attempt to be deleted.
     */
    public static void deleteDir(File file) {
        logger.traceEntry();
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (Files.isSymbolicLink(f.toPath())) {
                    logger.warn(
                            "{} is a symbolic link, please action manually.",
                            file.getAbsolutePath());
                    continue;
                }
                deleteDir(f);
            }
        }

        logger.info("Attempting to delete {}", file.getAbsolutePath());
        if (file.delete()) {
            logger.info("Deleted {}", file.getAbsolutePath());
        } else {
            logger.warn("Unable to delete {}", file.getAbsolutePath());
        }
        logger.traceExit();
    }

    /**
     * Generates a temporary download directory that can be used for a single driver instance.
     *
     * @param uuid - The UUID that will be used as the name of the directory.
     * @return - The generated directory as a File.
     * @throws IOException - If we are unable to generate the directory.
     */
    public static File generateTempDownloadDirectory(UUID uuid) throws IOException {
        logger.traceEntry(() -> uuid);
        String separator = File.separator;
        String basePath =
                System.getProperty("user.dir")
                        + separator
                        + ".temp"
                        + separator
                        + "downloads"
                        + separator;

        File directory = new File(basePath + uuid.toString());

        if (!directory.mkdirs()) {
            throw new IOException(
                    "Unable to create download directory " + directory.getAbsolutePath());
        }

        logger.traceExit(directory);
        return directory;
    }
}
