package com.mishaki.cartracktest.ui

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.TextureMapView
import com.baidu.mapapi.model.LatLng
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ResourceUtils
import com.blankj.utilcode.util.ToastUtils
import com.daimajia.numberprogressbar.NumberProgressBar
import com.mishaki.cartracktest.R
import com.mishaki.cartracktest.entity.CarEntity
import com.mishaki.cartracktest.entity.EbikePoint
import com.mishaki.cartracktest.manager.car.CarTrackManager
import com.mishaki.cartracktest.manager.car.MoveOnlineTrackManager
import com.mishaki.cartracktest.manager.ebike.AbsEbikeTrackManager
import com.mishaki.cartracktest.utils.LogUtils
import com.mishaki.cartracktest.utils.UIUtils
import org.jetbrains.anko.collections.forEachWithIndex

class MyEbikeActivity:AppCompatActivity() {
    private lateinit var mapView: TextureMapView
    private lateinit var baiduMap: BaiduMap
    private val carMarker by lazy { BitmapDescriptorFactory.fromResource(R.drawable.marker_car) }
    private val startMarker by lazy { BitmapDescriptorFactory.fromResource(R.drawable.ic_ebike_start) }
    private val endMarker by lazy { BitmapDescriptorFactory.fromResource(R.drawable.ic_ebike_end) }
    private val pointList: ArrayList<EbikePoint> = ArrayList()


    private lateinit var ibtnPlay: ImageButton
    private lateinit var ibtnReplay: ImageButton
    private lateinit var ibtnFast: ImageButton

    private lateinit var npBar:NumberProgressBar
    private lateinit var tvNum:TextView

    private val carTrackManager by lazy { AbsEbikeTrackManager.newEbikeOnlineInstance(this,baiduMap, carMarker,startMarker,endMarker).apply {
        this.setOnMoveListener(object : AbsEbikeTrackManager.OnMoveListener{
            override fun onStart() {
                showToast("开始")
                ibtnPlay.background = UIUtils.getDrawable(R.drawable.ic_map_pause)
            }

            override fun onFinish() {
                showToast("结束")
                ibtnPlay.background = UIUtils.getDrawable(R.drawable.ic_map_play)
            }

            override fun onProgress(currentSize: Int, totalSize: Int) {
                runOnUiThread {
                    LogUtils.logGGQ("当前进度->${currentSize}--总进度->${totalSize}")
                    npBar.progress = currentSize
                    tvNum.text = "${currentSize}/${totalSize}"
                }
            }
        })
    } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_ebike)
        LogUtils.logGGQ("------MyEbikeActivity------>>>>")
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

        npBar = findViewById(R.id.np_bar)
        tvNum = findViewById(R.id.tv_num)

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
//        val carJson = ResourceUtils.readAssets2String("car_2021-02-05-000000-2021-02-06-235959.json")
        val carEntity = GsonUtils.fromJson<CarEntity>(carJson, CarEntity::class.java)


        carEntity?.let {
            it.data?.trajectoryList?.let { list ->
                list.forEachWithIndex constituting@{ index, item ->
                    //过滤重复点 当经度 纬度 和 角度相同 时
                    if (index > 0 && index + 1 < list.size) {
                        val nextItem = list.get(index + 1)
                        if (item.lat == nextItem.lat && item.lng == nextItem.lng && item.direction == nextItem.direction) {
                            return@constituting
                        }
                    }

                    pointList.add(EbikePoint(LatLng(item.lat, item.lng),item.addr,item.direction,item.updateTime,item.stopFlag))
                }

                npBar.max = pointList.size
                npBar.progress = 0
                tvNum.text = "${0}/${pointList.size}"
                LogUtils.logGGQ("---size-->${pointList.size}")
                // 如果 distance >= 1 方法内部自动过滤 重复点
                carTrackManager.setPointList(pointList,0)
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