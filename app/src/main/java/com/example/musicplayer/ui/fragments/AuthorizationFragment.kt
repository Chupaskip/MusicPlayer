package com.example.musicplayer.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.musicplayer.R
import com.example.musicplayer.databinding.FragmentAuthorizationBinding
import com.example.musicplayer.db.UserEntity
import com.example.musicplayer.ui.MainActivity

class AuthorizationFragment : BaseFragment<FragmentAuthorizationBinding>() {
    override val viewBinding: FragmentAuthorizationBinding
        get() = FragmentAuthorizationBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.isLogin.observe(viewLifecycleOwner){
            binding.root.isVisible = !it
        }
        viewModel.isBottomMenuVisible.value = false
        viewModel.isLogin.value = false
        binding.btnAuthorization.setOnClickListener {
            if(binding.etLogin.text.toString().isEmpty() &&  binding.etPassword.text.toString().isEmpty())
                return@setOnClickListener
            if(binding.btnAuthorization.text == "Авторизироваться"){
                viewModel.login(UserEntity(username = binding.etLogin.text.toString(), password = binding.etPassword.text.toString()))

            }else{
                if (binding.etPasswordAgain.text.toString() != binding.etPassword.text.toString()) {
                    Toast.makeText(context, "Пароли не совпадают!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.register(UserEntity(username = binding.etLogin.text.toString(), password = binding.etPassword.text.toString())){
                    binding.btnAuthorization.text = "Авторизироваться"
                    binding.tvSign.text = "Регистрация"
                    binding.inputPasswordAgain.isVisible = false
                    Toast.makeText(context, "Вы зарегестрированы!", Toast.LENGTH_SHORT).show()
                }
            }

        }

        binding.tvSign.setOnClickListener {
            if(binding.tvSign.text=="Регистрация"){
                binding.tvSign.text = "Авторизация"
                binding.btnAuthorization.text = "Зарегистрироваться"
                binding.inputPasswordAgain.isVisible = true
            }else{
                binding.tvSign.text = "Регистрация"
                binding.btnAuthorization.text = "Авторизироваться"
                binding.inputPasswordAgain.isVisible = false
                binding.inputPasswordAgain.visibility = View.GONE
            }
        }
    }
}