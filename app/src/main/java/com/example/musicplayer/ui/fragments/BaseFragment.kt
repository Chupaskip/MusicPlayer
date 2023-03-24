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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    protected fun setPaddingRv(recyclerView: RecyclerView, left: Int, top: Int, right: Int, bottom:Int) {
        recyclerView.setPadding(fromDpToPixels(left),
            fromDpToPixels(top),
            fromDpToPixels(right),
            fromDpToPixels(bottom))
    }

    private fun fromDpToPixels(dps:Int): Int {
        val scale: Float = context?.resources?.displayMetrics?.density!!
        return (dps * scale + 0.5f).toInt()
    }

    protected val viewModel:MusicViewModel by activityViewModels()

}