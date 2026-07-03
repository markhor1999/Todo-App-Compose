package com.codingwithsalman.voicenotes.feature.capture;

import android.content.Context;
import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator;
import com.codingwithsalman.voicenotes.core.database.NotesRepository;
import com.codingwithsalman.voicenotes.core.media.AudioRecorderController;
import com.codingwithsalman.voicenotes.core.media.RecordingsStorage;
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
public final class RecordingSessionManager_Factory implements Factory<RecordingSessionManager> {
  private final Provider<Context> contextProvider;

  private final Provider<AudioRecorderController> recorderProvider;

  private final Provider<RecordingsStorage> storageProvider;

  private final Provider<NotesRepository> repositoryProvider;

  private final Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider;

  public RecordingSessionManager_Factory(Provider<Context> contextProvider,
      Provider<AudioRecorderController> recorderProvider,
      Provider<RecordingsStorage> storageProvider, Provider<NotesRepository> repositoryProvider,
      Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider) {
    this.contextProvider = contextProvider;
    this.recorderProvider = recorderProvider;
    this.storageProvider = storageProvider;
    this.repositoryProvider = repositoryProvider;
    this.transcriptionCoordinatorProvider = transcriptionCoordinatorProvider;
  }

  @Override
  public RecordingSessionManager get() {
    return newInstance(contextProvider.get(), recorderProvider.get(), storageProvider.get(), repositoryProvider.get(), transcriptionCoordinatorProvider.get());
  }

  public static RecordingSessionManager_Factory create(Provider<Context> contextProvider,
      Provider<AudioRecorderController> recorderProvider,
      Provider<RecordingsStorage> storageProvider, Provider<NotesRepository> repositoryProvider,
      Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider) {
    return new RecordingSessionManager_Factory(contextProvider, recorderProvider, storageProvider, repositoryProvider, transcriptionCoordinatorProvider);
  }

  public static RecordingSessionManager newInstance(Context context,
      AudioRecorderController recorder, RecordingsStorage storage, NotesRepository repository,
      TranscriptionCoordinator transcriptionCoordinator) {
    return new RecordingSessionManager(context, recorder, storage, repository, transcriptionCoordinator);
  }
}
