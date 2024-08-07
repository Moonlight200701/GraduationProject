package com.example.mockproject

import android.Manifest
import android.app.AlertDialog
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
import android.view.WindowManager
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
import com.example.mockproject.view.AdminFragment
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
    private var mPermissionList = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
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
    private lateinit var mAdminFragment: AdminFragment

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
    private var fAuth = FirebaseAuth.getInstance()
    private val mUser = fAuth.currentUser
    private val fStore = FirebaseFirestore.getInstance()
    private val mDocRef = fStore.collection("Users").document(mUser!!.uid)

    private var backPressedCount = 0


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Firebase
        var userId = ""
        if (mUser != null) {
            userId = mUser.uid
        }

        //Show the user if the admin marked them or not
        mDocRef.get().addOnSuccessListener {
            val isAdmin = it.getString("isAdmin")
            if(isAdmin != null && isAdmin != "1") {
                val isMarked = it.getBoolean("Marked")!!
                if (isMarked) {
                    val dialogBuilder = AlertDialog.Builder(this)
                    val dialog = dialogBuilder.create()
                    dialog.setCancelable(true)
                    dialog.setTitle("Attention")
                    dialog.setMessage("You have been marked by the admin")
                    dialog.window?.attributes?.dimAmount = 0.9f
                    dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    dialog.show()
                }
            }
        }

        mTabTitleList = mutableListOf(
            "Movie", "Favorite", "Setting", "Suggest", "Accounts"
        )
        mTabIconList = mutableListOf(
            R.drawable.ic_home_24,
            R.drawable.ic_favorite_24,
            R.drawable.ic_settings_24,
            R.drawable.ic_about_24,
            R.drawable.ic_admin_24
        )
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        mProfileSharedPreferences = getSharedPreferences(PROFILE_PREF, MODE_PRIVATE)

        //Get the stored movie
        mDatabaseOpenHelper = DatabaseOpenHelper(this, null)
        mMovieReminderList = mDatabaseOpenHelper.getListReminder(userId)
        mMovieFavouriteList = mDatabaseOpenHelper.getListMovie(userId)
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


        mAboutFragment = AboutFragment(mDatabaseOpenHelper)
        mAboutFragment.setBadgeListener(this)
        mAboutFragment.setToolbarTitleListener(this)
        mAboutFragment.setDetailListener(this)
//        mAboutFragment.setMovieListener(this)
        mAboutFragment.setRemindListener(this)

        mReminderFragment = ReminderFragment(mDatabaseOpenHelper)
        mReminderFragment.setToolbarTitleListener(this)
        mReminderFragment.setRemindListener(this)

        mAdminFragment = AdminFragment(mDatabaseOpenHelper)

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
            val dialogBuilder2 = AlertDialog.Builder(this)
            dialogBuilder2.setCancelable(true)
            dialogBuilder2.setTitle("Confirmation")
            dialogBuilder2.setMessage("Are you sure you want to log out?")
            dialogBuilder2.setPositiveButton("Yes") { dialog2, _ ->
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                dialog2.dismiss()
                finish()

            }
            dialogBuilder2.setNegativeButton("No") { dialog2, _ ->
                dialog2.dismiss()
            }
            val dialog2 = dialogBuilder2.create()
            dialog2.window?.attributes?.dimAmount = 0.9f
            dialog2.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialog2.show()

        }

        mChangePasswordFragment = ChangePasswordFragment()
