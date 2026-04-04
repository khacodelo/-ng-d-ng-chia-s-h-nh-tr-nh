package com.example.apptheodihnhtrnh.ui.feed

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.apptheodihnhtrnh.R
import com.example.apptheodihnhtrnh.ui.history.HistoryMapActivity
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

data class UserInfo(val email: String?)
data class CheckpointInfo(val note: String?, val imageUrl: String?, val lat: Double?, val lng: Double?)
data class PointData(val lat: Double, val lng: Double)
data class JourneyData(
    val userId: UserInfo?,
    val startTime: Date?,
    val distance: Float?,
    val points: List<PointData>?,
    val checkpoints: List<CheckpointInfo>?
)

class JourneyAdapter(private val journeys: List<JourneyData>) : RecyclerView.Adapter<JourneyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUser: TextView = view.findViewById(R.id.tvUserEmail)
        val tvInfo: TextView = view.findViewById(R.id.tvJourneyInfo)
        val tvNotes: TextView = view.findViewById(R.id.tvNotes)
        val imageContainer: LinearLayout = view.findViewById(R.id.imageContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_journey, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val journey = journeys[position]
        
        holder.tvUser.text = journey.userId?.email ?: "Ẩn danh"
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        holder.tvInfo.text = "${if (journey.startTime != null) sdf.format(journey.startTime) else ""} - %.2f km".format((journey.distance ?: 0f) / 1000)
        
        val notes = journey.checkpoints?.filter { !it.note.isNullOrEmpty() }?.joinToString("; ") { it.note!! }
        holder.tvNotes.text = if (!notes.isNullOrEmpty()) "Ghi chú: $notes" else ""

        holder.imageContainer.removeAllViews()
        journey.checkpoints?.forEach { cp ->
            if (!cp.imageUrl.isNullOrEmpty()) {
                val imageView = ImageView(holder.itemView.context)
                val params = LinearLayout.LayoutParams(250, 250)
                params.setMargins(0, 0, 12, 0)
                imageView.layoutParams = params
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(holder.itemView.context).load(cp.imageUrl).into(imageView)
                holder.imageContainer.addView(imageView)
            }
        }

        // Sự kiện: Nhấn vào để xem bản đồ lịch sử
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, HistoryMapActivity::class.java)
            // Chuyển dữ liệu points và checkpoints sang dạng JSON để truyền đi
            intent.putExtra("POINTS_JSON", Gson().toJson(journey.points))
            intent.putExtra("CHECKPOINTS_JSON", Gson().toJson(journey.checkpoints))
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = journeys.size
}
