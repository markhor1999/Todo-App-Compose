package com.codingwithsalman.voicenotes.core.billing;

import android.content.Context;
import com.codingwithsalman.voicenotes.core.datastore.EntitlementStore;
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
public final class BillingRepository_Factory implements Factory<BillingRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<EntitlementStore> entitlementStoreProvider;

  public BillingRepository_Factory(Provider<Context> contextProvider,
      Provider<EntitlementStore> entitlementStoreProvider) {
    this.contextProvider = contextProvider;
    this.entitlementStoreProvider = entitlementStoreProvider;
  }

  @Override
  public BillingRepository get() {
    return newInstance(contextProvider.get(), entitlementStoreProvider.get());
  }

  public static BillingRepository_Factory create(Provider<Context> contextProvider,
      Provider<EntitlementStore> entitlementStoreProvider) {
    return new BillingRepository_Factory(contextProvider, entitlementStoreProvider);
  }

  public static BillingRepository newInstance(Context context, EntitlementStore entitlementStore) {
    return new BillingRepository(context, entitlementStore);
  }
}
