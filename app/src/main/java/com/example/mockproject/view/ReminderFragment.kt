package com.example.mockproject.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mockproject.R
import com.example.mockproject.adapters.ReminderAdapter
import com.example.mockproject.database.DatabaseOpenHelper
import com.example.mockproject.eventbus.ReminderEvent
import com.example.mockproject.listenercallback.ReminderListener
import com.example.mockproject.listenercallback.ToolbarTitleListener
import com.example.mockproject.model.Movie
import com.example.mockproject.util.NotificationUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ReminderFragment(private var mDatabaseOpenHelper: DatabaseOpenHelper) : Fragment(),
    ReminderAdapter.ReminderListListener {
    private lateinit var mReminderAdapter: ReminderAdapter
    private lateinit var mReminderRecyclerView: RecyclerView
    private lateinit var mReminderList: ArrayList<Movie>
    private lateinit var mToolbarTitleListener: ToolbarTitleListener
    private lateinit var mReminderListener: ReminderListener

    //Firebase
    private var fAuth = FirebaseAuth.getInstance()
    private val user: FirebaseUser? = fAuth.currentUser
    private val fStore = FirebaseFirestore.getInstance()
    private val df = fStore.collection("Users").document(user!!.uid).collection("Reminder")

    fun setToolbarTitleListener(toolbarTitleListener: ToolbarTitleListener) {
        this.mToolbarTitleListener = toolbarTitleListener
    }

    fun setRemindListener(reminderListener: ReminderListener) {
        this.mReminderListener = reminderListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_reminder, container, false)
        mReminderRecyclerView = view.findViewById(R.id.recycler_view_reminder_frg)
        mReminderRecyclerView.layoutManager = LinearLayoutManager(context)
        mReminderRecyclerView.setHasFixedSize(true)
        loadReminderList()
        setHasOptionsMenu(true)
        return view
    }


    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.change_view)
        val item2 = menu.findItem(R.id.action_search)
        item.isVisible = false
        item2.isVisible = false
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showDeleteDialog(position: Int) {
        var userId = ""
        if (user != null) {
            userId = user.uid
        }
        val movie = mReminderList[position]
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm delete reminder")
            .setMessage("Do you want to delete this reminder?")
            .setNegativeButton("No") { _, _ ->
            }
            .setPositiveButton("Yes") { _, _ ->
                if (mDatabaseOpenHelper.deleteReminderByMovieId(movie.id, userId) > -1) {
                    NotificationUtil().cancelNotification(movie.id, requireContext())

                    //Firestore delete when the User delete the reminder
                    df.document(movie.id.toString()).delete()

                    mReminderList.removeAt(position)
                    mReminderAdapter.notifyDataSetChanged()
                    mReminderListener.onLoadReminder()
                    if (mReminderList.size <= 0) {
                        requireActivity().supportFragmentManager.beginTransaction().remove(this)
                            .commit()
                    }
                }
            }
            .create().apply {
                window?.attributes?.dimAmount = 0.9f
                window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
            .show()
    }

    override fun onClickItemReminder(movie: Movie) {
        mReminderListener.onReminderGoToMovieDetail(movie)
    }

    override fun onLongClickItemReminder(position: Int): Boolean {
        showDeleteDialog(position)
        return false
    }

    private fun loadReminderList() {
        var userId = ""
        if (user != null) {
            userId = user.uid
        }
        mReminderList = mDatabaseOpenHelper.getListReminder(userId)
        mReminderAdapter = ReminderAdapter(mReminderList, ReminderAdapter.REMINDER_ALL)
        mReminderAdapter.setReminderListener(this)
        mReminderRecyclerView.adapter = mReminderAdapter
        mReminderAdapter.updateData(mReminderList)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    /**
     * Delete Reminder when notification pushed
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDeleteReminderEvent(reminderEvent: ReminderEvent) {
        loadReminderList()
    }
}