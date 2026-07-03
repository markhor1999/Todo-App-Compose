package com.codingwithsalman.voicenotes.feature.note;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator;
import com.codingwithsalman.voicenotes.core.billing.BillingRepository;
import com.codingwithsalman.voicenotes.core.database.NotesRepository;
import com.codingwithsalman.voicenotes.core.datastore.EntitlementStore;
import com.codingwithsalman.voicenotes.core.media.AudioPlayerController;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata
@QualifierMetadata({
    "dagger.hilt.android.qualifiers.ApplicationContext",
    "com.codingwithsalman.voicenotes.core.common.di.IoDispatcher"
})
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
public final class NoteDetailViewModel_Factory implements Factory<NoteDetailViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<Context> contextProvider;

  private final Provider<NotesRepository> repositoryProvider;

  private final Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider;

  private final Provider<EntitlementStore> entitlementStoreProvider;

  private final Provider<BillingRepository> billingProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  private final Provider<AudioPlayerController> playerProvider;

  public NoteDetailViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<Context> contextProvider, Provider<NotesRepository> repositoryProvider,
      Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider,
      Provider<EntitlementStore> entitlementStoreProvider,
      Provider<BillingRepository> billingProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider,
      Provider<AudioPlayerController> playerProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.contextProvider = contextProvider;
    this.repositoryProvider = repositoryProvider;
    this.transcriptionCoordinatorProvider = transcriptionCoordinatorProvider;
    this.entitlementStoreProvider = entitlementStoreProvider;
    this.billingProvider = billingProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
    this.playerProvider = playerProvider;
  }

  @Override
  public NoteDetailViewModel get() {
    return newInstance(savedStateHandleProvider.get(), contextProvider.get(), repositoryProvider.get(), transcriptionCoordinatorProvider.get(), entitlementStoreProvider.get(), billingProvider.get(), ioDispatcherProvider.get(), playerProvider.get());
  }

  public static NoteDetailViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider, Provider<Context> contextProvider,
      Provider<NotesRepository> repositoryProvider,
      Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider,
      Provider<EntitlementStore> entitlementStoreProvider,
      Provider<BillingRepository> billingProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider,
      Provider<AudioPlayerController> playerProvider) {
    return new NoteDetailViewModel_Factory(savedStateHandleProvider, contextProvider, repositoryProvider, transcriptionCoordinatorProvider, entitlementStoreProvider, billingProvider, ioDispatcherProvider, playerProvider);
  }

  public static NoteDetailViewModel newInstance(SavedStateHandle savedStateHandle, Context context,
      NotesRepository repository, TranscriptionCoordinator transcriptionCoordinator,
      EntitlementStore entitlementStore, BillingRepository billing,
      CoroutineDispatcher ioDispatcher, AudioPlayerController player) {
    return new NoteDetailViewModel(savedStateHandle, context, repository, transcriptionCoordinator, entitlementStore, billing, ioDispatcher, player);
  }
}
