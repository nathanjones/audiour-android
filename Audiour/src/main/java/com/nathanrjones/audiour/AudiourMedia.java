package com.nathanrjones.audiour;

import java.util.List;

public class AudiourMedia {

    public String AudioFileId;
    public String Title;
    public String Description;
    public String Duration;
    public String Url;
    public String Mp3Url;
    public boolean Starred;
    public List<Integer> Soundwave;

    public AudiourMedia() {
    }

    public AudiourMedia(String audioFileId, String title, String url, String mp3Url) {
        AudioFileId = audioFileId;
        Title = title;
        Url = url;
        Mp3Url = mp3Url;
    }
}
