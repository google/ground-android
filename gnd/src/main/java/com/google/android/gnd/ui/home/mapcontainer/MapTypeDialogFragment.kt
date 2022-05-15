package com.google.android.gnd.ui.home.mapcontainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gnd.R
import com.google.android.gnd.databinding.MapTypeDialogFragmentBinding
import com.google.android.gnd.repository.MapsRepository
import com.google.android.gnd.ui.map.MapType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MapTypeDialogFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var mapsRepository: MapsRepository

    private lateinit var binding: MapTypeDialogFragmentBinding
    private lateinit var mapTypes: Array<MapType>
    private lateinit var items: List<ItemsViewModel>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mapTypes = MapTypeDialogFragmentArgs.fromBundle(arguments!!).mapTypes
        items = mapTypes.map { it.toItemViewModel() }
        binding = MapTypeDialogFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dialogCloseBtn.setOnClickListener { dismiss() }

        val recyclerview = binding.recyclerView
        recyclerview.adapter = MapTypeAdapter(items, getSelectedItemIndex()) { position ->
            mapsRepository.saveMapType(mapTypes[position].type)
        }
    }

    private fun getSelectedItemIndex(): Int {
        val selectedMapType = mapsRepository.mapType.value
        for ((index, mapType) in mapTypes.withIndex()) {
            if (mapType.type == selectedMapType) return index
        }
        return -1
    }

    // Remove this once drawable is added to MapType model
    private fun MapType.toItemViewModel(): ItemsViewModel {
        return ItemsViewModel(R.drawable.ground_logo, getString(labelId))
    }
}
