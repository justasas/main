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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SubtitleFile that = (SubtitleFile) o;

		return fileName.equals(that.fileName);

	}

	@Override
	public int hashCode() {
		return fileName.hashCode();
	}
}
