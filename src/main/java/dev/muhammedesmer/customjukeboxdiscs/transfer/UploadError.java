package dev.muhammedesmer.customjukeboxdiscs.transfer;

import java.util.Locale;

public enum UploadError {
    NONE,
    PERMISSION_DENIED,
    DENIED_PLAYER,
    ACTIVE_SESSION_LIMIT,
    SIZE_LIMIT,
    UNSUPPORTED_FORMAT,
    INVALID_TITLE,
    URL_NOT_ALLOWED,
    URL_FETCH_FAILED,
    PLAYER_QUOTA,
    SERVER_QUOTA,
    CHUNK_TOO_LARGE,
    WRONG_OFFSET,
    RATE_LIMIT,
    TIMEOUT,
    HASH_MISMATCH,
    MALFORMED_AUDIO,
    DURATION_LIMIT,
    INCOMPLETE_UPLOAD,
    SESSION_NOT_FOUND,
    INVALID_WRITER,
    STORAGE_FAILURE;

    /** {@return the key that explains this failure to the player in their own language} */
    public String translationKey() {
        return "upload.customjukeboxdiscs.error." + name().toLowerCase(Locale.ROOT);
    }
}