//        mChangePasswordFragment.setToolbarTitleListener(this)
        mChangePassBtn.setOnClickListener {
            supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.nav_default_enter_anim,R.anim.nav_default_exit_anim)
                add(
                    R.id.layout_main,
                    mChangePasswordFragment,
                    Constant.FRAGMENT_CHANGEPASS_TAG
                )
                addToBackStack(null)
                commit()
            }
        }

        mEditBtn.setOnClickListener {
            if (this.mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                this.mDrawerLayout.closeDrawer(GravityCompat.START)
            }
            val bundle = Bundle()
            bundle.putString(
                Constant.PROFILE_AVATAR_KEY,
//                mProfileSharedPreferences.getString(
//                    Constant.PROFILE_AVATAR_KEY,
//                    Constant.PROFILE_AVATAR_DEFAULT
//                )
                intent.getStringExtra("Avatar")
            )
            bundle.putString(Constant.PROFILE_NAME_KEY, mNameText.text.toString())
            bundle.putString(Constant.PROFILE_EMAIL_KEY, mEmailText.text.toString())
            bundle.putString(Constant.PROFILE_BIRTHDAY_KEY, mBirthDayText.text.toString())
            bundle.putString(Constant.PROFILE_GENDER_KEY, mGenderText.text.toString())

            mEditProfileFragment = EditProfileFragment()
            mEditProfileFragment.setToolbarTitleListener(this)
            mEditProfileFragment.setProfileListener(this)
            mEditProfileFragment.arguments = bundle
            supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.nav_default_enter_anim,R.anim.nav_default_exit_anim)
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

        if (checkPermissionDenied(Manifest.permission.CAMERA) && checkPermissionDenied(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            requestPermissions(mPermissionList, 0)
        }

        createNotificationChannel()

    }

    override fun onBackPressed() {
        if (this.mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.mDrawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val fragmentManager = supportFragmentManager
            if (fragmentManager.backStackEntryCount > 0) {
                //If there is still a fragment that is added, pop it out
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
        val isAdmin = intent.getStringExtra("isAdmin")
        if (isAdmin == "1") {
            val menu1 = menu!!.findItem(R.id.action_favorite)
            val menu2 = menu.findItem(R.id.action_setting)
            val menu3 = menu.findItem(R.id.action_about)
            menu1.title = "Setting"
            menu2.title = "Suggest"
            menu3.title = "Accounts"

        }
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
                return true
            }

            //every time the text is entered, search for that immediately
            override fun onQueryTextChange(newText: String?): Boolean {
                mMovieFragment.updateMovies(newText ?: "")
                return false
            }

        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.change_view
            -> {
                mIsGridView = !mIsGridView
                //If the current View is a GridView, that means the icon of the menu of gonna be the ListView Icon.
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

    //This is for updating the number in the favorite number badge on the top of the favorite icon
    override fun onUpdateBadgeNumber(isFavourite: Boolean) {
        val tabView = mTabLayout.getTabAt(1)!!.customView
        val badgeText = tabView!!.findViewById<TextView>(R.id.tab_badge)
        if (isFavourite)
            badgeText.setText("${++mFavouriteCount}", TextView.BufferType.EDITABLE)
        else
            badgeText.setText("${--mFavouriteCount}", TextView.BufferType.EDITABLE)
    }

    //Updating the movie if the user marks or unmarks the movie in the movie fragment
    override fun onUpdateFromMovie(movie: Movie, isFavourite: Boolean) {
        mFavoriteFragment.updateFavouriteList(movie, isFavourite)
    }


    override fun onUpdateFromFavorite(movie: Movie) {
        mMovieFragment.updateMovieList(movie, false) //Remove the movie from favorite in the Movie Fragment
        val detailFragment = supportFragmentManager.findFragmentByTag(Constant.FRAGMENT_DETAIL_TAG)
        Log.d("Detail Fragment", detailFragment.toString())
        //thIS CODE UPDATE the detail fragment if it is added in the movieFragment
        if (detailFragment != null && detailFragment.isAdded) {
            detailFragment as DetailFragment
            detailFragment.updateMovie(movie.id)
        }
    }


    //Updating the movie if the user marks or unmarks the movie in the detail fragment
    override fun onUpdateFromDetail(movie: Movie, isFavourite: Boolean) {
        mMovieFragment.updateMovieList(movie, isFavourite)
        mFavoriteFragment.updateFavouriteList(movie, isFavourite)
    }

    //Changing the movie based on the setting fragment
    override fun onUpdateFromSetting() {
        mMovieFragment.setListMovieByCondition()
    }

    override fun onLoadReminder() {
        loadReminderList()
    }

    override fun onReminderGoToMovieDetail(movie: Movie) {
        mViewPager.currentItem = 0
        supportActionBar!!.title = movie.title //change the action bar according to the movies

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
        bundle.putSerializable(Constant.PREVIOUS_FRAGMENT_KEY, "Movie")
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
//        val edit = mProfileSharedPreferences.edit()
        val bitmapString = if (avatarBitmap != null) {
            mBitmapConverter.encodeBase64(avatarBitmap)
        } else {
            null
        }
//        edit.putString(Constant.PROFILE_AVATAR_KEY, bitmapString)
//        edit.putString(Constant.PROFILE_NAME_KEY, name)
//        edit.putString(Constant.PROFILE_EMAIL_KEY, email)
//        edit.putString(Constant.PROFILE_BIRTHDAY_KEY, birthday)
//        edit.putBoolean(Constant.PROFILE_GENDER_KEY, isMale)
//        edit.apply()

        //Update UI
        mAvatarImg.setImageBitmap(avatarBitmap)
        Log.d("AvatarBitmap", mAvatarImg.toString())
        mNameText.text = name
        mEmailText.text = email
        mBirthDayText.text = birthday
        if (isMale) {
            mGenderText.text = Constant.GENDER_MALE
        } else {
            mGenderText.text = Constant.GENDER_FEMALE
        }

        val profile = hashMapOf(
            "FullName" to name,
            "Email" to email,
            "Birthday" to mBirthDayText.text,
            "Gender" to mGenderText.text,
            "Avatar" to bitmapString.toString()
        )

        mDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                mDocRef.update(profile as Map<String, Any>).addOnSuccessListener {
                    Log.d(Constant.FIREBASE_ADD_TAG, "Updated successfully")
                }.addOnFailureListener {
                    Log.d(Constant.FIREBASE_ADD_TAG, "Updated failed")
                }
            } else {
                Log.d(Constant.FIREBASE_ADD_TAG, "Document not exists")
            }
        }
    }

    private fun setUpDrawerLayout() {
        // Find the toolbar view in the layout and set its properties
        mToolbar = findViewById(R.id.toolbar)
        mToolbar.setTitleTextColor(Color.WHITE)

        // Set the toolbar as the action bar for the activity
        setSupportActionBar(mToolbar)

        // Set the title of the action bar to the first item in mTabTitleList
        supportActionBar!!.title = mTabTitleList[0]

        // Find the DrawerLayout view in the layout
        mDrawerLayout = findViewById(R.id.drawer_layout)

        // Create an ActionBarDrawerToggle, which ties together the drawer and the action bar
        toggle = ActionBarDrawerToggle(
            this,
            mDrawerLayout,
            mToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        // Add the toggle as a listener for drawer events
        mDrawerLayout.addDrawerListener(toggle)

        // Sync the toggle state with the drawer's current state
        toggle.syncState()

        // Set a click listener for the navigation icon in the toolbar
        toggle.toolbarNavigationClickListener = View.OnClickListener {
            mDrawerLayout.openDrawer(GravityCompat.START)
        }

        // Enable the display of the "home" button in the action bar
        supportActionBar!!.setDisplayShowHomeEnabled(true)
    }

    //set up tab for the view pager
    private fun setUpTabs() {
        mViewPager = findViewById(R.id.view_pager)
        mTabLayout = findViewById(R.id.tab_layout)
        val isAdmin = intent.getStringExtra("isAdmin")

        mViewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        mViewPagerAdapter.addFragment(mMovieFragment, "Movie")
        if (isAdmin == "0") {
            mViewPagerAdapter.addFragment(mFavoriteFragment, "Favorite")
        }
        mViewPagerAdapter.addFragment(mSettingFragment, "Setting")

        mViewPagerAdapter.addFragment(mAboutFragment, "Suggest")

        if (isAdmin == "1") {
            mViewPagerAdapter.addFragment(mAdminFragment, "Accounts")
        }
        mViewPager.offscreenPageLimit = 4

        mViewPager.adapter = mViewPagerAdapter
        mTabLayout.setupWithViewPager(mViewPager)

        val countFragment = mViewPagerAdapter.count
        for (index in 0 until countFragment) {
            mTabLayout.getTabAt(index)!!.setCustomView(R.layout.tab_item)
            val tabView = mTabLayout.getTabAt(index)!!.customView
            val titleTab = tabView!!.findViewById<TextView>(R.id.tab_title)
            val iconTab = tabView.findViewById<ImageView>(R.id.tab_icon)

            if (isAdmin == "0") {
                //If this is a normal user:
                when (index) {
                    1 -> {
                        val badgeText = tabView.findViewById<TextView>(R.id.tab_badge)
                        badgeText.visibility = View.VISIBLE
                        badgeText.setText("$mFavouriteCount", TextView.BufferType.EDITABLE)
                        titleTab.text = mTabTitleList[index]
                        iconTab.setImageResource(mTabIconList[index])
                    }

//                    2 -> {
//                        titleTab.text = mTabTitleList[index + 1]
//                        iconTab.setImageResource(mTabIconList[index + 1])
//                    }

                    else -> {
                        titleTab.text = mTabTitleList[index]
                        iconTab.setImageResource(mTabIconList[index])
                    }
                }
            } else {
                // Admin condition: skip the second element aka skip the favorite icon and the favorite title
                if (index >= 1) {
                    titleTab.text = mTabTitleList[index + 1]
                    iconTab.setImageResource(mTabIconList[index + 1]) // Skip the second element
                } else {
                    // For index 0, set the first element
                    titleTab.text = mTabTitleList[index]
                    iconTab.setImageResource(mTabIconList[index])
                }
            }
        }
        setTitleFragment()
    }

    //Reset the title when moving to another fragment in the viewpager2
    private fun setTitleFragment() {
        val isAdmin = intent.getStringExtra("isAdmin")
        mViewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                mTabLayout.nextFocusRightId = position
                val adjustedPosition =
                    if (isAdmin == "1" && position > 0) position + 1 else position
                supportActionBar!!.title = mTabTitleList[adjustedPosition]
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    //Load profile data
    private fun loadProfileData() {
        try {
            mAvatarImg.setImageBitmap(
                mBitmapConverter.decodeBase64(
//                    mProfileSharedPreferences.getString(
//                        Constant.PROFILE_AVATAR_KEY,
//                        Constant.PROFILE_AVATAR_DEFAULT
//                    )
                    intent.getStringExtra("Avatar")
                )
            )
        } catch (e: Exception) {
            mAvatarImg.setImageResource(R.drawable.ic_person_24)
        }
        val userName = intent.getStringExtra("Username")
        mNameText.text = userName
        val email = intent.getStringExtra("Email")
        mEmailText.text = email
        val isAdmin = intent.getStringExtra("isAdmin")
        val birthday = intent.getStringExtra("Birthday") ?: "2023/01/01"
        mBirthDayText.text = birthday
        val gender = intent.getStringExtra("Gender") ?: "Unknown"
        mGenderText.text = gender
    }

    //Load the reminder of each users
    private fun loadReminderList() {
        //Load the current user's reminder list
        var userId = ""
        if (mUser != null) {
            userId = mUser.uid
        }
        mMovieReminderList = mDatabaseOpenHelper.getListReminder(userId)
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

    //pass the favorite list from the Favorite Fragment to the About Fragment aka the Suggest Fragment
    override fun fromFavoriteToRecommendation(movieList: ArrayList<Movie>) {
        val isAdmin = intent.getStringExtra("isAdmin")
        if (isAdmin == "0") {
            val aboutFragment = mViewPagerAdapter.getItem(3) as AboutFragment
            aboutFragment.displayReceivedMovieFavoriteList(movieList)
        }
    }

}
