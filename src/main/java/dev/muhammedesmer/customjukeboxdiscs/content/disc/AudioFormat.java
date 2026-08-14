package dev.muhammedesmer.customjukeboxdiscs.content.disc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

public enum AudioFormat {
    MP3,
    OGG;

    public static final Codec<AudioFormat> CODEC = Codec.STRING.comapFlatMap(
            AudioFormat::decode,
            AudioFormat::serializedName);

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static AudioFormat fromSerializedName(String value) {
        for (AudioFormat format : values()) {
            if (format.serializedName().equals(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unsupported audio format: " + value);
    }

    private static DataResult<AudioFormat> decode(String value) {
        try {
            return DataResult.success(fromSerializedName(value));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }
}
