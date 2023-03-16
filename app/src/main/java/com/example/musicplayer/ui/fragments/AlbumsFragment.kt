package com.example.musicplayer.ui.fragments

import com.example.musicplayer.databinding.FragmentAlbumsBinding

class AlbumsFragment:BaseFragment<FragmentAlbumsBinding>() {
    override val viewBinding: FragmentAlbumsBinding
        get() = FragmentAlbumsBinding.inflate(layoutInflater)
}