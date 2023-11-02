# Contributing to Ground for Android

This document covers everything you need to know about contributing to Ground
for Android. Before you get started, please make sure you read all of the 
sections and are able to sign our CLA.

## Contributor License Agreement (CLA)

Contributions to this project must be accompanied by a Contributor License
Agreement. You (or your employer) retain the copyright to your contribution;
this simply gives us permission to use and redistribute your contributions as
part of the project. Head over to <https://cla.developers.google.com/> to see
your current agreements on file or to sign a new one.

You generally only need to submit a CLA once, so if you've already submitted one
(even if it was for a different project), you probably don't need to do it
again.

## Code reviews

Before starting work on a change, comment on one of the [open issues](https://github.com/google/ground-android/issues?utf8=%E2%9C%93&q=is%3Aissue+is%3Aopen)
saying you'd like to take it on. If one does not exist, you can also
[create one here](https://github.com/google/ground-android/issues/new).

Be sure to reference this issue number in your pull request (e.g., 
"Fixes #27").

Make sure all checks on GitHub are passing.

All submissions, including submissions by project members, require review. We
use GitHub pull requests for this purpose. Consult
[GitHub Help](https://help.github.com/articles/about-pull-requests/) for more
information on using pull requests.

## Community Guidelines

This project follows
[Google's Open Source Community Guidelines](https://opensource.google.com/conduct/).

## Getting started

The following instructions describe how to fork this repository in order to
contribute to the ground-android codebase.

1. Fork this repository, see <https://help.github.com/articles/fork-a-repo/>.

2. Clone your fork:
    
    `git clone https://github.com/<username>/ground-android.git`
    
    Where `<username>` is your github username.

3. Add the base repository as a remote:
    
    `git remote add upstream https://github.com/google/ground-android.git`

4. Follow the instructions under the [Initial build
configuration](#initial-build-configuration) section of this readme to set up
your development environment.

## Developer workflow

After you have forked and cloned the repository, use the following steps to make
and manage changes. After you have finished making changes, you can submit them
to the base repository using a pull request. 

1. Pull changes from the base repository's master branch:
    
    `git pull upstream master`

1. Create a new branch to track your changes:
    
    `git checkout -b <branch>`
    
    Where `<branch>` is a meaningful name for the branch you'll use to track
    changes.

1. Make and test changes locally.

1. Run code health checks locally and fix any errors.

   1. Using Command-line:
      1. `$ ./gradlew checkCode`
    
   1. Using Android Studio
      1. Expand `gradle` side-tab (top-right corner in Android Studio IDE).
      1. Click on `Execute gradle task` button (the one with gradle logo)
      1. Type `checkCode` and press OK
      
1. Add your changes to the staging area:
    
    `git add <files>`
    
    Where `<files>` are the files you changed.
    
    > **Note:** Run `git add .` to add all currently modified files to the
    > staging area.

1. Commit your changes:
    
    `git commit -m <message>`
    
    Where `<message>` is a meaningful, short message describing the purpose of
    your changes.

1. Pull changes from the base repository's master branch, resolve conflicts if
necessary:
      
    `git pull upstream master`

1. Push your changes to your github account:
    
    `git push -u origin <branch>`
    
    Where `<branch>` is the branch name you used in step 2.

1. Create a [pull
request](https://help.github.com/articles/about-pull-requests/) to have your
changes reviewed and merged into the base repository. Reference the
[issue](https://github.com/google/ground-android/issues) your changes resolve in
either the commit message for your changes or in your pull request.
    
    > :exclamation: Any subsequent changes committed to the branch you used
    > to open your PR are automatically included in the PR. If you've opened a
    > PR but would like to continue to work on unrelated changes, be sure to
    > start a new branch to track those changes. 

    For more information on creating pull requests, see
    <https://help.github.com/articles/creating-a-pull-request/>. 
    
    To learn more about referencing issues in your pull request or commit
    messages, see
    <https://help.github.com/articles/closing-issues-using-keywords/>.

1. Celebrate!

## Initial build configuration

### Add Google Maps API Key(s)

### Set up Firebase

1. Create a new Firebase project at:

    https://console.firebase.google.com/

2. Add a new Android app with package name `com.google.android.ground`.

3. Add the debug SHA-1 of your device.

   To view the SHA-1 of the debug key generated by Android Studio run:

    ``` 
    $ keytool -list -v -keystore "$HOME/.android/debug.keystore" -alias androiddebugkey -storepass android -keypass android
    ```

4. Download the config file for the Android app to `ground/src/debug/google-services.json`

### Set up Google Cloud Build (optional)

Used to build with Google Cloud and for running integration tests:

1. Install [google-cloud-sdk](https://cloud.google.com/sdk/docs/install)

2. gcloud init
 
3. gcloud auth login
  
4. gcloud config set project [PROJECT_ID]

### Updating GCB Docker Image to latest android sdk tool

1. Ensure that the gcloud project setup is done

2. Open `cloud-builder/Dockerfile-base`

3. Search for `build-tools` in the file and replace the version with the latest version

4. Run the following command 

```
$ cd cloud-builder
$ gcloud builds submit --config=cloudbuild.yaml --substitutions=_ANDROID_VERSION=30
```

5. Ensure that the Docker image is uploaded under "Container Registry" in cloud project

### Troubleshooting

* App crashes on start with following stacktrace:
 
   ```
   java.lang.RuntimeException: Unable to get provider com.google.firebase.provider.FirebaseInitProvider: java.lang.IllegalArgumentException: Given String is empty or null
   ```
    
  Solution: Ensure `ground/src/debug/google-services.json` exists and is valid, as per instructions in [Set up Firebase](#set-up-firebase). You may need to perform a clean build in Android Studio by going to Build -> Clean Project.

* Gradle Build fails with "License for package Android SDK Platform ... not accepted":

  Open to SDK Manager in Android Studio and download and install the required version of the Android
  SDK, accepting relevant licenses agreements when prompted.