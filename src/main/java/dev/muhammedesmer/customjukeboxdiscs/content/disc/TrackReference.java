package dev.muhammedesmer.customjukeboxdiscs.content.disc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record TrackReference(
        String sha256,
        String title,
        UUID uploaderUuid,
        String uploaderName,
        long durationMillis,
        AudioFormat format) {
    public static final int MAX_TITLE_CODE_POINTS = 64;
    public static final int MAX_UPLOADER_NAME_CODE_POINTS = 16;
    public static final long MAX_DURATION_MILLIS = 600_000L;

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Codec<String> HASH_CODEC = validatedString(TrackReference::validateHash);
    private static final Codec<String> TITLE_CODEC = validatedString(value -> validateText(
            "title", value, MAX_TITLE_CODE_POINTS));
    private static final Codec<String> UPLOADER_NAME_CODEC = validatedString(value -> validateText(
            "uploader name", value, MAX_UPLOADER_NAME_CODE_POINTS));
    private static final Codec<Long> DURATION_CODEC = Codec.LONG.validate(TrackReference::validateDurationResult);

    public static final Codec<TrackReference> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            HASH_CODEC.fieldOf("sha256").forGetter(TrackReference::sha256),
            TITLE_CODEC.fieldOf("title").forGetter(TrackReference::title),
            UUIDUtil.CODEC.fieldOf("uploader_uuid").forGetter(TrackReference::uploaderUuid),
            UPLOADER_NAME_CODEC.fieldOf("uploader_name").forGetter(TrackReference::uploaderName),
            DURATION_CODEC.fieldOf("duration_millis").forGetter(TrackReference::durationMillis),
            AudioFormat.CODEC.fieldOf("format").forGetter(TrackReference::format)
    ).apply(instance, TrackReference::new));

    public static final StreamCodec<FriendlyByteBuf, TrackReference> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TrackReference decode(FriendlyByteBuf buffer) {
            return new TrackReference(
                    buffer.readUtf(64),
                    buffer.readUtf(MAX_TITLE_CODE_POINTS * 4),
                    buffer.readUUID(),
                    buffer.readUtf(MAX_UPLOADER_NAME_CODE_POINTS * 4),
                    buffer.readVarLong(),
                    AudioFormat.fromSerializedName(buffer.readUtf(3)));
        }

        @Override
        public void encode(FriendlyByteBuf buffer, TrackReference value) {
            buffer.writeUtf(value.sha256, 64);
            buffer.writeUtf(value.title, MAX_TITLE_CODE_POINTS * 4);
            buffer.writeUUID(value.uploaderUuid);
            buffer.writeUtf(value.uploaderName, MAX_UPLOADER_NAME_CODE_POINTS * 4);
            buffer.writeVarLong(value.durationMillis);
            buffer.writeUtf(value.format.serializedName(), 3);
        }
    };

    public TrackReference {
        Objects.requireNonNull(uploaderUuid, "uploaderUuid");
        Objects.requireNonNull(format, "format");
        requireValid(validateHash(sha256));
        requireValid(validateText("title", title, MAX_TITLE_CODE_POINTS));
        requireValid(validateText("uploader name", uploaderName, MAX_UPLOADER_NAME_CODE_POINTS));
        requireValid(validateDuration(durationMillis));
    }

    private static Codec<String> validatedString(Function<String, String> validator) {
        return Codec.STRING.validate(value -> {
            String error = validator.apply(value);
            return error == null ? DataResult.success(value) : DataResult.error(() -> error);
        });
    }

    private static String validateHash(String value) {
        return value != null && SHA_256.matcher(value).matches()
                ? null
                : "sha256 must contain exactly 64 lowercase hexadecimal characters";
    }

    private static String validateText(String field, String value, int maximumCodePoints) {
        if (value == null) {
            return field + " is required";
        }
        int length = value.codePointCount(0, value.length());
        return length >= 1 && length <= maximumCodePoints
                ? null
                : field + " must contain 1-" + maximumCodePoints + " code points";
    }

    private static String validateDuration(long value) {
        return value > 0 && value <= MAX_DURATION_MILLIS
                ? null
                : "duration must be between 1 and " + MAX_DURATION_MILLIS + " milliseconds";
    }

    private static DataResult<Long> validateDurationResult(long value) {
        String error = validateDuration(value);
        return error == null ? DataResult.success(value) : DataResult.error(() -> error);
    }

    private static void requireValid(String error) {
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
    }
}
