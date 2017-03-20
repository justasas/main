/*
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
 * @version "0.6, 08/11/13"
 */
package google2srtOLD;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Vector;

public class GUI {

    enum tIdioma {
        ca, en, es, it, pt_BR, zh_HanS, zh_HanT
    }

    ;

    private String defaultURL = "";
    // http://www.youtube.com/watch?v=c8RGPpcenZY (4 real tracks)
    // http://www.youtube.com/watch?v=XraeBDMm2PM (5 real tracks with names)
    // http://www.youtube.com/watch?v=IElqf-FCMs8 (EN ASR + EN track)
    // http://www.youtube.com/watch?v=UOfn1cTARrY (ES ASR)
    // http://www.youtube.com/watch?v=PH8JuizIXw8 (EN ASR + many real tracks)

    private List<List<NetSubtitle>> lSubsWithTranslations;

    private boolean msgInfileInvalidFormat, msgIOException;

    public GUI() {
        this.lSubsWithTranslations = new Vector<List<NetSubtitle>>();
        this.lSubsWithTranslations.add(new Vector<NetSubtitle>());
        this.lSubsWithTranslations.add(new Vector<NetSubtitle>());

        // defaultURL;
        // defaultFileOut;

    }

    private void initComponents() {

        this.setLanguage("en");
    }

    private void setLanguage(String s) {

        if (!islSubsWithTranslationsNull()) {
            List<NetSubtitle> _lSubs = this.lSubsWithTranslations.get(1);
            if (_lSubs != null) {
            }
        }
    }

    private boolean islSubsWithTranslationsNull() {
        List<List<NetSubtitle>> swt = this.lSubsWithTranslations;
        return (swt == null || swt.size() < 2 || swt.get(0) == null || swt.get(0).isEmpty() || swt.get(1) == null
                || swt.get(1).isEmpty());
    }

    private void showMsgInfileInvalidFormat() {
        String msg;

        msg = "msg.infile.invalid.format";
        System.out.println(msg);
    }

    public void setMsgInfileInvalidFormat() {
        this.msgInfileInvalidFormat = true;
    }

    private void showMsgIOException() {
        String msg;

        msg = "msg.io.error";
        System.out.println(msg);
    }

    public void setMsgIOException() {
        this.msgIOException = true;
    }

    public void msgFileNoSubtitles() {
        String msg;

        msg = "msg.infile.no.subtitles.found";
        System.out.println(msg);
    }

    public void msgConversionOk() {
        String msg;

        msg = "msg.conversion.done.ok";
        System.out.println(msg);
    }

    public void msgConversionFinished() { // conversion finished (either
        // successfully or not)
        String msg;

        msg = "msg.conversion.finished";
        System.out.println(msg);
    }

    public void msgConversioErrors() {
        String msg;

        msg = "msg.conversion.done.error";
        System.out.println(msg);
    }

    public void prepareNewConversion() {

        if (this.msgIOException)
            this.showMsgIOException();
        if (this.msgInfileInvalidFormat)
            this.showMsgInfileInvalidFormat();

        this.msgIOException = false;
        this.msgInfileInvalidFormat = false;
    }

    public void retrieveSubtitles(String url) {
        String msg;

        this.lSubsWithTranslations = new Vector<List<NetSubtitle>>();
        this.lSubsWithTranslations.add(new Vector<NetSubtitle>());
        this.lSubsWithTranslations.add(new Vector<NetSubtitle>());

        // Check if URL is valid
        try {
            System.out.println("msg.status.connecting");
            lSubsWithTranslations = Network.getSubtitlesWithTranslations(url);
        } catch (Network.HostNoGV e) {
            msg = "msg.url.unknown.host";
            prepareNewConversion();
            return;
        } catch (Network.NoDocId e) {
            msg = "msg.url.parameter.docid.not.found";
            System.out.println(msg);
            prepareNewConversion();
            return;
        } catch (Network.NoQuery e) {
            msg = "msg.url.parameter.not.found";
            System.out.println(msg);
            prepareNewConversion();
            return;
        } catch (Network.InvalidDocId e) {
            msg = "msg.url.parameter.docid.invalid";
            javax.swing.JOptionPane.showMessageDialog(null, msg);
            prepareNewConversion();
            return;
        } catch (Network.NoSubs e) {
            this.msgFileNoSubtitles();
            prepareNewConversion();
            return;
        } catch (MalformedURLException e) {
            msg = "msg.url.invalid.format";
            System.out.println(msg);
            prepareNewConversion();
            return;
        } catch (org.jdom.input.JDOMParseException e) {
            msg = "msg.url.unexpected.format";
            System.out.println(msg);
            prepareNewConversion();
            return;
        } catch (java.net.UnknownHostException e) {
            msg = "msg.net.unknown.host";
            System.out.println(msg);
            prepareNewConversion();
            return;
        } catch (java.io.FileNotFoundException e) {
            msg = "msg.url.does.not.exist";
            System.out.println(msg);
            prepareNewConversion();
        } catch (Network.NoYouTubeParamV e) {
            msg = "msg.net.missing.video.param";
            System.out.println(msg);
            prepareNewConversion();
        } catch (Exception e) {
            msg = "msg.unknown.error";
            System.out.println(msg);
            prepareNewConversion();
            return;
        }

        // No problems found.
        if (!islSubsWithTranslationsNull()) {
        }

        prepareNewConversion();
    }

