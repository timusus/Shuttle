package com.simplecity.amp_library.di.app

import android.content.Context
import com.simplecity.amp_library.data.AlbumArtistsRepository
import com.simplecity.amp_library.data.AlbumsRepository
import com.simplecity.amp_library.data.BlacklistRepository
import com.simplecity.amp_library.data.GenresRepository
import com.simplecity.amp_library.data.PlaylistsRepository
import com.simplecity.amp_library.data.Repository
import com.simplecity.amp_library.data.SongsRepository
import com.simplecity.amp_library.data.WhitelistRepository
import com.simplecity.amp_library.di.app.RepositoryModule.AbsRepositoryModule
import com.simplecity.amp_library.sql.databases.BlacklistWhitelistDbOpenHelper
import com.squareup.sqlbrite2.BriteDatabase
import com.squareup.sqlbrite2.SqlBrite
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import javax.inject.Singleton

@Module(includes = [AbsRepositoryModule::class])
class RepositoryModule {

    @Provides
    @Singleton
    fun provideInclExclDatabase(context: Context): BriteDatabase {
        return SqlBrite.Builder()
            .build()
            .wrapDatabaseHelper(BlacklistWhitelistDbOpenHelper(context), Schedulers.io())
    }

    @Module
    abstract class AbsRepositoryModule {

        @Binds
        @Singleton
        abstract fun bindSongsRepository(songsRepository: SongsRepository): Repository.SongsRepository

        @Binds
        @Singleton
        abstract fun bindAlbumsRepository(albumsRepository: AlbumsRepository): Repository.AlbumsRepository

        @Binds
        @Singleton
        abstract fun bindAlbumArtistsRepository(albumArtistsRepository: AlbumArtistsRepository): Repository.AlbumArtistsRepository

        @Binds
        @Singleton
        abstract fun bindGenresRepository(genresRepository: GenresRepository): Repository.GenresRepository

        @Binds
        @Singleton
        abstract fun bindPlaylistsRepository(playlistsRepository: PlaylistsRepository): Repository.PlaylistsRepository

        @Binds
        @Singleton
        abstract fun bindBlacklistRepository(blacklistRepository: BlacklistRepository): Repository.BlacklistRepository

        @Binds
        @Singleton
        abstract fun bindWhitelistRepository(whitelistRepository: WhitelistRepository): Repository.WhitelistRepository
    }
}