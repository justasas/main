package main;

import java.awt.image.RenderedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import javax.imageio.ImageIO;

import main.parsers.RedditParser;
import main.parsers.SubsceneSubsParser;
import main.youtube.downloaders.Google2SRTDownloader;
import org.apache.log4j.Logger;
import org.eclipse.jgit.util.StringUtils;

public class Main {

    public static List<String> youtubeLinks = new ArrayList<String>();
    public final static String SUBTITLES_YOUTUBE_FOLDER = "subtitles/youtube/";
    private static Logger logger = Logger.getLogger(Main.class);

    private static Map<String, Movie> ytIdToMovieWithOriginalSubs = readMoviesWithOriginalSubs();
    private static Map<String, Movie> ytIdToMovieWithSyncedSubs = readMoviesWithSubsDownloadLocation("moviesWithSyncedSubtitles.txt");
    private static Map<String, Movie> ytIdToMovieWithMultiplePossibleGoodSubtitles = readMoviesWithSubsDownloadLocation("moviesWithMultiplePossibleGoodSubtitles.txt");
    private static Map<String, Movie> ytIdToMovie = readAllMovies();
    //    private static Map<String, Movie> ytIdToMovieWithCcSubsFolder = readMoviesWithSubsDownloadLocation("moviesWithAutoCaptionedSubtitles.txt");

    private static Map<String, Movie> readAllMovies() {
        HashMap<String, Movie> ret = new HashMap<>();
        ret.putAll(ytIdToMovieWithMultiplePossibleGoodSubtitles);
        ret.putAll(ytIdToMovieWithSyncedSubs);
        ret.putAll(ytIdToMovieWithOriginalSubs);
        return ret;
    }

    private static Map<String, Movie> readMoviesWithSubsDownloadLocation(String fileName) {
        HashMap<String, Movie> ret = new HashMap<String, Movie>();
        List<Movie> movies = readMoviesFromFileWithSubsDownloadLocation(fileName);

        movies.forEach(movie -> {
            ret.put(movie.getYoutubeId(), movie);
        });

        return ret;
    }

    private static HashMap<String, Movie> readMoviesWithOriginalSubs()
    {
        HashMap<String, Movie> ret = new HashMap<String, Movie>();
        List<Movie> movies = readMoviesFromFile("moviesWithOriginalSubs.txt");

        List<String> files = getFileNamesFromFolder("subtitles/youtube/");
        Map<String, String> ytIdToFileName = new HashMap<>();

        files.forEach(file -> {
            String ytId = file.split("_")[1];
            ytIdToFileName.put(ytId, file);
        });

        movies.forEach(movie -> {
            movie.setSubsLocations(Arrays.asList("subtitles/youtube/" + ytIdToFileName.get(movie.getYoutubeId())));
            ret.put(movie.getYoutubeId(), movie);
        });

        return ret;
    }

