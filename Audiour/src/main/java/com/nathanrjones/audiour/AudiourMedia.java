package com.nathanrjones.audiour;

public class AudiourMedia {

    private String mId;
    private String mTitle;
    private String mVideoUrl;

    /**
     * Creates a new CastMedia object for the media with the given title and URL.
     */
    public AudiourMedia(String mId, String title, String videoUrl) {
        this.mId = mId;
        this.mTitle = title;
        this.mVideoUrl = videoUrl;
    }

    public String getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getUrl() {
        return mVideoUrl;
    }

    public String toString () {
        return mTitle + " (" + mId + ")";
    }
}
