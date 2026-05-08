package app.olauncher.ui

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.olauncher.R
import app.olauncher.data.GeocodeResult
import app.olauncher.databinding.DialogWeatherLocationBinding
import app.olauncher.databinding.ItemGeocodeResultBinding
import app.olauncher.helper.WeatherClient
import app.olauncher.helper.applyTypefaceRecursively
import app.olauncher.helper.resolveCustomTypeface
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object WeatherLocationPickerDialog {

    private const val DEBOUNCE_MS = 300L

    fun show(
        context: Context,
        scope: LifecycleCoroutineScope,
        fontFamily: Int,
        onPicked: (GeocodeResult) -> Unit
    ) {
        val binding = DialogWeatherLocationBinding.inflate(LayoutInflater.from(context))
        val adapter = GeocodeAdapter()
        binding.resultsList.layoutManager = LinearLayoutManager(context)
        binding.resultsList.adapter = adapter

        resolveCustomTypeface(context, fontFamily)?.let { binding.root.applyTypefaceRecursively(it) }

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.weather_location)
            .setView(binding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        adapter.onClick = { result ->
            onPicked(result)
            dialog.dismiss()
        }

        var debounceJob: Job? = null
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                debounceJob?.cancel()
                val query = s?.toString().orEmpty().trim()
                if (query.length < 2) {
                    adapter.submit(emptyList())
                    binding.statusText.visibility = View.GONE
                    return
                }
                debounceJob = scope.launch {
                    delay(DEBOUNCE_MS)
                    binding.statusText.setText(R.string.weather_searching)
                    binding.statusText.visibility = View.VISIBLE
                    val results = try {
                        WeatherClient.searchCity(query)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    adapter.submit(results)
                    if (results.isEmpty()) {
                        binding.statusText.setText(R.string.weather_no_results)
                        binding.statusText.visibility = View.VISIBLE
                    } else {
                        binding.statusText.visibility = View.GONE
                    }
                }
            }
        })

        dialog.show()
    }

    private class GeocodeAdapter : RecyclerView.Adapter<GeocodeAdapter.VH>() {
        private val items = mutableListOf<GeocodeResult>()
        var onClick: ((GeocodeResult) -> Unit)? = null

        fun submit(newItems: List<GeocodeResult>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemGeocodeResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(binding)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.binding.geocodeLabel.text = item.displayLabel()
            holder.binding.root.setOnClickListener { onClick?.invoke(item) }
        }

        override fun getItemCount(): Int = items.size

        class VH(val binding: ItemGeocodeResultBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
