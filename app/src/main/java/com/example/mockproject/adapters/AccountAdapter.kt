package com.example.mockproject.adapters

import android.accounts.Account
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.example.mockproject.R
import com.google.firebase.auth.FirebaseUser

class AccountAdapter(private val accountList: List<FirebaseUser>) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {
    inner class AccountViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val accountName: TextView = itemView.findViewById(R.id.frg_account_username)
        val accountAvatar: ImageView = itemView.findViewById(R.id.frg_account_accountAvatar)
        fun bind(user: FirebaseUser){
            accountName.text = user.email
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.account_item, parent, false)
        return AccountViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return accountList.size
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {

    }
}