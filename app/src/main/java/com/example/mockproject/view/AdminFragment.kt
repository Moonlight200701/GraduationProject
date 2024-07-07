package com.example.mockproject.view


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
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
    //Search actions
    private lateinit var searchButton: ImageButton
    private lateinit var searchEt: EditText

    private lateinit var mAccountAdapter: AccountAdapter
    private lateinit var mAccountRecyclerView: RecyclerView
    private var accountList = arrayListOf<Account>()
    private var fStore = Firebase.firestore
    private var mDocRef = fStore.collection(Constant.COLLECTION_NAME)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.change_view)
        val item2 = menu.findItem(R.id.action_search)
        item.isVisible = false
        item2.isVisible = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Get the account from the firestore
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
                    val createdTime = document.getString("CreatedTime") ?: "Unknown"
                    val lastLoginTime = document.getString("Last Login time") ?: "Unknown"
                    val marked = document.getBoolean("Marked") ?: false
                    val avatar = document.getString("Avatar") ?: ""
                    accountList.add(
                        Account(
                            id,
                            fullName,
                            email,
                            birthday,
                            gender,
                            status,
                            createdTime,
                            lastLoginTime,
                            marked,
                            avatar
                        )
                    )
                }
            }
        mAccountRecyclerView = view.findViewById(R.id.frg_admin_recyclerview)
        mAccountAdapter = AccountAdapter(accountList = accountList, this)
        mAccountRecyclerView.adapter = mAccountAdapter

        //Search Button
        searchEt = view.findViewById(R.id.frg_admin_search_text)
        searchButton = view.findViewById(R.id.frg_admin_search_button)

        searchEt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Clear focus here from EditText
                searchEt.clearFocus()
                //Hide the keyboard
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchEt.windowToken, 0)

                handleSearch(searchEt.text.toString())
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        searchButton.setOnClickListener {
            handleSearch(searchEt.text.toString())
        }

    }

    private fun handleSearch(searchName: String) {
        if (searchName.trim().isNotEmpty()) {
            val matchingAccounts = accountList.filter {
                it.userName.contains(
                    searchName,
                    ignoreCase = true
                ) || it.email.contains(searchName, ignoreCase = true)
            } as ArrayList<Account>
            searchEt.clearFocus()
            mAccountAdapter = AccountAdapter(accountList = matchingAccounts, this)
            mAccountRecyclerView.adapter = mAccountAdapter
        } else {
            mAccountAdapter = AccountAdapter(accountList = accountList, this)
            mAccountRecyclerView.adapter = mAccountAdapter
        }
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

            R.id.frg_account_deleteBtn -> handleTerminateButtonClick(view)

            R.id.frg_account_markButton -> handleMarkButtonClick(view)

        }
    }


    private fun handleMarkButtonClick(view: View) {
        val position = view.tag as Int
        val accountItem = accountList[position]
        mDocRef.document(accountItem.accountId).get().addOnSuccessListener {
            val isMarked = it.getBoolean("Marked")
            val newMarked: HashMap<String, Any>
            if (isMarked == false) {
                newMarked = hashMapOf("Marked" to true)
                mDocRef.document(accountItem.accountId).update(newMarked).addOnSuccessListener {
                    accountItem.marked = newMarked["Marked"] as Boolean
                    Toast.makeText(context, "Marked the account successfully", Toast.LENGTH_SHORT)
                        .show()
                    mAccountAdapter.notifyItemChanged(position)
                }.addOnFailureListener {
                    Toast.makeText(context, "Unexpected error occurs", Toast.LENGTH_SHORT).show()
                }

            } else {
                newMarked = hashMapOf("Marked" to false)
                mDocRef.document(accountItem.accountId).update(newMarked).addOnSuccessListener {
                    accountItem.marked = newMarked["Marked"] as Boolean
                    Toast.makeText(context, "Unmarked the account successfully", Toast.LENGTH_SHORT)
                        .show()
                    mAccountAdapter.notifyItemChanged(position)
                }.addOnFailureListener {
                    Toast.makeText(context, "Unexpected error occurs", Toast.LENGTH_SHORT).show()
                }

            }
        }


    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleTerminateButtonClick(view: View) {
//        Toast.makeText(context, "Deleted :>", Toast.LENGTH_SHORT).show()
        val position = view.tag as Int
        val accountItem = accountList[position]
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setCancelable(true)
        dialogBuilder.setTitle("Confirmation")
        dialogBuilder.setMessage("Are you sure you want to terminate this account?")
        dialogBuilder.setPositiveButton("Yes") { _, _ ->
            val dialogBuilder2 = AlertDialog.Builder(context)
            dialogBuilder2.setCancelable(true)
            dialogBuilder2.setTitle("Confirmation again")
            dialogBuilder2.setMessage("Are you REALLY sure you want to terminate this account??")
            dialogBuilder2.setPositiveButton("Yes") { dialog2, _ ->
                mDocRef.document(accountItem.accountId).delete().addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(
                            context,
                            "Account terminated, cannot redo :<",
                            Toast.LENGTH_SHORT
                        ).show()
                        accountList.removeAt(position)
                        mDatabaseOpenHelper.deleteMovieByUser(accountItem.accountId)
                        mAccountAdapter.notifyDataSetChanged()
                    }
                }
                dialog2.dismiss()
            }
            dialogBuilder2.setNegativeButton("No") { dialog2, _ ->
                dialog2.dismiss()
            }
            val dialog2 = dialogBuilder2.create()
            dialog2.window?.attributes?.dimAmount = 0.9f
            dialog2.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialog2.show()
        }
        dialogBuilder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = dialogBuilder.create()
        dialog.window?.attributes?.dimAmount = 0.9f
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.show()


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
        val accountTimeCreated =
            dialogView.findViewById<TextView>(R.id.detail_dialog_timeCreated_tv)
        val accountLastLoginTime =
            dialogView.findViewById<TextView>(R.id.detail_dialog_lastLoginTime_tv)

        // Create the dialog builder
        accountName.text = account.userName
        accountEmail.text = "Email: ${account.email}"
        accountBirthday.text = "Birthday: ${account.birthdayDate}"
        accountGender.text = "Gender: ${account.gender}"
        accountTimeCreated.text = "Time Created: ${account.createdTime}"
        accountLastLoginTime.text = "Last login time: ${account.lastLoginTime}"

        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setView(dialogView)

        // Set the dialog to cancel when touching outside
        dialogBuilder.setCancelable(true)

        // Create and show the dialog
        val dialog = dialogBuilder.create()
        dialog.window?.attributes?.dimAmount = 0.9f
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
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
        val dialog = builder.create()
        dialog.window?.attributes?.dimAmount = 0.9f
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.show()
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