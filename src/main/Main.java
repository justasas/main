package main;

import java.awt.image.RenderedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

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

//        downloadMoviesAndSubtitles();

        downloadMoviesAndTryToFindSubtitlesAndSyncThem();
    }

    private static void downloadMoviesAndTryToFindSubtitlesAndSyncThem() throws IOException {
        RedditParser reddirParser = new RedditParser();
        List<Movie> movies = reddirParser.start();

        List<Movie> moviesWithSubtitles = downloadSubs(movies, true);

        for(Movie movie : moviesWithSubtitles) {
//            String movieName = "Hobgoblins";
            String movieName = movie.getName();
            String releaseYear = movie.getReleaseYear();
//            String ytId = "B9pHNwTyh7o";
            String ytId = movie.getYoutubeId();
            BufferedReader reader = new BufferedReader(
                    new FileReader(movie.getSubDownloadLocation()));
//        4OSSSp1qKQE, B9pHNwTyh7o
            SubtitleFile ytSubtitleFile = new SubtitleFile(SubtitlesUtils.parseSubtitles(reader), ytId);

            SubsceneSubsParser subsceneParser = new SubsceneSubsParser();
            Set<SubtitleFile> subtitlesList = subsceneParser.getSubtitles(movieName, releaseYear);
            System.out.println(subtitlesList);
//
            SubtitleFile mostSimiliarSubsFile = SubtitlesUtils.findMostSimiliarSubs(subtitlesList, ytSubtitleFile.subtitles);
//
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


            SubtitlesUtils.writeSubtitlesToFile(mostSimiliarSubsFile.subtitles,
                    Google2SRTDownloader.OUTPUT_FOLDER + movieName + "_" + new Date().toString().replace(":", ".") + "_" + ytId);
            saveToFile(moviesWithSubtitles);
//         downloadYtPictures(RedditParser.getExistingMovies());
        }
    }

    private static void downloadMoviesAndSubtitles() {
        RedditParser reddirParser = new RedditParser();
        List<Movie> movies = reddirParser.start();

        List<Movie> moviesWithSubtitles = downloadSubs(movies, false);
        saveToFile(moviesWithSubtitles);
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
                    movie.setSubDownloadLocation(Main.SUBTITLES_YOUTUBE_FOLDER + fileName);
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

    private static void saveToFile(List<Movie> movies) {

        for (Movie movie : movies) {
            try {
                String movieString = movie.getName() + "\n" + movie.getYoutubeId() + "\n" + movie.getReleaseYear()
                        + "\n" + (movie.getGenres() != null ? StringUtils.join(movie.getGenres(), ",") : "") + "\n\n";

                Files.write(Paths.get("myfile.txt"), movieString.getBytes(), StandardOpenOption.APPEND);
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