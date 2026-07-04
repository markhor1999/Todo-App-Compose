package com.codingwithsalman.voicenotes.feature.settings;

import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator;
import com.codingwithsalman.voicenotes.core.billing.BillingRepository;
import com.codingwithsalman.voicenotes.core.datastore.EntitlementStore;
import com.codingwithsalman.voicenotes.core.datastore.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SettingsRepository> settingsProvider;

  private final Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider;

  private final Provider<BillingRepository> billingProvider;

  private final Provider<EntitlementStore> entitlementStoreProvider;

  public SettingsViewModel_Factory(Provider<SettingsRepository> settingsProvider,
      Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider,
      Provider<BillingRepository> billingProvider,
      Provider<EntitlementStore> entitlementStoreProvider) {
    this.settingsProvider = settingsProvider;
    this.transcriptionCoordinatorProvider = transcriptionCoordinatorProvider;
    this.billingProvider = billingProvider;
    this.entitlementStoreProvider = entitlementStoreProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(settingsProvider.get(), transcriptionCoordinatorProvider.get(), billingProvider.get(), entitlementStoreProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<SettingsRepository> settingsProvider,
      Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider,
      Provider<BillingRepository> billingProvider,
      Provider<EntitlementStore> entitlementStoreProvider) {
    return new SettingsViewModel_Factory(settingsProvider, transcriptionCoordinatorProvider, billingProvider, entitlementStoreProvider);
  }

  public static SettingsViewModel newInstance(SettingsRepository settings,
      TranscriptionCoordinator transcriptionCoordinator, BillingRepository billing,
      EntitlementStore entitlementStore) {
    return new SettingsViewModel(settings, transcriptionCoordinator, billing, entitlementStore);
  }
}
