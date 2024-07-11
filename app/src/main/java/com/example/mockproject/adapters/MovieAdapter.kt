package com.example.mockproject.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mockproject.R
import com.example.mockproject.constant.APIConstant
import com.example.mockproject.model.Movie
import com.squareup.picasso.Picasso

class MovieAdapter(
    private var mMovieList: MutableList<Movie>,
    private var mViewType: Int,
    private var mViewClickListener: View.OnClickListener,
    private var mIsFavouriteList: Boolean,
    private var isAdmin: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TYPE_LIST = 0
        const val TYPE_GRID = 1
        const val TYPE_LOADING_LIST = 2
        const val TYPE_LOADING_GRID = 3
    }

    fun setViewType(viewType: Int) {
        this.mViewType = viewType
    }

    fun setupMovieFavorite(movieFavouriteList: ArrayList<Movie>) {
        for (i in 0 until mMovieList.size) {
            for (j in 0 until movieFavouriteList.size) {
                if (mMovieList[i].id == movieFavouriteList[j].id) {
                    mMovieList[i].isFavorite = true
                }
            }
        }
    }

    fun setupMovieBySetting(
        movieList: ArrayList<Movie>,
        rate: Int,
        releaseYear: String,
        sortBy: String,
    ) {
        movieList.removeAll { it.voteAverage < rate }

        val convertYear: Int? = if (releaseYear.length > 3) {
            releaseYear.substring(0, 4).trim().toIntOrNull()
        } else {
            null
        }

        for(movie in movieList){
            Log.d("Release Date", "${movie.id} = ${movie.releaseDate}")
        }

        //Sometimes, each pages doesn't have movie with the suitable year set by the setting -> no film found

        if (convertYear != null) {
            movieList.removeAll {
                if(it.releaseDate.length > 4) it.releaseDate.substring(0, 4).trim() != releaseYear else it.releaseDate != releaseYear

            }
        }

        if (sortBy == "Release Date")
            movieList.sortByDescending { it.releaseDate }
        else if (sortBy == "Rating") {
            movieList.sortByDescending { it.voteAverage }
        }
    }

    fun removeItemLoading() {
        if (mMovieList.isNotEmpty()) {
            val lastPosition = mMovieList.size - 1
            mMovieList.removeAt(lastPosition)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return mMovieList.size
    }

    override fun getItemViewType(position: Int): Int {
        // Suggest that if that is the last item of the recycler view, if it is, the next item is a loading item
        return if (!mIsFavouriteList && mMovieList.isNotEmpty() && position == mMovieList.size - 1) {
            if (mViewType == TYPE_LIST) {
                TYPE_LOADING_LIST
            } else {
                TYPE_LOADING_GRID
            }
        } else {
            mViewType
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_LIST -> {
                ListViewHolder(
                    mViewClickListener, mMovieList, isAdmin,
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.movie_item_list, parent, false)
                )
            }

            TYPE_GRID -> {
                GridViewHolder(
                    mMovieList,
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.movie_item_grid, parent, false)
                )
            }

            TYPE_LOADING_LIST -> {
                LoadListViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.movie_item_load_list, parent, false)
                )
            }

            else -> LoadGridViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.movie_item_load_grid, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.tag = position
        holder.itemView.setOnClickListener(mViewClickListener)
        if (holder is GridViewHolder) {
            holder.bindDataGrid(position)
        } else if (holder is ListViewHolder) {
            holder.bindDataList(position)
        }
    }

    class ListViewHolder(
        private var mViewClickListener: View.OnClickListener,
        private var movieList: MutableList<Movie>,
        private var isAdmin: String,
        itemView: View
    ) :
        RecyclerView.ViewHolder(itemView) {
        private var titleText: TextView = itemView.findViewById(R.id.item_list_title_text)
        private var movieImage: ImageView = itemView.findViewById(R.id.item_list_image)
        private var releaseDateText: TextView =
            itemView.findViewById(R.id.release_date_text)
        private var rateText: TextView = itemView.findViewById(R.id.item_list_rate_text)
        private var adultImage: ImageView = itemView.findViewById(R.id.item_list_adult_image)
        private var favouriteImgBtn: ImageButton =
            itemView.findViewById(R.id.item_list_favourite_image_button)
        private var overviewText: TextView = itemView.findViewById(R.id.overview_text)

        fun bindDataList(position: Int) {
            val movie = movieList[position]
            titleText.text = movie.title
            val url = APIConstant.BASE_IMG_URL + movie.posterPath
            Picasso.get().load(url).into(movieImage)
            releaseDateText.text = movie.releaseDate
            "${movie.voteAverage}/10".also { rateText.text = it }
            if (movie.adult) {
                adultImage.visibility = View.VISIBLE
            } else {
                adultImage.visibility = View.GONE
            }
            overviewText.text = movie.overview
            if (isAdmin == "1") {
                favouriteImgBtn.setImageResource(R.drawable.ic_close_24)
            } else {
                if (movie.isFavorite) {
                    favouriteImgBtn.setImageResource(R.drawable.ic_star_black_24)
                } else {
                    favouriteImgBtn.setImageResource(R.drawable.ic_star_outline_24)
                }
            }
            favouriteImgBtn.tag = position
            favouriteImgBtn.setOnClickListener(mViewClickListener)
        }
    }


    class GridViewHolder(private var movieList: MutableList<Movie>, itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private var movieImage: ImageView = itemView.findViewById(R.id.item_grid_image)
        private var titleText: TextView = itemView.findViewById(R.id.item_grid_title_text)

        fun bindDataGrid(position: Int) {
            val movie = movieList[position]
            val url = APIConstant.BASE_IMG_URL + movie.posterPath
            Picasso.get().load(url).into(movieImage)
            titleText.text = movie.title
        }
    }

    class LoadListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class LoadGridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)


}
