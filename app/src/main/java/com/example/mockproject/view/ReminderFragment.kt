package com.example.mockproject.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
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
        item.isVisible = false
    }

    private fun showDeleteDialog(position: Int) {
        val movie = mReminderList[position]
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm delete reminder")
            .setMessage("Do you want to delete this reminder?")
            .setNegativeButton("No") { _, _ ->
            }
            .setPositiveButton("Yes") { _, _ ->
                if (mDatabaseOpenHelper.deleteReminderByMovieId(movie.id) > -1) {
                    NotificationUtil().cancelNotification(movie.id, requireContext())
                    mReminderList.removeAt(position)
                    mReminderAdapter.notifyDataSetChanged()
                    mReminderListener.onLoadReminder()
                    if (mReminderList.size <= 0) {
                        requireActivity().supportFragmentManager.beginTransaction().remove(this)
                            .commit()
                    }
                }
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
        mReminderList = mDatabaseOpenHelper.getListReminder()
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