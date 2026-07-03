package com.codingwithsalman.voicenotes.asr.sherpa;

import androidx.hilt.work.WorkerAssistedFactory;
import androidx.work.ListenableWorker;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import javax.annotation.processing.Generated;

@Generated("androidx.hilt.AndroidXHiltProcessor")
@Module
@InstallIn(SingletonComponent.class)
@OriginatingElement(
    topLevelClass = TranscriptionWorker.class
)
public interface TranscriptionWorker_HiltModule {
  @Binds
  @IntoMap
  @StringKey("com.codingwithsalman.voicenotes.asr.sherpa.TranscriptionWorker")
  WorkerAssistedFactory<? extends ListenableWorker> bind(
      TranscriptionWorker_AssistedFactory factory);
}
