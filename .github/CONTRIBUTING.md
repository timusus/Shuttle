### Contributing

Thank you for visiting this page. Contributions is one of the main reasons Shuttle was open-sourced, so I'm happy to have you here!  

The following is a set of guidelines for contributing to Shuttle. These are just guidelines, not rules. Use your best judgement, and feel free to propose changes to this document in a pull request.


#### Contributing Code

There are a couple of important things to know:

  1. You must be aware of the GPL v3 license, and agree to the [Contributors License Agreement](https://cla-assistant.io/timusus/Shuttle). 
  2. Not all proposed contributions can be accepted. The more effort you invest, the better you should clarify in advance whether the contribution fits: the best way would be to just open an issue to discuss the feature you plan to implement (make it clear you intend to contribute).
  
When you contribute (code, documentation, or anything else), you have to be aware that your contribution is covered by the same GPLv3 License that is applied to Shuttle Music Player itself. In particular you need to agree to the Individual Contributor License Agreement, which can be found [here](https://cla-assistant.io/timusus/Shuttle). (This applies to all contributors, including those contributing on behalf of a company). If you agree to its content, you simply have to click on the link posted by the CLA assistant as a comment to the pull request. Click it to check the CLA, then accept it on the following screen if you agree to it. CLA assistant will save this decision for upcoming contributions and will notify you if there is any change to the CLA in the meantime.


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

I strongly encourage you to join the [Slack](http://shuttle-slack-inviter.herokuapp.com) channel, where you can get help compiling the app, or discuss proposed changes.

Clone the `dev` branch. Shuttle has been setup so you should be able to run it without any additional configuration.

[Change the build variant in Android Studio](https://developer.android.com/studio/run/index.html#changing-variant) to `devDebug` to avoid initial build problems. The `release` variants are for Shuttle Play Store releases, and will not compile for you as you need private API keys & keystore information.

If you're using the Last.fm artwork a lot, please go to https://www.last.fm/api/account/create and create your own key.


#### Git

Shuttle uses the [Git-flow](https://datasift.github.io/gitflow/IntroducingGitFlow.html) branching model. Feature branches should branch off `dev`.


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


#### Contact

Shuttle is developed and maintained by Tim Malseed. Get in touch via [Slack](http://shuttle-slack-inviter.herokuapp.com), email or hangouts: t.malseed@gmail.com
