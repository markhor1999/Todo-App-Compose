package com.codingwithsalman.voicenotes.asr.sherpa;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class TranscriptionProgressBus_Factory implements Factory<TranscriptionProgressBus> {
  @Override
  public TranscriptionProgressBus get() {
    return newInstance();
  }

  public static TranscriptionProgressBus_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TranscriptionProgressBus newInstance() {
    return new TranscriptionProgressBus();
  }

  private static final class InstanceHolder {
    static final TranscriptionProgressBus_Factory INSTANCE = new TranscriptionProgressBus_Factory();
  }
}
