package com.nathanrjones.audiour;

public class AudiourMedia {

    private String mId;
    private String mTitle;
    private String mUrl;
    private String mMp3Url;

    /**
     * Creates a new CastMedia object for the media with the given title and URL.
     */
    public AudiourMedia(String mId, String title, String url, String mp3Url) {
        this.mId = mId;
        this.mTitle = title;
        this.mUrl = url;
        this.mMp3Url = mp3Url;
    }

    public String getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getMp3Url() {
        return mMp3Url;
    }

    public String toString () {
        return mTitle + " (" + mId + ")";
    }
}
