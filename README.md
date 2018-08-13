# PromiseKt
A simple and easy to use of Promise library for Kotlin on JVM and Android.


# Installation For Gradle
## Step 1 - Add it in your root build.gradle at the end of repositories:

``` gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```


## Step 2 - Add the dependency:
[![Release](https://jitpack.io/v/swarmnyc/promisekt.svg)](https://jitpack.io/#swarmnyc/promisekt) <-- Latest Version
``` gradle
dependencies {
    // use one of these dependencies. 
    // promisekt for JVM projects and promisekt-android for Android projects

    implementation 'com.github.swarmnyc.promisekt:promisekt:$version'

    implementation 'com.github.swarmnyc.promisekt:promisekt-android:$version'
}
```

the main different of promisekt-android is that promisekt-android uses Looper.getMainLooper() switches threads to the UI thread when executing the handlers of `.thenUi` and `.catchUi`


# How to

1. [Create root Promise objects](#1-create-root-promise-objects)
1. [States](#2-states)
1. [Create children Promise objects](#3-create-children-promise-objects)
1. [Multiple Children](#4-multiple-children)
1. [Handle multiple Promise objects](#5-handle-multiple-promise-objects)
1. [Cancel and Timeout](#6-cancel-and-timeout)
1. [Uncaught Error](#7-uncaught-error)
1. [Threads](#8-threads)
1. [Options](#9-options)
1. [proguard settings](#10-proguard-settings)

## 1. Create root Promise objects
There are four ways to create root Promise objects:

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

This way returns a fulfilled Promise object directly. For example,

``` kotlin
val p = Promise.resolve("Foo")
```

- `Promise.reject(error: Throwable): Promise<*>`

This way returns a rejected Promise object directly. For example,

``` kotlin
val p = Promise.reject(Exception("Bar"))
```


## 2. States
Each Promise has 3 states:
- Pending: the default state of a new Promise object.
- Fulfilled: once the `resolve` method of a Promise object is called, the Promise object changes to this state.
- Rejected: once the `reject` method of a Promise object is called or a exception thrown, the Promise object changes to this state. 

Once a Promise object finishes execution, the state changes to either fulfilled or rejected. Then, it invokes its children Promise objects. 
Also, once the state of the promise object is changed, the state cannot be changed again. Fore example, 

``` kotlin
Promise<String> { resolve, reject ->
    resolve("Foo") // only this resolve takes effect and changes the state to Fulfilled
    resolve("Bar") // because the state is changed to Fulfilled, this resolve doesn't take effect.
    reject(Exception("Foo")) // because the state is changed to Fulfilled, this reject doesn't take effect.
}
```


## 3. Create children Promise objects

There are 6 ways to create children Promise objects:

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

This method serves as same as `.then`, but the action is executed by the UI thread for Android. For example,

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

This method serves as same as `.catch`, but the action is executed by the UI thread for Android.


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
    }.catch {
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

This method serves as same as `.thenChain`, but the action is executed by the UI thread for Android.

## 4. Multiple Children

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

## 5. Handle multiple Promise objects

There are 2 ways of handling multiple Promise objects.

- `Promise.all(vararg promises: Promise<*>): Promise<Array<Any>>`
- `Promise.all(promises: Collection<Promise<*>>): Promise<Array<Any>>`

These methods wraps all given Promise objects and return a new Promise object. It is fulfilled when all given Promise objects are fulfilled and it is rejected when one of them is rejected. The result of this Promise object is a list that contains all of the result of all given Promise objects. The order of list is the same as the given Promise objects, too. For example,

``` kotlin
val p1 = Promise { resolve, _ ->
    resolve(1)
}

val p2 = Promise<String>.then { // it = 1
    "Foo" 
}

Promise.all(p1, p2).then {
    var v1 = it[0] // v1 = 1
    var v2 = it[1] // v2 = "Foo"
}.catch {
   // This action is invoked if one of Promise object is rejected. 
   // However, this action can be only invoked once although there might be multiple errors. 
}
```

- `Promise.race(vararg promises: Promise<*>): Promise<Any>`
- `Promise.race(promises: Collection<Promise<*>>): Promise<Any>`

This method wraps all given Promise objects and return a new Promise object. It is fulfilled or rejected when the first finished Promise object is fulfilled or rejected. For example,

``` kotlin
val p1 = Promise { resolve, _ ->
    Thread.sleep(100)
    resolve(1)
}

val p2 = Promise<String>.then { resolve, _ ->
    Thread.sleep(50)
    resolve("Foo")
}

Promise.race(p1, p2).then {
    var v:Any = it // v = "Foo"
}
```

## 6. Cancel and Timeout
PromiseKt supports cancel and timeout. 

- `promise.cancel(throwError: Boolean = false)`
This method cancels the execution by interrupting the execution thread if the thread isn't finished. Once the method is called, the state changed to rejected. If the parameter `throwError` is false, then the InterruptedException is ignored. Otherwise, the InterruptedException is thrown and it can be catch by `.catch`. For example,
``` kotlin
val p = Promise { resolve, _ ->
    Thread.sleep(1000)
    resolve(1)
}.catch {
    // it = InterruptedException
}

p.cancel(true)
```

- `promise<T>.timeout(timeMs: Long, throwError: Boolean = false): Promise<T>`

This method sets a timer and delays for the given time. After delaying, if the state of the Promise object is still pending, calls `.cancel(throwError)`. For example,

``` kotlin
Promise { resolve, _ ->
    Thread.sleep(1000)
    resolve(1)
}.catch {
    // it = InterruptedException
}.timeout(500, true)
```

By default, if `.timeout` isn't called, Promise objects run forever until resolve or reject is called.

## 7. Uncaught Error

If there is a error which isn't caught, then the error passes to `Promise.uncaught`. The default handler does logging it and throwing it again. Therefore, by default, if there is an uncaught error, it causes the program crash. In order to avoid it, you can set your handler to handle uncaught error. For example,

``` kotlin
// there two situations cause uncaught errors

// 1. .catch isn't called
Promise<String> { resolve, reject ->
    reject(Exception("Foo"))
}.then { }

// 2. a error inside of catch action
Promise<String> { resolve, reject ->
    reject(Exception("Foo"))
}.catch {
    throw Exception("Bar")
}

// set your uncaughtError handler
Promise.uncaughtError = {
    // handle the error
    it.printStackTrace()
}
```

## 8. Threads

Each root Promise objects use its own thread and the children Promise objects use the thread as its parent unless using one of .thenUi, .thenChainUi or .catchUi to create the children Promise objects. For example,

``` kotlin
// the current Thread id is 1

Promise<String> { resolve, reject ->
    // running in a new Thread, The Thread id is 2
}.then { 
    // running in the same Thread, its id is 2
}.catch {
    // running in the same Thread, its id is 2
}

Promise<String> { resolve, reject ->
    // running in a new Thread, The Thread id is 3
}.thenUi { 
    // running in the UI Thread
}.catchUi {
    // running in the UI Thread
}
```

## 9. Options

By default, if you don't assign a specific options, every Promise objects use the default options and the children Promise objects use the same options as its parent. For example,

``` kotlin
// define a new options
val options = object : PromiseOptions() {
    // make .thenUi, .thenChainUi and .catchUi run in the same Thread as its parent
    override var uiExecutor: Executor = Executor { command ->  command.run() }
}

// use the specific options for this Promise object and its children
Promise<String> (options) { resolve, reject ->
    ...
}

// or you can change the default options
Promise.defaultOptions.uiExecutor = Executor { command ->  command.run() }
```

## 10. proguard settings
This is settings for proguard

```
-keep class com.swarmnyc.promisekt.** { *; }
-keep interface com.swarmnyc.promisekt.** { *; }
-dontnote com.swarmnyc.promisekt.**
```