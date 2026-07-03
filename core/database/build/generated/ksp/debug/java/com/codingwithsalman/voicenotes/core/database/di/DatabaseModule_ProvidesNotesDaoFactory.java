package com.codingwithsalman.voicenotes.core.database.di;

import com.codingwithsalman.voicenotes.core.database.NotesDao;
import com.codingwithsalman.voicenotes.core.database.VoiceNotesDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvidesNotesDaoFactory implements Factory<NotesDao> {
  private final Provider<VoiceNotesDatabase> databaseProvider;

  public DatabaseModule_ProvidesNotesDaoFactory(Provider<VoiceNotesDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public NotesDao get() {
    return providesNotesDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvidesNotesDaoFactory create(
      Provider<VoiceNotesDatabase> databaseProvider) {
    return new DatabaseModule_ProvidesNotesDaoFactory(databaseProvider);
  }

  public static NotesDao providesNotesDao(VoiceNotesDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providesNotesDao(database));
  }
}
