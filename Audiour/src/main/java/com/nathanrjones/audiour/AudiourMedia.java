package com.nathanrjones.audiour;

public class AudiourMedia {

    private String mTitle;
    private String mVideoUrl;

    /**
     * Creates a new CastMedia object for the media with the given title and URL.
     */
    public AudiourMedia(String title, String videoUrl) {
        mTitle = title;
        mVideoUrl = videoUrl;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getUrl() {
        return mVideoUrl;
    }
}
