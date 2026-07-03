package com.codingwithsalman.voicenotes.feature.library;

import android.content.Context;
import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator;
import com.codingwithsalman.voicenotes.core.database.NotesRepository;
import com.codingwithsalman.voicenotes.core.media.AudioProbe;
import com.codingwithsalman.voicenotes.core.media.RecordingsStorage;
import com.codingwithsalman.voicenotes.core.media.WaveformExtractor;
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
public final class LibraryViewModel_Factory implements Factory<LibraryViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<NotesRepository> repositoryProvider;

  private final Provider<RecordingsStorage> storageProvider;

  private final Provider<AudioProbe> audioProbeProvider;

  private final Provider<WaveformExtractor> waveformExtractorProvider;

  private final Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public LibraryViewModel_Factory(Provider<Context> contextProvider,
      Provider<NotesRepository> repositoryProvider, Provider<RecordingsStorage> storageProvider,
      Provider<AudioProbe> audioProbeProvider,
      Provider<WaveformExtractor> waveformExtractorProvider,
      Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.contextProvider = contextProvider;
    this.repositoryProvider = repositoryProvider;
    this.storageProvider = storageProvider;
    this.audioProbeProvider = audioProbeProvider;
    this.waveformExtractorProvider = waveformExtractorProvider;
    this.transcriptionCoordinatorProvider = transcriptionCoordinatorProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public LibraryViewModel get() {
    return newInstance(contextProvider.get(), repositoryProvider.get(), storageProvider.get(), audioProbeProvider.get(), waveformExtractorProvider.get(), transcriptionCoordinatorProvider.get(), ioDispatcherProvider.get());
  }

  public static LibraryViewModel_Factory create(Provider<Context> contextProvider,
      Provider<NotesRepository> repositoryProvider, Provider<RecordingsStorage> storageProvider,
      Provider<AudioProbe> audioProbeProvider,
      Provider<WaveformExtractor> waveformExtractorProvider,
      Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new LibraryViewModel_Factory(contextProvider, repositoryProvider, storageProvider, audioProbeProvider, waveformExtractorProvider, transcriptionCoordinatorProvider, ioDispatcherProvider);
  }

  public static LibraryViewModel newInstance(Context context, NotesRepository repository,
      RecordingsStorage storage, AudioProbe audioProbe, WaveformExtractor waveformExtractor,
      TranscriptionCoordinator transcriptionCoordinator, CoroutineDispatcher ioDispatcher) {
    return new LibraryViewModel(context, repository, storage, audioProbe, waveformExtractor, transcriptionCoordinator, ioDispatcher);
  }
}
