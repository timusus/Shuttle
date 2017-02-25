### Shuttle Music Player

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE.md)

Shuttle is an open source, local music player for Android.

Shuttle comes in two flavours: 

- [Shuttle (free)](https://play.google.com/store/apps/details?id=another.music.player)
- [Shuttle+](https://play.google.com/store/apps/details?id=com.simplecity.amp_pro)

The free version includes an option to upgrade via an IAP, which unlocks the features otherwise available in Shuttle+.

#### Features

- Local playback only (based on the MediaStore)
- Built in equalizer
- Sleep timer
- Folder browser
- Scrobbling
- Widgets
- Theming
- Album-artist support
- Artwork scraping (Last.FM & iTunes)

Paid features:

- Tag editing
- Folder browsing
- Chromecast
- Additional theme options
- Additional artwork editing options


#### Future:

Here's a list of some of the things I'd like to work towards.

- Smart playlists
	
	The ability to pick certain criteria, and have Shuttle automatically populate a playlist to match would be great. Particularly 'songs in this genre, from year x to year y'. That type of thing.

- Replay gain
	
	There have been many requests for volume normalisation over the years.
	Currently considering using a 3rd party Media Player such as [SuperPowered](http://superpowered.com/) (but it has limited filetype support)

- Crossfade
	
	My guess is this would be done by managing two MediaPlayer instances, and their volume. Needs to be implemented in such a way that Gapless still works properly for those who don't use crossfade.

- Improved equalizer
	
	The current EQ is based off of CyanogenMods EQ (which I believe is based off the native Android EQ). It's buggy, doesn't work for everyone, and doesn't sound that good on most devices.

- Weaning off of the MediaStore

	The MediaStore is the worst thing about Shuttle. It's buggy. Slow to update. It doesn't scan certain paths. Often it doesn't pickup song/album year information. It doesn't tidy up genres when they no longer exist. Worst of all, it makes the FileBrowser almost useless. Shuttle is coupled so tightly to the MediaStore that a song cannot be played unless it is present in the MediaStore. 

- UI Improvements
	
	I'm currently in the midst of reworking the 'now playing' screen, which should make it look a bit better. I've also done some work on the artist detail screen, with the hopes of making albums expandable/collapsible.

- Add support for pluggable media sources (including streaming sources)

	This kind of ties in with the point above (and depends on it). It would be the greatest thing ever to have support for pluggable sources, so I could begin working on, for example, a DLNA plugin, Google Drive plugin, Spotify plugin, or just whatever.

- Add tests
	
	There are 0 tests in Shuttle. I would almost be ashamed except - well, I don't exactly how to write unit tests, and I don't know what to test. If someone could add a couple of tests to give me an idea of how it's done, I'll gladly start building upon them.

- Cleaning up / modernising the codebase

	I'd like to start using dependency injection, mostly just to get an understanding of how to use it. There's plenty more RX-ifying to be done as well.


#### History:

Shuttle started some time in early 2012, as a project to learn Java (and programming in general). Initially, it was based off of the [Random Music Player](https://github.com/android/platform_development/tree/master/samples/RandomMusicPlayer/src/com/example/android/musicplayer) sample project by Google. I clearly recall how happy I was when I managed to wrangle that project to play a song of my choosing!

After lots of blog reading and trial and error, I eventually stumbled upon Googles [Default Music Player](https://github.com/android/platform_packages_apps_music). At the time I had no idea what I was doing, but for better or worse, I made a habit of not copying classes, but writing the code out myself. This tedious process lasted a while, but it was my way of trying to ensure I understood what was going on when I copied a class into my app.

Initially, Shuttle was relased as AMP (another.music.player). A patent troll threatened legal action if I didn't change the name (don't want a native Android app getting confused with an audio codec!) and I was naive enough to comply - and so the name was changed.

As I developed Shuttle, I received a lot of really positive feedback from reddit ([/r/android](https://www.reddit.com/r/android)). I continued to work on customisation of holo-based themes and looked on in awe as Shuttle ticked over to install number 10.

I eventually discovered Cyanogenmod, and the then-famous Apollo Music Player, which seemed to solve a few problems I wasn't able to solve myself. I discovered the beautiful thing that is open source software, and found out that someone else had already solved these problems for me, and I was allowed to copy them! That someone else was Andrew Neal, the developer of Apollo Music Player. Initially via email, then to hangouts and now on Slack, Andrew unwittingly became my mentor. I attribute a large portion of Shuttle's success to both Andrew and his app Apollo.

Shuttle continued to gain momentum over the years. Holo was phased out and something post-Holo-pre-Material came in. I celebrated 500,000 downloads with an actual cake sometime in 2014. I got a job as an Android developer.. Glide came out, RXJava became popular and Material design came in.

I've continued to work on Shuttle at times daily, at the least weekly, ever since its initial release. It's come a hell of a long way since day one. It is my proudest accomplishment. It now has almost 4 million downloads!

#### Motivation for Open Source:

Without open source, Shuttle wouldn't exist, and I probably wouldn't be an Android developer. One of my motivations for open sourcing this project is to 'give back'. Maybe Shuttle can be to someone else what Apollo Music Player was to me.

Secondly, I don't feel like I'm able to dedicate as much time to the project as I have in the past. Each year that goes by, I feel like there's just more for me to do and less time do it. I'm hoping that there are some enthusiastic developers out there who like Shuttle, and would like to improve it in some way. Shuttle seems like it's slowly dying. The rate of installs has dropped back to what it was 2.5 years ago. I think this may be due to the rise of cloud players, but I'm really not sure. I'd like to see what can be done with it while it still has a heartbeat.


#### License

Shuttle Music Player is released under the GNU General Public License v3.0 (GPLv3), which can be found here: [License](https://github.com/timusus/Shuttle/blob/master/LICENSE.md)