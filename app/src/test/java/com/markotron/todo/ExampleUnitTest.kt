package com.markotron.todo

import io.reactivex.Observable
import io.reactivex.subjects.ReplaySubject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).

 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {

    @Test
    @Throws(Exception::class)
    fun addition_isCorrect() {
        assertEquals(4, (2 + 2).toLong())
    }

    @Test
    fun systemOperator() {

        val feedback1: (Observable<Int>) -> Observable<String> = {
            println("start")
            it.map { it.toString() }
        }
        val feedback2: (Observable<Int>) -> Observable<String> = { it.map { (it * it).toString() } }

        val replaySubject = ReplaySubject.createWithSize<Int>(1)
        val feedbacks = arrayOf(feedback1, feedback2)

        val merged = Observable.merge(feedbacks.map { it(replaySubject) })
        merged.doOnNext {
            println(it)
            Thread.sleep(1000)
            replaySubject.onNext(it.toInt())
        }.subscribe()

        replaySubject.onNext(2)
    }
}