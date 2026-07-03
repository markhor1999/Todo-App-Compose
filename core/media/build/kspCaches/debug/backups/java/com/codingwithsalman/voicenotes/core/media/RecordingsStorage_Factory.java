package com.codingwithsalman.voicenotes.core.media;

import android.content.Context;
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
public final class RecordingsStorage_Factory implements Factory<RecordingsStorage> {
  private final Provider<Context> contextProvider;

  public RecordingsStorage_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public RecordingsStorage get() {
    return newInstance(contextProvider.get());
  }

  public static RecordingsStorage_Factory create(Provider<Context> contextProvider) {
    return new RecordingsStorage_Factory(contextProvider);
  }

  public static RecordingsStorage newInstance(Context context) {
    return new RecordingsStorage(context);
  }
}
