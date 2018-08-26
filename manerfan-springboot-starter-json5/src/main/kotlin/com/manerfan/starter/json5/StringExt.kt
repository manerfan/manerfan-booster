@file:JvmName("StringExt")

package com.manerfan.starter.json5

import com.alibaba.fastjson.JSON

fun String.parseMap() = JsonParser(this).toMap()
fun <T> String.parseObject(clazz: Class<T>): T = JSON.parseObject(JSON.toJSONString(this.parseMap()), clazz)
fun <T> String.parseArray(clazz: Class<T>): List<T> = JSON.parseArray(JSON.toJSONString(this.parseMap()[JsonParser.DEFAULT_LIST_KEY]), clazz)
