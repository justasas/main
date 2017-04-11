package main;

import java.util.List;

public class SubtitleFile {
	public List<Subtitle> subtitles;
	public String fileName;

	public String location;

	public SubtitleFile(List<Subtitle> subtitles, String fileName, String location)
	{
		this.subtitles = subtitles;
		this.fileName = fileName;
		this.location = location;
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

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
}
