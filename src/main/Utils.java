package main;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {

	public final static String UNWANTED_SYMBOLS_IN_FOLDER_NAME_REGEX = "[^a-zA-Z0-9']";

	public static void createDirectoriesToFile(String filePath) {
		try {
			Path pathToFile = Paths.get(filePath);
			Files.createDirectories(pathToFile.getParent());
		} catch (Exception e) { }
	}

	public static void createFolderAndMissingDirectoriesToIt(String filePath) {
		try {
			Path pathToFile = Paths.get(filePath);
			Files.createDirectories(pathToFile);
		} catch (Exception e) { }
	}

	public static boolean directoryHasFiles(String subtitlesDestinationFolder) {
		return new File(subtitlesDestinationFolder).list().length == 0;
	}
}
