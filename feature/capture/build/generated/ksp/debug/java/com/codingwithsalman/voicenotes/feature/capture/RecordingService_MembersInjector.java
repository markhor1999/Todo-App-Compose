package com.codingwithsalman.voicenotes.feature.capture;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class RecordingService_MembersInjector implements MembersInjector<RecordingService> {
  private final Provider<RecordingSessionManager> sessionManagerProvider;

  public RecordingService_MembersInjector(
      Provider<RecordingSessionManager> sessionManagerProvider) {
    this.sessionManagerProvider = sessionManagerProvider;
  }

  public static MembersInjector<RecordingService> create(
      Provider<RecordingSessionManager> sessionManagerProvider) {
    return new RecordingService_MembersInjector(sessionManagerProvider);
  }

  @Override
  public void injectMembers(RecordingService instance) {
    injectSessionManager(instance, sessionManagerProvider.get());
  }

  @InjectedFieldSignature("com.codingwithsalman.voicenotes.feature.capture.RecordingService.sessionManager")
  public static void injectSessionManager(RecordingService instance,
      RecordingSessionManager sessionManager) {
    instance.sessionManager = sessionManager;
  }
}
