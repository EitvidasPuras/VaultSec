package com.vaultsec.vaultsec.ui.note

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StableIdKeyProvider
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.security.crypto.MasterKey
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.SortOrder
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.databinding.FragmentNotesBinding
import com.vaultsec.vaultsec.util.*
import com.vaultsec.vaultsec.viewmodel.NoteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * A simple [Fragment] subclass.
 */
@AndroidEntryPoint
class NotesFragment : Fragment(R.layout.fragment_notes), NoteAdapter.OnItemClickListener {

    private val noteViewModel: NoteViewModel by viewModels()

    private lateinit var noteAdapter: NoteAdapter
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        playSlidingAnimation(true, requireActivity())

        val mainKey = MasterKey.Builder(requireContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

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
//                itemAnimator?.changeDuration = 0
                addItemDecoration(NoteOffsetDecoration(resources.getInteger(R.integer.staggered_grid_layout_offset_spacing)))
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) recyclerView.invalidateItemDecorations()
                    }
                })
            }
            swiperefreshlayout.setOnRefreshListener {
                noteViewModel.onManualNoteSync()
            }
            fabNotes.setOnClickListener {
                noteViewModel.onAddNewNoteClick()
                playSlidingAnimation(false, requireActivity())
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
                                noteViewModel.onNoteSelection(noteAdapter.currentList[key.toInt()])
                            }
                        } else {
                            if (!tracker?.selection!!.contains(key)) {
                                noteViewModel.onNoteDeselection(noteAdapter.currentList[key.toInt()])
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Error ", e.message!!)
                }
            }
        })
        noteAdapter.tracker = tracker

        setFragmentResultListener("com.vaultsec.vaultsec.ui.note.AddEditNoteFragment") { _, bundle ->
            val result = bundle.getInt("AddEditResult")
            noteViewModel.onAddEditResult(result)
        }

        noteViewModel.notes.observe(viewLifecycleOwner) {
            /*
            * An interesting behavior would occur after sorting a small amount
            * of items in the recyclerview. The recycler would only redraw some of the items,
            * causing duplicates.
            * A similar issue also applies to a search function if there are custom
            * OffsetDecorations created. Since the recyclerview only redraws some of the items, this
            * causes items to be displayed misaligned.
            * The code below submits null as a list forcing the recycler to redraw all of the items
            * */
            if (it.data?.equals(noteAdapter.currentList) == false) {
                noteAdapter.submitList(null)
                noteAdapter.submitList(it.data) {
                    binding.recyclerviewNotes.scheduleLayoutAnimation()
                }
            }
            binding.swiperefreshlayout.isRefreshing = it is Resource.Loading

            binding.recyclerviewNotes.isVisible = !it.data.isNullOrEmpty()
            binding.textviewEmptyNotes.isVisible = it.data.isNullOrEmpty()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            noteViewModel.notesEvent.collect { event ->
                when (event) {
                    is NoteViewModel.NotesEvent.ShowUndoDeleteNoteMessage -> {
                        Snackbar.make(
                            requireView(),
                            if (event.noteList.size == 1) {
                                getString(
                                    R.string.notes_one_deleted,
                                    event.noteList.size
                                )
                            } else {
                                getString(
                                    R.string.notes_more_than_one_deleted,
                                    event.noteList.size
                                )
                            },
                            Snackbar.LENGTH_LONG
                        )
                            .setAction("UNDO") {
                                noteViewModel.onUndoDeleteClick(event.noteList)
                            }.show()
                    }
                    is NoteViewModel.NotesEvent.NavigateToAddNoteFragment -> {
                        val action =
                            NotesFragmentDirections.actionFragmentNotesToFragmentAddEditNote(title = "New note")
                        findNavController().navigate(action)
                    }
                    is NoteViewModel.NotesEvent.NavigateToEditNoteFragment -> {
                        val action =
                            NotesFragmentDirections.actionFragmentNotesToFragmentAddEditNote(
                                event.note,
                                "Edit note"
                            )
                        findNavController().navigate(action)
                    }
                    is NoteViewModel.NotesEvent.ShowNoteSavedConfirmationMessage -> {
                        /*
                        * According to Google Material Design guidelines that's how it should be
                        * */
                        Snackbar.make(requireView(), event.message, Snackbar.LENGTH_SHORT)
                            .setAnchorView(binding.fabNotes)
                            .show()
                    }
                    is NoteViewModel.NotesEvent.DoShowRefreshing -> {
                        binding.swiperefreshlayout.isRefreshing = event.visible
                    }
                    is NoteViewModel.NotesEvent.DoShowEmptyRecyclerViewMessage -> {
                        binding.textviewEmptyNotes.isVisible = event.visible
                        binding.recyclerviewNotes.isVisible = !event.visible
                    }
                }
            }
        }

    }

    private fun test101(text: String, password: String) {
        var saltBytes: ByteArray = getRandomBytes()
        Log.e("Random bytes", Base64.encodeToString(saltBytes, Base64.DEFAULT))

        val factory: SecretKeyFactory = SecretKeyFactory.getInstance("PBEwithHmacSHA256AndAES_256")
        Log.e("Factory", factory.toString())
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, 40000, 256)
        val secretKey = factory.generateSecret(spec)
        Log.e("secretKey object", secretKey.toString())
        Log.e("secretKey base64", Base64.encodeToString(secretKey.encoded, Base64.DEFAULT))
        val secreyKeySpec = SecretKeySpec(secretKey.encoded, "AES")
        Log.e("secretKeySpec object", secreyKeySpec.toString())
        Log.e("secretKeySpec base64", Base64.encodeToString(secreyKeySpec.encoded, Base64.DEFAULT))

        val cipher = Cipher.getInstance("AES_256/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secreyKeySpec)
        val params = cipher.parameters
        Log.e("Params", Base64.encodeToString(params.encoded, Base64.DEFAULT))
        val ivBytes = params.getParameterSpec(IvParameterSpec::class.java).iv
        Log.e("IvBytes", Base64.encodeToString(ivBytes, Base64.DEFAULT))
        val encrypterByteArray = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        Log.e("EncryptedByteArray", Base64.encodeToString(encrypterByteArray, Base64.DEFAULT))

        val everything = ByteArray(saltBytes.size + ivBytes.size + encrypterByteArray.size)

        Log.e("Everything", Base64.encodeToString(everything, Base64.DEFAULT))

        Log.e("Random playaround", "random".encodeToByteArray().decodeToString())
    }

    private fun getRandomBytes(): ByteArray {
        val ba = ByteArray(512)
        SecureRandom().nextBytes(ba)
        return ba
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
                    noteViewModel.onDeleteSelectedNotesClick()
                    tracker?.clearSelection()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(p0: ActionMode?) {
            noteViewModel.onMultiSelectActionModeClose()
            tracker?.clearSelection()
            mActionMode = null
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
        noteViewModel.onNoteClicked(note)
        playSlidingAnimation(false, requireActivity())
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
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Log.e("onResume", "onResume")
//        test101("textToEncrypt", "password123")
//        Log.e("hashString", hashString("masterpasswordamazing"))
//        Log.e("hashString length", hashString("masterpasswordamazing").length.toString())
//        noteViewModel.onManualNoteSync()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mActionMode?.finish()
        binding.recyclerviewNotes.adapter = null
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        NetworkUtil().checkNetworkInfo(requireContext(), object : OnConnectionStatusChange {
            override fun onChange(isAvailable: Boolean) {
                if (isAvailable) {
                    Log.e("$isNetworkAvailable", "AVAILABLE")
                } else {
                    Log.e("$isNetworkAvailable", "UNAVAILABLE")
                }
            }
        })
        Log.e("hasInternetConnection START", "${hasInternetConnection(requireActivity())}")
        noteViewModel.onStart()
    }
}