package com.mishaki.cartracktest.entity

import com.baidu.mapapi.model.LatLng

data class EbikePoint(val latLng: LatLng,val addr:String,val direction:String,val time:String,val flag:Int)