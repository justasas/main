package main.parsers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.eclipse.jgit.util.StringUtils;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import main.CacheRepository;
import main.Main;
import main.Movie;

public class RedditParser {

	private static int PAGES_COUNT_TO_PARSE = 50;
	private static Logger logger = Logger.getLogger(Main.class);

	public static List<String> existingMovies = getExistingMoviesYTuri("file.txt");

	private CacheRepository cache = new CacheRepository();

	public List<Movie> start() {
		BasicConfigurator.configure();
		List<Movie> movies = parseAllPages("https://www.reddit.com/r/fullmoviesonyoutube/");
		Iterator<Movie> iterator = movies.iterator();
		while (iterator.hasNext()) {
			if (existingMovies.contains(iterator.next().getYoutubeId()))
				iterator.remove();
		}
		return movies;
	}

	private static List<String> getExistingMoviesYTuri(String fileName) {
		List<String> ret = new ArrayList<String>();

		List<Movie> movies = getExistingMovies(fileName);
		for (Movie movie : movies) {
			ret.add(movie.getYoutubeId());
		}
		return ret;
	}

	public static List<Movie> getExistingMovies(String filename) {
		List<Movie> movies = new ArrayList<Movie>();

		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line;
			while ((line = br.readLine()) != null) {
				Movie m = new Movie();
				m.setName(line);
				m.setYoutubeId(br.readLine());
				m.setReleaseYear(br.readLine());
				m.setGenres(Arrays.asList(br.readLine().split(",")));
				br.readLine();
				movies.add(m);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return movies;
	}

	public List<Movie> parseAllPages(String link) {
		Pattern yearPattern = Pattern.compile("\\([0-9]{4}\\)");

		List<Movie> ret = new ArrayList<Movie>();

		try {

			int currentPageInd = 0;
			while (currentPageInd++ < PAGES_COUNT_TO_PARSE) {

				logger.debug("current page index: " + currentPageInd);

				logger.debug("getting document: " + link);
				Document document = getDocument(link);

				for (Element movieHtmlBlock : document.getElementsByAttributeValue("class",
						"title may-blank outbound ")) {

					String movieIDFromYoutubeLink = getMovieIDFromYoutubeLink(movieHtmlBlock.attr("href"));
					if (movieIDFromYoutubeLink.length() != 11) {
						logger.debug("Skipping id :" + movieIDFromYoutubeLink);
						continue;
					}
					String title = movieHtmlBlock.text();
					Movie movie = new Movie(title, movieIDFromYoutubeLink);
					Matcher m = yearPattern.matcher(title);
					if (m.find()) {
						movie.setReleaseYear(m.group().replace("(", "").replace(")", ""));
					}
					try {
						String genresAttribute = movieHtmlBlock.parent().getElementsByClass("linkflairlabel").iterator()
								.next().attr("title");
						if (genresAttribute.contains("|")) {
							movie.setGenres(Arrays.asList(genresAttribute.split(" \\| ")));
						} else {
							movie.setGenres(Arrays.asList(genresAttribute));
						}
					} catch (Exception e) {
						System.out.println("genres for " + movie.getName() + "not found");
					}
					ret.add(movie);
				}

				if (document.getElementsByClass("next-button").isEmpty())
					break;
				else
					link = document.getElementsByClass("next-button").iterator().next().getElementsByAttribute("href")
							.attr("href");

				Thread.sleep(1000);
			}

			return ret;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;
	}

	private Document getDocument(String link) throws IOException {
		Document document;
//		String html = cache.find(link);
//		if (html != null)
//			document = Jsoup.parse(html);
//		else {
			org.jsoup.Connection connect = Jsoup.connect(link).timeout(10000)
					.userAgent(
							"Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					.referrer("http://www.google.com");

			Response response = connect.execute();
			document = response.parse();
//			cache.insert(link, document.toString());
//		}
		return document;
	}

	private String getMovieIDFromYoutubeLink(String linkToMovie) {
		logger.debug("Geeting id from link: " + linkToMovie);
		for (String ytLink : Main.youtubeLinks) {
			linkToMovie = linkToMovie.replace(ytLink, "").replaceAll("(&)[^=]+=[^&$]+", "");
		}
		logger.debug("Found ID: " + linkToMovie);
		return linkToMovie;
	}

}
