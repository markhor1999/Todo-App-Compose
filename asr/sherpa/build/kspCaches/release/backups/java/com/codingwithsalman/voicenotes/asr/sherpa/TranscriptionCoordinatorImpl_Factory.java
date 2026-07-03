package com.codingwithsalman.voicenotes.asr.sherpa;

import android.content.Context;
import com.codingwithsalman.voicenotes.core.database.NotesRepository;
import com.codingwithsalman.voicenotes.core.datastore.EntitlementStore;
import com.codingwithsalman.voicenotes.core.datastore.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class TranscriptionCoordinatorImpl_Factory implements Factory<TranscriptionCoordinatorImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<ModelStore> modelStoreProvider;

  private final Provider<NotesRepository> repositoryProvider;

  private final Provider<SettingsRepository> settingsProvider;

  private final Provider<EntitlementStore> entitlementStoreProvider;

  private final Provider<TranscriptionProgressBus> progressBusProvider;

  public TranscriptionCoordinatorImpl_Factory(Provider<Context> contextProvider,
      Provider<ModelStore> modelStoreProvider, Provider<NotesRepository> repositoryProvider,
      Provider<SettingsRepository> settingsProvider,
      Provider<EntitlementStore> entitlementStoreProvider,
      Provider<TranscriptionProgressBus> progressBusProvider) {
    this.contextProvider = contextProvider;
    this.modelStoreProvider = modelStoreProvider;
    this.repositoryProvider = repositoryProvider;
    this.settingsProvider = settingsProvider;
    this.entitlementStoreProvider = entitlementStoreProvider;
    this.progressBusProvider = progressBusProvider;
  }

  @Override
  public TranscriptionCoordinatorImpl get() {
    return newInstance(contextProvider.get(), modelStoreProvider.get(), repositoryProvider.get(), settingsProvider.get(), entitlementStoreProvider.get(), progressBusProvider.get());
  }

  public static TranscriptionCoordinatorImpl_Factory create(Provider<Context> contextProvider,
      Provider<ModelStore> modelStoreProvider, Provider<NotesRepository> repositoryProvider,
      Provider<SettingsRepository> settingsProvider,
      Provider<EntitlementStore> entitlementStoreProvider,
      Provider<TranscriptionProgressBus> progressBusProvider) {
    return new TranscriptionCoordinatorImpl_Factory(contextProvider, modelStoreProvider, repositoryProvider, settingsProvider, entitlementStoreProvider, progressBusProvider);
  }

  public static TranscriptionCoordinatorImpl newInstance(Context context, ModelStore modelStore,
      NotesRepository repository, SettingsRepository settings, EntitlementStore entitlementStore,
      TranscriptionProgressBus progressBus) {
    return new TranscriptionCoordinatorImpl(context, modelStore, repository, settings, entitlementStore, progressBus);
  }
}
