package com.mishaki.cartracktest.manager.ebike

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.mishaki.cartracktest.R
import com.mishaki.cartracktest.manager.car.splitPoint4MoveDistance
import com.mishaki.cartracktest.utils.LogUtils
import org.jetbrains.anko.async
import org.jetbrains.anko.uiThread

class EbikeOnlineManager(context: Context,baiduMap: BaiduMap,carIcon: BitmapDescriptor,startIcon:BitmapDescriptor,endIcon:BitmapDescriptor) : AbsEbikeTrackManager(context,baiduMap,carIcon, startIcon, endIcon),BaiduMap.OnMarkerClickListener {
    protected var firstIndex = 0
    protected var secondIndex = 0
    protected var lastMarker: Marker? = null
    protected var moveLatLngList = ArrayList<ArrayList<LatLng>>()

    private val overLayList:MutableList<Overlay> = mutableListOf()

    private val gcoding by lazy { BitmapDescriptorFactory.fromResource(R.drawable.ic_gcoding) }

    private var trackLine: Polyline? = null

    init {
        removeUiSetting()
        moveDistance = 15
        sleepTime = 20L
        // marker 点击监听
        baiduMap.setOnMarkerClickListener(this)
    }

    override fun onSetTrackLatLngListFinish() {
        moveLatLngList = splitPoint4MoveDistance(pointList, moveDistance)
    }

    override fun start() {
        if (isRunning) {
            return
        }
        isRunning = true
        isPlay = true
        if(overLayList.isNotEmpty()){
            this.baiduMap.removeOverLays(overLayList)
        }
        this.listener?.onStart()
        asyncTask = threadController.async {
            isStop = false
            for (i in firstIndex + 1 until pointList.size - 1) {
                try {
                    if (isStop) {
                        return@async
                    }
                    if (isPause) {
                        isPause = false
                    } else {
                        Thread.sleep(sleepTime)
                    }
                    moveCar(moveLatLngList[i])
                    //每循环一小段,添加一个marker

                    val overlay = baiduMap.addOverlay(MarkerOptions().position(pointList[i].latLng).apply {
                        //添加 点击marker
                        val bundle: Bundle = Bundle()
                        bundle.putString("addr",pointList[i].addr)
                        bundle.putString("time",pointList[i].time)
                        bundle.putString("direction",pointList[i].direction)
                        bundle.putInt("flag",pointList[i].flag)
                        this.extraInfo(bundle)
                    }.icon(gcoding))
                    overLayList.add(overlay)
                    onMoveUpScreen(pointList[i + 1].latLng)
                    firstIndex = i
                    secondIndex = 0
                } catch (e: MoveCarStopException) {
                    return@async
                }
            }
            try {
                moveCar(moveLatLngList[pointList.lastIndex])
                moveCarFinish()
            } catch (e: MoveCarStopException) {
                return@async
            }
        }
    }

    @Throws(MoveCarStopException::class)
    protected fun moveCar(list: ArrayList<LatLng>) {
        LogUtils.logGGQ("移动段->>${list.size}")
        lastMarker?.position = list[0]
        for (i in secondIndex + 1 until list.size) {
            if (isStop) {
                LogUtils.logGGQ("stop-->>>${isStop}")
                throw MoveCarStopException()
            }
            Thread.sleep(sleepTime)
            lastMarker!!.position = list[i]
            secondIndex = i
        }
    }

    fun moveCarFinish() {
        this.firstIndex = 0
         this.secondIndex = 0
         this.isRunning = false
         this.isPlay = false
         this.listener?.onFinish()
        threadController.uiThread {
            listener?.onFinish()
        }
    }

    override fun resetIndex() {
        firstIndex = 0
        secondIndex = 0
    }


    fun onLine(){
        if (isRunning) {
            return
        }
        if (firstIndex == 0 && secondIndex == 0) {
            threadController.async {
                lastMarker?.remove()
                val ood = generateCarMarker(pointList[0].latLng)
                lastMarker = baiduMap.addOverlay(ood) as Marker
                val u = MapStatusUpdateFactory.newLatLngZoom(pointList[0].latLng, animateZoom)
                baiduMap.animateMapStatus(u)
                baiduMap.addOverlays(generateStartEndMarker(pointList.first(),pointList.last(),startIcon,endIcon))

                trackLine?.remove()
                val line = generatePolylineOptions(pointList.map { it.latLng })
                trackLine = baiduMap.addOverlay(line) as Polyline
            }
        }
    }



    protected var isPlay:Boolean = false

    fun isCarPlay():Boolean{
        return isPlay
    }


    fun onPause(){
        isStop = true
        isPause = true
        isRunning = false
        isPlay = false
    }

    fun onResume(){
        this.start()
        if(overLayList.isNotEmpty()){
            overLayList.clear()
        }
    }


    fun onFastBySleepTime(time:Long){
        this.sleepTime = time
    }



    //重新播放
    fun onReplay(){
        this.resetIndex()
        this.isStop = false
        this.isPause = false
        this.isRunning = false
        this.isPlay = false
        this.threadController.weakRef.clear()
        this.asyncTask?.cancel(true)
        this.start()
    }


    // 点击marker 回调
    override fun onMarkerClick(marker: Marker?): Boolean {
        marker?.let {
            val bundle: Bundle = it.extraInfo
            val addr:String = bundle.getString("addr","")
            val direction:String = bundle.getString("direction","")
            val time:String = bundle.getString("time","")
            val view = View.inflate(context,R.layout.window_marker_info,null)
            view?.apply {
                findViewById<TextView>(R.id.tv_addr)?.text = "${addr}"
                findViewById<TextView>(R.id.tv_direction)?.text = "方向：${direction}度"
                findViewById<TextView>(R.id.tv_time)?.text = "最后出现时间：${time}"
            }

            // 点击关闭所有
            view?.findViewById<LinearLayout>(R.id.info_window)?.setOnClickListener {
                baiduMap.hideInfoWindow()
            }
            val infoWindow:InfoWindow = InfoWindow(view,marker.position,-56)

            //false 点击另一个不隐藏
            //baiduMap.showInfoWindow(infoWindow,false)

            baiduMap.showInfoWindow(infoWindow)
        }
        return false
    }

}