package com.example.mockproject.view

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.mockproject.R
import com.example.mockproject.constant.Constant
import com.example.mockproject.constant.Constant.Companion.PROFILE_AVATAR_KEY
import com.example.mockproject.listenercallback.ProfileListener
import com.example.mockproject.listenercallback.ToolbarTitleListener
import com.example.mockproject.util.BitmapConverter
import java.util.*

class EditProfileFragment : Fragment() {
    private lateinit var mAvatarImg: ImageView
    private lateinit var mNameEdit: EditText
    private lateinit var mEmailEdit: EditText
    private lateinit var mDateOfBirthText: TextView
    private lateinit var mRadioGroup: RadioGroup
    private lateinit var mRadioMale: RadioButton
    private lateinit var mRadioFemale: RadioButton
    private lateinit var mSaveBtn: Button

    private val mCameraResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                val imageBitmap: Bitmap = intent?.extras?.get("data") as Bitmap
                mProfileBitmap = imageBitmap
                mAvatarImg.setImageBitmap(imageBitmap)
            }
        }
    private var mProfileBitmap: Bitmap? = null
    private var mIsMale: Boolean = false

    private lateinit var mToolbarTitleListener: ToolbarTitleListener
    private lateinit var mProfileListener: ProfileListener

    fun setToolbarTitleListener(toolbarTitleListener: ToolbarTitleListener) {
        this.mToolbarTitleListener = toolbarTitleListener
    }

    fun setProfileListener(profileListener: ProfileListener) {
        this.mProfileListener = profileListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        mAvatarImg = view.findViewById(R.id.avatar_img)
        mAvatarImg.setOnClickListener {
            pickAvatar()
        }
        mNameEdit = view.findViewById(R.id.name_edit)
        mEmailEdit = view.findViewById(R.id.email_edit)
        mDateOfBirthText = view.findViewById(R.id.birthday_text)
        mDateOfBirthText.setOnClickListener {
            pickDateOfBirth()
        }
        mRadioGroup = view.findViewById(R.id.gender_radio_group)
        mRadioMale = view.findViewById(R.id.radio_male)
        mRadioFemale = view.findViewById(R.id.radio_female)
        mRadioGroup.setOnCheckedChangeListener { _, id ->
            if (id == R.id.radio_male) {
                mIsMale = true
            } else if (id == R.id.radio_female) {
                mIsMale = false
            }
        }
        mSaveBtn = view.findViewById(R.id.btn_save)
        mSaveBtn.setOnClickListener {
            val name = mNameEdit.text.toString()
            val email = mEmailEdit.text.toString()
            val birthday = mDateOfBirthText.text.toString()
            if (name == "" || email == "" || birthday == "") {
                Toast.makeText(context, "Fill all information!", Toast.LENGTH_SHORT).show()
            } else
                mProfileListener.onSaveProfile(name, email, birthday, mIsMale, mProfileBitmap)
            requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
        }

        val bundle = arguments
        if (bundle != null) {
            try {
                mAvatarImg.setImageBitmap(
                    BitmapConverter().decodeBase64(
                        bundle.getString(PROFILE_AVATAR_KEY)
                    )
                )
            } catch (e: Exception) {
                mAvatarImg.setImageResource(R.drawable.ic_person_24)
            }
            val name = bundle.getString(Constant.PROFILE_NAME_KEY)
            val email = bundle.getString(Constant.PROFILE_EMAIL_KEY)
            val birthday = bundle.getString(Constant.PROFILE_BIRTHDAY_KEY)
            val isMale = bundle.getBoolean(Constant.PROFILE_GENDER_KEY, false)
            mNameEdit.setText(name, TextView.BufferType.EDITABLE)
            mEmailEdit.setText(email, TextView.BufferType.EDITABLE)
            mDateOfBirthText.setText(birthday, TextView.BufferType.EDITABLE)
            if (isMale) {
                mRadioMale.isChecked = true
                mRadioFemale.isChecked = false
            } else {
                mRadioMale.isChecked = false
                mRadioFemale.isChecked = true
            }
        }
        setHasOptionsMenu(true)
        return view
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.change_view)
        item.isVisible = false
    }

    private fun pickAvatar() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        mCameraResultLauncher.launch(cameraIntent)
    }

    private fun pickDateOfBirth() {
        val currentDateTime = Calendar.getInstance()
        val startYear = currentDateTime.get(Calendar.YEAR)
        val startMonth = currentDateTime.get(Calendar.MONTH)
        val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val pickedDateTime = Calendar.getInstance()
            pickedDateTime.set(year, month, day)
            currentDateTime.set(year, month, day)
            val monthDisplay = month + 1
            val timeHour = "$year/$monthDisplay/$day"
            mDateOfBirthText.setText(timeHour, TextView.BufferType.EDITABLE)
        }, startYear, startMonth, startDay).show()
    }
}