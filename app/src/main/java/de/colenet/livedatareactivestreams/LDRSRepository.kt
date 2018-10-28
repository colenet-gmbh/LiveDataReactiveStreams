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
    val githubService by inject<GithubRepositoriesService>()
    val repos: PublishSubject<Outcome<List<Repo>>> = PublishSubject.create()
    val cache: QNCache<Outcome<List<Repo>>>
    val KEY = "REPOS"
    val subscription = CompositeDisposable()

    val USER_KEY = "USER"
    val userCache: QNCache<Outcome<User>>
    val user: PublishSubject<Outcome<User>> = PublishSubject.create()

    init {
        cache = QNCache(false, 600, 60000)
        userCache = QNCache(false, 600, 60000)
    }

    fun fetchRepos(loginName: String?) {
        if (loginName != null) {
            repos.loading(true, cache, KEY)
            updateInBackground(loginName)
        }
    }

    fun pingUser(userName: String?) {
        val c = userCache.get(USER_KEY)
        if (c != null) {
            user.onNext(c)
        }
        else {
            if (userName != null) {
                fetchUser(userName)
            } else {
                user.empty(userCache, USER_KEY)
            }
        }
    }

    fun pingRepo(userName: String?) {
        val c = cache.get(KEY)
        if (c != null) {
            repos.onNext(c)
        }
        else {
            if (userName != null) {
                updateInBackground(userName)
            }
            else {
                repos.empty(cache, KEY)
            }
        }
    }

    fun fetchUser(userName: String) {
        subscription.clear()
        user.loading(true, userCache, USER_KEY)
        subscription.add(githubService.user(userName)
            .subscribeOn(Schedulers.io())
            .delay(4000, TimeUnit.MILLISECONDS, true)
            .subscribe(
                {
                    user.success(it, userCache, USER_KEY)
                    fetchRepos(it.login)
                },
                {
                    user.failed(it, userCache, USER_KEY)
                    repos.failed(it, cache, KEY)
                })
        )
    }

    private fun updateInBackground(loginName: String) {
        subscription.clear()
        subscription.add(githubService.reposForUser(loginName)
            .subscribeOn(Schedulers.io())
            .delay(4000, TimeUnit.MILLISECONDS, true)
            .subscribe(
                {
                    repos.success(it, cache, KEY)
                },
                {
                    repos.failed(it, cache, KEY)
                })
        )
    }
}