package com.codingwithsalman.voicenotes.asr.sherpa;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.codingwithsalman.voicenotes.core.database.NotesRepository;
import com.codingwithsalman.voicenotes.core.datastore.EntitlementStore;
import com.codingwithsalman.voicenotes.core.datastore.SettingsRepository;
import dagger.internal.DaggerGenerated;
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
public final class TranscriptionWorker_Factory {
  private final Provider<SherpaTranscriptionEngine> engineProvider;

  private final Provider<NotesRepository> repositoryProvider;

  private final Provider<SettingsRepository> settingsProvider;

  private final Provider<EntitlementStore> entitlementStoreProvider;

  private final Provider<TranscriptionProgressBus> progressBusProvider;

  public TranscriptionWorker_Factory(Provider<SherpaTranscriptionEngine> engineProvider,
      Provider<NotesRepository> repositoryProvider, Provider<SettingsRepository> settingsProvider,
      Provider<EntitlementStore> entitlementStoreProvider,
      Provider<TranscriptionProgressBus> progressBusProvider) {
    this.engineProvider = engineProvider;
    this.repositoryProvider = repositoryProvider;
    this.settingsProvider = settingsProvider;
    this.entitlementStoreProvider = entitlementStoreProvider;
    this.progressBusProvider = progressBusProvider;
  }

  public TranscriptionWorker get(Context appContext, WorkerParameters params) {
    return newInstance(appContext, params, engineProvider.get(), repositoryProvider.get(), settingsProvider.get(), entitlementStoreProvider.get(), progressBusProvider.get());
  }

  public static TranscriptionWorker_Factory create(
      Provider<SherpaTranscriptionEngine> engineProvider,
      Provider<NotesRepository> repositoryProvider, Provider<SettingsRepository> settingsProvider,
      Provider<EntitlementStore> entitlementStoreProvider,
      Provider<TranscriptionProgressBus> progressBusProvider) {
    return new TranscriptionWorker_Factory(engineProvider, repositoryProvider, settingsProvider, entitlementStoreProvider, progressBusProvider);
  }

  public static TranscriptionWorker newInstance(Context appContext, WorkerParameters params,
      SherpaTranscriptionEngine engine, NotesRepository repository, SettingsRepository settings,
      EntitlementStore entitlementStore, TranscriptionProgressBus progressBus) {
    return new TranscriptionWorker(appContext, params, engine, repository, settings, entitlementStore, progressBus);
  }
}
