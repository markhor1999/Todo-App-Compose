package com.codingwithsalman.voicenotes.core.database.di;

import android.content.Context;
import com.codingwithsalman.voicenotes.core.database.VoiceNotesDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvidesDatabaseFactory implements Factory<VoiceNotesDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvidesDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public VoiceNotesDatabase get() {
    return providesDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvidesDatabaseFactory create(Provider<Context> contextProvider) {
    return new DatabaseModule_ProvidesDatabaseFactory(contextProvider);
  }

  public static VoiceNotesDatabase providesDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providesDatabase(context));
  }
}
