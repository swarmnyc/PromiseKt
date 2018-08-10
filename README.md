# PromiseKt
A simple and easy to use of Promise library for Kotlin on JVM and Android.


---
## Installation For Gradle
### Step 1 - Add it in your root build.gradle at the end of repositories:
``` gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

### Step 2 - Add the dependency (use promisekt for JVM projects, use promisekt-android for Android projects):

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

---
## How to
### 1. Create root Promise objects
There are four ways to create a root Promise object.

- `Promise<T>(executor: (resolve: (T)->Unit, reject: (Throwable) -> Unit))`

This way is the same way of using Promise in Javascript. For example,

``` kotlin
val p = Promise<String> { resolve, reject ->
    resolve("Foo")
    // or 
    reject(Exception("Bar"))
}
```

- `Promise<T>(executor: (promise:Promise<T>))`

This way gives you more control of the Promise object. For example,

``` kotlin
val p = Promise<Int> { promise ->
    promise.resolve(1)
    // or 
    promise.reject(Exception("Bar"))
}
```

- `Promise.resolve<T>(value: T): Promise<T>`

This way gives you return a fulfilled Promise object directly. For example,

``` kotlin
val p = Promise.resolve("Foo")
```

- `Promise.reject(error: Throwable): Promise<*>`

This way gives you return a rejected Promise object directly. For example,

``` kotlin
val p = Promise.reject(Exception("Bar"))
```

### 2. States

Each Promise has 3 states:
- Pending: the default state of a new Promise object.
- Fulfilled: once the `resolve` method of a Promise object is called, the Promise object changes to this state.
- Rejected: once the `reject` method of a Promise object is called or a exception happens, the Promise object changes to this state. 

Once a Promise object finishes execution, the state changes to either fulfilled or rejected. Then, it invokes its children Promise objects. 
Also, once the state of the promise object is changed, the state cannot be changed again. Fore example, 

``` kotlin
Promise<String> { resolve, reject ->
    resolve("Foo") // only this resolve takes effect and changes the state to Fulfilled
    resolve("Bar") // because the state is changed to Fulfilled, this resolve doesn't take effect.
    reject(Exception("Foo")) // because the state is changed to Fulfilled, this resolve doesn't take effect.
}
```

### 3. Create children Promise objects
There are 6 ways to create children Promise objects

- `promise<T>.then(action: (result:T)-> R): Promise<R>`

This method create a new children Promise object with a different result type. Once its parent Promise object is fulfilled, the action invokes. For example,

``` kotlin
Promise<Int> { resolve, _ ->
    resolve(1)
}.then { 
    System.out.println(it) // it = 1
    "Foo" // the return type is Promise<String>
}.then {
    System.out.println(it) // it = "Foo"
    // the return type is Promise<Unit>
}
```

- `promise<T>.thenUi(action: (result:T)-> R): Promise<R>`

This method is same as `.then`, but the action is executed by the UI thread for Android. For example,

``` kotlin
Promise<Int> { resolve, _ ->
    ...
}.thenUi { 
    // update UI
    ...
}
```

However, JVM doesn't have any UI thread, so the action is executed on a new thread for JVM.

- `promise<T>.catch(action: (error:Throwable)-> Unit): Promise<T>`

This method create a new children Promise object with a the same result type. Once its parent Promise object is rejected, the action invokes. For example,

``` kotlin
Promise<Int> { _, reject ->
    reject(Exception("Foo"))
}.then { 
    // this action won't be invoked
}.catch {
    // this action will be invoked
}
```

Once a Promise object is rejected, all its children are rejected too. Also, if an exception is thrown during a catch action, the error changes to the exception. For example,

``` kotlin
val error1 = Exception("Foo")
val error2 = Exception("Bar")
Promise.reject(error1).catch { 
    // this action will be invoked, and it = error1
}.catch {
    // this action will be invoked, and it = error1
}.catch {
    // this action will be invoked, and it = error1
    throw error2 // a new exception
}.catch {
    // this action will be invoked, and it = error2
}
```

- `promise<T>.catchUi(action: (error:Throwable)-> Unit): Promise<T>`

This method is same as `.catch`, but the action is executed by the UI thread for Android.


- `promise<T>.thenChain(action: (result:T)-> R): Promise<R>`

This method is similar to `.then`, but it can unwrap the return value if it is another Promise object. For example,
``` kotlin
fun method1(): Promise<Int> { ... }
fun method2(value: Int): Promise<String> { ... }

// it is the example of handling a nested Promise object without .thenChain
val p1: Promise<Promise<String>> = method1().then{ method2(it) }
p1.then { p2 ->
    p2.then { 
        // it = the result of method 2
    }.catch{
        // have to catch the error of p2 here
    }
}.catch {
    // this catch action cannot catch any error of p2
}

// it is the example of handling a nested Promise object with .thenChain
val p1: Promise<String> = method1().thenChain{ method2(it) /*<-p2*/ }
p1.then {
    // it = the result of method 2
}.catch{
    // this catch action can catch all of errors from p1 and p2.
}
```

- `promise<T>.thenChainUi(action: (result:T)-> R): Promise<R>`

This method is same as `.thenChain`, but the action is executed by the UI thread for Android.

### 4. Multiple Children

Each Promise objects can have multiple children Promise objects. For example,

``` kotlin
val p1: Promise<Int> = Promise { resolve, _ ->
    resolve(1)
}

// create 2 children Promise objects of p1
val p2: Promise<String> = p1.then { // it = 1
    "Foo" 
}

val p3: Promise<Boolean> = p1.then { // it = 1
    true
}

// create a children Promise object of p2
val p4: Promise<Double> = p2.then { // it = "Foo"
    1.1
}
```

### 5. Handle multiple Promise objects

There are 2 ways of handling multiple Promise objects.

- `Promise.all(vararg promises: Promise<*>): Promise<Array<Any>>`
- `Promise.all(promises: Collection<Promise<*>>): Promise<Array<Any>>`

This method wraps all given Promise objects and return a new Promise object. It is fulfilled when all given Promise objects are fulfilled and it is rejected when one of them is rejected. The result of this Promise object is a list that contains all of the result of all given Promise objects. The order of list is the same as the given Promise objects, too. For example,

``` kotlin
val p1 = Promise { resolve, _ ->
    resolve(1)
}

// create 2 children Promise object of p1
val p2 = Promise<String>.then { // it = 1
    "Foo" 
}

Promise.all(p1, p2).then {
    var r1 = it[0] // = 1
    var r2 = it[1] // = "Foo"
}.catch {
   // this action is invoked if one of Promise object is rejected. However, this action can be only invoked once though there might be multiple errors. 
}
```

- `Promise.race<T>(vararg promises: Promise<*>): Promise<Any>>`
- `Promise.all<T>(promises: Collection<Promise<*>>): Promise<Array<Any>>`


This way gives you return a fulfilled Promise object directly. For example,

``` kotlin
val p = Promise.resolve("Foo")
```

- `Promise.reject(error: Throwable): Promise<*>`

This way gives you return a rejected Promise object directly. For example,



### Timeout and Cancel
### Uncaught Error
### config
#### Promise.all
#### Promise.race

### proguard
```
-keep class com.swarmnyc.** { *; }
-keep interface com.swarmnyc.** { *; }
-dontnote com.swarmnyc.**
```