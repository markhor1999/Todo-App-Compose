package com.codingwithsalman.voicenotes.feature.onboarding;

import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator;
import com.codingwithsalman.voicenotes.core.datastore.EntitlementStore;
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
public final class OnboardingViewModel_Factory implements Factory<OnboardingViewModel> {
  private final Provider<EntitlementStore> entitlementStoreProvider;

  private final Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider;

  public OnboardingViewModel_Factory(Provider<EntitlementStore> entitlementStoreProvider,
      Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider) {
    this.entitlementStoreProvider = entitlementStoreProvider;
    this.transcriptionCoordinatorProvider = transcriptionCoordinatorProvider;
  }

  @Override
  public OnboardingViewModel get() {
    return newInstance(entitlementStoreProvider.get(), transcriptionCoordinatorProvider.get());
  }

  public static OnboardingViewModel_Factory create(
      Provider<EntitlementStore> entitlementStoreProvider,
      Provider<TranscriptionCoordinator> transcriptionCoordinatorProvider) {
    return new OnboardingViewModel_Factory(entitlementStoreProvider, transcriptionCoordinatorProvider);
  }

  public static OnboardingViewModel newInstance(EntitlementStore entitlementStore,
      TranscriptionCoordinator transcriptionCoordinator) {
    return new OnboardingViewModel(entitlementStore, transcriptionCoordinator);
  }
}
