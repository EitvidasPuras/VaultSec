package com.vaultsec.vaultsec.ui.note

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.SortOrder
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.databinding.FragmentNotesBinding
import com.vaultsec.vaultsec.viewmodel.NoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.sql.Timestamp

/**
 * A simple [Fragment] subclass.
 */
class NotesFragment : Fragment(R.layout.fragment_notes) {

    private lateinit var noteViewModel: NoteViewModel
    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        return binding.root

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        playSlidingAnimation(true)

        val layoutM = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
        val noteAdapter = NoteAdapter()
        noteAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.apply {
            recyclerviewNotes.apply {
                adapter = noteAdapter
                layoutManager = layoutM
                setHasFixedSize(true)
                addItemDecoration(NoteOffsetDecoration(resources.getInteger(R.integer.staggered_grid_layout_offset_spacing)))
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) recyclerView.invalidateItemDecorations()
                    }
                })
            }

        }

        noteViewModel = ViewModelProvider(this).get(NoteViewModel::class.java)
        noteViewModel.notes.observe(viewLifecycleOwner) {
            noteAdapter.submitList(it)
            // For the search functionality to display items properly
            // A slight delay so that the search query would have enough time to be processed
            viewLifecycleOwner.lifecycleScope.launch {
                delay(20L)
                binding.recyclerviewNotes.invalidateItemDecorations()
            }
            binding.recyclerviewNotes.scheduleLayoutAnimation()
        }

        displayMessageIfRecyclerViewIsEmpty()
        createNewNoteIfNecessary()

        noteAdapter.setOnItemClickListener(object : NoteAdapter.OnItemClickListener {
            override fun onItemClick(note: Note) {
                binding.recyclerviewNotes.invalidateItemDecorations()
                Toast.makeText(context, note.createdAt.toString(), Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val navCon = Navigation.findNavController(view)
        binding.fabNotes.setOnClickListener {
            playSlidingAnimation(false)
            view.findNavController().navigate(R.id.action_fragment_notes_to_fragment_new_note)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.notes_fragment_menu, menu)

        val searchItem = menu.findItem(R.id.item_search)
        val searchView = searchItem.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE
        setSearchViewListeners(searchView, searchItem, menu)

        viewLifecycleOwner.lifecycleScope.launch {
            val direction = menu.findItem(R.id.item_sort_direction)
            if (noteViewModel.preferencesFlow.first().isAsc) {
                direction.setIcon(R.drawable.ic_baseline_vertical_align_bottom)
            } else {
                direction.setIcon(R.drawable.ic_baseline_vertical_align_top)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_sort_by_title -> {
                noteViewModel.onSortOrderSelected(SortOrder.BY_TITLE)
                true
            }
            R.id.item_sort_by_created_date -> {
                noteViewModel.onSortOrderSelected(SortOrder.BY_DATE_CREATED)
                true
            }
            R.id.item_sort_by_updated_date -> {
                noteViewModel.onSortOrderSelected(SortOrder.BY_DATE_UPDATED)
                true
            }
            R.id.item_sort_by_color -> {
                noteViewModel.onSortOrderSelected(SortOrder.BY_COLOR)
                true
            }
            R.id.item_sort_by_font_size -> {
                noteViewModel.onSortOrderSelected(SortOrder.BY_FONT_SIZE)
                true
            }
            R.id.item_sort_direction -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    if (noteViewModel.preferencesFlow.first().isAsc) {
                        noteViewModel.onSortDirectionSelected(false)
                        item.setIcon(R.drawable.ic_baseline_vertical_align_top)
                    } else {
                        noteViewModel.onSortDirectionSelected(true)
                        item.setIcon(R.drawable.ic_baseline_vertical_align_bottom)
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setItemsVisibility(menu: Menu, exception: MenuItem, visible: Boolean) {
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item != exception) item.isVisible = visible
        }
    }

    private fun playSlidingAnimation(inOrOut: Boolean) {
        val navbar = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        val shadow = requireActivity().findViewById<View>(R.id.bottom_nav_shadow)
        if (inOrOut) {
            if (navbar.visibility == View.GONE) {

                navbar.translationX = -navbar.width.toFloat()
                shadow.translationX = -navbar.width.toFloat()
                navbar.visibility = View.VISIBLE
                shadow.visibility = View.VISIBLE
                navbar.animate().translationX(0f).setDuration(320).setListener(null)
                shadow.animate().translationX(0f).setDuration(320).setListener(null)
            }
        } else {
            shadow.animate().translationX(-shadow.width.toFloat()).setDuration(400)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(p0: Animator?) {
                        shadow.visibility = View.GONE
                    }
                })
            navbar.animate().translationX(-navbar.width.toFloat()).setDuration(400)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(p0: Animator?) {
                        navbar.visibility = View.GONE
                    }
                })
        }

    }

    private fun displayMessageIfRecyclerViewIsEmpty() {
        if (noteViewModel.getItemCount() == 0) {
            binding.textviewEmptyNotes.visibility = View.VISIBLE
            binding.recyclerviewNotes.visibility = View.GONE
        } else {
            binding.textviewEmptyNotes.visibility = View.GONE
            binding.recyclerviewNotes.visibility = View.VISIBLE
        }
    }

    private fun createNewNoteIfNecessary() {
        if (!requireArguments().getString("title").isNullOrEmpty() ||
            !requireArguments().getString("text").isNullOrEmpty()
        ) {
            val title = requireArguments().getString("title")
            val text = requireArguments().getString("text")
            val fontSize = requireArguments().getInt("fontSize")
            val color = requireArguments().getString("color")
            val createdAt = Timestamp(System.currentTimeMillis())
            Log.e("date:", createdAt.toString())

            val note = Note(title, text!!, color!!, fontSize, createdAt, createdAt)
            noteViewModel.insert(note)
            requireArguments().clear()
        }
    }

    private fun setSearchViewListeners(searchView: SearchView, searchItem: MenuItem, menu: Menu) {
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                setItemsVisibility(menu, searchItem, false)
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                setItemsVisibility(menu, searchItem, true)
                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                hideKeyboard()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                noteViewModel.searchQuery.value = newText.orEmpty()
                return true
            }
        })
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
