package com.vaultsec.vaultsec.ui.note

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.databinding.FragmentNewNoteBinding

class NewNoteFragment : Fragment() {
    private var _binding: FragmentNewNoteBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewNoteBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

//        requireActivity().actionBar?.setDisplayHomeAsUpEnabled(true)
//        requireActivity().actionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow)

//        val navbar = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_view)
//        val shadow = requireActivity().findViewById<View>(R.id.bottom_nav_shadow)
//        navbar.visibility = View.GONE
//        shadow.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.new_note_menu, menu)
    }

    private fun hideKeyboard() {
        val inputManager =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        requireActivity().currentFocus?.let {
            inputManager.hideSoftInputFromWindow(
                requireActivity().currentFocus?.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val currentFontSize =
            binding.textfieldNoteText.textSize / resources.displayMetrics.scaledDensity

        val noteColorIntValue = binding.textfieldNoteText.backgroundTintList?.defaultColor
        val noteColorStringValue = Integer.toHexString(noteColorIntValue!!)
        val noteColorStringValueClean = noteColorStringValue.replaceRange(0, 2, "#")

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

        return when (item.itemId) {
            R.id.item_save_note -> {
                val textInput = binding.textfieldNoteText.text.toString()
                if (textInput.isEmpty() || textInput.trim().matches("".toRegex())) {
                    Toast.makeText(requireContext(), "Text cannot be empty", Toast.LENGTH_LONG)
                        .show()
                } else {
                    val args = bundleOf(
                        "title" to binding.textfieldNoteTitle.text.toString(),
                        "text" to binding.textfieldNoteText.text.toString(),
                        "fontSize" to (binding.textfieldNoteText.textSize / resources.displayMetrics.scaledDensity).toInt(),
                        "color" to noteColorStringValueClean
                    )
                    findNavController().setGraph(R.navigation.nav_graph, args)
                    binding.root.findNavController().navigateUp()
                    hideKeyboard()
                    return true
                }
                true
            }
            R.id.item_customize -> {
                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.ThemeOverlay_App_MaterialAlertDialog
                )
                    .setTitle(R.string.new_note_customize)
                    .setView(holderLayout)
                    .setNegativeButton("Cancel") { dialog, _ ->
                        binding.textfieldNoteText.textSize = currentFontSize
                        binding.textfieldNoteText.backgroundTintList = ColorStateList.valueOf(
                            noteColorIntValue
                        )
                        binding.textfieldNoteTitle.backgroundTintList = ColorStateList.valueOf(
                            noteColorIntValue
                        )
                        dialog.cancel()
                    }
                    .setPositiveButton("Confirm") { dialog, _ ->
                        binding.textfieldNoteText.textSize = fontSizeSlider.value
                        dialog.cancel()
                    }
                    .show()
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
            }
        }
    }
}
