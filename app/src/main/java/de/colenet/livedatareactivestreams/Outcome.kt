package de.colenet.livedatareactivestreams

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fewlaps.quitnowcache.QNCache
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

sealed class Outcome<T> {
    data class Progress<T>(var loading: Boolean) : Outcome<T>()
    data class Success<T>(var data: T) : Outcome<T>()
    data class Failure<T>(val e: Throwable) : Outcome<T>()
    data class Empty<T>(val empty: Boolean = true) : Outcome<T>()

    companion object {
        fun <T> loading(isLoading: Boolean): Outcome<T> = Progress(isLoading)

        fun <T> success(data: T): Outcome<T> = Success(data)

        fun <T> failure(e: Throwable): Outcome<T> = Failure(e)

        fun <T> empty(): Outcome<T> = Empty()
    }
}

/**
 * Extension function to convert a Publish subject into a LiveData by subscribing to it.
 **/
fun <T> PublishSubject<T>.toLiveData(compositeDisposable: CompositeDisposable): LiveData<T> {
    val data = MutableLiveData<T>()
    compositeDisposable.add(this
        .subscribeOn(AndroidSchedulers.mainThread())
        .subscribe({ t: T -> data.value = t }))
    return data
}

/**
 * Extension function to push a failed event with an exception to the observing outcome
 * */
fun <T> PublishSubject<Outcome<T>>.failed(e: Throwable) {
    val outcome = Outcome.failure<T>(e)
    with(this){
        loading(false)
        onNext(outcome)
    }
}

/**
 * Extension function to push  a success event with data to the observing outcome
 * */
fun <T> PublishSubject<Outcome<T>>.success(t: T) {
    val outcome = Outcome.success(t)
    with(this){
        loading(false)
        onNext(outcome)
    }
}

/**
 * Extension function to push the loading status to the observing outcome
 * */
fun <T> PublishSubject<Outcome<T>>.loading(isLoading: Boolean) {
    val outcome = Outcome.loading<T>(isLoading)
    this.onNext(outcome)
}

/**
 * Extension function to push the loading status to the observing outcome
 * */
fun <T> PublishSubject<Outcome<T>>.empty() {
    val outcome = Outcome.empty<T>()
    this.onNext(outcome)
}