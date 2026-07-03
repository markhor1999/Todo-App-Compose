package com.codingwithsalman.voicenotes.core.media;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class AudioPlayerController_Factory implements Factory<AudioPlayerController> {
  private final Provider<Context> contextProvider;

  public AudioPlayerController_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AudioPlayerController get() {
    return newInstance(contextProvider.get());
  }

  public static AudioPlayerController_Factory create(Provider<Context> contextProvider) {
    return new AudioPlayerController_Factory(contextProvider);
  }

  public static AudioPlayerController newInstance(Context context) {
    return new AudioPlayerController(context);
  }
}
