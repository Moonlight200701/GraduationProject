package com.example.mockproject.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mockproject.R
import com.example.mockproject.adapters.MovieAdapter
import com.example.mockproject.constant.Constant
import com.example.mockproject.database.DatabaseOpenHelper
import com.example.mockproject.listenercallback.BadgeListener
import com.example.mockproject.listenercallback.DetailListener
import com.example.mockproject.listenercallback.FavouriteListener
import com.example.mockproject.listenercallback.ReminderListener
import com.example.mockproject.listenercallback.ToolbarTitleListener
import com.example.mockproject.model.Movie

class FavoriteFragment(
    private var mDatabaseOpenHelper: DatabaseOpenHelper,
    private var mMovieFavouriteList: ArrayList<Movie>,
) : Fragment(),
    View.OnClickListener {
    private lateinit var mMovieRecyclerView: RecyclerView
    private lateinit var mMovieAdapter: MovieAdapter
    private lateinit var mBadgeListener: BadgeListener
    private lateinit var mFavouriteListener: FavouriteListener
    private lateinit var mToolbarTitleListener: ToolbarTitleListener
    private lateinit var mDetailListener: DetailListener
    private lateinit var mReminderListener: ReminderListener

    fun setToolbarTitleListener(toolbarTitleListener: ToolbarTitleListener) {
        this.mToolbarTitleListener = toolbarTitleListener
    }
    fun setBadgeListener(badgeListener: BadgeListener) {
        this.mBadgeListener = badgeListener
    }


    fun setFavouriteListener(favouriteListener: FavouriteListener) {
        this.mFavouriteListener = favouriteListener
    }

    fun setDetailListener(detailListener: DetailListener) {
        this.mDetailListener = detailListener
    }

    fun setRemindListener(reminderListener: ReminderListener) {
        this.mReminderListener = reminderListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_favorite, container, false)
        mMovieRecyclerView = view.findViewById(R.id.list_recycleView_favorite)
        loadFavouriteList()
        setHasOptionsMenu(true)
        return view
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.item_list_favourite_image_button -> {
                val position = view.tag as Int
                val movieItem: Movie = mMovieFavouriteList[position]
                if (mDatabaseOpenHelper.deleteMovie(movieItem.id) > -1) {
                    mMovieFavouriteList.remove(movieItem)
                    mMovieAdapter.notifyDataSetChanged()
                    mBadgeListener.onUpdateBadgeNumber(false)
                    mFavouriteListener.onUpdateFromFavorite(movieItem)
                } else {
                    Toast.makeText(context, "Remove Failed ${movieItem.id}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            R.id.movie_item -> {
                val position = view.tag as Int
                val movieItem: Movie = mMovieFavouriteList[position]
                val bundle = Bundle()
                bundle.putSerializable(Constant.MOVIE_KEY, movieItem)
                val detailFragment = DetailFragment(mDatabaseOpenHelper)
                detailFragment.setToolbarTitleListener(mToolbarTitleListener)
                detailFragment.setBadgeListener(mBadgeListener)
                detailFragment.setDetailListener(mDetailListener)
                detailFragment.setRemindListener(mReminderListener)
                detailFragment.arguments = bundle
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    replace(R.id.frg_favorite, detailFragment, Constant.FRAGMENT_DETAIL_TAG)
                    addToBackStack(null)  // Add the transaction to the back stack
                    commit()  // Commit the transaction
                }
                mToolbarTitleListener.onUpdateToolbarTitle(movieItem.title)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.change_view)
        val item2 = menu.findItem(R.id.action_search)
        item.isVisible = false
        item2.isVisible = false
    }

    private fun loadFavouriteList() {
        mMovieAdapter = MovieAdapter(mMovieFavouriteList, MovieAdapter.TYPE_LIST, this, true)
        mMovieRecyclerView.layoutManager = LinearLayoutManager(activity)
        mMovieRecyclerView.setHasFixedSize(true)
        mMovieRecyclerView.adapter = mMovieAdapter
    }

    fun updateFavouriteList(movie: Movie, isFavourite: Boolean) {
        if (isFavourite) {
            mMovieFavouriteList.add(movie)
        } else {
            var position = -1
            for (index in 0 until mMovieFavouriteList.size) {
                if (mMovieFavouriteList[index].id == movie.id) {
                    position = index
                    break
                }
            }
            if (position != -1) {
                mMovieFavouriteList.removeAt(position)
            }
        }
        loadFavouriteList()
    }
}