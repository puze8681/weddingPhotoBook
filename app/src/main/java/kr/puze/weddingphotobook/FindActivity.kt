package kr.puze.weddingphotobook

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import kotlinx.android.synthetic.main.activity_find.*
import kr.puze.weddingphotobook.Adapter.FindRecyclerAdapter
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import kr.puze.weddingphotobook.Data.FindData
import kr.puze.weddingphotobook.Utils.DialogUtil
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


@RequiresApi(Build.VERSION_CODES.M)
class FindActivity : AppCompatActivity() {

    companion object{
        lateinit var findAdapter : FindRecyclerAdapter
        lateinit var dialogUtil: DialogUtil
        private val REQUEST_PERMISSION_CODE = 111
        private val GALLERY_CODE = 222
        var addPathList: ArrayList<String> = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find)

        init()
    }

    private fun init(){
        dialogUtil = DialogUtil(this@FindActivity)
        val path = ((Environment.getExternalStorageDirectory().absolutePath))
        checkPermission(path)
        image_back.setOnClickListener { finish() }
        text_add_find.setOnClickListener {
            if(addPathList.size > 0){
                dialogUtil.dialogAdd(addPathList)
            }else{
                Toast.makeText(this@FindActivity, "추가할 eBook 을 선택해주세요.", Toast.LENGTH_LONG).show()
            }

        }
    }

    private fun setRecyclerView(pathItem: ArrayList<FindData>) {
        addPathList.clear()
        findAdapter = FindRecyclerAdapter(pathItem, this@FindActivity)
        recycler_find.adapter = findAdapter
        (recycler_find.adapter as FindRecyclerAdapter).notifyDataSetChanged()
        findAdapter.itemClick = object : FindRecyclerAdapter.ItemClick {
            override fun onItemClick(view: View?, position: Int) {
                if(!findAdapter.items[position].isEPUB){
                    setRecyclerView(getDirectoryList(findAdapter.items[position].path!!))
                }
            }
        }
    }

    @SuppressLint("LongLogTag")
    private fun getDirectoryList(path: String): ArrayList<FindData>{
        text_path.text = path
        var datas: ArrayList<FindData> = ArrayList()
        datas.clear()
        val f = File(path)
        val files: Array<File>? = f.listFiles()
        if (files != null) {
            if(files.isNotEmpty()){
                for(file in files){
                    Log.d("LOGTAG, filepath", file.path)
                    Log.d("LOGTAG, fileabsolute", file.absolutePath)
                    if(file.name.toLowerCase(Locale.US).endsWith(".epub")){
                        datas.add(FindData(file.path, true))
                    }else{
                        datas.add(FindData(file.path, false))
                    }
                }
            }
        }
        return datas
    }

    private fun checkPermission(path: String) {
        if ( checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) { Toast.makeText(this@FindActivity, "eBook을 불러오기 위해 권한을 허용해주세요.", Toast.LENGTH_SHORT).show() }
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_CODE)
        } else {
            setRecyclerView(getDirectoryList(path))
        }
    }

    //카메라 촬영, 촬영한 이미지 저장 기능은 주석처리함
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_CODE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this@FindActivity, "eBook을 불러오기 위해 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
