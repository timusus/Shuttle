package com.simplecity.amp_library.ui.screens.search

import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Song

class SearchResult(internal var albumArtists: List<AlbumArtist>, internal var albums: List<Album>, internal var songs: List<Song>)
