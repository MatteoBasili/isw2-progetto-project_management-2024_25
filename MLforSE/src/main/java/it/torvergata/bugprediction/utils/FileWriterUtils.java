package it.torvergata.bugprediction.utils;

import java.io.File;
import java.util.logging.Logger;

public class FileWriterUtils {

    private static final Logger LOGGER = Logger.getLogger(FileWriterUtils.class.getName());

    private FileWriterUtils() {}

    /**
     * Returns the path to the output file.
     * Create "data" directory if it does not exist.
     * If it can't create it, save the file in the current directory.
     *
     * @param fileName name of the file
     * @return full path to the file to use for writing
     */
    public static String prepareOutputDataFilePath(String fileName) {
        File outDir = new File("data");
        String outFileName;

        if (!outDir.exists()) {
            boolean created = outDir.mkdirs();
            if (!created) {
                LOGGER.warning("Could not create directory 'data'. Using current folder instead.");
                outFileName = fileName; // fallback to the current folder
            } else {
                outFileName = "data/" + fileName;
            }
        } else {
            outFileName = "data/" + fileName;
        }

        return outFileName;
    }

}
