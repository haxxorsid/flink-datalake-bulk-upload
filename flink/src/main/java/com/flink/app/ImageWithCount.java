package com.flink.app;

import java.util.Date;

// Class for storing result (occurence of each Image) with URL and window end time to store in the SQL database
public class ImageWithCount {
    Image image;
    long count;
    long timeStamp;
    String url;

    public ImageWithCount() {
    }

    public ImageWithCount (Image image, long count, long timeStamp) {
        this.image = image;
        this.count = count;
        this.timeStamp = timeStamp;
    }

    public long getCount() {
        return count;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String str) {
        this.url = str;
    }

    @Override
    public String toString() {
        return "ImageWithCount{" +
                "image=" + image +
                ", count=" + count +
                ", timestamp=" + new Date(timeStamp) +
                ", url=" + url +
                '}';
    }
}
