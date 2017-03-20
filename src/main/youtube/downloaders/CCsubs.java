package main.youtube.downloaders;

import main.CacheRepository;
import main.Main;
import main.Movie;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by justas.rutkauskas on 2/18/2017.
 */
public class CCsubs implements YoutubeSubsDownloader {
    public static List<Movie> downloadSubs(List<Movie> movies) {
        CacheRepository cacheRepository = new CacheRepository();
        List<Movie> ret = new ArrayList<Movie>();
        for (Movie movie : movies) {
            try {
                String videoId = movie.getYoutubeId();
                if (cacheRepository.find("noSubsForThisVideo#" + videoId) == null) {
                    sendRequest(videoId);
                    Thread.sleep(2000);
                    boolean downloadSucceed = Main.downloadFile("http://ccsubs.com/video/yt:" + videoId + "/"
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
                cacheRepository.insert("noSubsForThisVideo#" + movie.getYoutubeId(), "y");
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

}
