package main;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import main.parsers.RedditParser;

public class Main {

	public static List<String> youtubeLinks = new ArrayList<String>();
	public final static String SUBTITLES_YOUTUBE_FOLDER = "subtitles/youtube/";
	private static Logger logger = Logger.getLogger(Main.class);

	static {
		youtubeLinks.add("https://www.youtube.com/watch?v=");
		youtubeLinks.add("http://www.youtube.com/watch?v=");
		youtubeLinks.add("www.youtube.com/watch?v=");
		youtubeLinks.add("https://youtu.be/");
		youtubeLinks.add("https://m.youtube.com/watch?v=");
		youtubeLinks.add("www.m.youtube.com/watch?v=");
	}

	public static void main(String[] args) throws Exception {

		// String movieName = "Test8";
		// String releaseYear = "1997";
		// String youtubeLink = "https://www.youtube.com/watch?v=esWeD7MxpEU";
		// YTSubsParser ytParser = new YTSubsParser();
		// SubtitleFile ytSubtitleFile = ytParser.getSubtitles(movieName,
		// youtubeLink);
		RedditParser reddirParser = new RedditParser();
		reddirParser.start();
		// SubsceneSubsParser subsceneParser = new SubsceneSubsParser();
		// List<SubtitleFile> subtitlesList =
		// subsceneParser.getSubtitles(movieName, releaseYear);
		//// System.out.println(subtitlesList);
		// int i = 0;
		//
		// for (SubtitleFile subtitleFile : subtitlesList) {
		// int avarageTimesDiff =
		// SubtitlesUtils.findAvarageTimesDiff(subtitleFile.subtitles,
		// ytSubtitleFile.subtitles);
		// System.out.println("avarage times diff: " + avarageTimesDiff);
		//
		//// for(Subtitle sub : subtitleFile.subtitles)
		//// {
		//// sub.start =
		// SubtitlesUtils.milliToSrt(SubtitlesUtils.srtSubToMilli(sub.start) +
		// avarageTimesDiff);
		//// sub.end =
		// SubtitlesUtils.milliToSrt(SubtitlesUtils.srtSubToMilli(sub.end) +
		// avarageTimesDiff);
		//// }
		////
		//// SubtitlesUtils.writeSubtitlesToFile(subtitleFile.subtitles,
		// movieName + " ( " + releaseYear + " ) " + "ver" + ++i);
		// }

	}

	public static List<Movie> downloadSubs(List<Movie> movies) {
		CacheRepository cacheRepository = new CacheRepository();
		List<Movie> ret = new ArrayList<Movie>();
		for (Movie movie : movies) {
			try {
				String videoId = movie.getYoutubeId();
				if (cacheRepository.find("noSubsForThisVideo#" + videoId) == null) {
					sendRequest(videoId);
					Thread.sleep(2000);
					boolean downloadSucceed = downloadFile("http://ccsubs.com/video/yt:" + videoId + "/"
							+ URLEncoder.encode(movie.getName(), "UTF-8") + "/download?format=srt&lang=en",
							Main.SUBTITLES_YOUTUBE_FOLDER + videoId);
					if (downloadSucceed) {
						movie.setSubDownloadLocation(Main.SUBTITLES_YOUTUBE_FOLDER + videoId);
						ret.add(movie);
					} else {
						cacheRepository.insert("noSubsForThisVideo#" + videoId, "y");
					}
				}
			} catch (IOException | InterruptedException e) {
				System.out.println("Did not download subs for movie: " + movie.getName());
			}
		}
		return ret;
	}

	private static void sendRequest(String videoId) throws IOException {
		URL url;
		url = new URL("http://ccsubs.com/fetch?id=yt:" + videoId);
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		httpCon.addRequestProperty("User-Agent",
				"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:31.0) Gecko/20100101 Firefox/31.0");

		BufferedInputStream in = new BufferedInputStream(httpCon.getInputStream());

		byte data[] = new byte[1024];
		int count = 0;

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while ((count = in.read(data, 0, 1024)) != -1) {
			out.write(data, 0, count);
		}
		System.out.println("Response code: " + httpCon.getResponseCode());
		System.out.println(out.toString());
		in.close();
		out.close();

		// httpCon.addRequestProperty("Accept-Encoding", "gzip, deflate");
		// httpCon.addRequestProperty("Accept-Language", "en-US,en;q=0.5");
		// httpCon.addRequestProperty("Connection", "keep-alive");
		// httpCon.addRequestProperty("Host", "subscene.com");
		// httpCon.addRequestProperty("Accept",
		// "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
	}

	private static boolean downloadFile(String link, String path) {
		try {
			if (Files.exists(Paths.get(path)))
				return true;
			logger.debug("Downloading file: " + link + " to " + path);
			Utils.createDirectoriesToFile(path);

			URL url = new URL(link);
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			httpCon.addRequestProperty("User-Agent",
					"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:31.0) Gecko/20100101 Firefox/31.0");
			// httpCon.addRequestProperty("Accept-Encoding", "gzip, deflate");
			// httpCon.addRequestProperty("Accept-Language", "en-US,en;q=0.5");
			// httpCon.addRequestProperty("Connection", "keep-alive");
			// httpCon.addRequestProperty("Host", "subscene.com");
			// httpCon.addRequestProperty("Accept",
			// "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
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

			FileOutputStream fos = new FileOutputStream(path.toString());
			fos.write(out.toByteArray());
			fos.close();
			return true;
		} catch (Exception ex) {
			System.out.println(ex);
		}
		return false;
	}

}