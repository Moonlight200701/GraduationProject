package com.example.mockproject.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mockproject.R
import com.example.mockproject.constant.APIConstant
import com.example.mockproject.model.CastAndCrew
import com.squareup.picasso.Picasso

class CastAndCrewAdapter(
    private var mCastAndCrewList: ArrayList<CastAndCrew>
) : RecyclerView.Adapter<CastAndCrewAdapter.CastAndCrewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastAndCrewViewHolder {
        return CastAndCrewViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.cast_crew_item, parent, false))
    }

    override fun onBindViewHolder(holderAndCrew: CastAndCrewViewHolder, position: Int) {
        holderAndCrew.bindDataCastAndCrew(mCastAndCrewList[position])
    }

    override fun getItemCount(): Int {
        return mCastAndCrewList.size
    }

    class CastAndCrewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var nameText: TextView = itemView.findViewById(R.id.cast_crew_item_name_text)
        private var avatarImg: ImageView = itemView.findViewById(R.id.cast_crew_item_avatar_image)

        fun bindDataCastAndCrew(castAndCrew: CastAndCrew) {
            val url = APIConstant.BASE_IMG_URL + castAndCrew.profilePath
            Picasso.get().load(url).error(R.drawable.ic_person_24).into(avatarImg)
            nameText.text = castAndCrew.name
        }
    }
}