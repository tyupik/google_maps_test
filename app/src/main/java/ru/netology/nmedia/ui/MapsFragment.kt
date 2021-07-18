package ru.netology.nmedia.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.maps.android.ktx.awaitAnimateCamera
import com.google.maps.android.ktx.awaitMap
import com.google.maps.android.ktx.model.cameraPosition
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import java.lang.reflect.Type

class MapsFragment : Fragment() {
    private lateinit var googleMap: GoogleMap
    private var myMarkers = mutableListOf<MarkerOptions>()
    private val markerInfoBSF = MarkerInfoBottomSheetFragment()
    private var canPlaceNew: Boolean = false
    private var removeMode = false
    private var editMode = false
    private var showMode = true

    @SuppressLint("MissingPermission")
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                googleMap.apply {
                    isMyLocationEnabled = true
                    uiSettings.isMyLocationButtonEnabled = true
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Без доступа функционал ограничен",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        loadMarkers()

//        googleMap.awaitAnimateCamera(
//                CameraUpdateFactory.newCameraPosition(
//                    cameraPosition {
//                        target(showCurre)
//                        zoom(15F)
//                    }
//                ))

        lifecycle.coroutineScope.launchWhenCreated {

            googleMap = mapFragment.awaitMap().apply {
                isTrafficEnabled = true
                isBuildingsEnabled = true

                uiSettings.apply {
                    isZoomControlsEnabled = true
                    setAllGesturesEnabled(true)
                }
            }

            myMarkers.forEach {
                googleMap.addMarker(MarkerOptions().position(it.position).title(it.title))
            }

            when {
                // 1. Проверяем есть ли уже права
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    googleMap.apply {
                        isMyLocationEnabled = true
                        uiSettings.isMyLocationButtonEnabled = true
                    }

                    val fusedLocationProviderClient = LocationServices
                        .getFusedLocationProviderClient(requireActivity())

                    fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                        println(it)
                    }
                }
                // 2. Должны показать обоснование необходимости прав
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                    Toast.makeText(
                        requireContext(),
                        "Нужны права для позиционирования",
                        Toast.LENGTH_LONG
                    ).show()
                }
                // 3. Запрашиваем права
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            googleMap.setOnMarkerClickListener { marker ->
                marker.showInfoWindow()
                if (showMode) {
                    val args = Bundle()
                    args.putString("title", marker.title)
                    args.putString("coordinates", convertCoordinates(marker.position))
                    markerInfoBSF.arguments = args
                    markerInfoBSF.show(childFragmentManager, "mark")
                }


                if (removeMode) {
                    marker.hideInfoWindow()
                    marker.remove()
                    myMarkers = myMarkers.filterNot { it.title == marker.title }.toMutableList()
                    removeMode = false
                    showMode = true
                }

                if (editMode) {
                    marker.hideInfoWindow()
                    showEditDialog(marker.title) { newTitle ->
                        myMarkers = myMarkers.map {
                            if (it.title == marker.title) {
                                MarkerOptions().position(it.position).title(newTitle)
                            } else {
                                it
                            }
                        }.toMutableList()
                        marker.title = newTitle
                        marker.showInfoWindow()
                        editMode = false
                        showMode = true
                    }
                }
                true
            }

