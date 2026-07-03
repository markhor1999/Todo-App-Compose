package com.codingwithsalman.voicenotes.core.database;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.codingwithsalman.voicenotes.core.common.di.IoDispatcher")
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
public final class NotesRepository_Factory implements Factory<NotesRepository> {
  private final Provider<NotesDao> daoProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public NotesRepository_Factory(Provider<NotesDao> daoProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.daoProvider = daoProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public NotesRepository get() {
    return newInstance(daoProvider.get(), ioDispatcherProvider.get());
  }

  public static NotesRepository_Factory create(Provider<NotesDao> daoProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new NotesRepository_Factory(daoProvider, ioDispatcherProvider);
  }

  public static NotesRepository newInstance(NotesDao dao, CoroutineDispatcher ioDispatcher) {
    return new NotesRepository(dao, ioDispatcher);
  }
}
