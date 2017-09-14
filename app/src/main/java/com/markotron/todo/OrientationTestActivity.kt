package com.markotron.todo

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.SystemClock
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class OrientationTestViewModel : ViewModel() {

    data class State(val inProgress: Boolean = false, val value: String = "")
    sealed class Command {
        object ButtonClick : Command()
        data class ResultFetched(val res: Int) : Command()
    }

    private fun feedback(s: Observable<State>) =
            s.switchMap { (inProgress) ->
                if (inProgress)
                    slowOperationMock
                            .subscribeOn(Schedulers.io())
                            .map { Command.ResultFetched(it) }
                else
                    Observable.empty()
            }

    private val uiCommands = PublishSubject.create<Command>()
    private val replaySubject = ReplaySubject.createWithSize<State>(1)

    private val feedbackCommands = feedback(replaySubject)

    val state: Observable<State> = Observable.merge(uiCommands, feedbackCommands)
            .scan(State()) { state, command ->
                when (command) {
                    is Command.ButtonClick -> state.copy(inProgress = true)
                    is Command.ResultFetched -> State(false, command.res.toString())
                }
            }
            .doOnNext { replaySubject.onNext(it) }
            .replay(1)
            .refCount()

    private val keepAlive = state.subscribe()

    override fun onCleared() {
        super.onCleared()
        keepAlive.dispose()
    }

    fun buttonClick() {
        uiCommands.onNext(Command.ButtonClick)
    }

    private val slowOperationMock = Observable.create<Int> { emitter ->
        Log.d("Operation", "start")
        SystemClock.sleep(5000)
        emitter.onNext(Random().nextInt())
        emitter.onComplete()
        Log.d("Operation", "end")
    }
}


class OrientationTestActivity : AppCompatActivity() {

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewModel = ViewModelProviders.of(this).get(OrientationTestViewModel::class.java)
        disposables.add(RxView.clicks(btn_fetch_value).subscribe { viewModel.buttonClick() })

        disposables.add(viewModel
                .state
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { (inProgress, value) ->
                    if (inProgress) inProgressEffect()
                    else idleEffect()
                    tv_value.text = value
                })
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    private fun inProgressEffect() {
        btn_fetch_value.isEnabled = false;
        progress_bar.visibility = View.VISIBLE
    }

    private fun idleEffect() {
        btn_fetch_value.isEnabled = true;
        progress_bar.visibility = View.GONE
    }

}
