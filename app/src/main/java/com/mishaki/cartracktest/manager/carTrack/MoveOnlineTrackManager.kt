package com.mishaki.cartracktest.manager.carTrack

import com.baidu.mapapi.map.*
import com.mishaki.cartracktest.R
import org.jetbrains.anko.async

class MoveOnlineTrackManager(baiduMap: BaiduMap, carIcon: BitmapDescriptor,val startIcon:BitmapDescriptor,val endIcon:BitmapDescriptor):MoveCarTrackManager(baiduMap,carIcon) {

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
                    // MarkerOptions options = new MarkerOptions().position(getLatLng(runLocal.get(index))).icon(BitmapDescriptorFactory.fromResource(resource));
                    val overlay = baiduMap.addOverlay(MarkerOptions().position(actualLatLngList[i]).icon(gcoding))
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

}