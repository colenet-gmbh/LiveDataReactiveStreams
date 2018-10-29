package de.colenet.livedatareactivestreams

import android.annotation.SuppressLint
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

fun <T> Observable<T>.toLiveData(compositeDisposable: CompositeDisposable): LiveData<T> {
    val data = MutableLiveData<T>()
    compositeDisposable.add(this
            // postValue will execute on mainThread so no observeOn
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
                is Outcome.Empty -> View.GONE
            }
        }
}


fun <T, O> Observable<Outcome<T>>.mapLoading(nonLoading: O, mapper: (Boolean) -> O): Observable<O> {
    return this
        .map { it ->
            when(it) {
                is Outcome.Success -> nonLoading
                is Outcome.Failure -> nonLoading
                is Outcome.Empty -> nonLoading
                is Outcome.Progress -> mapper(it.loading)
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
                is Outcome.Empty -> View.GONE
            }
        }
}

fun <T, O> Observable<Outcome<T>>.mapSuccess(nonSuccess: O, mapper: (T) -> O): Observable<O> {
    return this
        .map { it ->
            when(it) {
                is Outcome.Success -> mapper(it.data)
                is Outcome.Failure -> nonSuccess
                is Outcome.Progress -> nonSuccess
                is Outcome.Empty -> nonSuccess
            }
        }
}

fun <T, O> Observable<Outcome<T>>.mapError(nonError: O, mapper: (Throwable) -> O): Observable<O> {
    return this
        .map { it ->
            when(it) {
                is Outcome.Success -> nonError
                is Outcome.Failure -> mapper(it.e)
                is Outcome.Progress -> nonError
                is Outcome.Empty -> nonError
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

    val user_name: LiveData<String>
    val user_bio: LiveData<String>
    val search_name = MutableLiveData<String>()
    val user_visible: LiveData<Int>
    val repos_loading: LiveData<Int>

    init {
        repos = repository.repos
            .mapSuccess(nonSuccess = "") { it.joinToString(" ######### ")}
            .toLiveData(bag)

        // using generalized form instead of this:
        // loading = repository.repos
        //   .mapLoadingToVisibility()
        //   .toObservableField(bag)
        //
        loading = repository.user
            .mapLoading(nonLoading = GONE) { if (it) VISIBLE else GONE }
            .toObservableField(bag)

        error = Observables.combineLatest(
                    repository.repos.mapError(nonError = false) { true },
                    repository.user.mapError(nonError = false) { true}
            )
            .map { if (it.first || it.second) VISIBLE else GONE }
            .toLiveData(bag)

        error_text = repository.user
            .mapError("") {it.localizedMessage}
            .toLiveData(bag)

        user_name = repository.user
            .mapSuccess("") { if (it.login != null) it.login else ""}
            .toLiveData(bag)

        user_bio = repository.user
            .mapSuccess("") { if (it.bio != null) it.bio else "Nicht vorhanden"}
            .toLiveData(bag)

        user_visible = repository.user
            .mapSuccess(nonSuccess = GONE) { VISIBLE }
            .toLiveData(bag)

        repos_loading = repository.repos
            .mapLoadingToVisibility()
            .toLiveData(bag)
    }

    fun triggerPing() {
        repository.pingRepo(search_name.value)
        repository.pingUser(search_name.value)
    }

    fun triggerRefresh() {
        val name = search_name.value
        if (name != null) {
            repository.fetchUser(name)
        }
    }

    fun search() {
        val query = search_name.value
        if (query != null) {
            repository.fetchUser(query)
        }
    }
    override fun onCleared() {
        super.onCleared()
        bag.dispose()
    }

}