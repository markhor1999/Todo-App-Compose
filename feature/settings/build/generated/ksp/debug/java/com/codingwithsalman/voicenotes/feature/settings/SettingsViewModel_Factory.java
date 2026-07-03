package com.codingwithsalman.voicenotes.feature.settings;

import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator;
import com.codingwithsalman.voicenotes.core.datastore.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SettingsRepository> settingsProvider;

  private final Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider;

  public SettingsViewModel_Factory(Provider<SettingsRepository> settingsProvider,
      Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider) {
    this.settingsProvider = settingsProvider;
    this.transcriptionCoordinatorProvider = transcriptionCoordinatorProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(settingsProvider.get(), transcriptionCoordinatorProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<SettingsRepository> settingsProvider,
      Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider) {
    return new SettingsViewModel_Factory(settingsProvider, transcriptionCoordinatorProvider);
  }

  public static SettingsViewModel newInstance(SettingsRepository settings,
      TranscriptionCoordinator transcriptionCoordinator) {
    return new SettingsViewModel(settings, transcriptionCoordinator);
  }
}
