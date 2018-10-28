package de.colenet.livedatareactivestreams

import android.view.View
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

fun <T> Observable<T>.toLiveData(compositeDisposable: CompositeDisposable): LiveData<T> {
    val data = MutableLiveData<T>()
    compositeDisposable.add(this
        .subscribe({ t: T -> data.postValue(t) }))
    return data
}

fun <T> Observable<T>.toObservableField(compositeDisposable: CompositeDisposable): ObservableField<T> {
    val data = ObservableField<T>()
    compositeDisposable.add(
        this
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { t: T -> data.set(t) }
    )
    return data
}

fun <T> Observable<Outcome<T>>.mapLoadingToVisibility(): Observable<Int> {
    return this
        .map { it ->
            when(it) {
                is Outcome.Success -> View.GONE
                is Outcome.Failure -> View.GONE
                is Outcome.Progress -> if (it.loading) View.VISIBLE else View.GONE
            }
        }
}

fun <T> Observable<Outcome<T>>.mapErrorVisibility(): Observable<Int> {
    return this
        .map { it ->
            when(it) {
                is Outcome.Success -> View.GONE
                is Outcome.Failure -> View.VISIBLE
                is Outcome.Progress -> View.GONE
            }
        }
}

fun <T, O> Observable<Outcome<T>>.mapSuccess(default: O, mapper: (T) -> O): Observable<O> {
    return this
        .map { it ->
            when(it) {
                is Outcome.Success -> mapper(it.data)
                is Outcome.Failure -> default
                is Outcome.Progress -> default
            }
        }
}

fun <T, O> Observable<Outcome<T>>.mapError(default: O, mapper: (Throwable) -> O): Observable<O> {
    return this
        .map { it ->
            when(it) {
                is Outcome.Success -> default
                is Outcome.Failure -> mapper(it.e)
                is Outcome.Progress -> default
            }
        }
}


class MainViewModel: ViewModel(), KoinComponent {

    val repository by inject<LDRSRepository>()
    val bag = CompositeDisposable()
    var repos: LiveData<String>
    val loading: ObservableField<Int>
    var error: LiveData<Int>
    var error_text: LiveData<String>

    init {
        repos = repository.repos
            .mapSuccess(default = "") { it.joinToString(" ######### ")}
            .toLiveData(bag)

        loading = repository.repos
            .mapLoadingToVisibility()
            .toObservableField(bag)

        error = repository.repos
            .mapErrorVisibility()
            .toLiveData(bag)

        error_text = repository.repos
            .mapError("") {it.localizedMessage}
            .toLiveData(bag)
    }

    fun triggerPing() {
        repository.ping()
    }

    fun triggerRefresh() {
        repository.fetchRepos()
    }

    fun triggerError() {
        repository.fetchWithError()
    }

    override fun onCleared() {
        super.onCleared()
        bag.dispose()
    }

}