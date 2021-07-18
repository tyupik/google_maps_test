package ru.netology.nmedia.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_marker_info.*
import ru.netology.nmedia.R

class MarkerInfoBottomSheetFragment : BottomSheetDialogFragment() {
    private var isDeleted = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return  inflater.inflate(R.layout.fragment_marker_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        marker_title.text = arguments?.getString("title")
        marker_coordinates.text = arguments?.getString("coordinates")
    }

}