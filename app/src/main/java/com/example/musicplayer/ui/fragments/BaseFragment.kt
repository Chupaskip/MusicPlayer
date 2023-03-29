package com.example.musicplayer.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.musicplayer.ui.MusicViewModel
import com.example.musicplayer.ui.util.Space.Companion.fromDpToPixels

abstract class BaseFragment<VB : ViewBinding> : Fragment() {
    private var _binding: VB? = null
    val binding
        get() = _binding!!
    abstract val viewBinding: VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = viewBinding
        return binding.root
    }

    protected fun setPaddingRv(
        recyclerView: RecyclerView,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        recyclerView.setPadding(fromDpToPixels(requireContext(), left),
            fromDpToPixels(requireContext(), top),
            fromDpToPixels(requireContext(), right),
            fromDpToPixels(requireContext(), bottom))
    }


    protected val viewModel: MusicViewModel by activityViewModels()

}