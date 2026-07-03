package com.codingwithsalman.voicenotes.core.media;

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
public final class WaveformExtractor_Factory implements Factory<WaveformExtractor> {
  private final Provider<AudioDecoder> decoderProvider;

  public WaveformExtractor_Factory(Provider<AudioDecoder> decoderProvider) {
    this.decoderProvider = decoderProvider;
  }

  @Override
  public WaveformExtractor get() {
    return newInstance(decoderProvider.get());
  }

  public static WaveformExtractor_Factory create(Provider<AudioDecoder> decoderProvider) {
    return new WaveformExtractor_Factory(decoderProvider);
  }

  public static WaveformExtractor newInstance(AudioDecoder decoder) {
    return new WaveformExtractor(decoder);
  }
}
