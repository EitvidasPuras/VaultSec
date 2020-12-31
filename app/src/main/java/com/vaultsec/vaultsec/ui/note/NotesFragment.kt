package com.vaultsec.vaultsec.ui.note

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.databinding.FragmentNotesBinding
import com.vaultsec.vaultsec.viewmodel.NoteViewModel
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class NotesFragment : Fragment() {

    private lateinit var noteViewModel: NoteViewModel
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val navbar = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        val shadow = requireActivity().findViewById<View>(R.id.bottom_nav_shadow)
        if (navbar.visibility == View.GONE) {

            navbar.translationX = -navbar.width.toFloat()
            shadow.translationX = -navbar.width.toFloat()
            navbar.visibility = View.VISIBLE
            shadow.visibility = View.VISIBLE
            navbar.animate().translationX(0f).setDuration(300).setListener(null)
            shadow.animate().translationX(0f).setDuration(300).setListener(null)
        }

//        val animation = AnimationUtils.loadLayoutAnimation(activity, R.anim.layout_animation_fall_down)
        val adapter = NoteAdapter(context)
        binding.recyclerviewNotes.adapter = adapter
        binding.recyclerviewNotes.layoutManager =
            StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
//        binding.recyclerviewNotes.layoutManager = LinearLayoutManager(activity)
        binding.recyclerviewNotes.setHasFixedSize(true)
//        binding.recyclerviewNotes.layoutAnimation = animation
//        binding.recyclerviewNotes.itemAnimator = animation

        noteViewModel = ViewModelProvider(this).get(NoteViewModel::class.java)
//        noteViewModel.getAllNotes().observe(viewLifecycleOwner, Observer {
//            adapter.setNotes(it)
//        })
        noteViewModel.getAllNotes().observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
                binding.recyclerviewNotes.scheduleLayoutAnimation()
            }
        })

        if (!requireArguments().getString("title").isNullOrEmpty() ||
            !requireArguments().getString("text").isNullOrEmpty()
        ) {
            val title = requireArguments().getString("title")
            val text = requireArguments().getString("text")
            val fontSize = requireArguments().getInt("fontSize")
            val color = requireArguments().getString("color")
            val createdAt = Date(System.currentTimeMillis())

            val note = Note(0, title!!, text!!, color!!, fontSize, createdAt, createdAt)
            noteViewModel.insert(note)
        }

        adapter.setOnItemClickListener(object : NoteAdapter.OnItemClickListener {
            override fun onItemClick(note: Note) {
                Toast.makeText(context, "Clicked", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val navCon = Navigation.findNavController(view)
        binding.fabNotes.setOnClickListener {
            val navbar = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_view)
            val shadow = requireActivity().findViewById<View>(R.id.bottom_nav_shadow)

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
            view.findNavController().navigate(R.id.action_fragment_notes_to_fragment_new_note)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
