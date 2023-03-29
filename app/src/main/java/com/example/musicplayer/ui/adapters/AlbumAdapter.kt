package com.example.musicplayer.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.databinding.ItemAlbumBinding
import com.example.musicplayer.models.Album
import com.example.musicplayer.models.Song
import com.example.musicplayer.models.setImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlbumAdapter(private val listener: OnAlbumClickListener) :
    ListAdapter<Album, AlbumAdapter.AlbumViewHolder>(diffUtil) {

    inner class AlbumViewHolder(val binding: ItemAlbumBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<Album>() {
            override fun areItemsTheSame(oldItem: Album, newItem: Album): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Album, newItem: Album): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        return AlbumViewHolder(ItemAlbumBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false))
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = getItem(position)
        holder.binding.apply {
            CoroutineScope(Dispatchers.Main).launch {
                if (album.image == null)
                    album.setImage()
                Glide.with(this@apply.root)
                    .load(album.image)
//                    .placeholder(R.drawable.placeholder_no_art)
                    .error(R.drawable.placeholder_no_art)
                    .into(this@apply.ivAlbum)
            }

            tvAlbumTitle.text = album.title
            tvAlbumArtist.text = album.artist
            tvYear.text = album.year ?: ""
            root.setOnClickListener {
                listener.onAlbumClick(album)
            }
        }
    }

    interface OnAlbumClickListener {
        fun onAlbumClick(album: Album)
    }
}