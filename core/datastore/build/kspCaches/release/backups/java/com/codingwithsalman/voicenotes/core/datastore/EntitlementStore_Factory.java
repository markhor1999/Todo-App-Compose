package com.codingwithsalman.voicenotes.core.datastore;

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
public final class EntitlementStore_Factory implements Factory<EntitlementStore> {
  private final Provider<Context> contextProvider;

  public EntitlementStore_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public EntitlementStore get() {
    return newInstance(contextProvider.get());
  }

  public static EntitlementStore_Factory create(Provider<Context> contextProvider) {
    return new EntitlementStore_Factory(contextProvider);
  }

  public static EntitlementStore newInstance(Context context) {
    return new EntitlementStore(context);
  }
}
