package aakash.com.voiceandmapassignment.common.util

import android.content.Context
import android.widget.Toast
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.search.*
import java.lang.IllegalArgumentException

class GeoSearchHelper(private val context: Context, lat: Double, lng: Double) {
    private val seattle = GeoCoordinate(lat, lng)
    fun requestPlace(place: String, onSearchResultFetch: (discoveryResultPage: DiscoveryResultPage) -> Unit) {
        try {
            val request = SearchRequest(place).setSearchCenter(seattle)
            request.setCollectionSize(10)
            request.execute { discoveryResultPage, errorCode ->
                if (errorCode != ErrorCode.NONE) {
                    Toast.makeText(context, errorCode.name, Toast.LENGTH_LONG).show()
                } else {
                    onSearchResultFetch(discoveryResultPage)
                }
            }
        } catch (ex: IllegalArgumentException) {
            Toast.makeText(context, ex.message, Toast.LENGTH_LONG).show()
        }
    }

    fun autoSuggestPlaces(place: String, onAutoSuggestResultFetch: (autoSuggestList: List<AutoSuggest>) -> Unit) {
        try {
            val request = TextAutoSuggestionRequest(place).setSearchCenter(seattle)
            request.setCollectionSize(10)
            request.execute { mutableList, errorCode ->
                if (errorCode != ErrorCode.NONE) {
                    Toast.makeText(context, errorCode.name, Toast.LENGTH_LONG).show()
                } else {
                    onAutoSuggestResultFetch(mutableList)
                }
            }
        } catch (ex: IllegalArgumentException) {
            Toast.makeText(context, ex.message, Toast.LENGTH_LONG).show()
        }
    }

}