package main;

import org.apache.commons.exec.util.StringUtils;
import org.jsoup.Jsoup;
import org.junit.Test;

import static java.lang.Math.abs;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SubtitlesUtils {

	public static List<Subtitle> parseSubtitles(BufferedReader reader) throws IOException {

		List<Subtitle> subtitles = new ArrayList<>();
		String[] startEnd = null;

		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.matches("^[0-9]+$")) {
				// System.out.println(line);
				Subtitle sub = new Subtitle();
				startEnd = reader.readLine().split(" --> ");
				sub.start = startEnd[0];
				sub.end = startEnd[1];
				while ((line = reader.readLine()) != null && !line.isEmpty()) {
					sub.text = sub.text + " " + Jsoup.parse(line).text();
				}
				// System.out.println();
				subtitles.add(sub);
				// System.out.println(sub.start + " --> " + sub.end + " " +
				// sub.subtitle);
			}
		}

		return subtitles;
	}

	public static void copyFile(File sourceFile, File destFile) throws IOException {
		if (!destFile.exists()) {
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;

		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}

	public static int srtSubToMilli(String srt) {
		String[] start = srt.split(":");// Subs.get(i).start.split(":");

		int startHours = Integer.parseInt(start[0]);
		int startMins = Integer.parseInt(start[1]);
		int startSecs = Integer.parseInt(start[2].split(",")[0]);
//		String StartMilli = "000";
//		int startMilli;
//		if (!start[2].endsWith(",")) {
//			StartMilli = start[2].split(",")[1];
//			startMilli = Integer.parseInt(StartMilli);
//		} else {
//			startMilli = 0;
//		}
//
//		if (StartMilli.length() == 2) {
//			startMilli *= 10;
//		} else {
//			if (StartMilli.length() == 1) {
//				startMilli *= 100;
//			}
//		}
		return srtToMilli(startHours, startMins, startSecs, 0);
	}

	public static int srtToMilli(int hours, int mins, int secs, int milli) {
		return hours * 360000 + mins * 60000 + secs * 1000 + milli;
	}

	public static String milliToSrt(int time) {
		String srt;
		int hours, mins, secs, milli = 0;

		if ((hours = time / 3600000) > 0) {
			time = time % 3600000;
		}

		if ((mins = time / 60000) > 0) {
			time = time % 60000;
		}

		if ((secs = time / 1000) > 0) {
			time = time % 1000;
		}

		milli = time;
		String milliString = "";
		if (milli < 10) {
			milliString = "00" + milli;
		} else {
			if (milli < 100) {
				milliString = "0" + milli;
			} else {
				milliString = "" + milli;
			}
		}

		srt = "0" + hours + ":" + (mins > 9 ? mins : "0" + mins) + ":" + (secs > 9 ? secs : "0" + secs) + ","
				+ milliString;
		return srt;
	}

	private void printSubtitles(SubtitleFile subs, List<Subtitle> ytSubtitles) {
		List<Subtitle> subtitles = subs.subtitles;

		for (int i = 0; i < 200; i++) {
			System.out.println("Y: " + ytSubtitles.get(i).start + " --> " + ytSubtitles.get(i).end + " "
					+ ytSubtitles.get(i).text);
			System.out.println(
					subtitles.get(i + 1).start + " --> " + subtitles.get(i + 1).end + " " + subtitles.get(i + 1).text);
			System.out.println();
		}
	}

	public static Integer findAvarageTimesDiff(List<Subtitle> subs, List<Subtitle> ySubs) {

		List<TwoSimiliarSubtitle> similiarSubtitlesList = findSimiliarSubtitles(subs, ySubs);

		System.out.println("Similiar subtitles count: " + similiarSubtitlesList.size());

		System.out.println("\n\ndifferences:\n");

		int foundCount = 0;
		int sum = 0;

		for (TwoSimiliarSubtitle twoSubtitles : similiarSubtitlesList) {

			Subtitle ytSubtitle = twoSubtitles.ytSubtitle;
			Subtitle subtitle = twoSubtitles.subtitle;

			int indDifference = abs(twoSubtitles.subtitleIndex - twoSubtitles.ytSubtitleIndex);
			int startTimesDifference = srtSubToMilli(ytSubtitle.start) - srtSubToMilli(subtitle.start);
			int endTimesDifference = srtSubToMilli(ytSubtitle.end) - srtSubToMilli(subtitle.end);

			System.out.println("indDiff: " + indDifference);
			System.out.println("start times(yt,orig):       YT:" + ytSubtitle.start + "     ORIG:" + subtitle.start + "          DIFF:" + + startTimesDifference);
			System.out.println("ytSub ( " + twoSubtitles.ytSubtitleIndex + " ) :" + ytSubtitle.text);
			System.out.println("sub   ( " + twoSubtitles.subtitleIndex + " ) :" + subtitle.text);
			System.out.println();
//			indDiffSum += indDifference;

			foundCount++;

			sum += startTimesDifference;
		}

		System.out.println(foundCount);
		return similiarSubtitlesList.size() > 0 ? (sum / similiarSubtitlesList.size()) : Integer.MAX_VALUE;

		// mostFrequent(startAndEndsMilli);
		// moveSubtitles(subsToMove, subs);
		// System.out.println("max: "+ max + " index: " + index+ " time: "+ time
		// + " diff: "+ maxDiff);
	}

	private static List<TwoSimiliarSubtitle> findSimiliarSubtitles(List<Subtitle> subs, List<Subtitle> ySubs) {

		List<TwoSimiliarSubtitle> ret = new ArrayList<TwoSimiliarSubtitle>();

		final int SUBS_INDEXES_DISTANCE = 100;
		System.out.println("i: " + subs.size());
		System.out.println("y: " + ySubs.size());

		int biggestI = 0;
		int biggestY = 0;


		aa: for (int i = SUBS_INDEXES_DISTANCE; i < subs.size(); i++) {

			if(i > biggestI)
				biggestI = i;

			for (int y = i - SUBS_INDEXES_DISTANCE; (y < i + SUBS_INDEXES_DISTANCE) && y < ySubs.size(); y++) {

				if(y > biggestY)
					biggestY = y;

				Subtitle ytSubtitle = ySubs.get(y);
				Subtitle subtitle = subs.get(i);

				String ytWordsConjuction = "";
				for(int c = y; (c < y+1) && (y+3 < ySubs.size()); c++) {
					ytWordsConjuction += ySubs.get(c).text + " ";
				}
				int identicalWordsCount = checkIfSimiliar(ytWordsConjuction, subtitle.text);
				if (identicalWordsCount > 0) {
					Subtitle ytConjunctionSubtitle = new Subtitle();
					ytConjunctionSubtitle.text = ytWordsConjuction;
					ytConjunctionSubtitle.start = ytSubtitle.start;
					ytConjunctionSubtitle.end = ytSubtitle.end;
					TwoSimiliarSubtitle twoSimiliarSubtitle = new TwoSimiliarSubtitle(identicalWordsCount, i, y,
							subtitle, ytConjunctionSubtitle);
					ret.add(twoSimiliarSubtitle);
					break;
				}
			}

		}
		 System.out.println("biggestI: " + biggestI);
		 System.out.println("biggestY: " + biggestY);

		return ret;
	}
//
//	private static List<TwoSimiliarSubtitle> findSimiliarSubtitles(List<Subtitle> subs, List<Subtitle> ySubs) {
//
//		List<TwoSimiliarSubtitle> ret = new ArrayList<TwoSimiliarSubtitle>();
//
//		final int SUBS_INDEXES_DISTANCE = 100;
//		System.out.println("i: " + subs.size());
//		System.out.println("y: " + ySubs.size());
//
//		int biggestI = 0;
//		int biggestY = 0;
//
//
//		aa: for (int i = SUBS_INDEXES_DISTANCE; i < subs.size(); i++) {
//
//			if(i > biggestI)
//				biggestI = i;
//
//			for (int y = i - SUBS_INDEXES_DISTANCE; (y < i + SUBS_INDEXES_DISTANCE) && y < ySubs.size(); y++) {
//
//				if(y > biggestY)
//					biggestY = y;
//
//				Subtitle ytSubtitle = ySubs.get(y);
//				Subtitle subtitle = subs.get(i);
//
//				int identicalWordsCount = checkIfSimiliar(ytSubtitle.text, subtitle.text);
//				if (identicalWordsCount > 0) {
//					TwoSimiliarSubtitle twoSimiliarSubtitle = new TwoSimiliarSubtitle(identicalWordsCount, i, y,
//							subtitle, ytSubtitle);
//					ret.add(twoSimiliarSubtitle);
//					break;
//				}
//			}
//
//		}
//		System.out.println("biggestI: " + biggestI);
//		System.out.println("biggestY: " + biggestY);
//
//		return ret;
//	}


	@Test
	public void testCheckIfSimiliar()
	{
		Subtitle subtitle = new Subtitle();
		subtitle.text = "I wish I could say the same.";

		Subtitle ytSubtitle = new Subtitle();
		ytSubtitle.text = "I wish I could say the same";

		assertNotNull(checkIfSimiliar(ytSubtitle.text, subtitle.text));
	}

	public static int checkIfSimiliar(String ytSubtitle, String subtitle) {
		final int MIN_SUB_WORD_COUNT = 7;
		final int MIN_IDENTICAL_WORDS_COUNT = 7;
		final int MIN_WORD_LENGTH = 1;

		String[] ytSubWords = ytSubtitle.trim().replaceAll("[^a-zA-Z ']+", "").split("[ ]+");
		String[] subWords = subtitle.trim().replaceAll("[^a-zA-Z ']+", "").split("[ ]+");

		int ytSubWordCount = ytSubWords.length;
		int subWordCount = subWords.length;

		if (subWordCount < MIN_SUB_WORD_COUNT)
            return 0;
		if(ytSubWordCount < MIN_SUB_WORD_COUNT)
            return 0;
//				if(subtitle.text.split(" ").length != ytSubtitle.text.split(" ").length)
//					continue;

		List<String> ytSubWordsList = removeWordsShorterThan(MIN_WORD_LENGTH, ytSubWords);
		List<String> subWordsList = removeWordsShorterThan(MIN_WORD_LENGTH, subWords);

		int identicalWordsCount = calculateCountOfSameWords(ytSubWordsList, subWordsList);

//				if (identicalWordsCount > MIN_IDENTICAL_WORDS_COUNT && (subWordsList.size() == identicalWordsCount || ytSubWordsList.size() == identicalWordsCount)) {
		if (identicalWordsCount >= MIN_IDENTICAL_WORDS_COUNT) {
            return identicalWordsCount;
        }
		return 0;
	}

	private static List<String> removeWordsShorterThan(final int MIN_WORD_LENGTH, String[] ytSubWords) {
	
		List<String> ytWordsList = new LinkedList<>(Arrays.asList(ytSubWords));
		
		Iterator<String> it = ytWordsList.iterator();
		
		while(it.hasNext()){
			if(it.next().length() < MIN_WORD_LENGTH)
			{
				it.remove();
			}
		}
		
		return ytWordsList;
	}

	private static void intersectedWordsCount(final int MIN_IDENTICAL_WORDS_COUNT, Set<String> ytSubWordsSet,
			Set<String> subWordsSet) {
		// Set<String> ytSubWordsSet = new HashSet<>(Arrays.asList(ytSubWords));
		// Set<String> subWordsSet= new HashSet<>(Arrays.asList(subWords));

		Set<String> result = new HashSet<>(ytSubWordsSet);
		result.retainAll(subWordsSet);

		int shortWordsCount = 0;
		for (String r : result)
			shortWordsCount++;

		if (result.size() - shortWordsCount > MIN_IDENTICAL_WORDS_COUNT)
			System.out.println("INTERSECTION size: " + result.size());
	}

	private static void printSubsTimeDifferences(Subtitle ytSubtitle, Subtitle subtitle) {
		System.out.print(ytSubtitle.start + " --> " + ytSubtitle.end + "   Y:  ");
		System.out.println(ytSubtitle.text);
		System.out.print(subtitle.start + " --> " + subtitle.end + "  ");
		System.out.println(subtitle.text);
	}

	private static int calculateCountOfSameWords(List<String> ytSubWordsList, List<String> subWordsList) {

		int count = 0;

		Set<Integer> indexesToSkip = new HashSet<>();

		aa : for (String ytWord : ytSubWordsList) {

			int i = -1;

			for (String word : subWordsList) {

				if (indexesToSkip.contains(++i))
					continue;

				word = word.replace("&#39;", "'");

//				String pattern = word + "[^ ]+?";

				if (ytWord.contains(word)) {
					indexesToSkip.add(i);
					count++;
					continue aa;
				}
			}
		}

		return count;
	}

	public static void writeSubtitlesToFile(List<Subtitle> subs, String fileName)
			throws FileNotFoundException, UnsupportedEncodingException {
		int nr = 1;
		PrintWriter writer = new PrintWriter(fileName, "UTF-8");
		for (Subtitle sub : subs) {
			writer.println(nr);
			writer.println(sub.start + " --> " + sub.end);
			writer.println(sub.text);
			writer.println();
			nr++;
			// System.out.println(Sub.start + " --> "+ Sub.end);
			// System.out.println("Milli: start -
			// "+srtSubToMilli(Sub.start)+"end - "+srtSubToMilli(Sub.end));
			// System.out.println("Srt: "+milliToSrt(srtSubToMilli(Sub.start))+"
			// --> "+milliToSrt(srtSubToMilli(Sub.end)));
			// System.out.println();
		}
		writer.close();
	}

	private int mostFrequent(List<Integer> vector) {
		int max = 0;
		int index = -1;
		int time = 0;
		int diff = 0;
		int maxDiff = 0;
		for (int u = 0; u < vector.size(); u++) {
			int l = 0;
			int temp = vector.get(u);
			for (int t = 0; t < vector.size(); t++) {
				diff = temp - vector.get(t);
				if (diff < 100 && diff > -100) {
					l++;
				}
			}
			if (l > max) {
				maxDiff = diff;
				max = l;
				index = u;
				time = temp;
			}
		}
		return diff;
	}

}
