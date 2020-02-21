package kr.puze.weddingphotobook.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_find.view.*
import kr.puze.weddingphotobook.Data.FindData
import kr.puze.weddingphotobook.FindActivity
import kr.puze.weddingphotobook.R

class FindRecyclerAdapter(var items: ArrayList<FindData>, var context: Context) : RecyclerView.Adapter<FindRecyclerAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_find, null))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener {
            itemClick?.onItemClick(holder.itemView, position)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: FindData) {
            if(item.isEPUB){
                itemView.image_unchecked.visibility = View.VISIBLE
                itemView.image_checked.visibility = View.GONE
                itemView.image_folder.visibility = View.GONE
                itemView.image_enter.visibility = View.GONE
                itemView.text_path.text = item.path
                itemView.image_unchecked.setOnClickListener {
                    itemView.image_unchecked.visibility = View.GONE
                    itemView.image_checked.visibility = View.VISIBLE
                    FindActivity.addPathList.add(item.path!!)
                }

                itemView.image_checked.setOnClickListener {
                    itemView.image_unchecked.visibility = View.VISIBLE
                    itemView.image_checked.visibility = View.GONE
                    if(FindActivity.addPathList.contains(item.path!!)){
                        FindActivity.addPathList.remove(item.path!!)
                    }
                }
            }else{
                itemView.image_unchecked.visibility = View.GONE
                itemView.image_checked.visibility = View.GONE
                itemView.image_folder.visibility = View.VISIBLE
                itemView.image_enter.visibility = View.VISIBLE
                itemView.text_path.text = item.path
                itemView.image_folder.setOnClickListener {

                }
            }
        }
    }

    var itemClick: ItemClick? = null

    interface ItemClick {
        fun onItemClick(view: View?, position: Int)
    }
}