    public void convertSubtitles(String title, String outputFolder) {
        String msg;
        InputStreamReader input = null;
        Converter conv;
        boolean atLeastOneIsSelected = false;
        String fileName, s;

        try {
            input = new InputStreamReader(new FileInputStream("/home/unknown/workspace/main/subtitles/file.txt"), "UTF-8");
        } catch (FileNotFoundException ex) {
            this.setMsgIOException();
            prepareNewConversion();
            return;
        } catch (java.io.UnsupportedEncodingException ex) {
            System.out.println("(DEBUG) encoding not supported");
            return;
        }

        conv = new Converter(this, input, outputFolder, 0);
        conv.run();
        prepareNewConversion();
        msgConversionFinished();

        List<NetSubtitle> _lSubs = this.lSubsWithTranslations.get(0);

        for (int i = 0; i < _lSubs.size(); i++) {
            if (!_lSubs.get(i).getLang().equals("en"))
                continue;

            try {
                // When handling tracks, it is worth to try
                // signature method FOR EACH track,
                // even when a previous track retrieval via
                // signature method failed
                if (Network.getMagicURL().isEmpty())
                    throw new Exception("No *Magic* URL!");
                Network.setMethod(NetSubtitle.Method.YouTubeSignature);
                input = Network.readURL(_lSubs.get(i).getTrackURL());
                // input.close();
            } catch (Exception ex1) {
                System.out.println("(DEBUG) URL could not be read via Signature method...");
                System.out.println(String.format(
                        "(DEBUG) Method=%s, Type=%s, ID=%s, IDXML=%s, Lang=%s, Name=%s, Exception message='%s'",
                        Network.getMethod(), _lSubs.get(i).getType(), _lSubs.get(i).getId(), _lSubs.get(i).getIdXML(),
                        _lSubs.get(i).getLang(), _lSubs.get(i).getName(), ex1.getMessage()));

                if (_lSubs.get(i).getType() == NetSubtitle.Tipus.YouTubeASRTrack) {
                    // YouTube ASR cannot be retrieved by using
                    // Legacy method.
                    System.out.println(
                            "(DEBUG) YouTube ASR cannot be retrieved via Legacy method. Operation partially aborted.");
                    System.out.println(String.format("(DEBUG) Method=%s, Type=%s, ID=%s, IDXML=%s, Lang=%s, Name=%s",
                            Network.getMethod(), _lSubs.get(i).getType(), _lSubs.get(i).getId(),
                            _lSubs.get(i).getIdXML(), _lSubs.get(i).getLang(), _lSubs.get(i).getName()));

                    continue;
                } else if (_lSubs.get(i).getType() == NetSubtitle.Tipus.YouTubeTrack) {
                    // A YouTube track/target can be retrieved by
                    // using legacy method.
                    // However, GUI should not reach this point with
                    // a target
                    System.out.println("(DEBUG) Switching to YouTube Legacy mode and retrying...");
                    Network.setMethod(NetSubtitle.Method.YouTubeLegacy);

                    try {
                        input = Network.readURL(_lSubs.get(i).getTrackURL(NetSubtitle.Method.YouTubeLegacy));
                    } catch (Exception ex2) {
                        System.out.println(
                                "(DEBUG) URL could not be read with Legacy method. Operation partially aborted");
                        System.out.println(String.format(
                                "(DEBUG) Method=%s, Type=%s, ID=%s, IDXML=%s, Lang=%s, Name=%s, Exception message='%s'",
                                Network.getMethod(), _lSubs.get(i).getType(), _lSubs.get(i).getId(),
                                _lSubs.get(i).getIdXML(), _lSubs.get(i).getLang(), _lSubs.get(i).getName(),
                                ex2.getMessage()));

                        continue;
                    }
                } else {
                    // YouTube Target should not reach this point
                    // due to GUI.
                    // Google Track should not reach this point.
                    System.out.println("(DEBUG) Entered wrong section of code. Unexpected result.");
                    continue;
                }
            }
            fileName = "_" + _lSubs.get(i).getId();
            fileName += "_" + _lSubs.get(i).getIdXML();

            // if (jcbTrackName.isSelected()) {
            // s = _lSubs.get(i).getName();
            // if (s != null)
            // fileName += "_" + s;
            // }

            s = _lSubs.get(i).getLang();
            if (s != null)
                fileName += "_" + s;

            fileName += "_" + title + ".srt";

            conv = new Converter(this, input, Common.removeExtension(outputFolder) + fileName, 0);
            if (!conv.run()) {
                // Conversion failed
                // If Signature method was used and type is Track,
                // let's retry
                // Otherwise, operation is partially aborted
                if (Network.getMethod() == NetSubtitle.Method.YouTubeSignature
                        && _lSubs.get(i).getType() == NetSubtitle.Tipus.YouTubeTrack) {
                    // A YouTube track/target can be retrieved by
                    // using legacy method.
                    System.out.println("(DEBUG) Switching to YouTube Legacy mode and retrying...");
                    Network.setMethod(NetSubtitle.Method.YouTubeLegacy);

                    try {
                        input = Network.readURL(_lSubs.get(i).getTrackURL(NetSubtitle.Method.YouTubeLegacy));
                    } catch (Exception ex11) {
                        System.out.println(
                                "(DEBUG) URL could not be read with Legacy method. Operation partially aborted");
                        System.out.println(String.format(
                                "(DEBUG) Method=%s, Type=%s, ID=%s, IDXML=%s, Lang=%s, Name=%s, Exception message='%s'",
                                Network.getMethod(), _lSubs.get(i).getType(), _lSubs.get(i).getId(),
                                _lSubs.get(i).getIdXML(), _lSubs.get(i).getLang(), _lSubs.get(i).getName(),
                                ex11.getMessage()));

                        continue;
                    }

                    conv = new Converter(this, input, Common.removeExtension(outputFolder) + fileName, 0);

                    conv.run();
                }
            }
        }

        prepareNewConversion();
    }
}
