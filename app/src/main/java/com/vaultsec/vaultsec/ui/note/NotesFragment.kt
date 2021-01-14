package com.vaultsec.vaultsec.ui.note

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StableIdKeyProvider
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.SortOrder
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.databinding.FragmentNotesBinding
import com.vaultsec.vaultsec.util.hideKeyboard
import com.vaultsec.vaultsec.viewmodel.NoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.sql.Timestamp

/**
 * A simple [Fragment] subclass.
 */
class NotesFragment : Fragment(R.layout.fragment_notes), NoteAdapter.OnItemClickListener {

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var noteAdapter: NoteAdapter
    private var listOfNotesToDelete: ArrayList<Note> = arrayListOf()
    var tracker: SelectionTracker<Long>? = null
    var mActionMode: ActionMode? = null
    
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
        noteAdapter = NoteAdapter(this)
        noteAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.apply {
            recyclerviewNotes.apply {
                recycledViewPool.clear()
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

        tracker = SelectionTracker.Builder(
            "com.vaultsec.vaultsec.ui.note",
            binding.recyclerviewNotes,
            StableIdKeyProvider(binding.recyclerviewNotes),
            NoteAdapter.NoteDetailsLookup(binding.recyclerviewNotes),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(object : SelectionTracker.SelectionPredicate<Long>() {
            override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean =
                key != NoteAdapter.NoteDetailsLookup.EMPTY_ITEM.selectionKey

            override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean =
                position != NoteAdapter.NoteDetailsLookup.EMPTY_ITEM.position

            override fun canSelectMultiple(): Boolean = true
        }).build()

        tracker?.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                super.onSelectionChanged()
                val noteAmount = tracker?.selection!!.size()
                if (mActionMode == null) {
                    mActionMode = activity?.startActionMode(mActionModeCallBack)
                }
                mActionMode!!.title = "$noteAmount selected"

                if (noteAmount <= 0) {
                    mActionMode?.finish()
                }
            }

            override fun onItemStateChanged(key: Long, selected: Boolean) {
                super.onItemStateChanged(key, selected)
                try {
                    if (!tracker?.selection!!.isEmpty) {
                        if (selected) {
                            if (tracker?.selection!!.contains(key)) {
                                listOfNotesToDelete.add(noteAdapter.currentList[key.toInt()])
                            }
                        } else {
                            if (!tracker?.selection!!.contains(key)) {
                                listOfNotesToDelete.remove(noteAdapter.currentList[key.toInt()])
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Error ", e.message!!)
                }
            }
        })
        noteAdapter.tracker = tracker

        noteViewModel = ViewModelProvider(this).get(NoteViewModel::class.java)
        noteViewModel.notes.observe(viewLifecycleOwner) {
            // An interesting behavior would occur after sorting a small amount
            // of items in the recyclerview. The recycler would only redraw some of the items,
            // causing duplicates. The code below submits null as a list forcing the recycler to
            // redraw all of the items
            if (noteAdapter.itemCount <= 25) {
                noteAdapter.submitList(null)
            }
            noteAdapter.submitList(it)
            viewLifecycleOwner.lifecycleScope.launch {
                delay(50L)
                displayMessageIfRecyclerViewIsEmpty()
            }

            // For the search functionality to display items properly, aligning them adequately
            // A slight delay so that the search query would have enough time to be processed
            viewLifecycleOwner.lifecycleScope.launch {
                delay(70L)
                binding.recyclerviewNotes.invalidateItemDecorations()
            }
            binding.recyclerviewNotes.scheduleLayoutAnimation()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            noteViewModel.notesEvent.collect { event ->
                when (event) {
                    is NoteViewModel.NotesEvent.ShowUndoDeleteNoteMessage -> {
                        Snackbar.make(requireView(), "Notes deleted", Snackbar.LENGTH_LONG)
                            .setAction("UNDO") {
                                noteViewModel.onUndoDeleteClick(event.noteList)
                            }.show()
                    }
                }
            }
        }

        displayMessageIfRecyclerViewIsEmpty()
        createNewNoteIfNecessary()
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

    private val mActionModeCallBack = object : ActionMode.Callback {
        override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {
            p1?.clear()
            p0?.menuInflater?.inflate(R.menu.notes_fragment_multi_select_menu, p1)
            return true
        }

        override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {
            return when (p1?.itemId) {
                R.id.item_multi_select_all -> {
                    val mutableListIds = arrayListOf<Long>()
                    for (i in 0 until noteAdapter.itemCount) {
                        val longId = noteAdapter.getItemId(i)
                        mutableListIds.add(longId)
                        tracker?.setItemsSelected(mutableListIds.asIterable(), true)
                    }
                    true
                }
                R.id.item_multi_select_delete -> {
                    if (listOfNotesToDelete.isNotEmpty()) {
                        noteViewModel.deleteSelectedNotes(listOfNotesToDelete.clone() as ArrayList<Note>)
                        tracker?.clearSelection()
                        listOfNotesToDelete.clear()
                    }
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(p0: ActionMode?) {
            tracker?.clearSelection()
            mActionMode = null
            displayMessageIfRecyclerViewIsEmpty()
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

    override fun onItemClick(note: Note) {
        Toast.makeText(context, note.id.toString(), Toast.LENGTH_SHORT).show()
    }

    private fun setItemsVisibility(
        menu: Menu,
        exceptionSearchItem: MenuItem,
        visible: Boolean
    ) {
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item != exceptionSearchItem) {
                item.isVisible = visible
            }
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

    private fun setSearchViewListeners(
        searchView: SearchView,
        searchItem: MenuItem,
        menu: Menu
    ) {
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
                hideKeyboard(requireActivity())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                noteViewModel.searchQuery.value = newText.orEmpty()
                // For the search functionality to display items properly
                // A slight delay so that the search query would have enough time to be processed
//                viewLifecycleOwner.lifecycleScope.launch {
//                    delay(100L)
//                    Toast.makeText(requireContext(), "Post delay", Toast.LENGTH_SHORT).show()
//                    binding.recyclerviewNotes.invalidateItemDecorations()
//                }
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        displayMessageIfRecyclerViewIsEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mActionMode?.finish()
        _binding = null
    }
}
