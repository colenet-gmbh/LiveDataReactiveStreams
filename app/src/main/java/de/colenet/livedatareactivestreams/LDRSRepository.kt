package de.colenet.livedatareactivestreams

import com.fewlaps.quitnowcache.QNCache
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.concurrent.TimeUnit

class OutcomeCacheItem<T>(val publisher: PublishSubject<Outcome<T>>, ttl: Long, unit: TimeUnit, val key: String = "KEY") {

    val cache: QNCache<Outcome<T>>
    val keepAliveMillis: Long

    init {
        keepAliveMillis = unit.toMillis(ttl)
        val keepAlive: Int = (unit.toSeconds(ttl) * 2).toInt()
        cache = QNCache(false, keepAlive, keepAliveMillis )
    }

    fun publishOrRefresh(refresh: (OutcomeCacheItem<T>) -> Unit) {
        val cachedOutcome = cache.get(key)
        if (cachedOutcome != null) {
            when(cachedOutcome) {
                is Outcome.Success -> publisher.onNext(cachedOutcome)
                is Outcome.Progress -> publisher.onNext(cachedOutcome)
                is Outcome.Empty -> {
                    publisher.onNext(cachedOutcome)
                }
                is Outcome.Failure -> {
                    publisher.onNext(cachedOutcome)
                    refresh(this)
                }
            }
        }
        else {
            refresh(this)
        }
    }

    fun newValue(value: Outcome<T>) {
        cache.set(key, value)
        publisher.onNext(value)
    }

    fun clear() {
        cache.clear()
    }
}

fun <T> OutcomeCacheItem<T>.failed(e: Throwable) {
    val outcome = Outcome.failure<T>(e)
    with(this){
        publisher.loading(false)
        newValue(outcome)
    }
}

fun <T> OutcomeCacheItem<T>.success(v: T) {
    val outcome = Outcome.success(v)
    with(this){
        publisher.loading(false)
        newValue(outcome)
    }
}

fun <T> OutcomeCacheItem<T>.loading(isLoading: Boolean) {
    val outcome = Outcome.loading<T>(isLoading)
    with(this){
        newValue(outcome)
    }
}

fun <T> OutcomeCacheItem<T>.empty() {
    val outcome = Outcome.empty<T>()
    with(this){
        newValue(outcome)
    }
}


class LDRSRepository: KoinComponent {
    val githubService by inject<GithubRepositoriesService>()
    val subscription = CompositeDisposable()

    val repos: PublishSubject<Outcome<List<Repo>>> = PublishSubject.create()
    val user: PublishSubject<Outcome<User>> = PublishSubject.create()
    private val userCache = OutcomeCacheItem(user, 60, TimeUnit.SECONDS)
    private val reposCache = OutcomeCacheItem(repos, 60, TimeUnit.SECONDS)

    init {
        reposCache.empty()
        userCache.empty()
    }

    fun fetchRepos(loginName: String?) {
        reposCache.loading(true)
        if (loginName != null) {
            updateInBackground(loginName)
        }
    }

    fun pingUser(userName: String?) {
        userCache.publishOrRefresh {
            fetchUser(userName)
        }
    }

    fun pingRepo(userName: String?) {
        reposCache.publishOrRefresh {
            fetchRepos(userName)
        }
    }

    fun fetchUser(userName: String?) {
        if (userName != null) {
            subscription.clear()
            userCache.loading(true)
            subscription.add(githubService.user(userName)
                .subscribeOn(Schedulers.io())
                .delay(4000, TimeUnit.MILLISECONDS, true)
                .subscribe(
                    {
                        userCache.success(it)
                        reposCache.clear()
                        fetchRepos(it.login)
                    },
                    {
                        userCache.failed(it)
                    })
            )
        }
    }

    private fun updateInBackground(loginName: String) {
        subscription.clear()
        subscription.add(githubService.reposForUser(loginName)
            .subscribeOn(Schedulers.io())
            .delay(4000, TimeUnit.MILLISECONDS, true)
            .subscribe(
                {
                    reposCache.success(it)
                },
                {
                    reposCache.failed(it)
                })
        )
    }
}