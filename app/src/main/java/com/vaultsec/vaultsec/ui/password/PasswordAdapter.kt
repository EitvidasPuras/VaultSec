package com.vaultsec.vaultsec.ui.password

import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.entity.Password
import com.vaultsec.vaultsec.util.toPx

class PasswordAdapter(
    private val listener: OnItemClickListener
) : ListAdapter<Password, PasswordAdapter.PasswordHolder>(DIFF_CALLBACK) {

    init {
        setHasStableIds(true)
    }

    var tracker: SelectionTracker<Long>? = null

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Password>() {
            override fun areItemsTheSame(oldItem: Password, newItem: Password): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Password, newItem: Password): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class PasswordHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var container: MaterialCardView = itemView.findViewById(R.id.cardview_password)
        var textViewTitle: TextView = itemView.findViewById(R.id.textview_password_title)
        var textViewLogin: TextView = itemView.findViewById(R.id.textview_password_login)
        var textViewCategory: TextView = itemView.findViewById(R.id.textview_password_category)
        var context = itemView.context


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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordHolder {
        val itemView: View =
            LayoutInflater.from(parent.context).inflate(R.layout.password_item, parent, false)
        return PasswordHolder(itemView)
    }

    override fun onBindViewHolder(holder: PasswordHolder, position: Int) {
        val currentPassword: Password = getItem(position)
        tracker?.let {
            if (it.isSelected(position.toLong())) {
                holder.container.alpha = 0.5f
                holder.textViewTitle.text = currentPassword.title
                holder.textViewLogin.text = currentPassword.login
                holder.textViewCategory.text = holder.context.getString(
                    R.string.password_card_view_category_text,
                    currentPassword.category
                )
                holder.container.setCardBackgroundColor(Color.parseColor(currentPassword.color))


                if (holder.textViewTitle.text.isNullOrEmpty()) {
                    holder.textViewTitle.visibility = View.GONE
                } else {
                    holder.textViewTitle.visibility = View.VISIBLE
                }
                if (holder.textViewLogin.text.isNullOrEmpty()) {
                    holder.textViewLogin.visibility = View.GONE
                    val params =
                        holder.textViewTitle.layoutParams as ViewGroup.MarginLayoutParams
                    params.setMargins(0, 0, 0, 18.toPx)
                    holder.textViewTitle.requestLayout()
                } else {
                    holder.textViewLogin.visibility = View.VISIBLE
                    val params = holder.textViewTitle.layoutParams as ViewGroup.MarginLayoutParams
                    params.setMargins(0, 0, 0, 2.toPx)
                    holder.textViewTitle.requestLayout()
                }
            } else {
                holder.container.alpha = 1.0f
                holder.textViewTitle.text = currentPassword.title
                holder.textViewLogin.text = currentPassword.login
                holder.textViewCategory.text = holder.context.getString(
                    R.string.password_card_view_category_text,
                    currentPassword.category
                )
                holder.container.setCardBackgroundColor(Color.parseColor(currentPassword.color))

                if (holder.textViewTitle.text.isNullOrEmpty()) {
                    holder.textViewTitle.visibility = View.GONE
                } else {
                    holder.textViewTitle.visibility = View.VISIBLE
                }
                if (holder.textViewLogin.text.isNullOrEmpty()) {
                    holder.textViewLogin.visibility = View.GONE
                    val params =
                        holder.textViewTitle.layoutParams as ViewGroup.MarginLayoutParams
                    params.setMargins(0, 0, 0, 18.toPx)
                    holder.textViewTitle.requestLayout()
                } else {
                    holder.textViewLogin.visibility = View.VISIBLE
                    val params = holder.textViewTitle.layoutParams as ViewGroup.MarginLayoutParams
                    params.setMargins(0, 0, 0, 2.toPx)
                    holder.textViewTitle.requestLayout()
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
        fun onItemClick(password: Password)
    }

    class PasswordDetailsLookup(private val recyclerView: RecyclerView) :
        ItemDetailsLookup<Long>() {
        override fun getItemDetails(e: MotionEvent): ItemDetails<Long> {
            val view = recyclerView.findChildViewUnder(e.x, e.y) ?: return EMPTY_ITEM
            return (recyclerView.getChildViewHolder(view) as PasswordAdapter.PasswordHolder).getItemDetails()

        }

        object EMPTY_ITEM : ItemDetails<Long>() {
            override fun getPosition(): Int = Int.MAX_VALUE
            override fun getSelectionKey(): Long = Long.MAX_VALUE
        }
    }
}