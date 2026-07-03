package com.codingwithsalman.voicenotes.core.media;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class AudioDecoder_Factory implements Factory<AudioDecoder> {
  @Override
  public AudioDecoder get() {
    return newInstance();
  }

  public static AudioDecoder_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AudioDecoder newInstance() {
    return new AudioDecoder();
  }

  private static final class InstanceHolder {
    static final AudioDecoder_Factory INSTANCE = new AudioDecoder_Factory();
  }
}
