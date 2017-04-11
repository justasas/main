package main.parsers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thoughtworks.selenium.Selenium;

import main.Main;
import main.SubtitleFile;
import main.SubtitlesUtils;
import main.Utils;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class OpenSubtitlesParser {

	private final String URL = "http://www.opensubtitles.org/";
	private final String TEMP_SUBTITLES_FOLDER = "subtitles/subscene/temp/";
	private final String SUBTITLES_FOLDER = "subtitles/subscene/";
	private final int MAXIMUM_MOVIES_TO_CHECK = 1;
	private final int MAXIMUM_SUBTITLES_TO_DOWNLOAD_FOR_A_MOVIE = 3;
	private Utils utils = new Utils();

	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(OpenSubtitlesParser.class);

	// static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	// static final String DB_URL =
	// "jdbc:mysql://localhost:3306/web?useUnicode=true&characterEncoding=UTF-8";

	// Database credentials
	// static final String USER = "root";
	// static final String PASS = "toor";

	public List<SubtitleFile> getSubtitles(String title, String finalUrlOfPageWithEnSubtitlesList_OpenSubtitles) {

		List<SubtitleFile> ret = new ArrayList<>();

		try {

			title = title.replaceAll(Utils.UNWANTED_SYMBOLS_IN_FOLDER_NAME_REGEX, "+");
			List<String> linksToDifferentMovies = getLinksToMovies(title);

			int counter = 0;
			downloadAllSubtitles(finalUrlOfPageWithEnSubtitlesList_OpenSubtitles, title);

			String movieSubtitlesFolder = SUBTITLES_FOLDER + title + File.separator;
			String extension = "srt";
			String[] srtFiles = utils.filterFolderByExtension(movieSubtitlesFolder, extension);

			for (int srtIndex = 0; srtIndex < srtFiles.length; srtIndex++) {
				BufferedReader reader = new BufferedReader(new FileReader(movieSubtitlesFolder + srtFiles[srtIndex]));

				SubtitleFile subtitles = new SubtitleFile(SubtitlesUtils.parseSubtitles(reader), srtFiles[srtIndex], movieSubtitlesFolder + srtFiles[srtIndex]);

				ret.add(subtitles);
			}

			return ret;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private List<String> getLinksToMovies(String movieName) throws Exception {

		String pageWithLinksToMovies = downloadWebPageToString(movieName);

		List<String> linksToMovies = parseLinksToMovies(pageWithLinksToMovies);

		return linksToMovies;
	}

	private void downloadFile(String link, String tempSutitlesPath) {
		try {

			Utils.createDirectoriesToFile(tempSutitlesPath);

			URL url = new URL(link);
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			httpCon.addRequestProperty("User-Agent", "Mozilla/4.76");
			httpCon.setRequestProperty("Cookie", "LanguageFilter=13");

			ByteArrayOutputStream out = new ByteArrayOutputStream();

			BufferedInputStream in = new BufferedInputStream(httpCon.getInputStream());

			byte data[] = new byte[1024];
			int count = 0;

			while ((count = in.read(data, 0, 1024)) != -1) {
				out.write(data, 0, count);
			}

			out.close();
			in.close();

			FileOutputStream fos = new FileOutputStream(tempSutitlesPath.toString());
			fos.write(out.toByteArray());
			fos.close();

		} catch (Exception ex) {
			System.out.println(ex);
		}

	}

	private Selenium selenium;

	private String downloadWebPageToString(String link) {

		String ret = "";

		try {

			URL url = new URL(link);
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			httpCon.setRequestMethod("GET");
			httpCon.addRequestProperty("User-Agent",
					"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/36.0.1985.125 Chrome/36.0.1985.125 Safari/537.36");
			// httpCon.setRequestProperty("Accept-Encoding", "gzip, deflate");
			// httpCon.setRequestProperty("Cookie", "LanguageFilter=13");
			// httpCon.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			// httpCon.setRequestProperty("Accept",
			// "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			// httpCon.setRequestProperty("Connection", "keep-alive");

			BufferedReader in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));

			System.out.println("http response code " + httpCon.getResponseCode());
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				ret += inputLine;
			}

		} catch (Exception ex) {
			Logger.getLogger(OpenSubtitlesParser.class.getName()).log(Level.SEVERE, null, ex);
		}

		return ret;
	}

	private List<String> parseLinksToMovies(String page) {

		List<String> urls = new ArrayList<>();

		Pattern p = Pattern.compile("<div class=\"title\">\t+<a href=\"(.+?)\"");

		Matcher m = p.matcher(page);
		while (m.find()) {
			String title = m.group(1);
			urls.add(title);
		}

		return urls;
	}

	private void downloadAllSubtitles(String linkToFinalListOfSubtitles, String title) {

		try {
			String pageContainingLinksToSubtitlesFiles = downloadWebPageToString(linkToFinalListOfSubtitles);
			List<String> linksToDownloadPage = parseSubtitleDownloadLinks(pageContainingLinksToSubtitlesFiles);

			int subsDownloaded = 0;

			for (String linkToDownloadPage : linksToDownloadPage) {

				if (subsDownloaded++ < MAXIMUM_SUBTITLES_TO_DOWNLOAD_FOR_A_MOVIE) {

					String subtitlesLocation = linkToDownloadPage.replace("/subtitles/", "");

					String tempSubtitlesFilePath = TEMP_SUBTITLES_FOLDER + subtitlesLocation + ".zip";
					String subtitlesFilePath = SUBTITLES_FOLDER + title;

					if (!new File(tempSubtitlesFilePath).exists()) {
						String subtitlesDownloadPage = downloadWebPageToString(URL + linkToDownloadPage);
						String downloadLink = parseDownloadLink(subtitlesDownloadPage);

						downloadFile(URL + downloadLink, tempSubtitlesFilePath);

						int filesCountBeforeUnzip = utils.filterFolderByExtension(subtitlesFilePath, "srt").length;
						unzipSourceToDestination(tempSubtitlesFilePath, subtitlesFilePath);
						int filesCountAfterUnzip = utils.filterFolderByExtension(subtitlesFilePath, "srt").length;

						if((filesCountAfterUnzip - filesCountBeforeUnzip) > 1)
						{
							logger.debug("unziped more than one file");
						}
					}

				} else
					break;

			}
			// for(File file: dir.listFiles()) file.delete();
		} catch (Exception e) {
			System.out.println(e);
		}

	}

	private List<String> parseSubtitleDownloadLinks(String pageContainingLinkToSubtitlesFiles) {

		List<String> ret = new ArrayList<>();

		Matcher m2 = Pattern.compile("href=\"" + "(/subtitles/[^\"]+)").matcher(pageContainingLinkToSubtitlesFiles);
		// Matcher m2 = Pattern.compile("href=\"" + SUBSCENE_URL +
		// "/subtitles/english/[^\"]+").matcher(pageContainingLinkToSubtitlesFiles);
		//
		while (m2.find()) {
			ret.add(m2.group(1));
		}

		return ret;
	}

	private String parseDownloadLink(String page) {

		List<String> ret = new ArrayList<>();

		Matcher m2 = Pattern.compile("href=\"" + "(/subtitle/download?[^\"]+)").matcher(page);
		// Matcher m2 = Pattern.compile("href=\"" + SUBSCENE_URL +
		// "/subtitles/english/[^\"]+").matcher(pageContainingLinkToSubtitlesFiles);
		//
		m2.find();
		return m2.group(1);
	}

	private void createFile(String text, String filePath) {

		try {
			Utils.createDirectoriesToFile(filePath);
			// Files.createFile(pathToFile);
			FileOutputStream fos = new FileOutputStream(filePath);
			fos.write(text.getBytes());
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void unzipSourceToDestination(String source, String destination) {

		try {
			ZipFile zipFile = new ZipFile(source);

			// if (zipFile.isEncrypted()) {
			// zipFile.setPassword(password);
			// }
			zipFile.extractAll(destination);

		} catch (ZipException e) {
			e.printStackTrace();
		}

	}

	private void postLanguageSettings() {
		try {
			URL url = new URL("http://u.subscene.com/filter");

			HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
			httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");
			httpcon.setRequestMethod("POST");
			httpcon.setRequestProperty("Cookie", "LanguageFilter=13");
			httpcon.setDoOutput(true);

			OutputStreamWriter out = new OutputStreamWriter(httpcon.getOutputStream(), "UTF-8");
			out.close();
		} catch (Exception e) {
		}
	}

	// private void start()
	// {
	// Connection conn = null;
	// Statement stmt = null;
	//
	// try {
	// Class.forName("com.mysql.jdbc.Driver");
	// conn = DriverManager.getConnection(DB_URL, USER, PASS);
	// stmt = conn.createStatement();
	//
	// String sql = "SELECT title, year from movies";
	//
	// ResultSet titleAndYear = stmt.executeQuery(sql);
	//
	// while (titleAndYear.next()) {
	// String title =
	// titleAndYear.getString(1).toLowerCase().replaceAll("[^a-z0-9]+", " ");
	// int year = titleAndYear.getInt(2);
	// downloadSubtitle(title, readSearchResults(title.replace(" ", "+"),
	// Integer.toString(year)));
	// }
	//
	// } catch (SQLException ex) {
	// Logger.getLogger(SubtitlesDownload.class.getName()).log(Level.SEVERE,
	// null, ex);
	// } catch (Exception e) {
	// e.printStackTrace();
	// } finally {
	// try {
	// if (stmt != null) {
	// conn.close();
	// }
	// } catch (SQLException se) {
	// }
	// try {
	// if (conn != null) {
	// conn.close();
	// }
	// } catch (SQLException se) {
	// se.printStackTrace();
	// }
	// }
	// }

}

// TODO code application logic here
// SubtitlesDownload first = new SubtitlesDownload();
// Vector aa = first.readSearchResults("2009");

/*
 * try { Class.forName("com.mysql.jdbc.Driver");
 * 
 * conn = DriverManager.getConnection(DB_URL, USER, PASS);
 * 
 * stmt = conn.createStatement(); stmt2 = conn.createStatement();
 * 
 * String sql = "SELECT title from movies";
 * 
 * ResultSet title = stmt.executeQuery(sql);
 * 
 * while (title.next()) { stmt2.executeUpdate("UPDATE movies SET img = \""
 * +title.getString(1).replaceAll("[^A-Za-z0-9]+", "-").toLowerCase()+
 * "\" WHERE title =\"" + title.getString(1)+"\"");
 * 
 * } } catch (SQLException ex) {
 * Logger.getLogger(SubtitlesDownload.class.getName()).log(Level.SEVERE, null,
 * ex); } catch(Exception e) { e.printStackTrace(); } finally { try { if(stmt !=
 * null) conn.close(); } catch(SQLException se) { } try { if(conn != null)
 * conn.close(); } catch(SQLException se) { se.printStackTrace(); } }
 */
// new SubtitlesDownload().start();