            googleMap.setOnMapClickListener { coordinates ->
                if (canPlaceNew) {
                    val target = LatLng(coordinates.latitude, coordinates.longitude)
                    showNewMarkerDialog { title ->
                        googleMap.addMarker(MarkerOptions().position(target).title(title))
                        myMarkers.add(MarkerOptions().position(target).title(title))
                    }
                    canPlaceNew = false
                }
            }


//            val target = LatLng(55.751999, 37.617734)
//            val markerManager = MarkerManager(googleMap)
//            val collection: MarkerManager.Collection = markerManager.newCollection().apply {
//                addMarker {
//                    position(target)
//                    icon(getDrawable(requireContext(), R.drawable.ic_netology_48dp)!!)
//                    title("The Moscow Kremlin")
//                }.apply {
//                    tag = "Any additional data"  //Any
//                }
//            }
//            collection.setOnMarkerClickListener { marker ->
//                // TODO: work with marker
//                Toast.makeText(
//                    requireContext(),
//                    (marker.tag as String),
//                    Toast.LENGTH_LONG
//                ).show()
//                true
//            }

//            googleMap.setOnMapLongClickListener {
//                val mark = LatLng(it.latitude, it.longitude)
//                collection.addMarker {
//                    position(mark)
//                    icon(getDrawable(requireContext(), R.drawable.ic_netology_48dp)!!)
//                    title("test")
//                }
//            }
//            collection.setOnMarkerClickListener { marker ->
//                marker.showInfoWindow()
//                val args = Bundle()
//                args.putString("title", marker.title)
//                markerInfoBSF.arguments = args
//                markerInfoBSF.show(childFragmentManager, "test")
//                true
//            }

//            googleMap.awaitAnimateCamera(
//                CameraUpdateFactory.newCameraPosition(
//                    cameraPosition {
//                        target(target)
//                        zoom(15F)
//                    }
//                ))
        }
    }

    private fun loadMarkers() {
        val founderListType: Type =
            object : TypeToken<ArrayList<ru.netology.nmedia.ui.Marker>>() {}.type
        requireActivity().getPreferences(Context.MODE_PRIVATE)?.let { sp ->
            sp.getString("markers", "[]")?.let { list ->
                val prepList: ArrayList<ru.netology.nmedia.ui.Marker> = GsonBuilder()
                    .create()
                    .fromJson(list, founderListType)
                myMarkers =
                    prepList.map { MarkerOptions().position(it.coordinates).title(it.title) }
                        .toMutableList()
            }
        }
    }

    private fun showEditDialog(oldName: String, newName: (String) -> Unit) {
        var returnText = "-1"

        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Edit marker")
        val dialogView = layoutInflater.inflate(R.layout.new_marker, null)
        dialogBuilder.setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                returnText = dialogView.findViewById<EditText>(R.id.newPlace).text.toString()
                newName(returnText)
            }
            .setNegativeButton("Отмена") { _, _ -> }
        dialogView.findViewById<EditText>(R.id.newPlace).setText(oldName)
        dialogBuilder.create().show()
    }

    private fun showNewMarkerDialog(name: (String) -> Unit) {
        var returnText = "-1"

        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Новый маркер")
        val dialogView = layoutInflater.inflate(R.layout.new_marker, null)
        dialogBuilder.setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                returnText = dialogView.findViewById<EditText>(R.id.newPlace).text.toString()
                name(returnText)
                saveMakers()
            }
            .setNegativeButton("Отмена") { _, _ -> }
        dialogBuilder.create().show()
    }

    private fun showAreasDialog() {
        val charSequence: Array<CharSequence> =
            myMarkers.map { it.title as CharSequence }.toTypedArray()

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Списко маркеров:")
            .setSingleChoiceItems(charSequence, 0) { d, i ->
                lifecycleScope.launch {
                    googleMap.awaitAnimateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition {
                        target(myMarkers.filter { it.title == charSequence[i].toString() }[0].position)
                        zoom(15F)
                    }))
                }
                d.dismiss()
            }
            .setNegativeButton("Отмена") { _, _ -> }
        builder.create().show()
    }

    override fun onStop() {
        super.onStop()
        saveMakers()
    }

    private fun saveMakers() {
        val sharedPref = requireActivity().getPreferences(AppCompatActivity.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("markers", GsonBuilder().create().toJson(convert(myMarkers)))
            apply()
        }
    }

    private fun convert(markers: MutableList<MarkerOptions>): MutableList<Marker> {
        val items = mutableListOf<Marker>()
        markers.forEach {
            items.add(Marker(it.title ?: "empty", it.position))
        }
        return items
    }

    private fun convertCoordinates(coordinates: LatLng) : String {
        val lat = String.format("%.5f", coordinates.latitude)
        val long = String.format("%.5f", coordinates.longitude)
        return "$lat, $long"
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.newMarker -> {
                canPlaceNew = true
                editMode = false
                removeMode = false
                Toast.makeText(requireContext(), "Выберите место для нового маркера", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.editMarker -> {
                canPlaceNew = false
                editMode = true
                removeMode = false
                showMode = false
                Toast.makeText(requireContext(), "Выберите маркер для редактирования", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.deleteMarker -> {
                canPlaceNew = false
                editMode = false
                removeMode = true
                showMode = false
                Toast.makeText(requireContext(), "Выберите маркер для удаляния", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.listAllMarkers -> {
                showAreasDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}