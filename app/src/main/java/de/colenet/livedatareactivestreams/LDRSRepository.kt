package de.colenet.livedatareactivestreams

import android.annotation.SuppressLint
import android.util.Log
import com.fewlaps.quitnowcache.QNCache
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.concurrent.TimeUnit


class LDRSRepository: KoinComponent {
    val TAG = ">>>>>> THREADING"
    val githubService by inject<GithubRepositoriesService>()
    val repos: PublishSubject<Outcome<List<Repo>>> = PublishSubject.create()
    val cache: QNCache<Outcome<List<Repo>>>
    val KEY = "REPOS"
    val subscription = CompositeDisposable()

    init {
        cache = QNCache(false, 600, 60000)
    }

    @SuppressLint("CheckResult")
    fun fetchRepos() {
        repos.loading(true)
        cache.set(KEY, Outcome.loading(true))
        updateInBackground()
    }

    private fun httpObserver() =
        object : Observer<List<Repo>> {
            override fun onComplete() {
                Log.e(TAG, "fetchRepos: onComplete Thread ${Thread.currentThread().id}")
            }

            override fun onSubscribe(d: Disposable) {
                Log.e(TAG, "fetchRepos: onSubscribe Thread ${Thread.currentThread().id}")
            }

            override fun onNext(t: List<Repo>) {
                Log.e(TAG, "fetchRepos: onNext Thread ${Thread.currentThread().id}")
                cache.set(KEY, Outcome.success(t))
                repos.success(t)
            }

            override fun onError(e: Throwable) {
                Log.e(TAG, "fetchRepos: onError Thread ${Thread.currentThread().id}")
                cache.set(KEY, Outcome.failure(e))
                repos.failed(e)
            }
        }


    fun fetchWithError() {
        subscription.clear()
        repos.loading(true)
        subscription.add(githubService.reposForUser("idonotexist-really")
            .subscribeOn(Schedulers.io())
            .delay(4000, TimeUnit.MILLISECONDS, true)
            .subscribe( {
                    cache.set(KEY, Outcome.success(it))
                    repos.success(it)
                },
                {
                    cache.set(KEY, Outcome.failure(it))
                    repos.failed(it)
                }
            )
        )
    }

    fun ping() {
        val c = cache.get(KEY)
        if (c != null) {
            repos.onNext(c)
            updateInBackground()
        }
        else {
            fetchRepos()
        }
    }

    private fun updateInBackground() {
        subscription.clear()
        subscription.add(githubService.reposForUser("colenet-gmbh")
            .subscribeOn(Schedulers.io())
            .delay(4000, TimeUnit.MILLISECONDS, true)
            .subscribe(
                {
                    Log.e(TAG, "fetchRepos: onNext Thread ${Thread.currentThread().id}")
                    cache.set(KEY, Outcome.success(it))
                    repos.success(it)
                },
                {
                    Log.e(TAG, "fetchRepos: onError Thread ${Thread.currentThread().id}")
                    cache.set(KEY, Outcome.failure(it))
                    repos.failed(it)
                })
        )
    }
}