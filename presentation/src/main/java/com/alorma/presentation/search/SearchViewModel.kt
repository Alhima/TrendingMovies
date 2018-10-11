package com.alorma.presentation.search

import com.alorma.domain.model.Configuration
import com.alorma.domain.model.Movie
import com.alorma.domain.usecase.ObtainConfigurationUseCase
import com.alorma.domain.usecase.SearchMoviesUseCase
import com.alorma.presentation.common.BaseViewModel
import com.alorma.presentation.common.Event
import com.alorma.presentation.common.observeOnUI
import io.reactivex.Single
import io.reactivex.functions.BiFunction

class SearchViewModel(
        private val states: SearchStates,
        private val searchRoutes: SearchRoutes,
        private val obtainMoviesUseCase: SearchMoviesUseCase,
        private val obtainConfigurationUseCase: ObtainConfigurationUseCase) :
        BaseViewModel<SearchStates.SearchState, SearchRoutes.SearchRoute, SearchActions.SearchAction, Event>() {

    private lateinit var query: String

    override infix fun reduce(action: SearchActions.SearchAction) {
        when (action) {
            is SearchActions.SearchAction.NewQuery -> {
                this.query = action.query
                search()
            }
            is SearchActions.SearchAction.LoadPage -> searchPage()
            SearchActions.SearchAction.CleanSearch -> {
                this.query = ""
            }
            SearchActions.SearchAction.Back -> navigate(searchRoutes.back())
            is SearchActions.SearchAction.OpenDetail -> openDetail(action)
            SearchActions.SearchAction.Retry -> search()
        }
    }

    private fun search() {
        clear()
        val disposable = Single.zip(
                obtainConfigurationUseCase.execute(),
                obtainMoviesUseCase.execute(query),
                BiFunction<Configuration, List<Movie>, Pair<Configuration, List<Movie>>> { conf, list ->
                    conf to list
                })
                .doOnSubscribe { render(states loading true) }
                .doOnSuccess { render(states loading false) }
                .observeOnUI()
                .subscribe(
                        { render(states.success(it)) },
                        { render(states error it) }
                )
        addDisposable(disposable)
    }

    private fun searchPage() {
        val disposable = Single.zip(
                obtainConfigurationUseCase.execute(),
                obtainMoviesUseCase.executeNextPage(query),
                BiFunction<Configuration, List<Movie>, Pair<Configuration, List<Movie>>> { conf, list ->
                    conf to list
                })
                .observeOnUI()
                .subscribe(
                        { render(states.success(it, true)) },
                        { render(states error it) }
                )
        addDisposable(disposable)
    }

    private fun openDetail(action: SearchActions.SearchAction.OpenDetail) {
        navigate(searchRoutes.detail(action.movie))
    }
}