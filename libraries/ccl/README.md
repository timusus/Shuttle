# CastCompanionLibrary-android

CastCompanionLibrary-android is a library project to enable developers integrate Cast capabilities into their applications faster and easier.

## Dependencies
* google-play-services_lib library from the Android SDK (at least version 9.8.+)
* android-support-v7-appcompat (version 25.0.0 or above)
* android-support-v7-mediarouter (version 25.0.0 or above)

## Set up Instructions
Set up the project dependencies. To use this library in your project, you have two options:

(1) Add this library from jCenter repo by adding the following line to your project's
dependencies:
```shell
 dependencies {
    ...
    compile 'com.google.android.libraries.cast.companionlibrary:ccl:2.9.1'
 }
```

(2) Use the GitHub source and include that as a module dependency by following these steps:
 * Clone this library into a project named CastCompanionLibrary, parallel to your own application project:
```shell
git clone https://github.com/googlecast/CastCompanionLibrary-android.git CastCompanionLibrary
```
 * In the root of your application's project edit the file "settings.gradle" and add the following lines:
```shell
include ':CastCompanionLibrary'
project(':CastCompanionLibrary').projectDir = new File('../CastCompanionLibrary/')
```
 * In your application's main module (usually called "app"), edit your build.gradle to add a new dependency:
```shell
 dependencies {
    ...
    compile project(':CastCompanionLibrary')
 }
```
Now your project is ready to use this library

