package kr.puze.weddingphotobook

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object{
        lateinit var mainAdapter: MainGridAdapter
        lateinit var prefUtil: PrefUtil
        var isOnEdit = false
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
    }

    private fun init(){
        prefUtil = PrefUtil(this@MainActivity)
        setGridView(prefUtil.getStringArrayPref(prefUtil.KEY))

        text_move_main.visibility = View.INVISIBLE
        text_delete_main.visibility = View.INVISIBLE
        text_edit_main.setOnClickListener {
            if(isOnEdit){
                text_move_main.visibility = View.INVISIBLE
                text_delete_main.visibility = View.INVISIBLE
                text_edit_main.text = "완료"
            }else{
                text_move_main.visibility = View.VISIBLE
                text_delete_main.visibility = View.VISIBLE
                text_edit_main.text = "편집"
            }
            isOnEdit = !isOnEdit
        }
    }

    private fun setGridView(items: ArrayList<String>) {
        mainAdapter = MainGridAdapter(items)
        Log.d("MainFittingFragment2", mainAdapter.count.toString())
        grid_main.adapter = mainAdapter
        grid_main.setOnItemClickListener { parent, view, position, id ->
            try {
            } catch (e: TypeCastException) {
            }
        }
    }
}
