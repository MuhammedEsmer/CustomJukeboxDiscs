package dev.hoodoo.customjukeboxdiscs.content.disc;

import java.util.Locale;

public enum AudioFormat {
    MP3("audio/mpeg", "mp3"),
    OGG("audio/ogg", "ogg");

    private final String mimeType;
    private final String extension;

    AudioFormat(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static AudioFormat fromSerializedName(String value) {
        for (AudioFormat format : values()) {
            if (format.serializedName().equalsIgnoreCase(value) || format.name().equalsIgnoreCase(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unsupported audio format: " + value);
    }
}
