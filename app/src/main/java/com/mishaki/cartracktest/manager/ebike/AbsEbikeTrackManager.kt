package com.mishaki.cartracktest.manager.ebike

import android.content.Context
import android.os.Bundle
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.utils.DistanceUtil
import com.mishaki.cartracktest.entity.EbikePoint
import com.mishaki.cartracktest.manager.car.*
import com.mishaki.cartracktest.utils.LogUtils
import org.jetbrains.anko.AnkoAsyncContext
import org.jetbrains.anko.async
import java.lang.ref.WeakReference
import java.util.concurrent.Future

abstract class AbsEbikeTrackManager(val context: Context,protected val baiduMap: BaiduMap, private val carIcon: BitmapDescriptor, val startIcon:BitmapDescriptor, val endIcon:BitmapDescriptor) {

    //随便写的,但Marker的ZIndex必须比Polyline的ZIndex大
    protected var carMarkerZIndex = -50
    protected var polylineLineZIndex = -100
    var polylineWidth = 10
    var moveDistance = 5
    var sleepTime = 20L

    var animateZoom = 14f

    protected val threadController = AnkoAsyncContext(WeakReference(this))
    protected var asyncTask: Future<Unit>? = null
    protected var isStop = false
    protected var isPause = false
    protected var isRunning = false

    protected var pointList = ArrayList<EbikePoint>()

    protected var listener:OnMoveListener? = null
    fun setOnMoveListener(l:OnMoveListener){
        this.listener = l
    }

    /**
     * 建议最后控制一下数据量,过大的话就导致计算不过来
     */
    fun setPointList(list: ArrayList<EbikePoint>, distance: Int = 0) {
        if (list.size <= 1) {
            pointList = list
            onSetTrackLatLngListFinish()
            return
        }
        if (distance <= 0) {
            pointList = list
            onSetTrackLatLngListFinish()
            return
        }
        pointList.clear()
        pointList.add(list[0])
        (1 until list.size).filter { DistanceUtil.getDistance(list[it - 1].latLng, list[it].latLng) > distance }.mapTo(pointList) { list[it] }
        onSetTrackLatLngListFinish()
    }

    protected open fun onSetTrackLatLngListFinish() {}

    var maxLevel = 19F
        set(maxLevel) {
            field = maxLevel
            baiduMap.setMaxAndMinZoomLevel(maxLevel, minLevel)
        }
    var minLevel = 4F
        set(minLevel) {
            field = minLevel
            baiduMap.setMaxAndMinZoomLevel(maxLevel, minLevel)
        }

    init {
        baiduMap.setMaxAndMinZoomLevel(maxLevel, minLevel)
    }

    companion object {
        init {
            LogUtils.logGGQ("--CarTrackManager---")
        }

        @JvmStatic
        fun newEbikeOnlineInstance(context: Context, baiduMap: BaiduMap, carIcon: BitmapDescriptor, startIcon:BitmapDescriptor, endIcon:BitmapDescriptor): EbikeOnlineManager {
            return EbikeOnlineManager(context,baiduMap, carIcon,startIcon,endIcon)
        }
    }

    /**
     * 可选，建议在构造器调用。移除会影响最终展示效果的UI设置，否则还要为这些设置写相应的代码去适配。
     */
    protected fun removeUiSetting() {
        baiduMap.uiSettings.isOverlookingGesturesEnabled = false
        baiduMap.uiSettings.isRotateGesturesEnabled = false
        baiduMap.uiSettings.isCompassEnabled = false
    }

    protected fun generateCarMarker(firstLatLng: LatLng): MarkerOptions {
        return MarkerOptions().anchor(0.5f, 0.5f).icon(carIcon).position(firstLatLng).zIndex(carMarkerZIndex)
    }


    //起点 和 终点 marker
    // 添加infoWindow
    protected fun generateStartEndMarker(start: EbikePoint,end:EbikePoint,startIcon:BitmapDescriptor,endIcon: BitmapDescriptor): List<OverlayOptions> {
        return listOf(MarkerOptions().position(start.latLng).icon(startIcon).apply {
            val bundle: Bundle = Bundle()
            bundle.putString("addr",start.addr)
            bundle.putString("time",start.time)
            bundle.putString("direction",start.direction)
            bundle.putInt("flag",start.flag)
            this.extraInfo(bundle)
        }, MarkerOptions().position(end.latLng).icon(endIcon).apply {
            val bundle: Bundle = Bundle()
            bundle.putString("addr",start.addr)
            bundle.putString("time",start.time)
            bundle.putString("direction",start.direction)
            bundle.putInt("flag",start.flag)
            this.extraInfo(bundle)
        })
    }

    protected fun generatePolylineOptions(pointList: List<LatLng>): PolylineOptions {
        return PolylineOptions().width(polylineWidth).points(pointList).zIndex(polylineLineZIndex).customTexture(
            BitmapDescriptorFactory.fromAsset("line_road.png"))
    }

    abstract fun start()

    open fun pause() {
        isStop = true
        isPause = true
        isRunning = false
    }

    open fun stop() {
        isStop = true
        isRunning = false
        threadController.async {
            Thread.sleep(sleepTime * 2)
            resetIndex()
        }
    }

    protected abstract fun resetIndex()

    fun release() {
        stop()
        asyncTask?.cancel(true)
        carIcon.recycle()
    }

    interface OnMoveListener {
        fun onStart()
        fun onFinish()
    }

    protected class MoveCarStopException : Exception()


    //防碰壁
    fun onMoveUpScreen(p:LatLng){
        try {
            baiduMap.let {
                LogUtils.logGGQ("纠偏-->${p}")
                val pt = it.mapStatus.targetScreen
                val point = it.projection.toScreenLocation(p)
                if(point.x < 0 || point.x > pt.x * 2 || point.y < 0 || point.y > pt.y * 2){
                    val mapStatus = MapStatus.Builder().target(p).zoom(15f).build()
                    it.animateMapStatus(MapStatusUpdateFactory.newMapStatus(mapStatus))
                }
            }
        }catch (e:java.lang.Exception){
            LogUtils.logGGQ("--onMoveCorrect--error->>${e.fillInStackTrace()}")
        }
    }


}