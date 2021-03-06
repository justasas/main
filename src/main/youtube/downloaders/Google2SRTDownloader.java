package main.youtube.downloaders;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import google2srt.Controller;
import google2srt.Settings;
import main.Subtitle;
import main.SubtitlesUtils;

public class Google2SRTDownloader implements YoutubeSubsDownloader {

    public static final String OUTPUT_FOLDER = "subtitles" + File.separator + "youtube" + File.separator;

    public String getSubtitle(String ytMovieURL, boolean autoCaptioned) throws MalformedURLException, IOException {

        try {

            String subtitlesDestinationFolder = this.OUTPUT_FOLDER;

            System.out.println("subtitles destination folder" + subtitlesDestinationFolder);
//			main.Utils.createFolderAndMissingDirectoriesToIt(subtitlesDestinationFolder);

            String[] subtitleFiles = new File(subtitlesDestinationFolder).list();

//			if (subtitleFiles.length == 0) {
            Settings settings = new Settings();
            settings.setURLInput(ytMovieURL);
            settings.setOutput(subtitlesDestinationFolder);
            settings.setIncludeTitleInFilename(true);
            settings.setIncludeTrackNameInFilename(true);
            settings.setRemoveTimingSubtitles(false);
            settings.setDownloadAutoCaptioned(autoCaptioned);

            Controller controller = new Controller(settings);
            controller.processInputURL();
            return controller.convertSubtitlesTracks();
//				controller.convertSubtitlesTracks(title, subtitlesDestinationFolder);
//			}

//            subtitleFiles = new File(subtitlesDestinationFolder).list(); // sitos reikia, nes retrieveSubtitles atsiuncia

//            BufferedReader reader = new BufferedReader(new FileReader(subtitlesDestinationFolder + subtitleFiles[0]));
//            SubtitleFile subtitles = new SubtitleFile(SubtitlesUtils.parseSubtitles(reader), title);

        } catch (Exception ex) {
            System.out.println(ex);
        }

        return null;
        // return SubtitlesUtils.parseSubtitles(in);
    }

    public List<Subtitle> getSubtitlesOld() throws Exception {

        String page = null;
        Pattern p = Pattern.compile("<textarea name= 'srt'(.+\n+)+");

        URL url = new URL("http://www.serpsite.com/transcript.php?videoid=https://www.youtube.com/watch?v=k_3RqAWI-ks");

        HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
        httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");

        BufferedReader in = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));

        String inputLine;
        while (!(inputLine = in.readLine()).startsWith("</textarea>")) {
            inputLine = inputLine + System.getProperty("line.separator");
            page += inputLine;
        }

        Matcher m = p.matcher(page);

        m.find();
        String srt = m.group().replace("<textarea name= 'srt' style = 'display:none;'>", "");

        System.out.println(srt);
        Writer writer = null;

        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(
                                    "/var/www/svetaine/web/uploads/subtitles/movies/fromYoutube/" + "asdd" + ".srt"),
                            "utf-8"));
            writer.write(srt);
        } catch (IOException ex) {
            // report
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {
            }
        }

        return SubtitlesUtils.parseSubtitles(in);
    }
}