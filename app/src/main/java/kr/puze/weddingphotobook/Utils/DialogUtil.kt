package kr.puze.weddingphotobook.Utils

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.util.Log
import android.view.Window
import kotlinx.android.synthetic.main.dialog_add.*
import kr.puze.weddingphotobook.LobbyActivity
import kr.puze.weddingphotobook.R

class DialogUtil(context: Context) {
    var context: Context = context
    var prefUtil: PrefUtil = PrefUtil(context)
    var progressDialog: ProgressDialog = ProgressDialog(context)
    var alertDialog: AlertDialog.Builder = AlertDialog.Builder(context)

    fun dialogAdd(pathes: ArrayList<String>) {
        Log.d("LOGTAG, pathes", pathes.toString())
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add)
        dialog.add_path.text = pathes.toString()
        dialog.add_cancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.add_confirm.setOnClickListener {
            for(path in pathes){
                prefUtil.addStringArrayPref("key", path)
            }
            (context as Activity).finish()
            dialog.dismiss()
        }
        dialog.show()
    }
}