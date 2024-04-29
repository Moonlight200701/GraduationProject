package com.example.mockproject

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.example.mockproject.adapters.ReminderAdapter
import com.example.mockproject.adapters.ViewPagerAdapter
import com.example.mockproject.broadcastreceiver.channelID
import com.example.mockproject.constant.APIConstant.Companion.PROFILE_PREF
import com.example.mockproject.constant.Constant
import com.example.mockproject.database.DatabaseOpenHelper
import com.example.mockproject.eventbus.ReminderEvent
import com.example.mockproject.listenercallback.BadgeListener
import com.example.mockproject.listenercallback.DetailListener
import com.example.mockproject.listenercallback.FavoriteToRecommendListener
import com.example.mockproject.listenercallback.FavouriteListener
import com.example.mockproject.listenercallback.MovieListener
import com.example.mockproject.listenercallback.ProfileListener
import com.example.mockproject.listenercallback.ReminderListener
import com.example.mockproject.listenercallback.SettingListener
import com.example.mockproject.listenercallback.ToolbarTitleListener
import com.example.mockproject.model.Movie
import com.example.mockproject.util.BitmapConverter
import com.example.mockproject.view.AboutFragment
import com.example.mockproject.view.ChangePasswordFragment
import com.example.mockproject.view.DetailFragment
import com.example.mockproject.view.EditProfileFragment
import com.example.mockproject.view.FavoriteFragment
import com.example.mockproject.view.MovieFragment
import com.example.mockproject.view.ReminderFragment
import com.example.mockproject.view.SettingFragment
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity(), ToolbarTitleListener, BadgeListener, FavouriteListener,
    MovieListener, SettingListener, DetailListener, ProfileListener, ReminderListener,
    FavoriteToRecommendListener {
    private var mPermissionList = arrayOf(Manifest.permission.CAMERA)
    private var mIsGridView: Boolean = false
    private lateinit var mViewPager: ViewPager
    private lateinit var mTabLayout: TabLayout
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var mNavigationView: NavigationView
    private lateinit var mToolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var mTabTitleRevert: String
    private lateinit var mTabTitleList: MutableList<String>
    private lateinit var mTabIconList: MutableList<Int>
    private lateinit var mViewPagerAdapter: ViewPagerAdapter

    private lateinit var mDatabaseOpenHelper: DatabaseOpenHelper
    private lateinit var mMovieReminderList: ArrayList<Movie>
    private lateinit var mMovieFavouriteList: ArrayList<Movie>
    private var mFavouriteCount: Int = 0

    private lateinit var mMovieFragment: MovieFragment
    private lateinit var mFavoriteFragment: FavoriteFragment
    private lateinit var mSettingFragment: SettingFragment
    private lateinit var mAboutFragment: AboutFragment
    private lateinit var mEditProfileFragment: EditProfileFragment
    private lateinit var mReminderFragment: ReminderFragment
    private lateinit var mChangePasswordFragment: ChangePasswordFragment

    // Navigation view
    private lateinit var mProfileSharedPreferences: SharedPreferences
    private val mBitmapConverter: BitmapConverter = BitmapConverter()
    private lateinit var mHeaderLayout: View
    private lateinit var mAvatarImg: ImageView
    private lateinit var mNameText: TextView
    private lateinit var mEmailText: TextView
    private lateinit var mBirthDayText: TextView
    private lateinit var mGenderText: TextView
    private lateinit var mEditBtn: Button
    private lateinit var mReminderLayout: LinearLayout
    private lateinit var mReminderRecyclerView: RecyclerView
    private lateinit var mReminderAdapter: ReminderAdapter
    private lateinit var mReminderShowAllBtn: Button
    private lateinit var mLogOutBtn: Button
    private lateinit var mChangePassBtn: Button

    //Firebase
    private lateinit var fAuth: FirebaseAuth
    private lateinit var fStore: FirebaseFirestore

    private var backPressedCount = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mTabTitleList = mutableListOf(
            "Movie", "Favorite", "Setting", "Suggest", "Accounts"
        )
        mTabIconList = mutableListOf(
            R.drawable.ic_home_24,
            R.drawable.ic_favorite_24,
            R.drawable.ic_settings_24,
            R.drawable.ic_about_24
        )
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mProfileSharedPreferences = getSharedPreferences(PROFILE_PREF, MODE_PRIVATE)

        mDatabaseOpenHelper = DatabaseOpenHelper(this, null)
        mMovieReminderList = mDatabaseOpenHelper.getListReminder()
        mMovieFavouriteList = mDatabaseOpenHelper.getListMovie()
        mFavouriteCount = mMovieFavouriteList.size

        // Navigation view
        mNavigationView = findViewById(R.id.navigation_view)
        mHeaderLayout = mNavigationView.getHeaderView(0)
        mNavigationView.bringToFront()

        // Fragment
        mMovieFragment = MovieFragment(mDatabaseOpenHelper)
        mMovieFragment.setToolbarTitleListener(this)
        mMovieFragment.setBadgeListener(this)
        mMovieFragment.setMovieListener(this)
        mMovieFragment.setDetailListener(this)
        mMovieFragment.setRemindListener(this)

        mFavoriteFragment = FavoriteFragment(mDatabaseOpenHelper, mMovieFavouriteList)
        mFavoriteFragment.setBadgeListener(this)
        mFavoriteFragment.setFavouriteListener(this)
        mFavoriteFragment.setToolbarTitleListener(this) //added a line right here
        mFavoriteFragment.setDetailListener(this) //add another line right here
        mFavoriteFragment.setRemindListener(this) //add another line right here

        mSettingFragment = SettingFragment()
        mSettingFragment.setSettingListener(this)

        mAboutFragment = AboutFragment()

        mReminderFragment = ReminderFragment(mDatabaseOpenHelper)
        mReminderFragment.setToolbarTitleListener(this)
        mReminderFragment.setRemindListener(this)

        setUpTabs()
        setUpDrawerLayout()

        // Profile
        mAvatarImg = mHeaderLayout.findViewById(R.id.img_avatar)
        mNameText = mHeaderLayout.findViewById(R.id.tv_name)
        mBirthDayText = mHeaderLayout.findViewById(R.id.birthday_text)
        mEmailText = mHeaderLayout.findViewById(R.id.tv_mail)
        mGenderText = mHeaderLayout.findViewById(R.id.tv_gender)
        mEditBtn = mHeaderLayout.findViewById(R.id.btn_edit_profile)
        mLogOutBtn = mHeaderLayout.findViewById(R.id.btn_log_out)
        mChangePassBtn = mHeaderLayout.findViewById(R.id.btn_change_password)
        mLogOutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        mChangePasswordFragment = ChangePasswordFragment()
        mChangePasswordFragment.setToolbarTitleListener(this)
        mChangePassBtn.setOnClickListener {
            supportFragmentManager.beginTransaction().apply {
                add(
                    R.id.layout_main,
                    mChangePasswordFragment,
                    Constant.FRAGMENT_CHANGEPASS_TAG
                )
                addToBackStack(null)
                commit()
            }
//            Toast.makeText(this, "You Clicked me", Toast.LENGTH_SHORT).show()
        }

        mEditBtn.setOnClickListener {
            if (this.mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                this.mDrawerLayout.closeDrawer(GravityCompat.START)
            }
            val bundle = Bundle()
            bundle.putString(
                Constant.PROFILE_AVATAR_KEY,
                mProfileSharedPreferences.getString(
                    Constant.PROFILE_AVATAR_KEY,
                    Constant.PROFILE_AVATAR_DEFAULT
                )
            )
            bundle.putString(Constant.PROFILE_NAME_KEY, mNameText.text.toString())
            bundle.putString(Constant.PROFILE_EMAIL_KEY, mEmailText.text.toString())
            bundle.putString(Constant.PROFILE_BIRTHDAY_KEY, mBirthDayText.text.toString())
            bundle.putString(Constant.PROFILE_GENDER_KEY, mBirthDayText.text.toString())

            mEditProfileFragment = EditProfileFragment()
            mEditProfileFragment.setToolbarTitleListener(this)
            mEditProfileFragment.setProfileListener(this)
            mEditProfileFragment.arguments = bundle
            supportFragmentManager.beginTransaction().apply {
                add(
                    R.id.layout_main,
                    mEditProfileFragment,
                    Constant.FRAGMENT_EDIT_PROFILE_TAG
                )
                addToBackStack(null)
                commit()
            }
            mTabTitleRevert = supportActionBar!!.title.toString()
        }
        loadProfileData()

        // Reminder
        mReminderLayout = mHeaderLayout.findViewById(R.id.reminder_layout)
        mReminderRecyclerView = mHeaderLayout.findViewById(R.id.reminder_recycler_view)
        mReminderShowAllBtn = mHeaderLayout.findViewById(R.id.reminder_all_button)
        loadReminderList()

        mReminderShowAllBtn.setOnClickListener {
            if (this.mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                this.mDrawerLayout.closeDrawer(GravityCompat.START)
            }

            mReminderFragment = ReminderFragment(mDatabaseOpenHelper)
            mReminderFragment.setToolbarTitleListener(this)
            mReminderFragment.setRemindListener(this)
            supportFragmentManager.beginTransaction().apply {
                add(
                    R.id.layout_main,
                    mReminderFragment,
                    Constant.FRAGMENT_REMINDER_TAG
                )
                addToBackStack(null)
                commit()
            }
            mTabTitleRevert = supportActionBar!!.title.toString()
        }

        if (checkPermissionDenied(Manifest.permission.CAMERA)) {
            requestPermissions(mPermissionList, 0)
        }
        createNotificationChannel()

        val userName = intent.getStringExtra("Username")
        mNameText.text = userName
        val email = intent.getStringExtra("Email")
        mEmailText.text = email
//        Log.d("Did i find the fragment?", mViewPagerAdapter.getItem(3).toString())

    }

    override fun onBackPressed() {
        if (this.mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.mDrawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val fragmentManager = supportFragmentManager
            if (fragmentManager.backStackEntryCount > 0) {
                fragmentManager.popBackStack()
            } else {
                if (backPressedCount <= 0) {
                    backPressedCount++
                    Log.d("BackPressedCount", "$backPressedCount")
                    Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
                } else {
                    super.onBackPressed()
                    finish()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val menuItem = menu?.findItem(R.id.action_search)
        val searchView = menuItem?.actionView as SearchView
        val manager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        /* cannot cast to search-view because the old one trying to cast an androidx.appcompat.widget.SearchView object to an android.widget.SearchView
        ---> to fix, need to use the androidx.appcompat.widget.search-view directly instead of casting to androidx.widget.Searchview
        */

        searchView.setSearchableInfo(manager.getSearchableInfo(componentName))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                searchView.setQuery("", false)
                menuItem.collapseActionView()
                mMovieFragment.updateMovies(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })

        Log.d("Finding your choice", "${R.id.action_search}")
        Log.d("Your searchView be like", "$searchView")

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.change_view
            -> {
                mIsGridView = !mIsGridView
                if (mIsGridView) {
                    item.setIcon(R.drawable.ic_view_list_24)
                } else {
                    item.setIcon(R.drawable.ic_view_grid_24)
                }
                mMovieFragment.changeView()
            }

            R.id.action_movie -> {
                mViewPager.currentItem = 0
            }

            R.id.action_favorite -> {
                mViewPager.currentItem = 1
            }

            R.id.action_setting -> {
                mViewPager.currentItem = 2
            }

            R.id.action_about -> {
                mViewPager.currentItem = 3
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onUpdateToolbarTitle(toolbarTitle: String) {
        supportActionBar!!.title = toolbarTitle
    }

    override fun onUpdateBadgeNumber(isFavourite: Boolean) {
        val tabView = mTabLayout.getTabAt(1)!!.customView
        val badgeText = tabView!!.findViewById<TextView>(R.id.tab_badge)
        if (isFavourite)
            badgeText.setText("${++mFavouriteCount}", TextView.BufferType.EDITABLE)
        else
            badgeText.setText("${--mFavouriteCount}", TextView.BufferType.EDITABLE)
    }

    override fun onUpdateFromMovie(movie: Movie, isFavourite: Boolean) {
        mFavoriteFragment.updateFavouriteList(movie, isFavourite)
    }

    override fun onUpdateFromFavorite(movie: Movie) {
        mMovieFragment.updateMovieList(movie, false)
        val detailFragment = supportFragmentManager.findFragmentByTag(Constant.FRAGMENT_DETAIL_TAG)
        Log.d("Detail Fragment", detailFragment.toString())
        if (detailFragment != null && detailFragment.isAdded) {
            detailFragment as DetailFragment
            detailFragment.updateMovie(movie.id)
        }
    }


    override fun onUpdateFromDetail(movie: Movie, isFavourite: Boolean) {
        mMovieFragment.updateMovieList(movie, isFavourite)
        mFavoriteFragment.updateFavouriteList(movie, isFavourite)
    }

    override fun onUpdateFromSetting() {
        mMovieFragment.setListMovieByCondition()
    }

    override fun onLoadReminder() {
        loadReminderList()
    }

    override fun onReminderGoToMovieDetail(movie: Movie) {
        mViewPager.currentItem = 0
        supportActionBar!!.title = movie.title

        if (this.mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.mDrawerLayout.closeDrawer(GravityCompat.START)
        }
        val reminderCurrentFragment =
            supportFragmentManager.findFragmentByTag(Constant.FRAGMENT_REMINDER_TAG)
        if (reminderCurrentFragment != null) {
            supportFragmentManager.beginTransaction().apply {
                remove(reminderCurrentFragment)
                commit()
            }
        }
        val detailCurrentFragment =
            supportFragmentManager.findFragmentByTag(Constant.FRAGMENT_DETAIL_TAG)
        if (detailCurrentFragment != null) {
            supportFragmentManager.beginTransaction().apply {
                remove(detailCurrentFragment)
                commit()
            }
        }

        val bundle = Bundle()
        bundle.putSerializable(Constant.MOVIE_KEY, movie)
        val detailFragment = DetailFragment(mDatabaseOpenHelper)
        detailFragment.setToolbarTitleListener(this)
        detailFragment.setBadgeListener(this)
        detailFragment.setDetailListener(this)
        detailFragment.setRemindListener(this)
        detailFragment.arguments = bundle
        supportFragmentManager.beginTransaction().apply {
            add(R.id.movie_fragment_content, detailFragment, Constant.FRAGMENT_DETAIL_TAG)
            addToBackStack(null)
            commit()
        }
    }

    override fun onSaveProfile(
        name: String,
        email: String,
        birthday: String,
        isMale: Boolean,
        avatarBitmap: Bitmap?
    ) {
        supportActionBar!!.title = mTabTitleList[0]
        val edit = mProfileSharedPreferences.edit()
        if (avatarBitmap != null)
            edit.putString(Constant.PROFILE_AVATAR_KEY, mBitmapConverter.encodeBase64(avatarBitmap))
        edit.putString(Constant.PROFILE_NAME_KEY, name)
        edit.putString(Constant.PROFILE_EMAIL_KEY, email)
        edit.putString(Constant.PROFILE_BIRTHDAY_KEY, birthday)
        edit.putBoolean(Constant.PROFILE_GENDER_KEY, isMale)
        edit.apply()

        mAvatarImg.setImageBitmap(avatarBitmap)
        mNameText.text = mProfileSharedPreferences.getString(
            Constant.PROFILE_NAME_KEY,
            Constant.PROFILE_NAME_DEFAULT
        )
        mEmailText.text = email
        mBirthDayText.text = birthday
        if (isMale) {
            mGenderText.text = Constant.GENDER_MALE
        } else {
            mGenderText.text = Constant.GENDER_FEMALE
        }
    }

    private fun setUpDrawerLayout() {
        mToolbar = findViewById(R.id.toolbar)
        mToolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(mToolbar)
        supportActionBar!!.title = mTabTitleList[0]
        mDrawerLayout = findViewById(R.id.drawer_layout)
        toggle = ActionBarDrawerToggle(
            this,
            mDrawerLayout,
            mToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        mDrawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.toolbarNavigationClickListener = View.OnClickListener {
            mDrawerLayout.openDrawer(GravityCompat.START)
        }
        supportActionBar!!.setDisplayShowHomeEnabled(true)
    }

    //set up tab for the view pager
    private fun setUpTabs() {
        mViewPager = findViewById(R.id.view_pager)
        mTabLayout = findViewById(R.id.tab_layout)
        val isAdmin = intent.getStringExtra("isAdmin")

        mViewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        mViewPagerAdapter.addFragment(mMovieFragment, "Movie")
        mViewPagerAdapter.addFragment(mFavoriteFragment, "Favorite")
        mViewPagerAdapter.addFragment(mSettingFragment, "Setting")
        mViewPagerAdapter.addFragment(mAboutFragment, "Suggest")
        mViewPager.offscreenPageLimit = 4

        mViewPager.adapter = mViewPagerAdapter
        mTabLayout.setupWithViewPager(mViewPager)

        val countFragment = mViewPagerAdapter.count
        for (index in 0 until countFragment) {
            mTabLayout.getTabAt(index)!!.setCustomView(R.layout.tab_item)
            val tabView = mTabLayout.getTabAt(index)!!.customView

            val titleTab = tabView!!.findViewById<TextView>(R.id.tab_title)
            titleTab.text = mTabTitleList[index]
            val iconTab = tabView.findViewById<ImageView>(R.id.tab_icon)
            iconTab.setImageResource(mTabIconList[index])
            if (index == 1) {
                val badgeText = tabView.findViewById<TextView>(R.id.tab_badge)
                badgeText.visibility = View.VISIBLE
                badgeText.setText("$mFavouriteCount", TextView.BufferType.EDITABLE)
            }

        }
        setTitleFragment()
    }

    private fun setTitleFragment() {
        mViewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                mTabLayout.nextFocusRightId = position
                supportActionBar!!.title = (mTabTitleList[position])
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    //Load profile data
    private fun loadProfileData() {
        try {
            mAvatarImg.setImageBitmap(
                mBitmapConverter.decodeBase64(
                    mProfileSharedPreferences.getString(
                        Constant.PROFILE_AVATAR_KEY,
                        Constant.PROFILE_AVATAR_DEFAULT
                    )
                )
            )
        } catch (e: Exception) {
            mAvatarImg.setImageResource(R.drawable.ic_person_24)
        }
        mNameText.text = mProfileSharedPreferences.getString(
            Constant.PROFILE_NAME_KEY,
            Constant.PROFILE_NAME_DEFAULT
        )
        mEmailText.text = mProfileSharedPreferences.getString(
            Constant.PROFILE_EMAIL_KEY,
            Constant.PROFILE_EMAIL_DEFAULT
        )
        mBirthDayText.text = mProfileSharedPreferences.getString(
            Constant.PROFILE_BIRTHDAY_KEY,
            Constant.PROFILE_BIRTHDAY_DEFAULT
        )
        if (mProfileSharedPreferences.getBoolean(
                Constant.PROFILE_GENDER_KEY, false
            )
        ) {
            mGenderText.text = Constant.GENDER_MALE
        } else {
            mGenderText.text = Constant.GENDER_FEMALE
        }
    }

    private fun loadReminderList() {
        mMovieReminderList = mDatabaseOpenHelper.getListReminder()
        if (mMovieReminderList.isEmpty()) {
            mReminderLayout.visibility = View.GONE
        } else {
            mReminderLayout.visibility = View.VISIBLE
            mReminderAdapter = ReminderAdapter(mMovieReminderList, ReminderAdapter.REMINDER_PROFILE)
            val linearLayoutManager = LinearLayoutManager(this)
            mReminderRecyclerView.layoutManager = linearLayoutManager

            mReminderRecyclerView.setHasFixedSize(true)
            mReminderRecyclerView.adapter = mReminderAdapter
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(channelID, "Movie!!!", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Time to watch a movie <3"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    private fun checkPermissionDenied(permission: String?): Boolean {
        return ContextCompat.checkSelfPermission(
            this, permission!!
        ) == PackageManager.PERMISSION_DENIED
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

    override fun fromFavoriteToRecommendation(movieList: ArrayList<Movie>) {
        val aboutFragment = mViewPagerAdapter.getItem(3) as AboutFragment
        aboutFragment.displayReceivedMovieFavoriteList(movieList)
    }
    //pass the favorite list from the Favorite Fragment to the About Fragment aka the Suggest Fragment
}
