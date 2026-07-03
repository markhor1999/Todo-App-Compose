package com.codingwithsalman.voicenotes.feature.capture;

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
public final class CaptureViewModel_Factory implements Factory<CaptureViewModel> {
  private final Provider<RecordingSessionManager> sessionManagerProvider;

  public CaptureViewModel_Factory(Provider<RecordingSessionManager> sessionManagerProvider) {
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public CaptureViewModel get() {
    return newInstance(sessionManagerProvider.get());
  }

  public static CaptureViewModel_Factory create(
      Provider<RecordingSessionManager> sessionManagerProvider) {
    return new CaptureViewModel_Factory(sessionManagerProvider);
  }

  public static CaptureViewModel newInstance(RecordingSessionManager sessionManager) {
    return new CaptureViewModel(sessionManager);
  }
}
