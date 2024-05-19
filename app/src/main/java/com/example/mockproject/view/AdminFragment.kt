package com.example.mockproject.view


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.mockproject.R
import com.example.mockproject.adapters.AccountAdapter
import com.example.mockproject.constant.Constant
import com.example.mockproject.database.DatabaseOpenHelper
import com.example.mockproject.model.Account
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

//Purpose of database param? delete the movie in the local as well if i want to delete the user
class AdminFragment(private var mDatabaseOpenHelper: DatabaseOpenHelper) : Fragment(),
    OnClickListener {
    private lateinit var mAccountAdapter: AccountAdapter
    private lateinit var mAccountRecyclerView: RecyclerView
    private var accountList = mutableListOf<Account>()
    private var fStore = Firebase.firestore
    private var mDocRef = fStore.collection(Constant.COLLECTION_NAME)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fStore.collection("Users")
            .whereEqualTo("isAdmin", "0") //Only displaying users apart from admin
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val id = document.id
                    val email = document.getString("Email") ?: "Unknown"
                    val fullName = document.getString("FullName") ?: "Unknown"
                    val birthday = document.getString("Birthday") ?: "Unknown"
                    val gender = document.getString("Gender") ?: "Unknown"
                    val status = document.getString("Status") ?: "Unknown"
                    accountList.add(Account(id, fullName, email, birthday, gender, status))
                }
            }
        mAccountRecyclerView = view.findViewById(R.id.frg_admin_recyclerview)
        mAccountAdapter = AccountAdapter(accountList = accountList, this)
        mAccountRecyclerView.adapter = mAccountAdapter

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onClick(view: View) {
        when (view.id) {
            R.id.account_item -> {
                val position = view.tag as Int
                Log.d("TAG", position.toString())
                val accountItem: Account = accountList[position]
                Log.d("Account Id", accountItem.accountId)
                showAccountDetail(accountItem)
            }

            R.id.frg_account_disableBtn -> handleDisableEnableButtonClick(view)

            R.id.frg_account_deleteBtn -> {
                Toast.makeText(context, "Deleting :<", Toast.LENGTH_SHORT).show()

            }
        }
    }

    //When the admin click each item, shows the detail of the accounts
    @SuppressLint("SetTextI18n")
    private fun showAccountDetail(account: Account) {
//        val account = accountList[position]
        // Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(context).inflate(R.layout.account_detail_dialog, null)
        val accountName = dialogView.findViewById<TextView>(R.id.detail_dialog_name_tv)
        val accountEmail = dialogView.findViewById<TextView>(R.id.detail_dialog_email_tv)
        val accountBirthday = dialogView.findViewById<TextView>(R.id.detail_dialog_birthday_tv)
        val accountGender = dialogView.findViewById<TextView>(R.id.detail_dialog_gender_tv)

        // Create the dialog builder
        accountName.text = account.userName
        accountEmail.text = "Email: ${account.email}"
        accountBirthday.text = "Birthday: ${account.birthdayDate}"
        accountGender.text = "Gender: ${account.gender}"

        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setView(dialogView)

        // Set the dialog to cancel when touching outside
        dialogBuilder.setCancelable(true)

        // Create and show the dialog
        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun handleDisableEnableButtonClick(view: View) {
        val position = view.tag as Int
        val accountItem: Account = accountList[position]
        mDocRef.document(accountItem.accountId).get().addOnSuccessListener { document ->
            val status = document.getString("Status") ?: "Unknown"
            if (status != "Disabled") {
                showConfirmationDialog("disable", accountItem, position)
            } else {
                showConfirmationDialog("enable", accountItem, position)
            }
        }
    }

    private fun showConfirmationDialog(action: String, accountItem: Account, position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Confirmation")
        builder.setMessage("Are you sure you want to $action this account?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            val newStatus = if (action == "disable") "Disabled" else "Enabled"
            updateAccountStatus(accountItem, newStatus, position)
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
        builder.setCancelable(true)
        builder.show()
    }

    private fun updateAccountStatus(accountItem: Account, newStatus: String, position: Int) {
        val updatedData = hashMapOf<String, Any>("Status" to newStatus)
        mDocRef.document(accountItem.accountId).update(updatedData).addOnSuccessListener {
            accountItem.status = newStatus // Update local list
            mAccountAdapter.notifyItemChanged(position) // Notify adapter about item change
            Toast.makeText(context, "$newStatus successfully", Toast.LENGTH_SHORT).show()
        }
    }
}