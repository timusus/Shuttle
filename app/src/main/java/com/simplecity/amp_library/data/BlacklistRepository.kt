package com.simplecity.amp_library.data

import android.content.ContentValues
import com.jakewharton.rxrelay2.BehaviorRelay
import com.simplecity.amp_library.model.InclExclItem
import com.simplecity.amp_library.model.Song
import com.simplecity.amp_library.sql.databases.BlacklistWhitelistDbOpenHelper
import com.squareup.sqlbrite2.BriteDatabase
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

open class InclExclRepository @Inject constructor(
    private val inclExclDatabase: BriteDatabase,
    @InclExclItem.Type private val type: Int
) : Repository.InclExclRepository {

    override fun add(inclExclItem: InclExclItem) {
        val values = ContentValues(2)
        values.put(BlacklistWhitelistDbOpenHelper.COLUMN_PATH, inclExclItem.path)
        values.put(BlacklistWhitelistDbOpenHelper.COLUMN_TYPE, inclExclItem.type)
        inclExclDatabase.insert(BlacklistWhitelistDbOpenHelper.TABLE_NAME, values)
    }

    override fun addAll(inclExclItems: List<InclExclItem>) {
        val transaction = inclExclDatabase.newTransaction()
        try {
            inclExclItems.map { inclExclItem ->
                val contentValues = ContentValues(2)
                contentValues.put(BlacklistWhitelistDbOpenHelper.COLUMN_PATH, inclExclItem.path)
                contentValues.put(BlacklistWhitelistDbOpenHelper.COLUMN_TYPE, inclExclItem.type)
                contentValues
            }.forEach { contentValues -> inclExclDatabase.insert(BlacklistWhitelistDbOpenHelper.TABLE_NAME, contentValues) }
            transaction.markSuccessful()
        } finally {
            transaction.end()
        }
    }

    override fun delete(inclExclItem: InclExclItem) {
        inclExclDatabase.delete(
            BlacklistWhitelistDbOpenHelper.TABLE_NAME,
            BlacklistWhitelistDbOpenHelper.COLUMN_PATH + " = '" + inclExclItem.path.replace("'".toRegex(), "\''") + "'" +
                " AND " + BlacklistWhitelistDbOpenHelper.COLUMN_TYPE + " = " + inclExclItem.type
        )
    }

    override fun addSong(song: Song) {
        add(InclExclItem(song.path, type))
    }

    override fun addAllSongs(songs: List<Song>) {
        addAll(songs.map { song -> InclExclItem(song.path, type) }.toList())
    }

    override fun deleteAll() {
        inclExclDatabase.delete(BlacklistWhitelistDbOpenHelper.TABLE_NAME, BlacklistWhitelistDbOpenHelper.COLUMN_TYPE + " = " + type)
    }

}

class WhitelistRepository @Inject constructor(private val inclExclDatabase: BriteDatabase) : InclExclRepository(inclExclDatabase, InclExclItem.Type.INCLUDE), Repository.WhitelistRepository {

    private var inclSubscription: Disposable? = null
    private val inclRelay = BehaviorRelay.create<List<InclExclItem>>()

    private fun getIncludeItems(): Observable<List<InclExclItem>> {
        return inclExclDatabase.createQuery(
            BlacklistWhitelistDbOpenHelper.TABLE_NAME,
            "SELECT * FROM " + BlacklistWhitelistDbOpenHelper.TABLE_NAME + " WHERE " + BlacklistWhitelistDbOpenHelper.COLUMN_TYPE + " = " + InclExclItem.Type.INCLUDE
        ).mapToList { InclExclItem(it) }
    }

    /**
     * @return a **continuous** stream of type [InclExclItem.Type.INCLUDE] , backed by a behavior relay for caching query results.
     */
    override fun getWhitelistItems(songsRepository: Repository.SongsRepository): Observable<List<InclExclItem>> {
        if (inclSubscription == null || inclSubscription?.isDisposed == true) {
            inclSubscription = getIncludeItems().subscribe(inclRelay)
        }
        return inclRelay.subscribeOn(Schedulers.io())
    }
}

class BlacklistRepository @Inject constructor(private val inclExclDatabase: BriteDatabase) : InclExclRepository(inclExclDatabase, InclExclItem.Type.EXCLUDE), Repository.BlacklistRepository {

    private var exclSubscription: Disposable? = null
    private val exclRelay = BehaviorRelay.create<List<InclExclItem>>()

    private fun getExcludeItems(): Observable<List<InclExclItem>> {
        return inclExclDatabase.createQuery(
            BlacklistWhitelistDbOpenHelper.TABLE_NAME,
            "SELECT * FROM " + BlacklistWhitelistDbOpenHelper.TABLE_NAME + " WHERE " + BlacklistWhitelistDbOpenHelper.COLUMN_TYPE + " = " + InclExclItem.Type.EXCLUDE
        )
            .mapToList { InclExclItem(it) }
    }

    /**
     * @return a **continuous** stream of type [InclExclItem.Type.EXCLUDE], backed by a behavior relay for caching query results.
     */
    override fun getBlacklistItems(songsRepository: Repository.SongsRepository): Observable<List<InclExclItem>> {
        if (exclSubscription == null || exclSubscription?.isDisposed == true) {
            exclSubscription = getExcludeItems()
                .subscribe(exclRelay)
        }
        return exclRelay.subscribeOn(Schedulers.io())
    }
}