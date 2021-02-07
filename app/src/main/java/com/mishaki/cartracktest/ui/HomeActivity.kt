package com.mishaki.cartracktest.ui

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.TextureMapView
import com.baidu.mapapi.model.LatLng
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ResourceUtils
import com.blankj.utilcode.util.ToastUtils
import com.mishaki.cartracktest.R
import com.mishaki.cartracktest.entity.CarEntity
import com.mishaki.cartracktest.manager.carTrack.CarTrackManager
import com.mishaki.cartracktest.manager.carTrack.MoveOnlineTrackManager
import com.mishaki.cartracktest.utils.LogUtils
import com.mishaki.cartracktest.utils.UIUtils
import org.jetbrains.anko.collections.forEachWithIndex

class HomeActivity : AppCompatActivity() {
    private lateinit var mapView: TextureMapView
    private lateinit var baiduMap: BaiduMap
    private val carMarker by lazy { BitmapDescriptorFactory.fromResource(R.drawable.marker_car) }
    private val startMarker by lazy { BitmapDescriptorFactory.fromResource(R.drawable.ic_ebike_start) }
    private val endMarker by lazy { BitmapDescriptorFactory.fromResource(R.drawable.ic_ebike_end) }
    private val pointList: ArrayList<LatLng> = ArrayList()


    private lateinit var ibtnPlay: ImageButton
    private lateinit var ibtnReplay: ImageButton
    private lateinit var ibtnFast: ImageButton

    private val carTrackManager by lazy { CarTrackManager.newMoveOnlineInstance(this,baiduMap, carMarker,startMarker,endMarker).apply {
        this.setListener(object :MoveOnlineTrackManager.CarListener{
            override fun onStart() {
                showToast("开始")
                ibtnPlay.background = UIUtils.getDrawable(R.drawable.ic_map_pause)
            }

            override fun onFinish() {
                showToast("结束")
                ibtnPlay.background = UIUtils.getDrawable(R.drawable.ic_map_play)
            }
        })
    } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        LogUtils.logGGQ("------HomeActivity------>>>>")
        mapView = findViewById(R.id.bmap_view)
        ibtnPlay = findViewById(R.id.ibtn_play)
        ibtnReplay = findViewById(R.id.ibtn_replay)
        ibtnFast = findViewById(R.id.ibtn_fast)
        mapView.let {
            baiduMap = it.map
            baiduMap.uiSettings?.isRotateGesturesEnabled = true
            // 开启定位图层
            baiduMap.isMyLocationEnabled = true
        }


        ibtnPlay.setOnClickListener {
            if(carTrackManager.isCarPlay()){
                ibtnPlay.background = UIUtils.getDrawable(R.drawable.ic_map_play)
                carTrackManager.onPause()
            }else{
                ibtnPlay.background = UIUtils.getDrawable(R.drawable.ic_map_pause)
                carTrackManager.onResume()
            }
        }


        ibtnReplay.setOnClickListener {
            carTrackManager.onReplay()
        }

        ibtnFast.setOnClickListener {
            stepMode++
            if(stepMode >= stepList.size){
                stepMode = 0
            }
            carTrackManager.onFastBySleepTime(stepList.get(stepMode))
        }




//        val carJson = ResourceUtils.readAssets2String("car.json")
//        val carJson = ResourceUtils.readAssets2String("car_2021-02-02-000000-2021-02-05-235959.json")
        val carJson = ResourceUtils.readAssets2String("car_2021-02-05-000000-2021-02-07-235959.json")
        val carEntity = GsonUtils.fromJson<CarEntity>(carJson, CarEntity::class.java)


        carEntity?.let {
            it.data?.trajectoryList?.let { list ->
                list.forEachWithIndex constituting@{ index, item ->
                    if (index > 0 && index + 1 < list.size) {
                        val nextItem = list.get(index + 1)
                        if (item.lat == nextItem.lat && item.lng == nextItem.lng && item.direction == nextItem.direction) {
                            return@constituting
                        }
                    }
                    pointList.add(LatLng(item.lat, item.lng))
                }

                LogUtils.logGGQ("---size-->${pointList.size}")
                carTrackManager.setTrackLatLngList(pointList,pointList.size)
                carTrackManager.onLine()
            }
        }
    }


    private fun showToast(s:String){
        ToastUtils.showShort(s)
    }

    private val stepList:List<Long> = arrayListOf(20L,12L,6L)
    private var stepMode:Int = 0

}