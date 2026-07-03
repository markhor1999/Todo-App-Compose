package com.codingwithsalman.voicenotes.asr.sherpa;

import com.codingwithsalman.voicenotes.core.media.AudioDecoder;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.codingwithsalman.voicenotes.core.common.di.DefaultDispatcher")
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
public final class SherpaTranscriptionEngine_Factory implements Factory<SherpaTranscriptionEngine> {
  private final Provider<ModelStore> modelStoreProvider;

  private final Provider<AudioDecoder> audioDecoderProvider;

  private final Provider<CoroutineDispatcher> defaultDispatcherProvider;

  public SherpaTranscriptionEngine_Factory(Provider<ModelStore> modelStoreProvider,
      Provider<AudioDecoder> audioDecoderProvider,
      Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    this.modelStoreProvider = modelStoreProvider;
    this.audioDecoderProvider = audioDecoderProvider;
    this.defaultDispatcherProvider = defaultDispatcherProvider;
  }

  @Override
  public SherpaTranscriptionEngine get() {
    return newInstance(modelStoreProvider.get(), audioDecoderProvider.get(), defaultDispatcherProvider.get());
  }

  public static SherpaTranscriptionEngine_Factory create(Provider<ModelStore> modelStoreProvider,
      Provider<AudioDecoder> audioDecoderProvider,
      Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    return new SherpaTranscriptionEngine_Factory(modelStoreProvider, audioDecoderProvider, defaultDispatcherProvider);
  }

  public static SherpaTranscriptionEngine newInstance(ModelStore modelStore,
      AudioDecoder audioDecoder, CoroutineDispatcher defaultDispatcher) {
    return new SherpaTranscriptionEngine(modelStore, audioDecoder, defaultDispatcher);
  }
}
