package google2srt;/*
    This file is part of Google2SRT.

    Google2SRT is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    Google2SRT is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Google2SRT.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @author kom
 * @author Zoltan Kakuszi
 * @version "0.7.4, 10/19/15"
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;


public class Controller {

    private List<Video> videos;                             // List of videos
    private Settings appSettings;                           // Application settings
    private List<List<NetSubtitle>> lSubsWithTranslations;  // Tracks (item 0) + Targets (item 1)
    public void addTracks(List<NetSubtitle> subtitles) {
        lSubsWithTranslations.get(0).addAll(subtitles);
    }

    public void addTargets(List<NetSubtitle> subtitles) {
        lSubsWithTranslations.get(1).addAll(subtitles);
    }

    public List<NetSubtitle> getTracks() {
        return lSubsWithTranslations.get(0);
    }

    public List<NetSubtitle> getTargets() {
        return lSubsWithTranslations.get(1);
    }

    public Controller(Settings settings) {
        this.appSettings = settings;

        initSubtitlesDataStructure();

    }


    // Data structure initialisation
    public final void initSubtitlesDataStructure() {
        lSubsWithTranslations = new ArrayList<List<NetSubtitle>>();
        lSubsWithTranslations.add(new ArrayList<NetSubtitle>());
        lSubsWithTranslations.add(new ArrayList<NetSubtitle>());
    }

    // Parses a text file and returns subtitles for each video URL found
    public void processURLListFile(InputStreamReader isr) {
        videos = Converter.parseURLListFile(isr);
        retrieveSubtitles();
    }

    // Returns subtitles for one video URL
    public void processInputURL() {
        videos = new ArrayList<Video>();
        videos.add(new Video(appSettings.getURLInput()));
        retrieveSubtitles();
    }

    // Returns true if there is not at least 1 track and 1 target
    public boolean islSubsWithTranslationsNull() {
        List<List<NetSubtitle>> swt = lSubsWithTranslations;
        return (swt == null || swt.size() < 2 ||
                swt.get(0) == null || swt.get(0).isEmpty() ||
                swt.get(1) == null || swt.get(1).isEmpty());
    }

    // Retrieves LIST of subtitles from the network
    public void retrieveSubtitles() {
        List<List<NetSubtitle>> al;
        List<Video> invalidVideos;

        initSubtitlesDataStructure();

        invalidVideos = new ArrayList<Video>();

        // If proxy exist in Settings, then set up proxy for each Video
        if (appSettings.isProxySet()) {
            for (Video v : this.videos) {
                v.setProxy(appSettings.getProxyHost(), appSettings.getPort());
            }
        }

        // Check if URL is valid
        for (Video v : this.videos) {
            try {
                al = v.getSubtitlesWithTranslations();
                addTracks(al.get(0)); // lSubsWithTranslations.get(0).addAll(al.get(0));
                if (getTargets().isEmpty()) // Only add targets of the *first video with targets* - technically wrong, it makes sense in practice
                    addTargets(al.get(1)); // lSubsWithTranslations.get(1).addAll(al.get(1));
            } catch (Video.HostNoGV e) {
                invalidVideos.add(v);
                continue;
            } catch (Video.NoDocId e) {
                invalidVideos.add(v);
                continue;
            } catch (Video.NoQuery e) {
                invalidVideos.add(v);
                continue;
            } catch (Video.InvalidDocId e) {
                invalidVideos.add(v);
                continue;
            } catch (Video.NoSubs e) {
                invalidVideos.add(v);
                continue;
            } catch (MalformedURLException e) {
                invalidVideos.add(v);
                continue;
            } catch (org.jdom.input.JDOMParseException e) {
                invalidVideos.add(v);
                continue;
            } catch (java.net.UnknownHostException e) {
                invalidVideos.add(v);
                continue;
            } catch (FileNotFoundException e) {
                invalidVideos.add(v);
                continue;
            } catch (Video.NoYouTubeParamV e) {
                invalidVideos.add(v);
                continue;
            } catch (SocketException e) {
                invalidVideos.add(v);
                continue;
            } catch (Exception e) {
                invalidVideos.add(v);
                continue;
            }
        }

        // Removing invalid "videos" (text lines not containing a URL or a video without subtitles)
        for (Video v : invalidVideos)
            this.videos.remove(v);
    }


    // Converts an XML file to SRT
    public void convertSubtitlesXML() {
        String fileName;
        Converter conv;
        InputStreamReader isr = null;

        try {
            isr = new InputStreamReader(new FileInputStream(appSettings.getFileInput()), "UTF-8");
        } catch (FileNotFoundException ex) {
            return;
        } catch (java.io.UnsupportedEncodingException ex) {
            if (Settings.DEBUG) System.out.println("(DEBUG) encoding not supported");
        }

        fileName = Common.removeExtension((new File(appSettings.getFileInput())).getName()) + ".srt";

        conv = new Converter(
                isr,
                Common.returnDirectory(appSettings.getOutput()) + fileName,
                appSettings.getRemoveTimingSubtitles());
        conv.run();
    }

    // Downloads multiple tracks from the network and converts them to SRT
    public String convertSubtitlesTracks() {
        Converter conv;
        Video v;

        String fileName, s;
        List<NetSubtitle> lTracks;

        InputStreamReader isr;

        lTracks = this.getTracks();

        v = getTracks().get(0).getVideo();

        for (int i = 0; i < lTracks.size(); i++) {
            if (!lTracks.get(i).getLang().equals("en"))
                continue;

            if ((appSettings.isDownloadAutoCaptioned() && lTracks.get(i).getType().equals(NetSubtitle.Tipus.YouTubeASRTrack))
                    || (!appSettings.isDownloadAutoCaptioned() && lTracks.get(i).getType().equals(NetSubtitle.Tipus.YouTubeTrack))) {
                try {
                    if (v.getMagicURL().isEmpty()) throw new Exception("No *Magic* URL!");
                    v.setMethod(NetSubtitle.Method.YouTubeSignature);
                    isr = v.readURL(lTracks.get(i).getTrackURL());
                } catch (Exception ex1) {
                    if (Settings.DEBUG) {
                        System.out.println("(DEBUG) URL could not be read via Signature method...");
                        System.out.println(
                                String.format("(DEBUG) Method=%s, Type=%s, ID=%s, IDXML=%s, Lang=%s, Name=%s, Exception message='%s'",
                                        v.getMethod(),
                                        lTracks.get(i).getType(),
                                        lTracks.get(i).getId(),
                                        lTracks.get(i).getIdXML(),
                                        lTracks.get(i).getLang(),
                                        lTracks.get(i).getName(),
                                        ex1.getMessage()));
                    }

                    if (lTracks.get(i).getType() == NetSubtitle.Tipus.YouTubeASRTrack) {
                        // YouTube ASR cannot be retrieved by using Legacy method.
                        if (Settings.DEBUG) {
                            System.out.println("(DEBUG) YouTube ASR cannot be retrieved via Legacy method. Operation partially aborted.");
                            System.out.println(
                                    String.format("(DEBUG) Method=%s, Type=%s, ID=%s, IDXML=%s, Lang=%s, Name=%s",
                                            v.getMethod(),
                                            lTracks.get(i).getType(),
                                            lTracks.get(i).getId(),
                                            lTracks.get(i).getIdXML(),
                                            lTracks.get(i).getLang(),
                                            lTracks.get(i).getName()));
                        }
                        return null;
                    } else if (lTracks.get(i).getType() == NetSubtitle.Tipus.YouTubeTrack) {
                        // A YouTube track/target can be retrieved by using legacy method.
                        // However, GUI should not reach this point with a target
                        if (Settings.DEBUG)
                            System.out.println("(DEBUG) Switching to YouTube Legacy mode and retrying...");
                        v.setMethod(NetSubtitle.Method.YouTubeLegacy);

                        try {
                            isr = v.readURL(lTracks.get(i).getTrackURL(NetSubtitle.Method.YouTubeLegacy));
                        } catch (Exception ex2) {
                            if (Settings.DEBUG) {
                                System.out.println("(DEBUG) URL could not be read with Legacy method. Operation partially aborted");
                                System.out.println(
                                        String.format("(DEBUG) Method=%s, Type=%s, ID=%s, IDXML=%s, Lang=%s, Name=%s, Exception message='%s'",
                                                v.getMethod(),
                                                lTracks.get(i).getType(),
                                                lTracks.get(i).getId(),
                                                lTracks.get(i).getIdXML(),
                                                lTracks.get(i).getLang(),
                                                lTracks.get(i).getName(),
                                                ex2.getMessage()));
                            }
                            return null;
                        }
                    } else {
                        // YouTube Target should not reach this point due to GUI.
                        // Google Track should not reach this point.
                        if (Settings.DEBUG)
                            System.out.println("(DEBUG) Entered wrong section of code. Unexpected result.");
                        return null;
                    }
                }

                fileName = "";
                if (appSettings.getIncludeTitleInFilename()) {
                    s = Common.removaInvalidFileNameChars(v.getTitle());
                    if (s != null)
                        fileName += s + "_";
                }

                fileName += lTracks.get(i).getId();
                fileName += "_" + lTracks.get(i).getIdXML();

                if (appSettings.getIncludeTrackNameInFilename()) {
                    s = lTracks.get(i).getName();
                    if (s != null)
                        fileName += "_" + s;
                }

                s = lTracks.get(i).getLang();
                if (s != null)
                    fileName += "_" + s;

                fileName += ".srt";


                conv = new Converter(
                        isr,
                        Common.returnDirectory(appSettings.getOutput()) + fileName,
                        appSettings.getRemoveTimingSubtitles());

                if (!conv.run()) {
                    // Conversion failed
                    // If Signature method was used and type is Track, let's retry
                    // Otherwise, operation is partially aborted
                    if (v.getMethod() == NetSubtitle.Method.YouTubeSignature &&
                            lTracks.get(i).getType() == NetSubtitle.Tipus.YouTubeTrack) {
                        // A YouTube track/target can be retrieved by using legacy method.
                        if (Settings.DEBUG)
                            System.out.println("(DEBUG) Switching to YouTube Legacy mode and retrying...");
                        v.setMethod(NetSubtitle.Method.YouTubeLegacy);

                        try {
                            isr = v.readURL(lTracks.get(i).getTrackURL(NetSubtitle.Method.YouTubeLegacy));
                        } catch (Exception ex1) {
                            if (Settings.DEBUG) {
                                System.out.println("(DEBUG) URL could not be read with Legacy method. Operation partially aborted");
                                System.out.println(
                                        String.format("(DEBUG) Method=%s, Type=%s, ID=%s, IDXML=%s, Lang=%s, Name=%s, Exception message='%s'",
                                                v.getMethod(),
                                                lTracks.get(i).getType(),
                                                lTracks.get(i).getId(),
                                                lTracks.get(i).getIdXML(),
                                                lTracks.get(i).getLang(),
                                                lTracks.get(i).getName(),
                                                ex1.getMessage()));
                            }
                            return null;
                        }

                        conv = new Converter(
                                isr,
                                Common.returnDirectory(appSettings.getOutput()) + fileName, //Common.removeExtension(appSettings.getOutput()) + fileName, // jtfOutput.getText()
                                appSettings.getRemoveTimingSubtitles());

                        conv.run();
                        return fileName;
                    }
                }
                return fileName;
            }
        }
        return null;
    }
}