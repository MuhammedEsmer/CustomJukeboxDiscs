package dev.hoodoo.customjukeboxdiscs.content.disc;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.nbt.NBTTagCompound;

public final class TrackReference {
    public static final int MAX_TITLE_CODE_POINTS = 64;
    public static final int MAX_UPLOADER_NAME_CODE_POINTS = 16;
    public static final long MAX_DURATION_MILLIS = 600_000L;

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private final String sha256;
    private final String title;
    private final UUID uploaderUuid;
    private final String uploaderName;
    private final long durationMillis;
    private final AudioFormat format;

    public TrackReference(
            String sha256,
            String title,
            UUID uploaderUuid,
            String uploaderName,
            long durationMillis,
            AudioFormat format) {
        this.uploaderUuid = Objects.requireNonNull(uploaderUuid, "uploaderUuid");
        this.format = Objects.requireNonNull(format, "format");
        requireValid(validateHash(sha256));
        requireValid(validateText("title", title, MAX_TITLE_CODE_POINTS));
        requireValid(validateText("uploader name", uploaderName, MAX_UPLOADER_NAME_CODE_POINTS));
        requireValid(validateDuration(durationMillis));

        this.sha256 = sha256;
        this.title = title;
        this.uploaderName = uploaderName;
        this.durationMillis = durationMillis;
    }

    public String getSha256() {
        return sha256;
    }

    public String sha256() {
        return sha256;
    }

    public String getTitle() {
        return title;
    }

    public String title() {
        return title;
    }

    public UUID getUploaderUuid() {
        return uploaderUuid;
    }

    public UUID uploaderUuid() {
        return uploaderUuid;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public String uploaderName() {
        return uploaderName;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public long durationMillis() {
        return durationMillis;
    }

    public AudioFormat getFormat() {
        return format;
    }

    public AudioFormat format() {
        return format;
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return tag;
    }

    public void writeToNBT(NBTTagCompound tag) {
        tag.setString("sha256", sha256);
        tag.setString("title", title);
        tag.setUniqueId("uploader_uuid", uploaderUuid);
        tag.setString("uploader_name", uploaderName);
        tag.setLong("duration_millis", durationMillis);
        tag.setString("format", format.serializedName());
    }

    public static TrackReference fromNBT(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey("sha256")) {
            return null;
        }
        try {
            String sha256 = tag.getString("sha256");
            String title = tag.getString("title");
            UUID uploaderUuid;
            if (tag.hasUniqueId("uploader_uuid")) {
                uploaderUuid = tag.getUniqueId("uploader_uuid");
            } else if (tag.hasKey("uploader_uuid_most") && tag.hasKey("uploader_uuid_least")) {
                uploaderUuid = new UUID(tag.getLong("uploader_uuid_most"), tag.getLong("uploader_uuid_least"));
            } else {
                uploaderUuid = UUID.fromString(tag.getString("uploader_uuid"));
            }
            String uploaderName = tag.getString("uploader_name");
            long durationMillis = tag.getLong("duration_millis");
            AudioFormat format = AudioFormat.fromSerializedName(tag.getString("format"));
            return new TrackReference(sha256, title, uploaderUuid, uploaderName, durationMillis, format);
        } catch (Exception e) {
            return null;
        }
    }

    public void writeToBuf(ByteBuf buf) {
        writeUtf(buf, sha256, 64);
        writeUtf(buf, title, MAX_TITLE_CODE_POINTS * 4);
        buf.writeLong(uploaderUuid.getMostSignificantBits());
        buf.writeLong(uploaderUuid.getLeastSignificantBits());
        writeUtf(buf, uploaderName, MAX_UPLOADER_NAME_CODE_POINTS * 4);
        buf.writeLong(durationMillis);
        writeUtf(buf, format.serializedName(), 8);
    }

    public static TrackReference fromBuf(ByteBuf buf) {
        String sha256 = readUtf(buf, 64);
        String title = readUtf(buf, MAX_TITLE_CODE_POINTS * 4);
        long most = buf.readLong();
        long least = buf.readLong();
        UUID uploaderUuid = new UUID(most, least);
        String uploaderName = readUtf(buf, MAX_UPLOADER_NAME_CODE_POINTS * 4);
        long durationMillis = buf.readLong();
        AudioFormat format = AudioFormat.fromSerializedName(readUtf(buf, 8));
        return new TrackReference(sha256, title, uploaderUuid, uploaderName, durationMillis, format);
    }

    private static void writeUtf(ByteBuf buf, String value, int maxBytes) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException("String exceeds byte limit: " + bytes.length + " > " + maxBytes);
        }
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }

    private static String readUtf(ByteBuf buf, int maxBytes) {
        int length = buf.readShort();
        if (length < 0 || length > maxBytes) {
            throw new IllegalArgumentException("Invalid string length in buffer: " + length);
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String validateHash(String value) {
        return value != null && SHA_256.matcher(value).matches()
                ? null
                : "sha256 must contain exactly 64 lowercase hexadecimal characters";
    }

    public static String validateText(String field, String value, int maximumCodePoints) {
        if (value == null) {
            return field + " is required";
        }
        int length = value.codePointCount(0, value.length());
        return length >= 1 && length <= maximumCodePoints
                ? null
                : field + " must contain 1-" + maximumCodePoints + " code points";
    }

    public static String validateDuration(long value) {
        return value > 0 && value <= MAX_DURATION_MILLIS
                ? null
                : "duration must be between 1 and " + MAX_DURATION_MILLIS + " milliseconds";
    }

    private static void requireValid(String error) {
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrackReference)) return false;
        TrackReference that = (TrackReference) o;
        return durationMillis == that.durationMillis &&
                Objects.equals(sha256, that.sha256) &&
                Objects.equals(title, that.title) &&
                Objects.equals(uploaderUuid, that.uploaderUuid) &&
                Objects.equals(uploaderName, that.uploaderName) &&
                format == that.format;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sha256, title, uploaderUuid, uploaderName, durationMillis, format);
    }

    @Override
    public String toString() {
        return "TrackReference{" +
                "sha256='" + sha256 + '\'' +
                ", title='" + title + '\'' +
                ", uploaderUuid=" + uploaderUuid +
                ", uploaderName='" + uploaderName + '\'' +
                ", durationMillis=" + durationMillis +
                ", format=" + format +
                '}';
    }
}
