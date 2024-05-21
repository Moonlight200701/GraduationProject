package com.example.mockproject.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.example.mockproject.R
import com.example.mockproject.model.Account

class AccountAdapter(
    private val accountList: MutableList<Account>,
    private var mViewClickListener: OnClickListener,

    ) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {
    inner class AccountViewHolder(private val mViewClickListener: OnClickListener, itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val accountName: TextView = itemView.findViewById(R.id.frg_account_username)
        private val accountEmail: TextView = itemView.findViewById(R.id.frg_account_email)
        private val accountStatus: TextView = itemView.findViewById(R.id.frg_account_status)
        private val disableButton: AppCompatButton =
            itemView.findViewById(R.id.frg_account_disableBtn)
//        private val deleteButton: AppCompatButton =
//            itemView.findViewById(R.id.frg_account_deleteBtn)

        fun bind(position: Int) {
            val account = accountList[position]
            accountName.text = account.userName
            accountEmail.text = account.email
            accountStatus.text = "Status: ${account.status}"
            //Button
            disableButton.tag = position
//            deleteButton.tag = position
            disableButton.setOnClickListener(mViewClickListener)
//            deleteButton.setOnClickListener(mViewClickListener)
            if(account.status.equals("Disabled", true)){
                disableButton.setText(R.string.enable_account)
            } else {
                disableButton.setText(R.string.disable_account)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.account_item, parent, false)
        return AccountViewHolder(mViewClickListener, itemView)
    }

    override fun getItemCount(): Int {
        return accountList.size
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.itemView.tag = position
        holder.bind(position)
        holder.itemView.setOnClickListener(mViewClickListener)
    }
}