package com.example.mockproject.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mockproject.R
import com.example.mockproject.adapters.MovieAdapter
import com.example.mockproject.constant.Constant
import com.example.mockproject.database.DatabaseOpenHelper
import com.example.mockproject.listenercallback.BadgeListener
import com.example.mockproject.listenercallback.DetailListener
import com.example.mockproject.listenercallback.FavoriteToRecommendListener
import com.example.mockproject.listenercallback.FavouriteListener
import com.example.mockproject.listenercallback.ReminderListener
import com.example.mockproject.listenercallback.ToolbarTitleListener
import com.example.mockproject.model.Movie
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class FavoriteFragment(
    private var mDatabaseOpenHelper: DatabaseOpenHelper,
    private var mMovieFavouriteList: ArrayList<Movie>,
) : Fragment(), View.OnClickListener {
    private lateinit var mMovieRecyclerView: RecyclerView
    private lateinit var mMovieAdapter: MovieAdapter
    private lateinit var mBadgeListener: BadgeListener
    private lateinit var mFavouriteListener: FavouriteListener
    private lateinit var mToolbarTitleListener: ToolbarTitleListener
    private lateinit var mDetailListener: DetailListener
    private lateinit var mReminderListener: ReminderListener
    private lateinit var mFavoriteToRecommendListener: FavoriteToRecommendListener

    //Firebase
    private lateinit var fAuth: FirebaseAuth

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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_favorite, container, false)
        mMovieRecyclerView = view.findViewById(R.id.list_recycleView_favorite)
        loadFavouriteList()
        Log.d("My favorite list", mMovieFavouriteList.toString())
        fAuth = FirebaseAuth.getInstance()
        setHasOptionsMenu(true)
        return view
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onClick(view: View) {
        when (view.id) {
            R.id.item_list_favourite_image_button -> {
                val position = view.tag as Int
                val movieItem: Movie = mMovieFavouriteList[position]
                val user: FirebaseUser? = fAuth.currentUser
                if (user != null) {
                    val userId = user.uid
                    val db = FirebaseFirestore.getInstance()
                    val favoritesRef =
                        db.collection("Users").document(userId).collection("Favorites")
                    if (movieItem.isFavorite) {
                        mDatabaseOpenHelper.deleteMovie(movieItem.id)
                        favoritesRef.document(movieItem.id.toString()).delete()
                            .addOnSuccessListener {
                                // Delete successful
                                movieItem.isFavorite = false
                                mMovieFavouriteList.remove(movieItem)
                                mBadgeListener.onUpdateBadgeNumber(false)
                                mFavouriteListener.onUpdateFromFavorite(movieItem)
                                mMovieAdapter.notifyDataSetChanged()
                                Toast.makeText(context, "Delete successfully", Toast.LENGTH_SHORT)
                                    .show()
                            }.addOnFailureListener {
                                // Handle failure
                                Toast.makeText(
                                    context, "Remove Failed ${movieItem.id}", Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
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
        mFavoriteToRecommendListener = activity as FavoriteToRecommendListener
        mFavoriteToRecommendListener.fromFavoriteToRecommendation(mMovieFavouriteList)
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
        Log.d("My favorite", mMovieFavouriteList.toString())
        loadFavouriteList()
        mFavoriteToRecommendListener = activity as FavoriteToRecommendListener
        mFavoriteToRecommendListener.fromFavoriteToRecommendation(mMovieFavouriteList)

    }

}