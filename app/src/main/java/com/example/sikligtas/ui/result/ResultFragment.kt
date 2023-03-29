package com.example.sikligtas.ui.result

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.navArgs
import com.example.sikligtas.R
import com.example.sikligtas.databinding.FragmentResultBinding
import com.example.sikligtas.ui.maps.MapsFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ResultFragment : BottomSheetDialogFragment() {

    private val args: ResultFragmentArgs by navArgs()

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentResultBinding.inflate(inflater, container, false)

        binding.distanceValueTextView.text = getString(R.string.result, args.result.distance)
        binding.timeValueTextView.text = args.result.time

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}