package com.vaultsec.vaultsec.ui.paymentcard

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.entity.PaymentCard
import com.vaultsec.vaultsec.util.SupportedPaymentCardTypes

class PaymentCardAdapter(
    private val listener: OnItemClickListener
) : ListAdapter<PaymentCard, PaymentCardAdapter.PaymentCardHolder>(DIFF_CALLBACK) {

    init {
        setHasStableIds(true)
    }

    var tracker: SelectionTracker<Long>? = null

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PaymentCard>() {
            override fun areItemsTheSame(oldItem: PaymentCard, newItem: PaymentCard): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PaymentCard, newItem: PaymentCard): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class PaymentCardHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var container: MaterialCardView = itemView.findViewById(R.id.cardview_payment_card)
        var textViewTitle: TextView = itemView.findViewById(R.id.textview_card_title)
        var textViewCardNumber: TextView = itemView.findViewById(R.id.textview_card_card_number)
        var imageViewType: ImageView = itemView.findViewById(R.id.imageview_card_type)

        init {
            itemView.setOnClickListener {
                val position = getItemDetails().position
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(position))
                }
            }
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int = bindingAdapterPosition
                override fun getSelectionKey(): Long = itemId
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentCardHolder {
        val itemView: View =
            LayoutInflater.from(parent.context).inflate(R.layout.payment_card_item, parent, false)
        return PaymentCardHolder(itemView)
    }

    override fun onBindViewHolder(holder: PaymentCardHolder, position: Int) {
        val currentPaymentCard: PaymentCard = getItem(position)

        tracker?.let {
            if (it.isSelected(position.toLong())) {
                holder.container.alpha = 0.5f
                holder.textViewTitle.text = currentPaymentCard.title
                holder.textViewCardNumber.text =
                    currentPaymentCard.cardNumber.replace("....".toRegex(), "$0 ")

                when (currentPaymentCard.type) {
                    SupportedPaymentCardTypes.VISA -> {
                        holder.imageViewType.setImageResource(R.drawable.ic_visa)
                    }
                    SupportedPaymentCardTypes.MasterCard -> {
                        holder.imageViewType.setImageResource(R.drawable.ic_mc_symbol)
                    }
                    else -> {
                        holder.imageViewType.setImageResource(R.drawable.ic_credit_card_bigger)
                    }
                }

                if (holder.textViewTitle.text.isNullOrEmpty()) {
                    holder.textViewTitle.visibility = View.GONE
                } else {
                    holder.textViewTitle.visibility = View.VISIBLE
                }

            } else {
                holder.container.alpha = 1.0f
                holder.textViewTitle.text = currentPaymentCard.title
                holder.textViewCardNumber.text =
                    currentPaymentCard.cardNumber.replace("....".toRegex(), "$0 ")

                when (currentPaymentCard.type) {
                    SupportedPaymentCardTypes.VISA -> {
                        holder.imageViewType.setImageResource(R.drawable.ic_visa)
                    }
                    SupportedPaymentCardTypes.MasterCard -> {
                        holder.imageViewType.setImageResource(R.drawable.ic_mc_symbol)
                    }
                    else -> {
                        holder.imageViewType.setImageResource(R.drawable.ic_credit_card_bigger)
                    }
                }

                if (holder.textViewTitle.text.isNullOrEmpty()) {
                    holder.textViewTitle.visibility = View.GONE
                } else {
                    holder.textViewTitle.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    interface OnItemClickListener {
        fun onItemClick(card: PaymentCard)
    }

    class PaymentCardDetailsLookup(private val recyclerView: RecyclerView) :
        ItemDetailsLookup<Long>() {
        override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
            val view = recyclerView.findChildViewUnder(e.x, e.y) ?: return EMPTY_ITEM
            return (recyclerView.getChildViewHolder(view) as PaymentCardAdapter.PaymentCardHolder).getItemDetails()
        }

        object EMPTY_ITEM : ItemDetails<Long>() {
            override fun getPosition(): Int = Int.MAX_VALUE
            override fun getSelectionKey(): Long = Long.MAX_VALUE
        }
    }
}