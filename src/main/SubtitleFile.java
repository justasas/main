package main;

import java.util.List;

public class SubtitleFile {
	public List<Subtitle> subtitles;
	public String fileName;
	
	public SubtitleFile(List<Subtitle> subtitles, String fileName)
	{
		this.subtitles = subtitles;
		this.fileName = fileName;
	}
}
