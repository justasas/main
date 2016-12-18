package main;

public class TwoSimiliarSubtitle {
	public int identicalWordsCount;
	public int ytSubtitleIndex;
	public int subtitleIndex;
	public Subtitle subtitle;
	public Subtitle ytSubtitle;
	
	public TwoSimiliarSubtitle(int identicalWordsCount, int ytSubtitleIndex, int subtitleIndex, Subtitle subtitle, Subtitle ytSubtitle)
	{
		this.identicalWordsCount = identicalWordsCount;
		this.ytSubtitleIndex = ytSubtitleIndex;
		this.subtitleIndex = subtitleIndex;
		this.subtitle = subtitle;
		this.ytSubtitle = ytSubtitle;
	}
}