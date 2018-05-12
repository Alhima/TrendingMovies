package com.alorma.myapplication.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.ImageView
import com.alorma.myapplication.R
import com.alorma.myapplication.TrendingTvApp.Companion.component
import com.alorma.myapplication.ui.common.BaseView
import com.alorma.myapplication.ui.common.adapterDsl
import com.alorma.myapplication.ui.common.dsl
import com.alorma.myapplication.ui.detail.di.DetailModule
import com.alorma.myapplication.ui.shows.ShowsActivity
import com.alorma.myapplication.ui.shows.TvShowVM
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.detail_activity.*
import kotlinx.android.synthetic.main.detail_content.*
import kotlinx.android.synthetic.main.detail_genre_chip.view.*
import kotlinx.android.synthetic.main.row_similar_show.view.*
import javax.inject.Inject

class ShowDetailActivity : AppCompatActivity(), BaseView<DetailStates.DetailState> {
    companion object {
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_TITLE = "extra_title"

        fun launch(context: Context, id: Int, title: String): Intent =
                Intent(context, ShowDetailActivity::class.java).apply {
                    putExtra(EXTRA_ID, id)
                    putExtra(EXTRA_TITLE, title)
                }
    }

    @Inject
    lateinit var actions: DetailActions

    @Inject
    lateinit var presenter: ShowDetailPresenter

    private val recyclerViewListener: RecyclerView.OnScrollListener by lazy {
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                (recyclerView.layoutManager as? LinearLayoutManager)?.apply {
                    val firstVisibleItemPosition = findFirstVisibleItemPosition()
                    val last = childCount + firstVisibleItemPosition
                    if (last >= itemCount - ShowsActivity.OFFSET_LAZY_LOAD) {
                        disablePagination()
                        presenter reduce actions.loadSimilarPage()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detail_activity)

        component add DetailModule(this) inject this

        presenter init this

        initData()

        toolbar.dsl {
            back { action = { presenter reduce actions.back() } }
        }
    }

    private fun initData() {
        intent.extras?.let {
            it.getString(EXTRA_TITLE)?.let {
                toolbar.title = it
            }
            it.getInt(EXTRA_ID, -1).takeIf { it != -1 }?.let {
                presenter reduce actions.load(it)
            } ?: presenter reduce actions.back()
        }
    }

    override fun render(state: DetailStates.DetailState) {
        when (state) {
            is DetailStates.DetailState.Success -> onSuccess(state)
            is DetailStates.DetailState.SimilarShows -> onSimilarShows(state)
        }
    }

    private fun onSuccess(state: DetailStates.DetailState.Success) {
        setTexts(state.detail)
        showImage(state.detail)
        showGenres(state.detail)
    }

    private fun setTexts(tvShowDetailVm: TvShowDetailVm) {
        toolbar.title = tvShowDetailVm.title
        toolbar.subtitle = tvShowDetailVm.date
        textDescription.text = tvShowDetailVm.overView
        textVotes.text = tvShowDetailVm.vote
    }

    private fun showImage(tvShowDetailVm: TvShowDetailVm) {
        val requestOptions = RequestOptions().apply {
            placeholder(R.color.grey_300)
            error(R.color.grey_300)
        }

        val requestManager = Glide.with(heroImage).setDefaultRequestOptions(requestOptions)
        requestManager
                .load(tvShowDetailVm.image)
                .thumbnail(requestManager.load(tvShowDetailVm.thumb))
                .into(heroImage)
    }

    private fun showGenres(tvShowDetailVm: TvShowDetailVm) {
        adapterDsl<String>(genresRecycler) {
            item {
                layout = R.layout.detail_genre_chip
                bindView { view, genre ->
                    view.genreText.text = genre
                }
            }
            diff { it.hashCode() }
        }.update(tvShowDetailVm.genres)
        genresRecycler.layoutManager = LinearLayoutManager(this@ShowDetailActivity,
                LinearLayoutManager.HORIZONTAL, false)
    }

    private fun onSimilarShows(state: DetailStates.DetailState.SimilarShows) {
        adapterDsl<TvShowVM>(similarShowsRecycler) {
            item {
                layout = R.layout.row_similar_show
                bindView { view, tvShow ->
                    loadSimilarShowImage(view.image, tvShow)
                    view.text.text = tvShow.title
                }
            }
            diff { it.id }
        }.update(state.shows)
        similarShowsRecycler.layoutManager = LinearLayoutManager(this@ShowDetailActivity,
                LinearLayoutManager.HORIZONTAL, false)
        enablePagination()
    }

    private fun loadSimilarShowImage(image: ImageView, tvShow: TvShowVM) {
        val requestOptions = RequestOptions().apply {
            placeholder(R.color.grey_300)
            error(R.color.grey_300)
        }

        val requestManager = Glide.with(image).setDefaultRequestOptions(requestOptions)
        requestManager
                .load(tvShow.image)
                .into(image)
    }

    private fun enablePagination() = similarShowsRecycler.addOnScrollListener(recyclerViewListener)

    private fun disablePagination() = similarShowsRecycler.removeOnScrollListener(recyclerViewListener)
}
