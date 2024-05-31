package com.example.mockproject.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.recyclerview.widget.RecyclerView
import com.example.mockproject.R
import com.example.mockproject.constant.APIConstant
import com.example.mockproject.model.MovieTrailer

class TrailerAdapter(private var mMovieTrailerList: ArrayList<MovieTrailer>) :
    RecyclerView.Adapter<TrailerAdapter.TrailerViewHolder>() {
    @SuppressLint("SetJavaScriptEnabled")
    inner class TrailerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val trailerVideo: WebView = itemView.findViewById(R.id.movie_video)
        init {
            // Enable JavaScript for the WebView
            trailerVideo.settings.javaScriptEnabled = true
        }

        fun bind(movieTrailer: MovieTrailer) {
            // Initialize the YouTubePlayerView with the trailer URL
            trailerVideo.loadUrl(APIConstant.YOUTUBE_URL + movieTrailer.key)

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrailerViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.trailer_item, parent, false)
        return TrailerViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return mMovieTrailerList.size
    }

    override fun onBindViewHolder(holder: TrailerViewHolder, position: Int) {
        val movieTrailer = mMovieTrailerList[position]
        holder.bind(movieTrailer)
    }
}