    static List<String> getFileNamesFromFolder(String folderPath)
    {
        List<String> ret= new ArrayList();

        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                ret.add(listOfFiles[i].getName());
            } else if (listOfFiles[i].isDirectory()) {
            }
        }

        return ret;
    }

    static {
        youtubeLinks.add("https://www.youtube.com/watch?v=");
        youtubeLinks.add("http://www.youtube.com/watch?v=");
        youtubeLinks.add("http://youtube.com/watch?v=");
        youtubeLinks.add("https://youtube.com/watch?v=");
        youtubeLinks.add("www.youtube.com/watch?v=");
        youtubeLinks.add("https://youtu.be/");
        youtubeLinks.add("https://m.youtube.com/watch?v=");
        youtubeLinks.add("www.m.youtube.com/watch?v=");
    }

    public static void main(String[] args) throws Exception {
//        RedditParser redditParser = new RedditParser();
//        List<Movie> movies = redditParser.start();
//        saveToFile(movies, "allMovies.txt");

//        downloadMoviesAndSubtitles();

//        downloadMoviesAndTryToFindSubtitlesAndSyncThem();
        downloadMoviesAndFindSubtitles();
    }

    private static void downloadMoviesAndTryToFindSubtitlesAndSyncThem() throws IOException {
//        RedditParser reddirParser = new RedditParser();
//        List<Movie> movies = reddirParser.start();

//        List<Movie> moviesWithSubtitles = downloadSubs(movies, true);
//        saveToFile(moviesWithSubtitles, "moviesWithAutoCaptionedSubtitles.txt");
        List<Movie> moviesWithAutocaptionedSubtitles = readMoviesFromFile("moviesWithAutoCaptionedSubtitles.txt");
        List<Movie> moviesWithSyncedFromSubsceneSubtitles = new ArrayList<>();

        for(Movie movie : moviesWithAutocaptionedSubtitles) {
//            String movieName = "Hobgoblins";
            String movieName = movie.getName();
            movieName = movieName.replaceAll("\\(.+?\\)", "").replaceAll("\\(.+?\\)", "").trim();

            String releaseYear = movie.getReleaseYear();
//            String ytId = "B9pHNwTyh7o";
            String ytId = movie.getYoutubeId();
            BufferedReader reader = new BufferedReader(
                    new FileReader(movie.getSubsLocations().iterator().next()));
//        4OSSSp1qKQE, B9pHNwTyh7o
            SubtitleFile ytSubtitleFile;
            try {
                ytSubtitleFile = new SubtitleFile(SubtitlesUtils.parseSubtitles(reader), ytId, movie.getSubsLocations().iterator().next());
            } catch(Exception e)
            {
                System.out.println(e);
                continue;
            }
            SubsceneSubsParser subsceneParser = new SubsceneSubsParser();
            Set<SubtitleFile> subtitlesList = subsceneParser.getSubtitles(movieName, releaseYear);
            System.out.println(subtitlesList);
//
            SubtitleFile mostSimiliarSubsFile = SubtitlesUtils.findMostSimiliarSubs(subtitlesList, ytSubtitleFile.subtitles);
//
            if(mostSimiliarSubsFile == null || mostSimiliarSubsFile.subtitles.size() == 0 || ytSubtitleFile.subtitles.size() == 0)
                continue;

            SubtitlesCommon subtitleCommon =
                    SubtitlesUtils.findCommonThings(mostSimiliarSubsFile.subtitles,
                            ytSubtitleFile.subtitles);

            System.out.println("avarage times diff: " + subtitleCommon.avarageTimeDiff);
            System.out.println("velocity: " + subtitleCommon.velocity);

            for (Subtitle sub : mostSimiliarSubsFile.subtitles) {
                sub.start =
                        SubtitlesUtils.milliToSrt(SubtitlesUtils.srtSubToMilli(sub.start) +
                                subtitleCommon.mostFrequentTimeDiff);
                sub.end =
                        SubtitlesUtils.milliToSrt(SubtitlesUtils.srtSubToMilli(sub.end) +
                                subtitleCommon.mostFrequentTimeDiff);
            }

            String subDownloadLocation = "subtitles/subscene/adjustedFromAutoCaptioned/" + (movieName + "_" + new Date().toString()).replaceAll("[^a-zA-Z0-9\\.-]+", "_") + "_" + ytId + ".srt";
            SubtitlesUtils.writeSubtitlesToFile(mostSimiliarSubsFile.subtitles,
                    subDownloadLocation);
//         downloadYtPictures(RedditParser.getExistingMovies());
            movie.setSubsLocations(Arrays.asList(subDownloadLocation));
            moviesWithSyncedFromSubsceneSubtitles.add(movie);
        }

        saveToFile(moviesWithSyncedFromSubsceneSubtitles, "moviesWithSyncedSubtitles.txt");
    }

    private static void downloadMoviesAndFindSubtitles() throws IOException {
        List<Movie> moviesWithSubtitles = new ArrayList<>();

        List<Movie> movies = readMoviesFromFileWithSubsDownloadLocation("allMovies.txt");

        for(Movie movie : movies) {

            if(ytIdToMovie.get(movie.getYoutubeId()) != null)
                continue;

//            String movieName = "Hobgoblins";
            String movieName = movie.getName();
            movieName = movieName.replaceAll("\\(.+?\\)", "").replaceAll("\\[.+?\\]", "").trim();
            String releaseYear = movie.getReleaseYear();

            SubsceneSubsParser subsceneParser = new SubsceneSubsParser();
            Set<SubtitleFile> subtitlesList = subsceneParser.getSubtitles(movieName, releaseYear);
            if(subtitlesList == null || subtitlesList.isEmpty())
                continue;
            ArrayList subs = new ArrayList();
            subtitlesList.forEach(subtitle -> {
                subs.add(subtitle.getLocation());
            });
            movie.setSubsLocations(subs);
            moviesWithSubtitles.add(movie);
            saveToFile(moviesWithSubtitles, "moviesWithMultiplePossibleGoodSubtitles.txt");
        }

    }

    private static List<Movie> readMoviesFromFileWithSubsDownloadLocation(String fileName) {
        List<Movie> movies = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));

            String line;
            while((line = br.readLine()) != null)
            {
                Movie m = new Movie();
                m.setName(line);
                m.setYoutubeId(br.readLine());
                m.setReleaseYear(br.readLine());
                m.setGenres(Arrays.asList(br.readLine().split(",")));
                m.setSubsLocations(Arrays.asList(br.readLine()));
                br.readLine();
                movies.add(m);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return movies;
    }

    private static List<Movie> readMoviesFromFile(String fileName) {
        List<Movie> movies = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));

            String line;
            while((line = br.readLine()) != null)
            {
                Movie m = new Movie();
                m.setName(line);
                m.setYoutubeId(br.readLine());
                m.setReleaseYear(br.readLine());
                m.setGenres(Arrays.asList(br.readLine().split(",")));
                br.readLine();
                movies.add(m);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return movies;
    }

    private static void downloadMoviesAndSubtitles() {
        RedditParser reddirParser = new RedditParser();
        List<Movie> movies = reddirParser.start();

        List<Movie> moviesWithSubtitles = downloadSubs(movies, false);
//        saveToFile(moviesWithSubtitles);
    }

    public static List<Movie> downloadSubs(List<Movie> movies, boolean downloadAutoCaptioned) {
        CacheRepository cacheRepository = new CacheRepository();
        List<Movie> ret = new ArrayList<Movie>();
        for (Movie movie : movies) {
            try {
                String videoId = movie.getYoutubeId();
//                if (cacheRepository.find("noSubsForThisVideo#" + videoId) == null) {
                String youtubeLink = "http://www.youtube.com/watch?v=" + videoId;
                Google2SRTDownloader ytParser = new Google2SRTDownloader();
                String fileName = ytParser.getSubtitle(youtubeLink, downloadAutoCaptioned);
                boolean found = fileName == null ? false : true;
                if (found) {
                    movie.setSubsLocations(Arrays.asList(Main.SUBTITLES_YOUTUBE_FOLDER + fileName));
                    ret.add(movie);
                } else {
//                    cacheRepository.insert("noSubsForThisVideo#" + videoId, "y");
                }
                Thread.sleep(2000);
//                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Did not download subs for movie: " + movie.getName());
//                cacheRepository.insert("noSubsForThisVideo#" + movie.getYoutubeId(), "y");
            }
        }
        return ret;
    }

    private static void saveToFile(List<Movie> movies, String fileName) {
        logger.debug("saving to file: " + fileName + " " + movies.size() + " movies");
        for (Movie movie : movies) {
            try {
                String movieString = movie.getName() + "\n" + movie.getYoutubeId() + "\n" + movie.getReleaseYear()
                        + "\n" + (movie.getGenres() != null ? StringUtils.join(movie.getGenres(), ",") : "") + "\n" + movie.getSubsLocations() + "\n\n";

                Files.write(Paths.get(fileName), movieString.getBytes(), StandardOpenOption.APPEND);
            } catch (Exception e) {
                // exception handling left as an exercise for the reader
            }
        }
    }

    private static void downloadYtPictures(List<Movie> list) {
        for (Movie movie : list) {
            try {
                RenderedImage image = null;
                URL url = new URL("https://img.youtube.com/vi/" + movie.getYoutubeId() + "/hqdefault.jpg");
                image = ImageIO.read(url);

                File file = new File("images/" + movie.getYoutubeId() + ".jpg");
                ImageIO.write(image, "jpg", file);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public static boolean downloadFile(String link, String path) {
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