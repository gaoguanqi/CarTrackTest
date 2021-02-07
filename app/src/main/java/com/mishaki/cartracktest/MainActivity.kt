package com.mishaki.cartracktest

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mishaki.cartracktest.ui.HomeActivity
import com.mishaki.cartracktest.ui.MyEbikeActivity
import com.mishaki.cartracktest.utils.PermissionUtil
import com.mishaki.cartracktest.utils.RequestPermission
import com.tbruyelle.rxpermissions2.RxPermissions

class MainActivity:AppCompatActivity() {

    private val rxPermissions: RxPermissions by lazy { RxPermissions(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyPermissions()

    }
    override fun onRestart() {
        super.onRestart()
        applyPermissions()
    }

    private fun applyPermissions() {
        PermissionUtil.applyPermissions(object : RequestPermission {
            override fun onRequestPermissionSuccess() {
                launchTarget()
            }

            override fun onRequestPermissionFailure(permissions: List<String>) {
                showToast("权限未通过")
            }

            override fun onRequestPermissionFailureWithAskNeverAgain(permissions: List<String>) {
                showToast("权限未通过")
            }
        }, rxPermissions)
    }

    private fun launchTarget() {
        showToast("权限通过")
//        startActivity(Intent(MainActivity@this, HomeActivity::class.java))
        startActivity(Intent(MainActivity@this, MyEbikeActivity::class.java))
        this.finish()
    }


    private fun showToast(s:String?){
        if(!TextUtils.isEmpty(s)){
            Toast.makeText(this,s, Toast.LENGTH_SHORT).show()
        }
    }
}