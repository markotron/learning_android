package com.markotron.todo

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.androidhuman.rxfirebase2.database.dataChangesOf
import com.androidhuman.rxfirebase2.database.rxSetValue
import com.google.firebase.database.FirebaseDatabase
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_todo.*
import java.util.*

object FirebaseRepo {
    private val database = FirebaseDatabase.getInstance()
    private val reference = database.getReference("v0/todos")

    fun saveRandomStringToFirebase() {
        val randomString = UUID.randomUUID().toString()
        reference.rxSetValue(randomString)
                .subscribe { Log.d("Log", "Value successfully set!") }
    }

    val changes = reference
            .dataChangesOf<String>()
}

class FirebaseTestViewModel : ViewModel() {

    private val disposableBag = CompositeDisposable()

    sealed class Command {
        object ButtonClickCommand : Command()
        data class ValueChanged(val value: String) : Command()
    }

    sealed class State {
        object SavingToFirebase : State()
        data class ValueChanged(val value: String = "No value") : State()
    }

    private val valueChangedFeedback = PublishSubject.create<Command>()
    private val uiChangedFeedback = PublishSubject.create<Command>()

    private val commands = valueChangedFeedback.mergeWith(uiChangedFeedback)

    val state = commands
            .scan(State.ValueChanged() as State) { _, command ->
                when (command) {
                    is Command.ButtonClickCommand -> State.SavingToFirebase
                    is Command.ValueChanged -> State.ValueChanged(command.value)
                }
            }
            .replay(1)
            .refCount()

    private val keepAlive = state.subscribe()

    init {
        disposableBag.add(keepAlive)
        disposableBag.add(FirebaseRepo
                .changes
                .subscribe {
                    if (it.isPresent)
                        valueChangedFeedback.onNext(Command.ValueChanged(it.get()))
                })
    }

    override fun onCleared() {
        super.onCleared()
        disposableBag.dispose()
    }

    // commands
    fun buttonClicked() {
        uiChangedFeedback.onNext(Command.ButtonClickCommand)
    }
}


class FirebaseTestActivity : AppCompatActivity() {

    private val disposableBag = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo)

        val viewModel = ViewModelProviders.of(this).get(FirebaseTestViewModel::class.java)

        RxView
                .clicks(btn_save)
                .subscribe { viewModel.buttonClicked() }

        disposableBag.add(viewModel
                .state
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        is FirebaseTestViewModel.State.SavingToFirebase ->
                            FirebaseRepo.saveRandomStringToFirebase()
                        is FirebaseTestViewModel.State.ValueChanged -> changeValue(it.value)
                    }

                })
    }

    private fun changeValue(s: String) {
        tv_random_string.text = s
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableBag.dispose()
    }
}
