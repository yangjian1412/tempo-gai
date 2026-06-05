package com.cappielloantonio.tempo.ui.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.cappielloantonio.tempo.BuildConfig;
import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.helper.ThemeHelper;
import com.cappielloantonio.tempo.interfaces.DialogClickCallback;
import com.cappielloantonio.tempo.interfaces.ScanCallback;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.ui.dialog.DeleteDownloadStorageDialog;
import com.cappielloantonio.tempo.ui.dialog.DownloadStorageDialog;
import com.cappielloantonio.tempo.ui.dialog.StarredSyncDialog;
import com.cappielloantonio.tempo.ui.dialog.StreamingCacheStorageDialog;
import com.cappielloantonio.tempo.util.DownloadUtil;
import com.cappielloantonio.tempo.util.Preferences;
import com.cappielloantonio.tempo.util.UIUtil;
import com.cappielloantonio.tempo.viewmodel.SettingViewModel;

import java.util.Locale;
import java.util.Map;

@OptIn(markerClass = UnstableApi.class)
public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";

    private MainActivity activity;
    private SettingViewModel settingViewModel;

    private ActivityResultLauncher<Intent> someActivityResultLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();

        View view = super.onCreateView(inflater, container, savedInstanceState);
        settingViewModel = new ViewModelProvider(requireActivity()).get(SettingViewModel.class);

        if (view != null) {
            getListView().setPadding(0, 0, 0, (int) getResources().getDimension(R.dimen.global_padding_bottom));
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.setBottomNavigationBarVisibility(false);
        activity.setBottomSheetVisibility(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        checkEqualizer();
        checkCacheStorage();
        checkStorage();

        setStreamingCacheSize();
        setAppLanguage();
        setVersion();

        actionLogout();
        actionScan();
        actionSyncStarredTracks();
        actionChangeStreamingCacheStorage();
        actionChangeDownloadStorage();
        actionDeleteDownloadStorage();
        actionDesktopLyrics();
        actionKeepScreenOn();
        actionLyricsNotificationDependency();
        actionDesktopLyricsDependency();
    }

    @Override
    public void onStop() {
        super.onStop();
        activity.setBottomSheetVisibility(true);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.global_preferences, rootKey);
        ListPreference themePreference = findPreference(Preferences.THEME);
        if (themePreference != null) {
            themePreference.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        String themeOption = (String) newValue;
                        ThemeHelper.applyTheme(themeOption);
                        return true;
                    });
        }
    }

    private void checkEqualizer() {
        Preference equalizer = findPreference("equalizer");

        if (equalizer == null) return;

        Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);

        if ((intent.resolveActivity(requireActivity().getPackageManager()) != null)) {
            equalizer.setOnPreferenceClickListener(preference -> {
                someActivityResultLauncher.launch(intent);
                return true;
            });
        } else {
            equalizer.setVisible(false);
        }
    }

    private void checkCacheStorage() {
        Preference storage = findPreference("streaming_cache_storage");

        if (storage == null) return;

        try {
            if (requireContext().getExternalFilesDirs(null)[1] == null) {
                storage.setVisible(false);
            } else {
                storage.setSummary(Preferences.getDownloadStoragePreference() == 0 ? R.string.download_storage_internal_dialog_negative_button : R.string.download_storage_external_dialog_positive_button);
            }
        } catch (Exception exception) {
            storage.setVisible(false);
        }
    }

    private void checkStorage() {
        Preference storage = findPreference("download_storage");

        if (storage == null) return;

        try {
            if (requireContext().getExternalFilesDirs(null)[1] == null) {
                storage.setVisible(false);
            } else {
                storage.setSummary(Preferences.getDownloadStoragePreference() == 0 ? R.string.download_storage_internal_dialog_negative_button : R.string.download_storage_external_dialog_positive_button);
            }
        } catch (Exception exception) {
            storage.setVisible(false);
        }
    }

    private void setStreamingCacheSize() {
        ListPreference streamingCachePreference = findPreference("streaming_cache_size");

        if (streamingCachePreference != null) {
            streamingCachePreference.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
                @Nullable
                @Override
                public CharSequence provideSummary(@NonNull ListPreference preference) {
                    CharSequence entry = preference.getEntry();

                    if (entry == null) return null;

                    long currentSizeMb = DownloadUtil.getStreamingCacheSize(requireActivity()) / (1024 * 1024);

                    return getString(R.string.settings_summary_streaming_cache_size, entry, String.valueOf(currentSizeMb));
                }
            });
        }
    }

    private void setAppLanguage() {
        ListPreference localePref = (ListPreference) findPreference("language");

        Map<String, String> locales = UIUtil.getLangPreferenceDropdownEntries(requireContext());

        CharSequence[] entries = locales.keySet().toArray(new CharSequence[locales.size()]);
        CharSequence[] entryValues = locales.values().toArray(new CharSequence[locales.size()]);

        localePref.setEntries(entries);
        localePref.setEntryValues(entryValues);

        localePref.setDefaultValue(entryValues[0]);
        localePref.setSummary(Locale.forLanguageTag(localePref.getValue()).getDisplayLanguage());

        localePref.setOnPreferenceChangeListener((preference, newValue) -> {
            LocaleListCompat appLocale = LocaleListCompat.forLanguageTags((String) newValue);
            AppCompatDelegate.setApplicationLocales(appLocale);
            return true;
        });
    }

    private void setVersion() {
        findPreference("version").setSummary(BuildConfig.VERSION_NAME);
    }

    private void actionLogout() {
        findPreference("logout").setOnPreferenceClickListener(preference -> {
            activity.quit();
            return true;
        });
    }

    private void actionScan() {
        findPreference("scan_library").setOnPreferenceClickListener(preference -> {
            settingViewModel.launchScan(new ScanCallback() {
                @Override
                public void onError(Exception exception) {
                    findPreference("scan_library").setSummary(exception.getMessage());
                }

                @Override
                public void onSuccess(boolean isScanning, long count) {
                    findPreference("scan_library").setSummary("Scanning: counting " + count + " tracks");
                    if (isScanning) getScanStatus();
                }
            });

            return true;
        });
    }

    private void actionSyncStarredTracks() {
        findPreference("sync_starred_tracks_for_offline_use").setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue instanceof Boolean) {
                if ((Boolean) newValue) {
                    StarredSyncDialog dialog = new StarredSyncDialog();
                    dialog.show(activity.getSupportFragmentManager(), null);
                }
            }
            return true;
        });
    }

    private void actionChangeStreamingCacheStorage() {
        findPreference("streaming_cache_storage").setOnPreferenceClickListener(preference -> {
            StreamingCacheStorageDialog dialog = new StreamingCacheStorageDialog(new DialogClickCallback() {
                @Override
                public void onPositiveClick() {
                    findPreference("streaming_cache_storage").setSummary(R.string.streaming_cache_storage_external_dialog_positive_button);
                }

                @Override
                public void onNegativeClick() {
                    findPreference("streaming_cache_storage").setSummary(R.string.streaming_cache_storage_internal_dialog_negative_button);
                }
            });
            dialog.show(activity.getSupportFragmentManager(), null);
            return true;
        });
    }

    private void actionChangeDownloadStorage() {
        findPreference("download_storage").setOnPreferenceClickListener(preference -> {
            DownloadStorageDialog dialog = new DownloadStorageDialog(new DialogClickCallback() {
                @Override
                public void onPositiveClick() {
                    findPreference("download_storage").setSummary(R.string.download_storage_external_dialog_positive_button);
                }

                @Override
                public void onNegativeClick() {
                    findPreference("download_storage").setSummary(R.string.download_storage_internal_dialog_negative_button);
                }
            });
            dialog.show(activity.getSupportFragmentManager(), null);
            return true;
        });
    }

    private void actionDeleteDownloadStorage() {
        findPreference("delete_download_storage").setOnPreferenceClickListener(preference -> {
            DeleteDownloadStorageDialog dialog = new DeleteDownloadStorageDialog();
            dialog.show(activity.getSupportFragmentManager(), null);
            return true;
        });
    }

    private void getScanStatus() {
        settingViewModel.getScanStatus(new ScanCallback() {
            @Override
            public void onError(Exception exception) {
                findPreference("scan_library").setSummary(exception.getMessage());
            }

            @Override
            public void onSuccess(boolean isScanning, long count) {
                findPreference("scan_library").setSummary("Scanning: counting " + count + " tracks");
                if (isScanning) getScanStatus();
            }
        });
    }

    private void actionKeepScreenOn() {
        findPreference("always_on_display").setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue instanceof Boolean) {
                if ((Boolean) newValue) {
                    activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
            return true;
        });
    }

    private void actionDesktopLyrics() {
        findPreference("desktop_lyrics").setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue instanceof Boolean) {
                if ((Boolean) newValue) {
                    if (!SettingsFragment.this.hasOverlayPermission()) {
                        SettingsFragment.this.requestOverlayPermission();
                        return false;
                    }
                } else {
                    com.cappielloantonio.tempo.service.DesktopLyricsOverlay.hide();
                }
            }
            return true;
        });
    }

    private void actionLyricsNotificationDependency() {
        androidx.preference.Preference lockScreenPref = findPreference("lyrics_notification_lock_screen");
        androidx.preference.Preference masterPref = findPreference("lyrics_notification");
        if (lockScreenPref == null || masterPref == null) return;
        boolean enabled = masterPref.getSharedPreferences().getBoolean("lyrics_notification", false);
        lockScreenPref.setEnabled(enabled);
        masterPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue instanceof Boolean) {
                lockScreenPref.setEnabled((Boolean) newValue);
            }
            return true;
        });
    }

    private void actionDesktopLyricsDependency() {
        androidx.preference.Preference fontSizePref = findPreference("desktop_lyrics_font_size");
        androidx.preference.Preference colorPref = findPreference("desktop_lyrics_color");
        androidx.preference.Preference bgAlphaPref = findPreference("desktop_lyrics_bg_alpha");
        androidx.preference.Preference lineCountPref = findPreference("desktop_lyrics_line_count");
        androidx.preference.Preference masterPref = findPreference("desktop_lyrics");
        if (masterPref == null) return;
        boolean enabled = masterPref.getSharedPreferences().getBoolean("desktop_lyrics", false);
        if (fontSizePref != null) fontSizePref.setEnabled(enabled);
        if (colorPref != null) colorPref.setEnabled(enabled);
        if (bgAlphaPref != null) bgAlphaPref.setEnabled(enabled);
        if (lineCountPref != null) lineCountPref.setEnabled(enabled);
        masterPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue instanceof Boolean) {
                boolean newEnabled = (Boolean) newValue;
                if (newEnabled) {
                    if (!SettingsFragment.this.hasOverlayPermission()) {
                        SettingsFragment.this.requestOverlayPermission();
                        return false;
                    }
                } else {
                    com.cappielloantonio.tempo.service.DesktopLyricsOverlay.hide();
                }
                if (fontSizePref != null) fontSizePref.setEnabled(newEnabled);
                if (colorPref != null) colorPref.setEnabled(newEnabled);
                if (bgAlphaPref != null) bgAlphaPref.setEnabled(newEnabled);
                if (lineCountPref != null) lineCountPref.setEnabled(newEnabled);
            }
            return true;
        });
    }

    private boolean hasOverlayPermission() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && android.provider.Settings.canDrawOverlays(requireContext());
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + requireContext().getPackageName()));
        try {
            someActivityResultLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            try {
                Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + requireContext().getPackageName()));
                someActivityResultLauncher.launch(fallback);
            } catch (ActivityNotFoundException ignored) {
            }
        }
    }
}
