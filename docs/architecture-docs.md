# Ground Android app architecture

The Ground Android app is written in Java using the [Android Jetpack](https://developer.android.com/jetpack) and [RxJava](https://github.com/ReactiveX/RxJava) libraries. This page provides an overview of the high-level app architecture, including key components and libraries.

<!-- Editable image source: https://docs.google.com/drawings/d/1UjetRJsudLHg3YsWaf0sKqHfmrPNNBH4UKI7Ivajx-0/ -->
![Ground Android app architecture diagram](android-architecture-diagram.png)

## Components

The app is comprised of the following components:

- **MainActivity**: Ground is a single-activity app. The main [activity](https://developer.android.com/guide/components/activities/intro-activities) contains all other app components.
- **Fragment**: Each part of the app interface is defined as a [fragment](https://developer.android.com/guide/components/fragments). Examples of fragments are the home screen and the map interface.
- **ViewModel**: Each fragment has a corresponding [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel), which encapsulates its UI state, logic, and functionality, as well as the lifecycle callback methods.
- **Managers**: The app managers abstract access to system services directly for the ViewModels. Examples of services handled by managers are location services and camera access.
- **Models**: Entity representations used by ViewModels, such as the app's representations of map concepts, are defined as models. Models allow the app to remain platform agnostic because we can update model definitions without affecting ViewModels or other app components. Examples of entities defined models are the app's representation of offline imagery.
- **Repository**: Each model has a corresponding repository that defines the APIs for interaction between ViewModels and data stores. 
    - **Local data store**: Local data is stored in a SQLite database and the app uses the [Room persistence library](https://developer.android.com/topic/libraries/architecture/room) to manage interaction with the library.
    - **Remote data store**: Remote data is stored in [Firebase Cloud Firestore](https://firebase.google.com/docs/firestore). For more information on how remote data is structured, see [Cloud Firestore Representation](https://github.com/google/ground-platform/wiki/Cloud-Firestore-Representation)
- **Workers** (not pictured in the diagram): Jobs that run on a background thread are implemented via [workers](https://developer.android.com/reference/androidx/work/Worker). Examples of tasks handed by workers are syncing local and remote data when a network connection is available.

## Data flows

- Data flows between components is managed via [LiveData](https://developer.android.com/topic/libraries/architecture/livedata) and [RxJava](https://github.com/ReactiveX/RxJava). 
- Dependencies between components are managed via [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) (Dagger).
- Data is always written locally first and then queued to be synced with the remote data store, in both directions.



