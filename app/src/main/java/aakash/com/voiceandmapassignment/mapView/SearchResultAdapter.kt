package aakash.com.voiceandmapassignment.mapView

import aakash.com.voiceandmapassignment.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.disposables.CompositeDisposable

class SearchResultAdapter(private val itemList: ArrayList<String>,
                          private val compositeDisposable: CompositeDisposable,
                          private val onItemClick : (position: Int) -> Unit) :
    RecyclerView.Adapter<SearchResultAdapter.SearchHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHolder {
        return SearchHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_search_results, parent, false))
    }

    override fun getItemCount() = itemList.size

    override fun onBindViewHolder(holder: SearchHolder, position: Int) {
        (holder.itemView as TextView).text = itemList[position]
        compositeDisposable.add(
            holder.itemView.clicks().subscribe {
                onItemClick(position)
            }
        )
    }

    inner class SearchHolder(view: View) : RecyclerView.ViewHolder(view)
}