### Shuttle Music Player

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE.md)
[![API](https://img.shields.io/badge/API-16%2B-green.svg?style=flat)](https://android-arsenal.com/api?level=16)
[![Build Status](https://travis-ci.org/timusus/Shuttle.svg?branch=dev)](https://travis-ci.org/timusus/Shuttle)
[![Slack invite](http://shuttle-slack-inviter.herokuapp.com/badge.svg)](http://shuttle-slack-inviter.herokuapp.com)

Shuttle is an open source, local music player for Android.

Shuttle comes in two flavours: 

- [Shuttle (free)](https://play.google.com/store/apps/details?id=another.music.player)
- [Shuttle+](https://play.google.com/store/apps/details?id=com.simplecity.amp_pro)

The free version includes an option to upgrade via an IAP, which unlocks the features otherwise available in Shuttle+.


##### Features

- Local playback only (based on the MediaStore)
- Built in equalizer
- Sleep timer
- Folder browser
- Scrobbling
- Widgets
- Theming
- Album-artist support
- Artwork scraping (Last.FM & iTunes)

##### Paid features:

- Tag editing
- Folder browsing
- Chromecast
- Additional theme options
- Additional artwork editing options


#### Contributing:

See the [Contributing](.github/CONTRIBUTING.md) document. This is/will become a good resource if you're just wondering how the app works.


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

Shuttle Music Player is released under the GNU General Public License v3.0 (GPLv3), which can be found here: [License](LICENSE.md)