package com.markotron.todo

import android.util.Log
import io.reactivex.Observable

/**
 * Created by markotron on 14/09/2017.
 */
fun <T> Observable<T>.log(id: String = ""): Observable<T> =
        this
                .doOnNext { Log.d("Log", "Next: $id:\t$it") }
                .doOnComplete { Log.d("Log", "Complete: $id") }
                .doOnError { Log.d("Log", "Error: $id:\t$it") }
                .doOnSubscribe { Log.d("Log", "Subscribe: $id") }
                .doOnDispose { Log.d("Log", "Dispose: $id") }
