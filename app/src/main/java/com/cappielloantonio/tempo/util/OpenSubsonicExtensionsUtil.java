package com.cappielloantonio.tempo.util;

import com.cappielloantonio.tempo.subsonic.models.OpenSubsonicExtension;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;

public class OpenSubsonicExtensionsUtil {
    public static void resetCache() {
        cachedExtensions = null;
    }

    private static List<OpenSubsonicExtension> cachedExtensions = null;

    private static List<OpenSubsonicExtension> getOpenSubsonicExtensions() {
        if (cachedExtensions != null) return cachedExtensions;

        if (Preferences.getOpenSubsonicExtensions() != null) {
            try {
                OpenSubsonicExtension[] array = new Gson().fromJson(
                        Preferences.getOpenSubsonicExtensions(),
                        OpenSubsonicExtension[].class
                );
                if (array != null) {
                    cachedExtensions = Arrays.asList(array);
                }
            } catch (Exception ignored) {
            }
        }

        return cachedExtensions;
    }

    private static OpenSubsonicExtension getOpenSubsonicExtension(String extensionName) {
        if (getOpenSubsonicExtensions() == null) return null;

        return getOpenSubsonicExtensions().stream().filter(openSubsonicExtension -> openSubsonicExtension.getName().equals(extensionName)).findAny().orElse(null);
    }

    public static boolean isTranscodeOffsetExtensionAvailable() {
        return getOpenSubsonicExtension("transcodeOffset") != null;
    }

    public static boolean isFormPostExtensionAvailable() {
        return getOpenSubsonicExtension("formPost") != null;
    }

    public static boolean isSongLyricsExtensionAvailable() {
        return getOpenSubsonicExtension("songLyrics") != null;
    }
}
