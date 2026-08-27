package dev.hoodoo.customjukeboxdiscs.network;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class PacketUtils {
    private PacketUtils() {
    }

    public static void writeUtf(ByteBuf buf, String value, int maxBytes) {
        if (value == null) {
            buf.writeShort(-1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException("String exceeds byte limit: " + bytes.length + " > " + maxBytes);
        }
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }

    public static String readUtf(ByteBuf buf, int maxBytes) {
        short length = buf.readShort();
        if (length < 0) {
            return null;
        }
        if (length > maxBytes) {
            throw new IllegalArgumentException("String in packet exceeds max bytes: " + length + " > " + maxBytes);
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeUUID(ByteBuf buf, UUID uuid) {
        if (uuid == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    }

    public static UUID readUUID(ByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        long most = buf.readLong();
        long least = buf.readLong();
        return new UUID(most, least);
    }

    public static void writeByteArray(ByteBuf buf, byte[] bytes) {
        if (bytes == null) {
            buf.writeInt(-1);
        } else {
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        }
    }

    public static byte[] readByteArray(ByteBuf buf, int maxBytes) {
        int length = buf.readInt();
        if (length < 0) {
            return null;
        }
        if (length > maxBytes) {
            throw new IllegalArgumentException("Byte array exceeds limit: " + length + " > " + maxBytes);
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return bytes;
    }
}
