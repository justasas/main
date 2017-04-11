package main;

import main.parsers.OpenSubtitlesParser;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {

	public final static String UNWANTED_SYMBOLS_IN_FOLDER_NAME_REGEX = "[^a-zA-Z']+";

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


	public String[] filterFolderByExtension(String folder, String extension) {
		File dir = new File(folder);
		if (dir.isDirectory() == false) {
			System.out.println("Directory does not exists : " + folder);
			return null;
		}

		GenericExtFilter filter = new GenericExtFilter(extension);
		String[] list = dir.list(filter);
		return list;
	}

	// inner class, generic extension filter
	public class GenericExtFilter implements FilenameFilter {

		private String ext;

		public GenericExtFilter(String ext) {
			this.ext = ext;
		}

		@Override
		public boolean accept(File dir, String name) {
			return (name.endsWith(ext));
		}
	}

}
