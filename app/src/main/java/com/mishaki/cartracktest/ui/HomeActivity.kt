package com.mishaki.cartracktest.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.TextureMapView
import com.baidu.mapapi.model.LatLng
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ResourceUtils
import com.mishaki.cartracktest.R
import com.mishaki.cartracktest.entity.CarEntity
import com.mishaki.cartracktest.manager.carTrack.*
import com.mishaki.cartracktest.manager.carTrack.CarTrackManager.Companion.newMoveHasLineInstance
import com.mishaki.cartracktest.utils.LogUtils
import org.jetbrains.anko.collections.forEachWithIndex

class HomeActivity : AppCompatActivity() {
    private lateinit var mapView: TextureMapView
    private lateinit var baiduMap: BaiduMap
    private val carMarker by lazy { BitmapDescriptorFactory.fromResource(R.drawable.marker_car) }
    private val pointList: ArrayList<LatLng> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        LogUtils.logGGQ("------HomeActivity------>>>>")
        mapView = findViewById(R.id.bmap_view)
        mapView.let {
            baiduMap = it.map
            baiduMap.uiSettings?.isRotateGesturesEnabled = true
            // 开启定位图层
            baiduMap.isMyLocationEnabled = true
        }

        val carJson = ResourceUtils.readAssets2String("car.json")
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
            }
        }

//        CarTrackManager.newMoveHasLineInstance(baiduMap, carMarker).setTrackLatLngList(pointList, 0)
        LogUtils.logGGQ("---size-->${pointList.size}")
//        CarTrackManager.newMoveHasLineInstance(baiduMap,carMarker).setTrackLatLngList(pointList,pointList.size)


//        CarTrackManager.newMoveHasLineInstance(baiduMap, carMarker).start()

        val man = newMoveHasLineInstance(baiduMap, carMarker)
        man.setTrackLatLngList(pointList,pointList.size)
        man.start()
    }
}