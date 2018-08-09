# PromiseKt
A simple and easy to use of Promise library for Kotlin on JVM and Android.


## Installation For Gradle
### Step 1
Add it in your root build.gradle at the end of repositories:
``` gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2
Add the dependency (use promisekt for jvm project, use promisekt-android project):

[![Release](https://jitpack.io/v/swarmnyc/promisekt.svg)](https://jitpack.io/#swarmnyc/promisekt) <-- Latest Version
``` gradle
dependencies {
    // for kotlin-jvm project
    implementation 'com.github.swarmnyc.promisekt:promisekt:$version'

    // for kotlin-android project
    implementation 'com.github.swarmnyc.promisekt:promisekt-android:$version'
}
```

the main different of promisekt-android is that promisekt-android uses Looper.getMainLooper() switches threads to the UI thread when executing the handlers of `.thenUi` or `.catchUi`

## Usages

-[Create Promises](#Create%20Promises)
-[Create Promises](#Create%20Promises)

### Create Promises
### Then and Catch
### ThenChain
### Threads and UI Thread
### Timeout and Cancel
### Uncaught Error

#### Promise.all
#### Promise.race