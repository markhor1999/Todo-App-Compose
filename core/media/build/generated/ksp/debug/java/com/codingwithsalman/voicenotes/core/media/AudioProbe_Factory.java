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
public final class AudioProbe_Factory implements Factory<AudioProbe> {
  @Override
  public AudioProbe get() {
    return newInstance();
  }

  public static AudioProbe_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AudioProbe newInstance() {
    return new AudioProbe();
  }

  private static final class InstanceHolder {
    static final AudioProbe_Factory INSTANCE = new AudioProbe_Factory();
  }
}
