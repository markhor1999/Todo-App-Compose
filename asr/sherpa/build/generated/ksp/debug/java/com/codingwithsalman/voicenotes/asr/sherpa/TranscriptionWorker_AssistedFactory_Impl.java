package com.codingwithsalman.voicenotes.asr.sherpa;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class TranscriptionWorker_AssistedFactory_Impl implements TranscriptionWorker_AssistedFactory {
  private final TranscriptionWorker_Factory delegateFactory;

  TranscriptionWorker_AssistedFactory_Impl(TranscriptionWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public TranscriptionWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<TranscriptionWorker_AssistedFactory> create(
      TranscriptionWorker_Factory delegateFactory) {
    return InstanceFactory.create(new TranscriptionWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<TranscriptionWorker_AssistedFactory> createFactoryProvider(
      TranscriptionWorker_Factory delegateFactory) {
    return InstanceFactory.create(new TranscriptionWorker_AssistedFactory_Impl(delegateFactory));
  }
}
