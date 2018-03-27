# Ground for Android

*Ground* is a free, map-centric platform for occasionally connected devices.

This is not an officially supported Google product; it is currently being
developed on a best-effort basis.

The project is currently undergoing major architectural and UI changes.
Please check back periodically for updates.

## Initial build configuration

### Add Google Maps API Key(s)

Create google_maps_api.xml files in gnd/src/debug/res/values and
gnd/src/release/res/values, replacing API_KEY with debug and release Google Maps
API keys:

``` xml
<resources>
  <string name="google_maps_key" templateMergeStrategy="preserve"
  translatable="false">API_KEY</string>
</resources>
```

You can generate new keys at:

  https://developers.google.com/maps/documentation/android-api/signup.

Be sure SHA1 fingerprint in keystore is registered in:

  https://pantheon.corp.google.com/apis/credentials

### Set up Firebase

1. Create a new Firebase project at:

    https://console.firebase.google.com/

2. Save config file for Android app to gnd/google-services.json:

    https://support.google.com/firebase/answer/7015592

This includes the API key and URL for your new Firebase project.
