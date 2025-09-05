package com.codingwithsalman.apps.todo.app.compose.di

/*
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun providesNoteDatabase(
        @ApplicationContext context: Context
    ) = Room.databaseBuilder(
        context,
        NoteDatabase::class.java,
        NOTE_DATABASE_NAME
    ).build()

    @Provides
    @Singleton
    fun providesNoteDao(
        noteDatabase: NoteDatabase
    ) = noteDatabase.noteDao

    @Provides
    @Singleton
    fun providesNoteRepository(noteDao: NoteDao): NoteRepository = NoteRepositoryImpl(noteDao)

    @Provides
    @Singleton
    fun providesNoteUseCases(
        repository: NoteRepository
    ): NoteUseCases = NoteUseCases(
        getNotes = GetNotes(repository = repository),
        deleteNote = DeleteNote(repository = repository),
        addNote = AddNote(repository = repository),
        getNote = GetNote(repository = repository)
    )
}*/
