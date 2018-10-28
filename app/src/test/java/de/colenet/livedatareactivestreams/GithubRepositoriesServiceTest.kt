package de.colenet.livedatareactivestreams

import io.reactivex.observers.TestObserver
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test

class GithubRepositoriesServiceTest {

    val service = GithubRepositoriesService()

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun reposForUser() {
        val test = TestObserver<List<Repo>>()

        val repos = service.reposForUser("colenet-gmbh")
        repos.subscribe(test)
        val actual = test.await()

        test.assertComplete()
        test.assertNoErrors()

        Assertions.assertThat(actual).isNotNull()
        Assertions.assertThat(actual.values().first().size).isGreaterThan(0)
    }
}