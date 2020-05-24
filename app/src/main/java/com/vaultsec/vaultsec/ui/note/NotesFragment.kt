package com.vaultsec.vaultsec.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.databinding.FragmentNotesBinding
import com.vaultsec.vaultsec.viewmodel.NoteViewModel

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
    ): View? {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        val view = binding.root

        // Inflate the layout for this fragment
        return view

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

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

        adapter.setOnItemClickListener(object : NoteAdapter.OnItemClickListener {
            override fun onItemClick(note: Note) {
                Toast.makeText(context, "Clicked", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
