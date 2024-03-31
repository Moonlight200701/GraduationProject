package com.example.mockproject.view

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import androidx.preference.*
import com.example.mockproject.R
import com.example.mockproject.constant.Constant
import com.example.mockproject.listenercallback.SettingListener

class SettingFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var mCategoryListPref: ListPreference
    private lateinit var mRateSeekBarPref: SeekBarPreference
    private lateinit var mReleaseYearEditTextPref: EditTextPreference
    private lateinit var mSortListPref: ListPreference

    private lateinit var mSettingListener: SettingListener

    fun setSettingListener(settingListener: SettingListener) {
        mSettingListener = settingListener
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.change_view)
        val item2 = menu.findItem(R.id.action_search)
        item.isVisible = false
        item2.isVisible = false
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences
            ?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
            ?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.fragment_setting, rootKey)
        setHasOptionsMenu(true)
        mCategoryListPref =
            findPreference(Constant.PREF_CATEGORY_KEY)!!
        mRateSeekBarPref =
            findPreference(Constant.PREF_RATE_KEY)!!
        mReleaseYearEditTextPref =
            findPreference(Constant.PREF_RELEASE_KEY)!!
        mSortListPref = findPreference(Constant.PREF_SORT_KEY)!!

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val category = sharedPreferences.getString(Constant.PREF_CATEGORY_KEY, "popular")
        val rate = sharedPreferences.getInt(Constant.PREF_RATE_KEY, 0)
        val releaseYear = sharedPreferences.getString(Constant.PREF_RELEASE_KEY, "")
        val sort = sharedPreferences.getString(Constant.PREF_SORT_KEY, "")

        mCategoryListPref.summary = when (category) {
            "popular" -> "Popular movie"
            "top_rated" -> "Top rated movies"
            "upcoming" -> "Up coming movies"
            "now_playing" -> "Now playing movies"
            else -> "Popular movies"
        }

        if (rate == 0) {
            mRateSeekBarPref.summary = ""
        } else {
            mRateSeekBarPref.summary = rate.toString()
        }

        if (releaseYear.isNullOrEmpty()) {
            mReleaseYearEditTextPref.summary = ""
        } else {
            mReleaseYearEditTextPref.summary = releaseYear
        }

        if (sort.isNullOrEmpty()) {
            mSortListPref.summary = ""
        } else {
            mSortListPref.summary = sort
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        val category = sharedPreferences.getString(Constant.PREF_CATEGORY_KEY, "popular")
        val rate = sharedPreferences.getInt(Constant.PREF_RATE_KEY, 0)
        val releaseYear = sharedPreferences.getString(Constant.PREF_RELEASE_KEY, "")
        val sort = sharedPreferences.getString(Constant.PREF_SORT_KEY, "")

        when {
            key.equals(Constant.PREF_CATEGORY_KEY) -> {
                mCategoryListPref.summary = when (category) {
                    "popular" -> "Popular movie"
                    "top_rated" -> "Top rated movies"
                    "upcoming" -> "Up coming movies"
                    "now_playing" -> "Now playing movies"
                    else -> "Popular movies"
                }
            }
            key.equals(Constant.PREF_RATE_KEY) -> {
                if (rate == 0) {
                    mRateSeekBarPref.summary = ""
                } else {
                    mRateSeekBarPref.summary = rate.toString()
                }
            }
            key.equals(Constant.PREF_RELEASE_KEY) -> {
                if (releaseYear.isNullOrEmpty()) {
                    mReleaseYearEditTextPref.summary = ""
                } else {
                    mReleaseYearEditTextPref.summary = releaseYear
                }
            }
            key.equals(Constant.PREF_SORT_KEY) -> {
                if (sort.isNullOrEmpty()) {
                    mSortListPref.summary = ""
                } else {
                    mSortListPref.summary = sort
                }
            }
        }
        mSettingListener.onUpdateFromSetting()
    }
}