## Documentation and Demo
See the "CastCompanionLibrary.pdf" inside the project for a more extensive documentation. The
[CastVideos-android](https://github.com/googlecast/CastVideos-android) reference sample app uses this library and
demonstrates how this library can be used in a real application.

## References and how to report bugs
* [Cast Developer Documentation](http://developers.google.com/cast/)
* [Design Checklist](http://developers.google.com/cast/docs/design_checklist)
* If you find any issues with this library, please open a bug here on GitHub
* Question are answered on [StackOverflow](http://stackoverflow.com/questions/tagged/google-cast)

## How to make contributions?
Please read and follow the steps in the CONTRIBUTING.md

## License
See LICENSE

## Terms
Your use of this sample is subject to, and by using or downloading the sample files you agree to comply with, the [Google APIs Terms of Service](https://developers.google.com/terms/) and the [Google Cast SDK Additional Developer Terms of Service](https://developers.google.com/cast/docs/terms/).

## Google+
Google Cast Developers Community on Google+ [http://goo.gl/TPLDxj](http://goo.gl/TPLDxj)

## Change List

2.9.1 (bug fix release)
 * Changed dependency on Cast SDK to 9.8.0 from 9.8.00

2.9.0 (bug fix release)

 * Addressed the following issues: #322, #324, #326
 * Updated to the latest versions of Cast SDK and support libraries.

2.8.4 (bug fix release)

 * Addressed the following issues: #287, #294, #298, #301, #309
 * Enabling debug through CastConfiguration now enables logging through LogUtils as well.

2.8.3 (available here and also from jCenter)

 * Updated support libraries to version 23.2.1, this addresses the crash issue on KitKat when IntroductoryOverlay is used (see issue #278)
 * Fixing an issue where InputStreams were not properly closed in FetchBitmapTask (credit goes to David Edwards)
 * Fixing an issue where failure to load a media in a queue was not closing the full screen VideoCastControllerActivity (credit goes to Luzian Wild)
 * Correcting some typos

2.8.2 (available here and also from jCenter)

 * BACKWARD INCOMPATIBLE CHANGE: Removed Simple Widgets that were introduced in 2.8.0
 * Updated support libraries to version 23.2.0

2.8.1 (available here and also from jCenter)

 * Fixed issue #274

2.8.0 (available here and also from jCenter)

 * CCL is now available from jCenter
 * Introduced a collection of Simple Widgets: a number of cast-aware widgets that can be used with
 no additional setup, managed by CCL.
 * Changed the callback BaseCastConsumer.onDeviceSelected(CastDevice) to
 BaseCastConsumer.onDeviceSelected(CastDevice, RouteInfo) to provide more information in the callback.
 * Added a new callback BaseCastConsumer.onRouteRemoved(RouteInfo) to be called when a route is removed.
 * Fixed #272 and a number of other minor issues.
 * In their manifests, clients no longer need to add any "intent-filters" to the services and the receiver that CCL provides,
 this enhances the security of the client applications.
 * Updated the documentation.

2.7.4

 * Fixed issue #269 where an unwanted scaling of a bitmap was resulting in reduced quality.

2.7.3

 * Added a configuration parameter to allow clients define their own MediaRouteDialogFactory. As a result,
 the protected method "getMediaRouteDialogFactory()" has been removed from the VideoCastManager and DataCastManager.
 If you were previously using this method, please update your code to use the CastConfiguration object
 to achieve the same.

2.7.2

 * Now in the full-screen cast controller (VideoCastControllerActivity) and on lockscreen, images are
 appropriately scaled down before being fetched, to avoid undesired OOM on devices with less memory.
 * Fixed a couple of minor issues in the PDF documentation.

2.7.1

 * Added a full-screen overlay for clients to introduce cast to users. This removes the
 need to use any additional libraries for this purpose. Look at the javadoc for IntroductoryOverlay class for
 the supported features, styling and customization of this view. For a working example, take a look at the
 [CastVideos-android](https://github.com/googlecast/CastVideos-android) reference sample app.
 * Fixed #256 (thanks to [curlyfrie](https://github.com/curlyfrie) for making the suggestion).
 * Updated to the latest Play Service library (v8.4.0).
 * Documentation has been updated to reflect the new changes.

2.7.0

 * Changing how clients configure "features" in CCL: previously, clients would call initialize followed by
 calling enableFeature() to add certain features, or would call certain direct APIs to control the behavior of the
 library (e.g. by calling VideoCastManager.setCastControllerImmersive() or calling setLaunchOptions(), etc.).
 Although that was working fine, it seemed more efficient  and organized to move all "configuration" parameters to its own class.
 This release introduces a new class "CastConfiguration". This class holds all those parameters that are used to control the
 behavior of the library. To initialize the library, one now needs to build an instance of the CastConfiguration object
 (using a Builder pattern) and then pass that as a parameter to the initialize() method of VideoCastManager or DataCastManager.
 Please see CastApplication.java class in [CastVideos-android](https://github.com/googlecast/CastVideos-android) sample for
 an example or see the documentation in the root of this project for more details.
 * Starting to use the MediaRouteControllerDialog from the media router support library. Previously, CCL
 was using a custom dialog since the one provided by the media router support library lacked many features that
 were called for in the UX Checklist. In v23.1.1 of that library, however, things have improved so that there is no
 longer a need for having a custom dialog. There is currently one missing feature in that support library's implementation
 that will be addressed in the next release of that library: tapping on the content area is not captured. CCL has all
 the required wiring and code for that to work properly as soon as the updated support library is released. You can switch back
 to the old custom dialog, if desired, by visiting VideosCastManager.getMediaRouteDialogFactory() and following the comments there.
 * Major update in Notification Service. Previously, the actions that were provided in the notification were limited
 to play/pause and disconnect. In this release, clients can choose what actions they want; the library provides
 the following actions out of the box: Play/Pause, Disconnect, Skip Next, Skip Previous, Fast Forward and
 Rewind. To select the desired actions, configure CastConfiguration accordingly. Fast Forward and Rewind take
 an additional "duration" parameter to indicate how much the current position in the media should be skipped forward
 or backward. For 10 and 30 seconds durations, special icons will be used but you can select any number that makes sense for
 your application. Moreover, clients can now completely implement their own Notification Service and register that
 with the library (again, using CastConfiguration) so that the start and stop of the service can be handled by the framework,
 while the details of the service is designed by the client application.
 It is recommended, however, to extend the existing VideoCastNotificationService class and override its
 build() method to just focus on the notification itself rather then the details that may not be so interesting (a number
 of methods in that class have been made "protected" to enable subclassing).
 * Updated documentation

2.6.1
 * Addressing #245
 * Fixing a small bug where in the custom CastControllerDialog, text was not readable when there was no
 media information
 * Added the appropriate PendingIntent to the MediaSessionCompat; this is in preparation to move
 to the CastControllerDialog from the media router library; there is currently a missing feature in
 that library that doesn't allow us to use this PendingIntent but when that feature gets added, this
 PendingIntent will be used to send the user to the appropriate target activity.
 * Updated Play Services and support libraries to the latest versions.

2.6
 * Addressing #242, #243
 * Some enhancement around usage of MediaSessionCompat and its setup.
 * Updated the appcompat and mediarouter support libraries to the latest version (23.1.0). Due to
 the changes in this version of media router library, the styling of the VideoCastControllerDialog
 is somewhat different, please make sure colors are fine with your application theme and if needed,
 update the appropriate resource aliases and color in your client application.

2.5.2
 * Fixing issue #233

2.5.1
 * Fixed an issue where not setting the LaunchOptions would have resulted in receiver not loading. Now the
   default behavior is to launch the app with the default value of relaunchIfRunning set to false.

2.5
 * MiniController component now has an attribute "auto_setup" that if set to "true", it instructs the
   framework to fully configure the component, so that clients would only need to add the MiniController
   to their layout and the rest will be handled by the library (i.e. if that attribute is set to true,
   there is no need to register or unregister that component with the cast manger anymore). The default
   value is "false" which falls back to the old behavior.
 * You can now set the LaunchOptions soon after initializing the Cast Manager by calling VideoCastManager.setLaunchOptions()
   (same with DataCastManager).
 * A new callback (onDisconnectionReason(int reason)) has been added that can inform the registered listeners
   of the reason a disconnect has happened. Understanding the reason behind a disconnect is somewhat non-trivial
   so this will hopefully make that task easier; see the JavaDoc for more details.
 * Now you can have the library automatically try to reconnect by enabling the FEATURE_AUTO_RECONNECT after
   initializing the Cast Manager; this means clients don't need to call reconnectSessionIfPossible() if that
   feature is enabled.
 * Updated the documentation.
 * Some cleanup, fixing some JavaDocs and comments, etc.

2.4
 * Fixed a number of bugs (#205, #204, #203)
 * Prepared the library for Marshmallow permissions related to the Play Services
 * Some code cleanup

2.3.2
 * Updated the icon for "queue list" in the library.

2.3.1
 * Updated gradle build to use the latest build tool and plugin version
 * Fixed #198. This is a fix for a memory leak in the VideoCastControllerActivity so it is strongly recommended to apply this update.

2.3

 * Moved to use MediaSessionCompat and removed all references to RemoteControlClient (RCC) across the library.
 In addition, started to use the MediaStyle added to the NotificationCompat in the v7 app compat support.
 library.
 * Updated Play Services version to use 7.8+
 * Persisting the policy on showing the next/prev for the full screen controller so that it is always honored.
 * Fixed a few issue around notification visibility when app is in background.
 * These issues have been addressed: #196, #194, #178

2.2

 * Removed a duplicate callback (onRemoteMediaPlayerQueueStatusUpdated()) as it was a duplicate of
 onMediaQueueUpdated(). If your code is currently using onRemoteMediaPlayerQueueStatusUpdated(), please replace that
 with onMediaQueueUpdated() which has an identical signature.
 * Fixed issues #185, #189 and #190. For #189, a new set of resource aliases are introduced which should make
 replacing those resources with your own simpler.

2.1.1

 * Now the MediaRouter support library added back the support for the volume on the cast dialog, so CCL is hiding that again.
 * Some typo fixes.

2.1

 * Added Queue related APIs for handling autoplay and queue
 * Added "stop" button to notification and lockscreen for live streams in Lollipop and above
 * Expanded callbacks in VideoCastConsumer interface to provide feedback on success of queue related API calls
 * Extended the full-screen VideoCastControllerActivity to include next/previous for navigation through queues.
  The visibility of these new buttons can be set through VideoCastManager.setNextPreviousVisibilityPolicy(policy)
 * The MiniController now has a modified UI with an additional item for showing an upcoming media item from the queue.
 * Addressed some issues

2.0.2

 * Addressing issues #171, #174
 * DataCastConsumer.onApplicationConnectionFailed() now returns void

2.0.1

 * Improving the management of MediaRouteButton
 * Working around a bug in Play Services, see issue #170
 * Fixing a typo

2.0
#### Notice: this release introduces a restructured and updated code base that has backward-incompatible changes. Most of these changes can be handled by a simple search-and-replace action but there are some changes to the API names and signatures that may need more manual handling. Below you can find a list of changes.

 * Change in the package name: CCL now has a new package name "com.google.android.libraries.cast.companionlibrary.cast"
 * All string, dimension and color resources now have "ccl_" as prefix. This allows developers to
 work with these resources without any collision with their own apps or other libraries. In addition, some
 unused resources have been removed from the "res/*" directories.
 * CCL no longer needs a reference to your "Activity" context. Instead, only an Application Context
 is adequate when you initialize it. Any API that may need an Activity Context (for example opening the
 VideoCastControllerActivity) will ask for such context as an argument. As a result, it is recommended
 to initialize the library in your Application's onCreate() and access the VideoCastManager singleton
 instance by VideoCastManager.getInstance(). Same applies to DataCastManager.
 * Most interface names have changed:
    * IMediaAuthListener -> MediaAuthListener
    * IMediaAuthService -> MediaAuthService
    * IBaseCastConsumer -> BaseCastConsumer
    * IDataCastConsumer -> DataCastConsumer
    * IVideoCastConsumer -> VideoCastConsumer
 * Some methods have been renamed:
    * IVideoVideoCastContoller#setLine1() -> VideoCastController#setTitle()
    * IVideoVideoCastContoller#setLine2() -> VideoCastController#setSubTitle()
    * IVideoVideoCastContoller#updateClosedCaption() -> VideoCastController#setClosedCaptionState()
    * VideoCastManager#getRemoteMovieUrl() -> getRemoteMediaUrl()
    * VideoCastManager#isRemoteMoviePlaying() -> isRemoteMediaPlaying()
    * VideoCastManager#isRemoteMoviePaused() -> isRemoteMediaPaused()
    * VideoCastManager#startCastControllerActivity() -> startVideoCastControllerActivity()
    * BaseCastManager#incremenetDeviceVolume() -> adjustDeviceVolume()
    * TracksPreferenceManager#setupPreferences() -> setUpPreferences()
    * VideoCastConsumer#onRemovedNamespace() -> onNamespaceRemoved()
    * MediaAuthService#start() -> startAuthorization()
    * MediaAuthService#setOnResult() -> setMediaAuthListener()
    * MediaAuthService#abort() -> abortAuthorization()
    * MediaAuthStatus#RESULT_AUTHORIZED -> AUTHORIZED
    * MediaAuthStatus#RESULT_NOT_AUTHORIZED -> NOT_AUTHORIZED
    * MediaAuthStatus#ABORT_TIMEOUT -> TIMED_OUT
    * MediaAuthStatus#ABORT_USER_CANCELLED -> CANCELED_BY_USER
    * VideoCastController#updateClosedCaption() -> setClosedCaptionStatus()
    * Utils#fromMediaInfo() -> mediaInfoToBundle()
    * Utils#toMediaInfo() -> bundleToMediaInfo()
    * Utils#scaleCenterCrop -> scaleAndCenterCropBitmap()
    * IMiniController.setSubTitle() -> setSubtitle()
    * MediaAuthListener#onResult() -> onAuthResult()
    * MediaAuthListener#onFailure() -> onAuthFailure()
    * BaseCastManager.clearContext() has been removed (see earlier comments)
 * All the "consumer" callbacks used to be wrapped inside a try-catch block inside the library. We have
 now removed this and expect the "consumers" to handle that in the client code; the previous approach was masking
 client issues in the library while they needed to be addressed inside the client itself.
 * BaseCastManager#addMediaRouterButton(MediaRouteButton button) now has no return value (it was redundant)
 * VideoCastConsumer#onApplicationConnectionFailed() no longer returns any value.
 * BaseCastConsumer#onConnectionFailed(() no longer returns any value.
 * [New] There is a new callback "void onMediaLoadResult(int statusCode)" in VideoCastConsumer to
 inform the consumers when a load operation is finished.
 * Updated the build to use the latest gradle binaries.
 * Updated to use the latest versions of Play Services and support libraries.

