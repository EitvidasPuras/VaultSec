package com.vaultsec.vaultsec.ui.note

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.FragmentAddEditNoteBinding
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.util.setProgressBarDrawable
import com.vaultsec.vaultsec.util.showKeyboard
import com.vaultsec.vaultsec.viewmodel.AddEditNoteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.sql.Timestamp

@AndroidEntryPoint
class AddEditNoteFragment : Fragment(R.layout.fragment_add_edit_note) {
    private var _binding: FragmentAddEditNoteBinding? = null
    private val binding get() = _binding!!

//    var mActionMode: ActionMode? = null

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var scanTextResult: String? = null

    private val addEditNoteViewModel: AddEditNoteViewModel by viewModels()

    companion object {
        private const val TAG = "com.vaultsec.vaultsec.ui.note.AddEditNoteFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditNoteBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    addEditNoteViewModel.onOpenCamera(
                        binding.textfieldNoteText.backgroundTintList?.defaultColor!!,
                        binding.textfieldNoteText.textSize / resources.displayMetrics.scaledDensity
                    )
                } else {
                    Log.e(
                        TAG,
                        "Permission not granted"
                    )
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setProgressBarDrawable(binding.progressbarAddEditNote)

        binding.apply {
            textviewDateEdited.text = getString(
                R.string.add_edit_note_edit_time_text,
                addEditNoteViewModel.noteDateUpdated.toString().substringBeforeLast(":")
            )
            textfieldNoteTitle.setText(addEditNoteViewModel.noteTitle)
            textfieldNoteText.setText(addEditNoteViewModel.noteText)
            textfieldNoteText.textSize = addEditNoteViewModel.noteFontSize.toFloat()
            textviewDateEdited.setBackgroundColor(Color.parseColor(addEditNoteViewModel.noteColor))
            textfieldNoteTitle.backgroundTintList = ColorStateList.valueOf(
                Color.parseColor(addEditNoteViewModel.noteColor)
            )
            textfieldNoteText.backgroundTintList = ColorStateList.valueOf(
                Color.parseColor(addEditNoteViewModel.noteColor)
            )

//            textfieldNoteTitle.isFocusable = true
//            textfieldNoteText.isFocusable = true
//            if (addEditNoteViewModel.note != null) {
//                textfieldNoteText.clearFocus()
//                textfieldNoteTitle.clearFocus()
//                textfieldNoteText.showSoftInputOnFocus = false
//                textfieldNoteText.isCursorVisible = false
//            }
        }

        setFragmentResultListener("com.vaultsec.vaultsec.ui.CameraFragment.recognizedText") { _, bundle ->
            scanTextResult = bundle.getString("Text")
            if (scanTextResult?.isNotEmpty() == true) {
                binding.textfieldNoteText.append(scanTextResult)
            }
        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "com.vaultsec.vaultsec.ui.CameraFragment.restoreSettings",
            viewLifecycleOwner
        ) { _, bundle ->
            val result = bundle.getBoolean("RestoreSettings")
            if (result) {
                if (addEditNoteViewModel.noteColorOnExit != -1 && addEditNoteViewModel.noteFontSizeOnExit != -1f) {
                    binding.textfieldNoteText.textSize = addEditNoteViewModel.noteFontSizeOnExit

                    binding.textfieldNoteText.backgroundTintList = ColorStateList.valueOf(
                        addEditNoteViewModel.noteColorOnExit
                    )
                    binding.textfieldNoteTitle.backgroundTintList = ColorStateList.valueOf(
                        addEditNoteViewModel.noteColorOnExit
                    )
                    binding.textviewDateEdited.setBackgroundColor(addEditNoteViewModel.noteColorOnExit)
                }
            }
        }

        /*
        * The intended behavior is: When a user clicks on a note it opens up a "View note" view
        * where a user cannot edit the note whatsoever. Once the user double clicks on a text or title
        * textinput fields, the fragment goes into "Edit note" mode, in which a user can edit the note
        * paste text in, etc.
        * The code below and above somewhat implements the said behavior, but with some
        * bugs and limitations
        * */
        // TODO: Come back to it later
//        val gestureDetector =
//            GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
//                override fun onDoubleTap(e: MotionEvent?): Boolean {
//                    if (mActionMode == null) {
//                        binding.textfieldNoteText.showSoftInputOnFocus = true
//                        binding.textfieldNoteText.isCursorVisible = true
//                        mActionMode =
//                        binding.textfieldNoteText.startActionMode(object : ActionMode.Callback {
//                            override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {
//                                p1?.clear()
//                                p0!!.menuInflater.inflate(R.menu.new_note_menu, p1)
//                                p0.title = "Edit note"
//                                binding.textfieldNoteText.showSoftInputOnFocus = true
//                                binding.textfieldNoteText.isCursorVisible = true
//                                binding.textfieldNoteText.customSelectionActionModeCallback = null
//
//                                return true
//                            }
//
//                            override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
//                                return true
//                            }
//
//                            override fun onActionItemClicked(
//                                p0: ActionMode?,
//                                p1: MenuItem?
//                            ): Boolean {
//                                return true
//                            }
//
//                            override fun onDestroyActionMode(p0: ActionMode?) {
//                                p0!!.menu.clear()
//                                hideKeyboard(requireActivity())
//                                binding.textfieldNoteText.showSoftInputOnFocus = false
//                                binding.textfieldNoteText.isCursorVisible = false
//                                mActionMode = null
//                            }
//                        })
//                    } else {
//                        binding.textfieldNoteText.setOnTouchListener { view, motionEvent ->
//                            true
//                        }
//                        return true
//                    }
//                    Log.e("Double tap", "Double is double")
//                    return true
//                }
//
//                override fun onLongPress(e: MotionEvent?) {
//                    if (mActionMode == null) {
//                        binding.textfieldNoteText.customSelectionActionModeCallback =
//                            object : ActionMode.Callback {
//                                override fun onCreateActionMode(
//                                    p0: ActionMode?,
//                                    p1: Menu?
//                                ): Boolean {
//                                    p1?.clear()
//                                    p0!!.menuInflater.inflate(R.menu.add_edit_note_context_menu, p1)
//                                    return true
//                                }
//
//                                override fun onPrepareActionMode(
//                                    p0: ActionMode?,
//                                    p1: Menu?
//                                ): Boolean {
//                                    return true
//                                }
//
//                                override fun onActionItemClicked(
//                                    p0: ActionMode?,
//                                    p1: MenuItem?
//                                ): Boolean {
//                                    val stringSelected =
//                                        binding.textfieldNoteText.text.toString().substring(
//                                            binding.textfieldNoteText.selectionStart,
//                                            binding.textfieldNoteText.selectionEnd
//                                        )
//                                    return when (p1?.itemId) {
//                                        R.id.item_copy -> {
//                                            val clipboard =
//                                                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                                            val clip =
//                                                ClipData.newPlainText("CopiedText", stringSelected)
//                                            clipboard.setPrimaryClip(clip)
//                                            p0!!.finish()
//                                            true
//                                        }
//                                        R.id.item_share -> {
//                                            val sharingIntent = Intent().apply {
//                                                action = Intent.ACTION_SEND
//                                                putExtra(Intent.EXTRA_TEXT, stringSelected)
//                                                type = "text/plain"
//                                            }
//                                            startActivity(Intent.createChooser(sharingIntent, null))
//                                            p0!!.finish()
//                                            true
//                                        }
//                                        else -> false
//                                    }
//                                }
//
//                                override fun onDestroyActionMode(p0: ActionMode?) {
//                                }
//                            }
//                    }
//                    Log.e("OnLongPress", "OnLongPress")
//                }
//            })
//
//        binding.textfieldNoteText.setOnTouchListener { p0, p1 ->
//            gestureDetector.onTouchEvent(p1)
//        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /*
    * For some weird reason the focus only gets removed if called inside onStop method
    * doesn't work on onDestroyView, onResume, onStart, etc.
    * */
    override fun onStop() {
        super.onStop()
        binding.textfieldNoteTitle.clearFocus()
        binding.textfieldNoteText.clearFocus()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        /*
        * The idea was to have two different menus: One for viewing the note and one for editing
        * */
//        if (addEditNoteViewModel.note == null) {
//            inflater.inflate(R.menu.new_note_menu, menu)
//        }
        inflater.inflate(R.menu.add_edit_note_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val currentFontSize =
            binding.textfieldNoteText.textSize / resources.displayMetrics.scaledDensity

        val noteColorIntValue = binding.textfieldNoteText.backgroundTintList?.defaultColor
        val noteColorStringValue = Integer.toHexString(noteColorIntValue!!)
        val noteColorStringValueClean = noteColorStringValue.replaceRange(0, 2, "#")

        Log.e("noteColorIntValue", noteColorIntValue.toString())
        Log.e("noteColorStringValue", noteColorStringValue)
        Log.e("noteColorStringValueClean", noteColorStringValueClean)

        val holderLayout = createLinearLayout()
        applyLinearLayoutSettings(holderLayout, true)

        val fontSizeSlider = createFontSizeSlider()
        applyFontSizeSliderSettings(fontSizeSlider)

        fontSizeSlider.addOnChangeListener { _, value, _ ->
            binding.textfieldNoteText.textSize = value
        }

        val colorLayout = createLinearLayout()
        applyLinearLayoutSettings(colorLayout, false)

        val firstColorFAB = createColorFAB()
        applyColorFABSettings(firstColorFAB, R.color.color_note_01)

        val secondColorFAB = createColorFAB()
        applyColorFABSettings(secondColorFAB, R.color.color_note_02)

        val thirdColorFAB = createColorFAB()
        applyColorFABSettings(thirdColorFAB, R.color.color_note_03)

        val fourthColorFAB = createColorFAB()
        applyColorFABSettings(fourthColorFAB, R.color.color_note_04)

        val fifthColorFAB = createColorFAB()
        applyColorFABSettings(fifthColorFAB, R.color.color_note_05)

        colorLayout.addView(firstColorFAB)
        colorLayout.addView(secondColorFAB)
        colorLayout.addView(thirdColorFAB)
        colorLayout.addView(fourthColorFAB)
        colorLayout.addView(fifthColorFAB)

        holderLayout.addView(colorLayout)
        holderLayout.addView(fontSizeSlider)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            addEditNoteViewModel.addEditTaskEvent.collect { event ->
                when (event) {
                    is AddEditNoteViewModel.AddEditNoteEvent.ShowInvalidInputMessage -> {
                        hideKeyboard(requireActivity())
                        binding.textfieldNoteText.clearFocus()
                        binding.textfieldNoteTitle.clearFocus()
                        Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG)
                            .show()
                    }
                    is AddEditNoteViewModel.AddEditNoteEvent.NavigateBackWithResult -> {
                        hideKeyboard(requireActivity())
                        setFragmentResult(
                            "com.vaultsec.vaultsec.ui.note.AddEditNoteFragment",
                            bundleOf(
                                "AddEditResult" to event.result
                            )
                        )
                        findNavController().popBackStack()
//                                findNavController().navigateUp()
                    }
                    is AddEditNoteViewModel.AddEditNoteEvent.NavigateBackWithoutResult -> {
                        hideKeyboard(requireActivity())
                        findNavController().popBackStack()
                    }
                    is AddEditNoteViewModel.AddEditNoteEvent.DoShowLoading -> {
                        hideKeyboard(requireActivity())
                        binding.progressbarAddEditNote.isVisible = event.visible
                    }
                    is AddEditNoteViewModel.AddEditNoteEvent.NavigateToCameraFragment -> {
                        hideKeyboard(requireActivity())
                        requireActivity().supportFragmentManager.setFragmentResult(
                            "com.vaultsec.vaultsec.ui.note.AddEditNoteFragment.openCamera",
                            bundleOf(
                                "OpenCamera" to true
                            )
                        )
                        val action =
                            AddEditNoteFragmentDirections.actionFragmentAddEditNoteToFragmentCamera()
                        findNavController().navigate(action)
                    }
                }
            }
        }

        return when (item.itemId) {
            R.id.item_save_note -> {
                addEditNoteViewModel.noteTitle =
                    if (binding.textfieldNoteTitle.text.toString().trim()
                            .isBlank()
                    ) null else binding.textfieldNoteTitle.text.toString().trim()
                addEditNoteViewModel.noteText = binding.textfieldNoteText.text.toString()
                addEditNoteViewModel.noteFontSize =
                    (binding.textfieldNoteText.textSize / resources.displayMetrics.scaledDensity).toInt()
                addEditNoteViewModel.noteColor = noteColorStringValueClean
                if (addEditNoteViewModel.note == null) {
                    addEditNoteViewModel.noteDateCreated = Timestamp(System.currentTimeMillis())
                    addEditNoteViewModel.noteDateUpdated = addEditNoteViewModel.noteDateCreated
                } else {
                    addEditNoteViewModel.noteDateUpdated = Timestamp(System.currentTimeMillis())
                }

                addEditNoteViewModel.onSaveNoteClick()
                true
            }
            R.id.item_customize -> {
                hideKeyboard(requireActivity())
                binding.textfieldNoteText.clearFocus()
                binding.textfieldNoteTitle.clearFocus()
                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.ThemeOverlay_App_MaterialAlertDialog
                )
                    .setTitle(R.string.add_edit_note_customize)
                    .setView(holderLayout)
                    .setNegativeButton("Cancel") { dialog, _ ->
                        binding.textfieldNoteText.textSize = currentFontSize
                        binding.textfieldNoteText.backgroundTintList = ColorStateList.valueOf(
                            noteColorIntValue
                        )
                        binding.textfieldNoteTitle.backgroundTintList = ColorStateList.valueOf(
                            noteColorIntValue
                        )
                        binding.textviewDateEdited.setBackgroundColor(noteColorIntValue)
                        dialog.cancel()
                    }
                    .setPositiveButton("Confirm") { dialog, _ ->
                        binding.textfieldNoteText.textSize = fontSizeSlider.value
                        dialog.cancel()
                    }
                    .setCancelable(false)
                    .show()
                true
            }
            R.id.item_camera -> {
                hideKeyboard(requireActivity())
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        addEditNoteViewModel.onOpenCamera(noteColorIntValue, currentFontSize)
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                        MaterialAlertDialogBuilder(
                            requireContext(),
                            R.style.ThemeOverlay_App_MaterialAlertDialog
                        )
                            .setTitle(R.string.add_edit_note_camera_permission_title)
                            .setMessage(R.string.add_edit_note_camera_permission_message)
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.cancel()
                            }
                            .setPositiveButton("OK") { dialog, _ ->
                                requestPermissionLauncher.launch(
                                    Manifest.permission.CAMERA
                                )
                                dialog.cancel()
                            }
                            .show()
                    }
                    else -> {
                        requestPermissionLauncher.launch(
                            Manifest.permission.CAMERA
                        )
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createLinearLayout(): LinearLayout = LinearLayout(requireContext())

    private fun applyLinearLayoutSettings(linearLayout: LinearLayout, orientation: Boolean) {
        linearLayout.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            if (orientation) linearLayout.orientation = LinearLayout.VERTICAL
            else linearLayout.orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
    }

    private fun createFontSizeSlider(): Slider = Slider(requireContext())

    private fun applyFontSizeSliderSettings(fontSizeSlider: Slider) {
        fontSizeSlider.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
            fontSizeSlider.valueFrom = 12f
            fontSizeSlider.valueTo = 22f
            fontSizeSlider.value =
                binding.textfieldNoteText.textSize / resources.displayMetrics.scaledDensity
            fontSizeSlider.stepSize = 1f
        }
    }

    private fun createColorFAB(): FloatingActionButton = FloatingActionButton(requireContext())

    private fun applyColorFABSettings(fab: FloatingActionButton, color: Int) {
        val width = resources.getDimension(R.dimen.color_picker_button_width).toInt()
        val height = resources.getDimension(R.dimen.color_picker_button_height).toInt()

        fab.layoutParams = LinearLayout.LayoutParams(
            width, height
        ).apply {
            fab.id = View.generateViewId()
            fab.setPadding(0, 0, 0, 0)
            fab.elevation = resources.getDimension(R.dimen.color_picker_button_elevation)
            setMargins(
                resources.getDimension(R.dimen.color_picker_button_margin_horizontal).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_vertical).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_horizontal).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_vertical).toInt()
            )
            fab.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    color
                )
            )
            fab.setOnClickListener {
                binding.textfieldNoteTitle.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        color
                    )
                )
                binding.textfieldNoteText.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        color
                    )
                )
                binding.textviewDateEdited.setBackgroundResource(color)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (addEditNoteViewModel.note == null && scanTextResult == null) {
            binding.textfieldNoteText.requestFocus()
            showKeyboard(requireActivity())
        }
        scanTextResult = null
    }
}
