package com.alorma.myapplication.ui.movies

import android.app.Instrumentation
import android.content.Intent
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.Intents.intending
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.alorma.myapplication.R
import com.alorma.myapplication.config.ProjectTestRule
import com.alorma.myapplication.config.configureRxThreading
import com.alorma.myapplication.domain.model.Images
import com.alorma.myapplication.domain.model.Movie
import com.alorma.myapplication.domain.repository.ConfigurationRepository
import com.alorma.myapplication.domain.repository.MoviesRepository
import com.alorma.myapplication.ui.detail.MovieDetailActivity
import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.schibsted.spain.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaListInteractions.clickListItem
import com.schibsted.spain.barista.interaction.BaristaListInteractions.scrollListToPosition
import io.reactivex.Single
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*

class MoviesActivityTest {
    @get:Rule
    val rule = ProjectTestRule(MoviesActivity::class.java, this)

    val moviesRepository: MoviesRepository = mock()
    val configRepository: ConfigurationRepository = mock()

    init {
        configureRxThreading()
    }

    @Before
    fun setup() {
        given(configRepository.getConfig()).willReturn(Single.just(mock()))
    }

    @Test
    fun onLoadError_showErrorOnScreen() {
        given(moviesRepository.listAll()).willReturn(Single.error(Exception()))

        rule.run()

        assertDisplayed(R.string.generic_error)
    }

    @Test
    fun onLoadItems_showOnScreen() {
        val items = generateItems(50)
        given(moviesRepository.listAll()).willReturn(Single.just(items))

        rule.run()

        assertRecyclerViewItemCount(R.id.recycler, 50)
        assertDisplayed("Title 1")
        assertDisplayed("Title 2")
    }

    @Test
    fun onClickItem_openDetail() {
        Intents.init()
        intending(getMatcherOpenDetailActivity()).respondWith(getGenericResult())

        val items = generateItems(50)
        given(moviesRepository.listAll()).willReturn(Single.just(items))

        rule.run()

        clickListItem(R.id.recycler, 1)

        intended(getMatcherOpenDetailActivity())
        Intents.release()
    }

    @Test
    fun onLoadManyItems_onScroll_loadMore() {
        val items = generateItems(50)
        given(moviesRepository.listAll()).willReturn(Single.just(items))
        val itemsPage = generateItems(75)
        given(moviesRepository.listNextPage()).willReturn(Single.just(itemsPage))

        rule.run()

        assertRecyclerViewItemCount(R.id.recycler, 50)

        scrollListToPosition(R.id.recycler, 49)

        assertRecyclerViewItemCount(R.id.recycler, 75)
    }

    private fun generateItems(number: Int): List<Movie> = (1..number).map { generateItem(it) }

    private fun generateItem(id: Int): Movie = Movie(id, "Title $id", "", Images("", ""), Date(), 0f, listOf())

    private fun getMatcherOpenDetailActivity(): Matcher<Intent> = hasComponent(MovieDetailActivity::class.java.name)

    private fun getGenericResult(): Instrumentation.ActivityResult = Instrumentation.ActivityResult(2, Intent())
}