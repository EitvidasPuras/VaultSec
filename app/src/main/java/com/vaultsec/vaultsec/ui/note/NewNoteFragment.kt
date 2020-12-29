package com.vaultsec.vaultsec.ui.note

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
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
        val view = binding.root

        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return view
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
        super.onCreateOptionsMenu(menu, inflater)
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

        val width = resources.getDimension(R.dimen.color_picker_button_width).toInt()
        val height = resources.getDimension(R.dimen.color_picker_button_height).toInt()

        val holderLayout = LinearLayout(requireContext())
        holderLayout.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            holderLayout.orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val fontSizeSlider = Slider(requireContext())
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

        fontSizeSlider.addOnChangeListener { _, value, _ ->
            binding.textfieldNoteText.textSize = value
        }

        val colorLayout = LinearLayout(requireContext())
        colorLayout.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            colorLayout.orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val firstColorFAB = FloatingActionButton(requireContext())
        firstColorFAB.layoutParams = LinearLayout.LayoutParams(
            width, height
        ).apply {
            firstColorFAB.id = View.generateViewId()
            firstColorFAB.setPadding(0, 0, 0, 0)
            firstColorFAB.elevation = resources.getDimension(R.dimen.color_picker_button_elevation)
            setMargins(
                resources.getDimension(R.dimen.color_picker_button_margin_horizontal).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_vertical).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_horizontal).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_vertical).toInt()
            )
            firstColorFAB.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_01
                )
            )
        }

        val secondColorFAB = FloatingActionButton(requireContext())
        secondColorFAB.layoutParams = LinearLayout.LayoutParams(
            width, height
        ).apply {
            secondColorFAB.id = View.generateViewId()
            secondColorFAB.setPadding(0, 0, 0, 0)
            secondColorFAB.elevation = resources.getDimension(R.dimen.color_picker_button_elevation)
            setMargins(
                resources.getDimension(R.dimen.color_picker_button_margin_horizontal).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_vertical).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_horizontal).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_vertical).toInt()
            )
            secondColorFAB.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_02
                )
            )
        }

        val thirdColorFAB = FloatingActionButton(requireContext())
        thirdColorFAB.layoutParams = LinearLayout.LayoutParams(
            width, height
        ).apply {
            thirdColorFAB.id = View.generateViewId()
            thirdColorFAB.setPadding(0, 0, 0, 0)
            thirdColorFAB.elevation = resources.getDimension(R.dimen.color_picker_button_elevation)
            setMargins(
                resources.getDimension(R.dimen.color_picker_button_margin_horizontal).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_vertical).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_horizontal).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_vertical).toInt()
            )
            thirdColorFAB.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_03
                )
            )
        }

        val fourthColorFAB = FloatingActionButton(requireContext())
        fourthColorFAB.layoutParams = LinearLayout.LayoutParams(
            width, height
        ).apply {
            fourthColorFAB.id = View.generateViewId()
            fourthColorFAB.setPadding(0, 0, 0, 0)
            fourthColorFAB.elevation = resources.getDimension(R.dimen.color_picker_button_elevation)
            setMargins(
                resources.getDimension(R.dimen.color_picker_button_margin_horizontal).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_vertical).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_horizontal).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_vertical).toInt()
            )
            fourthColorFAB.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_04
                )
            )
        }

        val fifthColorFAB = FloatingActionButton(requireContext())
        fifthColorFAB.layoutParams = LinearLayout.LayoutParams(
            width, height
        ).apply {
            fifthColorFAB.id = View.generateViewId()
            fifthColorFAB.setPadding(0, 0, 0, 0)
            fifthColorFAB.elevation = resources.getDimension(R.dimen.color_picker_button_elevation)
            setMargins(
                resources.getDimension(R.dimen.color_picker_button_margin_horizontal).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_vertical).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_horizontal).toInt(),
                resources.getDimension(R.dimen.color_picker_button_margin_vertical).toInt()
            )
            fifthColorFAB.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_05
                )
            )
        }

        colorLayout.addView(firstColorFAB)
        colorLayout.addView(secondColorFAB)
        colorLayout.addView(thirdColorFAB)
        colorLayout.addView(fourthColorFAB)
        colorLayout.addView(fifthColorFAB)

        holderLayout.addView(colorLayout)
        holderLayout.addView(fontSizeSlider)

        firstColorFAB.setOnClickListener {
            binding.textfieldNoteTitle.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_01
                )
            )
            binding.textfieldNoteText.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_01
                )
            )
        }
        secondColorFAB.setOnClickListener {
            binding.textfieldNoteTitle.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_02
                )
            )
            binding.textfieldNoteText.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_02
                )
            )
        }
        thirdColorFAB.setOnClickListener {
            binding.textfieldNoteTitle.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_03
                )
            )
            binding.textfieldNoteText.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_03
                )
            )
        }
        fourthColorFAB.setOnClickListener {
            binding.textfieldNoteTitle.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_04
                )
            )
            binding.textfieldNoteText.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_04
                )
            )
        }
        fifthColorFAB.setOnClickListener {
            binding.textfieldNoteTitle.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_05
                )
            )
            binding.textfieldNoteText.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_note_05
                )
            )
        }


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
                return true
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
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
