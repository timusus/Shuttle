### Contributing

Thank you for visiting this page. Contributions is one of the main reasons Shuttle was open-sourced, so I'm happy to have you here!

The following is a set of guidelines for contributing to Shuttle. These are just guidelines, not rules. Use your best judgement, and feel free to propose changes to this document in a pull request.


#### Development

One of the main reasons for open sourcing Shuttle is to get some help from fellow programmers. I encourage you to make pull requests, and I will do my best to look over them, and either approve, provide constructive criticism, or improve upon them myself before merging.


Keep in mind that Shuttle is currently on the Play Store, as a free and paid app. The paid features are open source and visible in this repository. I do make money from these features, and the current plan is to continue to do so. If you feel that it's unfair for me to profit from your contributions, I encourage you to stay away from the paid aspects of the app, including:

- Folder browsing
- Chromecast
- Theming
- Tag editing

Feel free to open an issue if you have any questions/concerns on this matter.


If you plan on working on a new feature, changing the existing code in some significant way, or make UI changes, please create an issue where we can discuss the idea/implementation/UI before working on it.

Please be aware of and try to keep under the 65k method limit (Shuttle is precariously close at the moment). 

Also, please respect the licenses of any code that is used/contributed to Shuttle. Credit should be given where it is due.


#### Getting started:

Clone the `dev` branch. Shuttle has been setup so you should be able to run it without any additional configuration.

[Change the build variant in Android Studio](https://developer.android.com/studio/run/index.html#changing-variant) to `devDebug` to avoid initial build problems. The `release` variants are for Shuttle Play Store releases, and will not compile for you as you need private API keys & keystore information.

If you're using the Last.fm artwork a lot, please go to https://www.last.fm/api/account/create and create your own key.


#### Git

The plan is to use the [Git-flow](https://datasift.github.io/gitflow/IntroducingGitFlow.html) branching model. Feature branches should branch off `dev`.


#### Bug Reports:

Bugs are tracked using the [Github Issue Tracker](https://github.com/timusus/Shuttle/issues)

Please be descriptive and include:

- Shuttle version
- Device, OS
- Description of bug
- Steps to reproduce
- Expected outcome
- Observations (actual outcome)


#### Code Style:

Shuttle doesn't actually have a very consistent code style at the moment. I'm moving away from using hungarian notation, so prefer if you don't use it.

Use your judgement, and try and keep the code as consistent as possible with neighbouring code.


#### Translations:

Translations are managed via [OneSky](http://shuttle.oneskyapp.com). Translations are imported from the OneSky project fairly frequently. Please don't perform translations directly on the source files.


#### Codebase:

Shuttle is a pretty old codebase. I've done my best to try and modernise and refactor what I can, but there are still some dark corners where you'll find some pretty bizarre stuff going on. 

Currently, the MediaStore is the database backing everything you see in the app. 

I've outlined some of the more commonly used / intricate aspects of Shuttle, in the hope that it helps with becoming familiar with the codebase:


##### Data Manager

I've started to recently adopt `RXJava`, which is best demonstrated in the `DataManager` class, which is used to build an in-memory version of the MediaStore, using `RXJava` `BehaviorSubject`s. The `DataManager` also does some parsing of songs on the device, to populate the AlbumArtist field.

Any time you need to get to a song/album/artist, `DataManager.getInstance()` is a good place to start.


##### ViewModelAdapter/ViewModel

There's a very loose implementation of the VM from MVVM when displaying items in a RecyclerView. Models are wrapped in an `AdaptableItem` which defines how the model should be presented in the `RecyclerView`. `AdaptableItem`s are passed to `ViewModelAdapter` (the base `RecyclerView.Adapter` used to back most `RecyclerView`s in Shuttle). The `ViewModelAdapter` uses `DiffUtil` to determine which changes to notify the `RecyclerView` of. The `ContentsComparator` interface is an interesting component here - it helps the `DiffUtil` decide whether two different `AdaptableItem`s represent the same underlying data (so it can potentially partially-refresh the view if need be)

##### MusicService

The `MusicService` is (unfortunately) the God-object responsible for almost everything to do with audio playback.


#### Contact

Shuttle is developed and maintained by Tim Masleed. Get in touch via email or hangouts: t.malseed@gmail.com