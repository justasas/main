package main.parsers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;

import main.SubtitleFile;
import main.SubtitlesUtils;
import main.Utils;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class SubsceneSubsParser {

	private final String SUBSCENE_URL = "https://subscene.com";
	private final String SUBTITLES_FOLDER = "subtitles/subscene/";
	private final int MAXIMUM_MOVIES_TO_CHECK = 3;
	private final int MAXIMUM_SUBTITLES_TO_DOWNLOAD_FOR_A_MOVIE = 3;

	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost:3306/web?useUnicode=true&characterEncoding=UTF-8";

	// Database credentials
	static final String USER = "root";
	static final String PASS = "toor";
	private static final String USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686) Gecko/20071127 Firefox/2.0.0.11";
	public final static String TEMP_SUBTITLES_FOLDER = "subtitles/subscene/temp/";

	public Set<SubtitleFile> getSubtitles(String title, String releaseYear) {

		Set<SubtitleFile> ret = new HashSet<>();

		try {

			title = title.replaceAll(Utils.UNWANTED_SYMBOLS_IN_FOLDER_NAME_REGEX, "+");
			List<String> linksToDifferentMovies = getLinksToMovies(this.SUBSCENE_URL + "/subtitles/title?q=" + title,
					releaseYear);

			int counter = 0;
			for (String linkToMovie : linksToDifferentMovies) {

				if (counter++ >= MAXIMUM_MOVIES_TO_CHECK)
					break;

				downloadAllSubtitles(linkToMovie, title);

				String movieSubtitlesFolder = SUBTITLES_FOLDER + title + File.separator;
				String extension = "srt";
				String[] srtFiles = filterFolderByExtension(movieSubtitlesFolder, extension);

				for (int srtIndex = 0; srtIndex < srtFiles.length; srtIndex++) {
					BufferedReader reader = new BufferedReader(
							new FileReader(movieSubtitlesFolder + srtFiles[srtIndex]));

					SubtitleFile subtitles = new SubtitleFile(SubtitlesUtils.parseSubtitles(reader),
							srtFiles[srtIndex]);

					ret.add(subtitles);
				}

			}

			return ret;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private List<String> getLinksToMovies(String searchUrl, String releaseYear) throws Exception {

		String pageWithLinksToMovies = getHtml(searchUrl);

		List<String> linksToMovies = parseLinksToMovies(pageWithLinksToMovies, releaseYear);

		return linksToMovies;
	}

	private void downloadFile(String link, String tempSutitlesPath) {
		try {

			Utils.createDirectoriesToFile(tempSutitlesPath);

			URL url = new URL(link);
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			httpCon.addRequestProperty("User-Agent",
					"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:31.0) Gecko/20100101 Firefox/31.0");
			httpCon.addRequestProperty("Accept-Encoding", "gzip, deflate");
			httpCon.addRequestProperty("Accept-Language", "en-US,en;q=0.5");
			httpCon.addRequestProperty("Connection", "keep-alive");
			httpCon.addRequestProperty("Host", "subscene.com");
			httpCon.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			BufferedInputStream in = new BufferedInputStream(httpCon.getInputStream());

			byte data[] = new byte[1024];
			int count = 0;

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			while ((count = in.read(data, 0, 1024)) != -1) {
				out.write(data, 0, count);
			}
			httpCon.getResponseCode();

			in.close();
			out.close();

			FileOutputStream fos = new FileOutputStream(tempSutitlesPath.toString());
			fos.write(out.toByteArray());
			fos.close();

		} catch (Exception ex) {
			System.out.println(ex);
		}

	}

	private String getHtml(String link) throws IOException {
		org.jsoup.Connection connect = Jsoup.connect(link);

		Map<String, String> m = new HashMap<String, String>();
		m.put("User-Agent", this.USER_AGENT);
		m.put("Cookie", "LanguageFilter=13");

		connect.request().headers().putAll(m);
		connect.request().cookie("LanguageFilter", "13");
		String html = connect.get().html();
		return html;
	}

	private List<String> parseLinksToMovies(String page, String releaseyear) {

		List<String> urls = new ArrayList<>();

		Pattern p = Pattern.compile("<div class=\"title\">\\s+<a href=\"(.+?)\">(.+?)</a>");

		Matcher m = p.matcher(page);
		while (m.find()) {
			String url = m.group(1);
			String titleAndYear = m.group(2);
			String year = titleAndYear.substring(titleAndYear.length() - 5, titleAndYear.length() - 1);
			if (year != null && year.matches("[0-9]+")) {
				urls.add(url);
			} else {
				System.out.println("cant parse movie release year from string: " + titleAndYear);
			}
		}

		return urls;
	}

	private void downloadAllSubtitles(String link, String title) {

		try {
			String pageContainingLinksToSubtitlesFiles = getHtml(SUBSCENE_URL + link);
			List<String> linksToDownloadPage = parseSubtitleDownloadLinks(pageContainingLinksToSubtitlesFiles);

			int subsDownloaded = 0;

			for (String linkToDownloadPage : linksToDownloadPage) {

				if (subsDownloaded++ < MAXIMUM_SUBTITLES_TO_DOWNLOAD_FOR_A_MOVIE) {

					String subtitlesLocation = linkToDownloadPage.replace("/subtitles/", "");

					String tempSubtitlesFilePath = TEMP_SUBTITLES_FOLDER + subtitlesLocation + ".zip";
					String subtitlesFilePath = SUBTITLES_FOLDER + title;

					if (!new File(tempSubtitlesFilePath).exists()) {
						String subtitlesDownloadPage = getHtml(SUBSCENE_URL + linkToDownloadPage);
						String downloadLink = parseDownloadLink(subtitlesDownloadPage);

						downloadFile(SUBSCENE_URL + downloadLink, tempSubtitlesFilePath);
						unzipSourceToDestination(tempSubtitlesFilePath, subtitlesFilePath);
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

	private String[] filterFolderByExtension(String folder, String extension) {
		File dir = new File(folder);
		if (dir.isDirectory() == false) {
			System.out.println("Directory does not exists : " + folder);
			return null;
		}

		GenericExtFilter filter = new GenericExtFilter(extension);
		String[] list = dir.list(filter);
		return list;
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