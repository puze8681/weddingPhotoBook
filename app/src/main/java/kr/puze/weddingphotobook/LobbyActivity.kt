package kr.puze.weddingphotobook

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_lobby.*
import kr.puze.weddingphotobook.Adapter.MainGridAdapter
import kr.puze.weddingphotobook.Epub.MainActivity
import kr.puze.weddingphotobook.Utils.PrefUtil

class LobbyActivity : AppCompatActivity() {
    companion object{
        lateinit var mainAdapter: MainGridAdapter
        lateinit var prefUtil: PrefUtil
        var isOnEdit = false
        var isOnLobby = false
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)
    }

    private fun init(){
        isOnLobby = true
        prefUtil = PrefUtil(this@LobbyActivity)
//        prefUtil.resetPref()
        prefUtil.getStringArrayPref(prefUtil.KEY).map { str -> Log.d("dudco", str) }
        setGridView(getBookData())

        text_move_main.visibility = View.INVISIBLE
        text_delete_main.visibility = View.INVISIBLE
        text_edit_main.setOnClickListener {
            if(isOnEdit){
                isOnEdit = !isOnEdit
                text_move_main.visibility = View.INVISIBLE
                text_delete_main.visibility = View.INVISIBLE
                text_edit_main.text = "편집"
                setGridView(getBookData())
            }else{
                isOnEdit = !isOnEdit
                text_move_main.visibility = View.VISIBLE
                text_delete_main.visibility = View.VISIBLE
                text_edit_main.text = "완료"
                setGridView(getBookData())
                prefUtil.removeArrayPref("check")
            }
        }
        text_add_main.setOnClickListener {
            startActivity(Intent(this@LobbyActivity, FindActivity::class.java))
        }
        text_delete_main.setOnClickListener {
            var list = prefUtil.getStringArrayPref("check")
            for(value in list){
                prefUtil.deleteStringArrayPref("key", value)
            }
            setGridView(getBookData())
            prefUtil.removeArrayPref("check")
        }

        // input창에 검색어를 입력시 "addTextChangedListener" 이벤트 리스너를 정의한다.
        edit_main.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence,
                i: Int,
                i1: Int,
                i2: Int
            ) {
            }

            override fun onTextChanged(
                charSequence: CharSequence,
                i: Int,
                i1: Int,
                i2: Int
            ) {
            }

            // input창에 문자를 입력할때마다 호출된다.
            override fun afterTextChanged(editable: Editable) {
            // search 메소드를 호출한다.
                val text: String = edit_main.text.toString()
                search(text)
            }
        })
    }

    fun setGridView(items: ArrayList<String>) {
        mainAdapter = MainGridAdapter(items, isOnEdit)
        Log.d("mainAdapter", mainAdapter.count.toString())
        grid_main.adapter = mainAdapter
        grid_main.setOnItemClickListener { parent, view, position, id ->
            try {
                if(!isOnEdit){
                    if(items[position] != null){
                        val intent = Intent(this@LobbyActivity, MainActivity::class.java)
                        intent.putExtra("path", items[position])
                        startActivity(intent)
                    }
                }
            } catch (e: TypeCastException) {
            }
        }
    }

    // 검색을 수행하는 메소드
    fun search(charText: String) { // 문자 입력시마다 리스트를 지우고 새로 뿌려준다.
        var list = ArrayList<String>()
        list.clear()
        // 문자 입력이 없을때는 모든 데이터를 보여준다.
        if (charText.isEmpty()) {
            setGridView(getBookData())
            Log.d("mainAdapter search", "empty")
        } else { // 리스트의 모든 데이터를 검색한다.
            var arraylist = prefUtil.getStringArrayPref(prefUtil.KEY)
            for (i in 0 until arraylist.size) { // arraylist의 모든 데이터에 입력받은 단어(charText)가 포함되어 있으면 true를 반환한다.
                if (arraylist[i].toLowerCase().contains(charText)) { // 검색된 데이터를 리스트에 추가한다.
                    list.add(arraylist[i])
                }
            }
            setGridView(getBookData(list))
            Log.d("mainAdapter search", "search")
        }
    }

    private fun getBookData(): ArrayList<String>{
        var list = prefUtil.getStringArrayPref(prefUtil.KEY)
        var bookList = ArrayList<String>()
        for(i in list){
            bookList.add(i)
        }
        return bookList
    }

    private fun getBookData(list: ArrayList<String>): ArrayList<String>{
        var bookList = ArrayList<String>()
        for(i in list){
            bookList.add(i)
        }
        return bookList
    }

    override fun onResume() {
        super.onResume()
        if(!isOnLobby){
            init()
        }
    }

    override fun onPause() {
        super.onPause()
        isOnLobby = false
    }
}
