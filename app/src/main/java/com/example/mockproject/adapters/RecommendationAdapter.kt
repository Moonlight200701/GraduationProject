package com.example.mockproject.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mockproject.R
import com.example.mockproject.constant.APIConstant
import com.example.mockproject.model.Movie
import com.squareup.picasso.Picasso

class RecommendationAdapter(
    private var mMovieList: MutableList<Movie>,
    private var mViewClickListener: OnClickListener
) : RecyclerView.Adapter<RecommendationAdapter.ListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.movie_item_list, parent, false)
        return ListViewHolder(itemView, mViewClickListener)
    }

    override fun getItemCount(): Int {
        return mMovieList.size
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.itemView.tag = position
        holder.bind(position)
        holder.itemView.setOnClickListener(mViewClickListener)
    }

    inner class ListViewHolder(itemView: View, private val mViewClickListener: OnClickListener) :
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

        fun bind(position: Int) {
            val movie = mMovieList[position]
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
            favouriteImgBtn.tag = position
            favouriteImgBtn.setOnClickListener(mViewClickListener)
        }
    }
}