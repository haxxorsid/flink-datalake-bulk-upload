package com.flink.app;

// Class for Deserializing each Kafka message to an Object
public class Image {
    String name;
    String buffer;
    String encoding;
    String mimetype;

    public Image() {
    }

    public Image(String name, String data, String encoding, String mimetype) {
        this.name = name;
        this.buffer = data;
        this.encoding = encoding;
        this.mimetype = mimetype;
    }

    public String getBuffer() {
        return buffer;
    }

    public String getEncoding() {
        return encoding;
    }

    public String getMimetype() {
        return mimetype;
    }

    public String getName() {
        return name;
    }

    public void setBuffer(String buffer) {
        this.buffer = buffer;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Image{" +
                "name='" + name + '\'' +
                '}';
    }
}
