import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sikligtas.R
import com.example.sikligtas.data.HistoryItem

class HistoryFragmentAdapter: RecyclerView.Adapter<HistoryFragmentAdapter.ViewHolder>() {
    private var historyItems: List<HistoryItem> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyItems[position]
        holder.date.text = item.date
        holder.startLoc.text = item.startLoc
        holder.endLoc.text = item.endLoc
        holder.elapsedTime.text = item.elapsedTime
        holder.distance.text = item.distance
    }

    override fun getItemCount(): Int {
        return historyItems.size
    }

    fun setHistoryItems(items: List<HistoryItem>) {
        historyItems = items
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.curr_date_time)
        val startLoc: TextView = view.findViewById(R.id.start_loc)
        val endLoc: TextView = view.findViewById(R.id.end_loc)
        val elapsedTime: TextView = view.findViewById(R.id.elapsed_time)
        val distance: TextView = view.findViewById(R.id.distance)
    }
}
