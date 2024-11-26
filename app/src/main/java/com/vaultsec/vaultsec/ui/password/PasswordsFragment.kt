package com.vaultsec.vaultsec.ui.password

import android.os.Bundle
import android.util.Log
import android.view.*
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
import com.google.android.material.snackbar.Snackbar
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.PasswordsSortOrder
import com.vaultsec.vaultsec.database.entity.Password
import com.vaultsec.vaultsec.databinding.FragmentPasswordsBinding
import com.vaultsec.vaultsec.util.*
import com.vaultsec.vaultsec.viewmodel.PasswordViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PasswordsFragment : Fragment(R.layout.fragment_passwords),
    PasswordAdapter.OnItemClickListener {

    private val passwordViewModel: PasswordViewModel by viewModels()

    private lateinit var passwordAdapter: PasswordAdapter
    var tracker: SelectionTracker<Long>? = null
    var mActionMode: ActionMode? = null

    private var _binding: FragmentPasswordsBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "com.vaultsec.vaultsec.ui.password.PasswordsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordsBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playSlidingAnimation(true, requireActivity())

        val layoutM = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
        passwordAdapter = PasswordAdapter(this)
        passwordAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.apply {
            recyclerviewPasswords.apply {
                recycledViewPool.clear()
                adapter = passwordAdapter
                layoutManager = layoutM
                setHasFixedSize(true)
                addItemDecoration(PasswordOffsetDecoration(resources.getInteger(R.integer.staggered_grid_layout_offset_spacing)))
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) recyclerView.invalidateItemDecorations()
                    }
                })
            }
            swiperefreshlayoutPasswords.setOnRefreshListener {
                passwordViewModel.onManualPasswordSync()
            }
            fabPasswords.setOnClickListener {
                passwordViewModel.onAddNewPasswordClick()
                playSlidingAnimation(false, requireActivity())
            }
        }

        tracker = SelectionTracker.Builder(
            "com.vaultsec.vaultsec.ui.password",
            binding.recyclerviewPasswords,
            StableIdKeyProvider(binding.recyclerviewPasswords),
            PasswordAdapter.PasswordDetailsLookup(binding.recyclerviewPasswords),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(object : SelectionTracker.SelectionPredicate<Long>() {
            override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean =
                key != PasswordAdapter.PasswordDetailsLookup.EMPTY_ITEM.selectionKey

            override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean =
                position != PasswordAdapter.PasswordDetailsLookup.EMPTY_ITEM.position

            override fun canSelectMultiple(): Boolean = true
        }).build()

        tracker?.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onItemStateChanged(key: Long, selected: Boolean) {
                super.onItemStateChanged(key, selected)
                try {
                    if (!tracker?.selection!!.isEmpty) {
                        if (selected) {
                            if (tracker?.selection!!.contains(key)) {
                                passwordViewModel.onPasswordSelection(passwordAdapter.currentList[key.toInt()])
                            }
                        } else {
                            if (!tracker?.selection!!.contains(key)) {
                                passwordViewModel.onPasswordDeselection(passwordAdapter.currentList[key.toInt()])
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("$TAG.onItemStateChanged", e.localizedMessage!!)
                }
            }

            override fun onSelectionChanged() {
                super.onSelectionChanged()
                val passwordAmount = tracker?.selection!!.size()
                if (mActionMode == null) {
                    mActionMode = activity?.startActionMode(mActionModeCallBack)
                }
                mActionMode!!.title = "$passwordAmount selected"

                if (passwordAmount <= 0) {
                    mActionMode?.finish()
                }
            }
        })
        passwordAdapter.tracker = tracker

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "com.vaultsec.vaultsec.ui.BottomNavigationActivity.seedDatabase",
            viewLifecycleOwner
        ) { _, _ ->
            val fragmentHolder = requireActivity().findViewById<View>(R.id.fragment_container_view)
            val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_nav_view)
            val bottomNavigationShadow =
                requireActivity().findViewById<View>(R.id.bottom_nav_shadow)
            passwordViewModel.onManualPasswordSync()
            bottomNavigationShadow.visibility = View.VISIBLE
            bottomNavigationView.visibility = View.VISIBLE
            fragmentHolder.visibility = View.VISIBLE
        }

        setFragmentResultListener("com.vaultsec.vaultsec.ui.password.AddEditPasswordFragment") { _, bundle ->
            val result = bundle.getInt("AddEditResult")
            passwordViewModel.onAddEditResult(result)
        }

        passwordViewModel.passwords.observe(viewLifecycleOwner) {
            if (it.data?.equals(passwordAdapter.currentList) == false) {
                passwordAdapter.submitList(null)
                passwordAdapter.submitList(it.data) {
                    binding.recyclerviewPasswords.scheduleLayoutAnimation()
                }
            }
            binding.swiperefreshlayoutPasswords.isRefreshing = it is Resource.Loading

//            val fragmentHolder = requireActivity().findViewById<View>(R.id.fragment_container_view)
            binding.recyclerviewPasswords.isVisible = !it.data.isNullOrEmpty()
            binding.textviewEmptyPasswords.isVisible =
                it.data.isNullOrEmpty() && it !is Resource.Loading
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            passwordViewModel.passwordsEvent.collect { event ->
                when (event) {
                    is PasswordViewModel.PasswordEvent.NavigateToAddPasswordFragment -> {
                        val action =
                            PasswordsFragmentDirections.actionFragmentPasswordsToFragmentAddEditPassword(
                                title = "New password"
                            )
                        findNavController().navigate(action)
                    }
                    is PasswordViewModel.PasswordEvent.NavigateToEditPasswordFragment -> {
                        val action =
                            PasswordsFragmentDirections.actionFragmentPasswordsToFragmentAddEditPassword(
                                password = event.password,
                                title = "Edit password"
                            )
                        findNavController().navigate(action)
                    }
                    is PasswordViewModel.PasswordEvent.ShowUndoDeletePasswordMessage -> {
                        Snackbar.make(
                            requireView(),
                            if (event.passwordList.size == 1) {
                                getString(
                                    R.string.passwords_one_deleted,
                                    event.passwordList.size
                                )
                            } else {
                                getString(
                                    R.string.passwords_more_than_one_deleted,
                                    event.passwordList.size
                                )
                            },
                            Snackbar.LENGTH_LONG
                        ).setAction("UNDO") {
                            passwordViewModel.onUndoDeleteClick(event.passwordList)
                        }.show()
                    }
                    is PasswordViewModel.PasswordEvent.ShowPasswordSavedConfirmationMessage -> {
                        Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG)
                            .setAnchorView(binding.fabPasswords)
                            .show()
                    }
                    is PasswordViewModel.PasswordEvent.DoShowRefreshing -> {
                        binding.swiperefreshlayoutPasswords.isRefreshing = event.visible
                    }
                }
            }
        }
    }

    override fun onItemClick(password: Password) {
        passwordViewModel.onPasswordClicked(password)
        playSlidingAnimation(false, requireActivity())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.passwords_fragment_menu, menu)

        val searchItem = menu.findItem(R.id.item_search_pass)
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView
        searchView.maxWidth = Int.MAX_VALUE
        setSearchViewListeners(searchView, searchItem, menu)

        viewLifecycleOwner.lifecycleScope.launch {
            val direction = menu.findItem(R.id.item_sort_direction_pass)
            if (passwordViewModel.preferencesFlow.first().isAscPasswords) {
                direction.setIcon(R.drawable.ic_baseline_vertical_align_bottom)
            } else {
                direction.setIcon(R.drawable.ic_baseline_vertical_align_top)
            }
        }
    }

    private val mActionModeCallBack = object : ActionMode.Callback {
        override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {
            p1?.clear()
            p0?.menuInflater?.inflate(R.menu.multi_select_menu, p1)
            return true
        }

        override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {
            return when (p1?.itemId) {
                R.id.item_multi_select_all -> {
                    val mutableListIds = arrayListOf<Long>()
                    for (i in 0 until passwordAdapter.itemCount) {
                        val longId = passwordAdapter.getItemId(i)
                        mutableListIds.add(longId)
                    }
                    tracker?.setItemsSelected(mutableListIds.asIterable(), true)
                    true
                }
                R.id.item_multi_select_delete -> {
                    passwordViewModel.onDeleteSelectedPasswordsClick()
                    tracker?.clearSelection()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(p0: ActionMode?) {
            passwordViewModel.onMultiSelectActionModeClose()
            tracker?.clearSelection()
            mActionMode = null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_sort_by_title_pass -> {
                passwordViewModel.onSortOrderSelected(PasswordsSortOrder.BY_TITLE)
                true
            }
            R.id.item_sort_by_created_date_pass -> {
                passwordViewModel.onSortOrderSelected(PasswordsSortOrder.BY_DATE_CREATED)
                true
            }
            R.id.item_sort_by_updated_date_pass -> {
                passwordViewModel.onSortOrderSelected(PasswordsSortOrder.BY_DATE_UPDATED)
                true
            }
            R.id.item_sort_by_color_pass -> {
                passwordViewModel.onSortOrderSelected(PasswordsSortOrder.BY_COLOR)
                true
            }
            R.id.item_sort_by_category_pass -> {
                passwordViewModel.onSortOrderSelected(PasswordsSortOrder.BY_CATEGORY)
                true
            }
            R.id.item_sort_direction_pass -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    if (passwordViewModel.preferencesFlow.first().isAscPasswords) {
                        passwordViewModel.onSortDirectionSelected(false)
                        item.setIcon(R.drawable.ic_baseline_vertical_align_top)
                    } else {
                        passwordViewModel.onSortDirectionSelected(true)
                        item.setIcon(R.drawable.ic_baseline_vertical_align_bottom)
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setItemsVisibility(
        menu: Menu,
        exception: MenuItem,
        visible: Boolean
    ) {
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item != exception) {
                item.isVisible = visible
            }
        }
    }

    private fun setSearchViewListeners(
        searchView: androidx.appcompat.widget.SearchView,
        searchItem: MenuItem,
        menu: Menu
    ) {
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                setItemsVisibility(menu, searchItem, false)
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                setItemsVisibility(menu, searchItem, true)
                return true
            }
        })

        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                hideKeyboard(requireActivity())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                passwordViewModel.searchQuery.value = newText.orEmpty()
                return true
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
        passwordViewModel.onStart()
    }
}
