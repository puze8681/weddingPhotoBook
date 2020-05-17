package kr.puze.weddingphotobook

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_thumbnail.*
import kr.puze.weddingphotobook.Adapter.ThumbnailGridAdapter

class ThumbnailActivity : AppCompatActivity() {

    companion object{
        lateinit var thimbnailAdapter: ThumbnailGridAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thumbnail)
        init()
    }

    private fun init(){
        text_back_thumbnail.setOnClickListener { finish() }
    }

    private fun setGridView(items: ArrayList<String>) {
        thimbnailAdapter = ThumbnailGridAdapter(items)
        Log.d("thimbnailAdapter", thimbnailAdapter.count.toString())
        grid_thumbnail.adapter = thimbnailAdapter
        grid_thumbnail.setOnItemClickListener { parent, view, position, id ->
            try {
            } catch (e: TypeCastException) {
            }
        }
    }
}
