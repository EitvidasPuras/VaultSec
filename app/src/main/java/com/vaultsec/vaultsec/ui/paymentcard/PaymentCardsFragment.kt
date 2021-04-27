package com.vaultsec.vaultsec.ui.paymentcard

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
import com.vaultsec.vaultsec.database.PaymentCardsSortOrder
import com.vaultsec.vaultsec.database.entity.PaymentCard
import com.vaultsec.vaultsec.databinding.FragmentPaymentCardsBinding
import com.vaultsec.vaultsec.util.*
import com.vaultsec.vaultsec.viewmodel.PaymentCardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PaymentCardsFragment : Fragment(R.layout.fragment_payment_cards),
    PaymentCardAdapter.OnItemClickListener {

    private val paymentCardViewModel: PaymentCardViewModel by viewModels()

    private lateinit var paymentCardAdapter: PaymentCardAdapter
    var tracker: SelectionTracker<Long>? = null
    var mActionMode: ActionMode? = null

    private var _binding: FragmentPaymentCardsBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "com.vaultsec.vaultsec.ui.paymentcard.PaymentCardsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentCardsBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playSlidingAnimation(true, requireActivity())

        val layoutM = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
        paymentCardAdapter = PaymentCardAdapter(this)
        paymentCardAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.apply {
            recyclerviewCards.apply {
                recycledViewPool.clear()
                adapter = paymentCardAdapter
                layoutManager = layoutM
                setHasFixedSize(true)
                addItemDecoration(PaymentCardOffsetDecoration(resources.getInteger(R.integer.staggered_grid_layout_offset_spacing)))
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) recyclerView.invalidateItemDecorations()
                    }
                })
            }
            swiperefreshlayoutCards.setOnRefreshListener {
                paymentCardViewModel.onManualPaymentCardSync()
            }
            fabCards.setOnClickListener {
                paymentCardViewModel.onAddNewPaymentCardClick()
                playSlidingAnimation(false, requireActivity())
            }
        }

        tracker = SelectionTracker.Builder(
            "com.vaultsec.vaultsec.ui.paymentcard",
            binding.recyclerviewCards,
            StableIdKeyProvider(binding.recyclerviewCards),
            PaymentCardAdapter.PaymentCardDetailsLookup(binding.recyclerviewCards),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(object : SelectionTracker.SelectionPredicate<Long>() {
            override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean =
                key != PaymentCardAdapter.PaymentCardDetailsLookup.EMPTY_ITEM.selectionKey

            override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean =
                position != PaymentCardAdapter.PaymentCardDetailsLookup.EMPTY_ITEM.position

            override fun canSelectMultiple(): Boolean = true
        }).build()

        tracker?.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onItemStateChanged(key: Long, selected: Boolean) {
                super.onItemStateChanged(key, selected)
                try {
                    if (!tracker?.selection!!.isEmpty) {
                        if (selected) {
                            if (tracker?.selection!!.contains(key)) {
                                paymentCardViewModel.onCardSelection(paymentCardAdapter.currentList[key.toInt()])
                            }
                        } else {
                            if (!tracker?.selection!!.contains(key)) {
                                paymentCardViewModel.onCardDeselection(paymentCardAdapter.currentList[key.toInt()])
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("$TAG.onItemStateChanged", e.localizedMessage!!)
                }
            }

            override fun onSelectionChanged() {
                super.onSelectionChanged()
                val cardAmount = tracker?.selection!!.size()
                if (mActionMode == null) {
                    mActionMode = activity?.startActionMode(mActionModeCallBack)
                }
                mActionMode!!.title = "$cardAmount selected"

                if (cardAmount <= 0) {
                    mActionMode?.finish()
                }
            }
        })
        paymentCardAdapter.tracker = tracker

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "com.vaultsec.vaultsec.ui.BottomNavigationActivity.seedDatabase",
            viewLifecycleOwner
        ) { _, _ ->
            val fragmentHolder = requireActivity().findViewById<View>(R.id.fragment_container_view)
            val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_nav_view)
            val bottomNavigationShadow =
                requireActivity().findViewById<View>(R.id.bottom_nav_shadow)
            paymentCardViewModel.onManualPaymentCardSync()
            bottomNavigationShadow.visibility = View.VISIBLE
            bottomNavigationView.visibility = View.VISIBLE
            fragmentHolder.visibility = View.VISIBLE
        }

        setFragmentResultListener("com.vaultsec.vaultsec.ui.paymentcard.AddEditPaymentCardFragment") { _, bundle ->
            val result = bundle.getInt("AddEditResult")
            paymentCardViewModel.onAddEditResult(result)
        }

        paymentCardViewModel.paymentCards.observe(viewLifecycleOwner) {
            Log.e("Resource.data", "${it.data}")
            if (it.data?.equals(paymentCardAdapter.currentList) == false) {
                paymentCardAdapter.submitList(null)
                paymentCardAdapter.submitList(it.data) {
                    binding.recyclerviewCards.scheduleLayoutAnimation()
                }
            }
            binding.swiperefreshlayoutCards.isRefreshing = it is Resource.Loading

            binding.recyclerviewCards.isVisible = !it.data.isNullOrEmpty()
            binding.textviewEmptyCards.isVisible =
                it.data.isNullOrEmpty() && it !is Resource.Loading
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            paymentCardViewModel.paymentCardsEvent.collect { event ->
                when (event) {
                    is PaymentCardViewModel.PaymentCardEvent.NavigateToAddPaymentCardFragment -> {
                        val action =
                            PaymentCardsFragmentDirections.actionFragmentPaymentCardsToFragmentAddEditPaymentCard(
                                title = "New card"
                            )
                        findNavController().navigate(action)
                    }
                    is PaymentCardViewModel.PaymentCardEvent.NavigateToEditPaymentCardFragment -> {
                        val action =
                            PaymentCardsFragmentDirections.actionFragmentPaymentCardsToFragmentAddEditPaymentCard(
                                event.card,
                                "Edit card"
                            )
                        findNavController().navigate(action)
                    }
                    is PaymentCardViewModel.PaymentCardEvent.ShowUndoDeleteCardMessage -> {
                        Snackbar.make(
                            requireView(),
                            if (event.cardList.size == 1) {
                                getString(
                                    R.string.cards_one_deleted,
                                    event.cardList.size
                                )
                            } else {
                                getString(
                                    R.string.cards_more_than_one_deleted,
                                    event.cardList.size
                                )
                            },
                            Snackbar.LENGTH_LONG
                        ).setAction("UNDO") {
                            paymentCardViewModel.onUndoDeleteClick(event.cardList)
                        }.show()
                    }
                    is PaymentCardViewModel.PaymentCardEvent.ShowCardSavedConfimationMessage -> {
                        Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG)
                            .setAnchorView(binding.fabCards)
                            .show()
                    }
                    is PaymentCardViewModel.PaymentCardEvent.DoShowRefreshing -> {
                        binding.swiperefreshlayoutCards.isRefreshing = event.visible
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.payment_cards_fragment_menu, menu)

        val searchItem = menu.findItem(R.id.item_search_card)
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView
        searchView.maxWidth = Int.MAX_VALUE
        setSearchViewListener(searchView, searchItem, menu)

        viewLifecycleOwner.lifecycleScope.launch {
            val direction = menu.findItem(R.id.item_sort_direction_card)
            if (paymentCardViewModel.preferencesFlow.first().isAscPaymentCards) {
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
                    for (i in 0 until paymentCardAdapter.itemCount) {
                        val longId = paymentCardAdapter.getItemId(i)
                        mutableListIds.add(longId)
                    }
                    tracker?.setItemsSelected(mutableListIds.asIterable(), true)
                    true
                }
                R.id.item_multi_select_delete -> {
                    paymentCardViewModel.onDeleteSelectedCardsClick()
                    tracker?.clearSelection()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(p0: ActionMode?) {
            paymentCardViewModel.onMultiSelectActionModeClose()
            tracker?.clearSelection()
            mActionMode = null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_sort_by_title_card -> {
                paymentCardViewModel.onSortOrderSelected(PaymentCardsSortOrder.BY_TITLE)
                true
            }
            R.id.item_sort_by_created_date_card -> {
                paymentCardViewModel.onSortOrderSelected(PaymentCardsSortOrder.BY_DATE_CREATED)
                true
            }
            R.id.item_sort_by_updated_date_card -> {
                paymentCardViewModel.onSortOrderSelected(PaymentCardsSortOrder.BY_DATE_UPDATED)
                true
            }
            R.id.item_sort_by_expiration_card -> {
                paymentCardViewModel.onSortOrderSelected(PaymentCardsSortOrder.BY_DATE_EXPIRATION)
                true
            }
            R.id.item_sort_by_type_card -> {
                paymentCardViewModel.onSortOrderSelected(PaymentCardsSortOrder.BY_TYPE)
                true
            }
            R.id.item_sort_direction_card -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    if (paymentCardViewModel.preferencesFlow.first().isAscPaymentCards) {
                        paymentCardViewModel.onSortDirectionSelected(false)
                        item.setIcon(R.drawable.ic_baseline_vertical_align_top)
                    } else {
                        paymentCardViewModel.onSortDirectionSelected(true)
                        item.setIcon(R.drawable.ic_baseline_vertical_align_bottom)
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onItemClick(card: PaymentCard) {
        paymentCardViewModel.onPaymentCardClicked(card)
        playSlidingAnimation(false, requireActivity())
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

    private fun setSearchViewListener(
        searchView: androidx.appcompat.widget.SearchView,
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

        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                hideKeyboard(requireActivity())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                paymentCardViewModel.searchQuery.value = newText.orEmpty()
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
        paymentCardViewModel.onStart()
    }
}
