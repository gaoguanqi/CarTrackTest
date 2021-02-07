package com.mishaki.cartracktest.manager.car

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.baidu.mapapi.map.*
import com.mishaki.cartracktest.R
import org.jetbrains.anko.async

class MoveOnlineTrackManager(val context: Context,baiduMap: BaiduMap, carIcon: BitmapDescriptor,val startIcon:BitmapDescriptor,val endIcon:BitmapDescriptor):MoveCarTrackManager(baiduMap,carIcon), BaiduMap.OnMarkerClickListener {

    private var listener:CarListener? = null
    private val overLayList:MutableList<Overlay> = mutableListOf()

    private val gcoding by lazy { BitmapDescriptorFactory.fromResource(R.drawable.ic_gcoding) }

    fun setListener(l:CarListener){
        this.listener = l
    }

    private var trackLine: Polyline? = null

    init {
        moveDistance = 15
        sleepTime = 20L
        // marker 点击监听
        baiduMap.setOnMarkerClickListener(this)
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
            for (i in firstIndex + 1 until actualLatLngList.size - 1) {
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
                    val overlay = baiduMap.addOverlay(MarkerOptions().position(actualLatLngList[i]).apply {
                        //添加 点击marker
                        val bundle:Bundle = Bundle()
                        bundle.putString("id",actualLatLngList[i].toString())
                        this.extraInfo(bundle)
                    }.icon(gcoding))
                    overLayList.add(overlay)
                    onMoveUpScreen(actualLatLngList[i + 1])
                    firstIndex = i
                    secondIndex = 0
                } catch (e: MoveCarStopException) {
                    return@async
                }
            }
            try {
                moveCar(moveLatLngList[actualLatLngList.lastIndex])
                moveCarFinish()
            } catch (e: MoveCarStopException) {
                return@async
            }
        }
    }

    fun onLine(){
        if (isRunning) {
            return
        }
        if (firstIndex == 0 && secondIndex == 0) {
            threadController.async {
                lastMarker?.remove()
                val ood = generateCarMarker(actualLatLngList[0], actualLatLngList[1])
                lastMarker = baiduMap.addOverlay(ood) as Marker
                val u = MapStatusUpdateFactory.newLatLngZoom(actualLatLngList[0], animateZoom)
                baiduMap.animateMapStatus(u)
                baiduMap.addOverlays(generateStartEndMarker(actualLatLngList.first(),actualLatLngList.last(),startIcon,endIcon))

                trackLine?.remove()
                val line = generatePolylineOptions(actualLatLngList)
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

    override fun moveCarFinish() {
        this.isPlay = false
        super.moveCarFinish()
        this.listener?.onFinish()
    }


    interface CarListener{
        fun onStart()
        fun onFinish()
    }

    // 点击marker 回调
    override fun onMarkerClick(marker: Marker?): Boolean {
        marker?.let {
            val bundle: Bundle = it.extraInfo
            val id:String = bundle.getString("id","")
            val view = View.inflate(context,R.layout.window_marker_info,null)
            view?.findViewById<TextView>(R.id.tv_addr)?.text = id

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