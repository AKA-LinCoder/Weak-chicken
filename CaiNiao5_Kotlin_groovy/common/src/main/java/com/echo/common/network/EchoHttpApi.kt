package com.echo.netdemo

import androidx.collection.SimpleArrayMap
import com.google.gson.Gson
import com.echo.netdemo.config.HeaderInterceptor
import com.echo.netdemo.config.KtHttpLogInterceptor
import com.echo.netdemo.config.LocalCookieJar
import com.echo.netdemo.config.RetryInterceptor
import com.echo.netdemo.support.EchoHttpCallback
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

//
//private constructor() 私有构造函数
//写在后面只是把构造函数私有化，类并没有私有化
//
 class EchoHttpApi private constructor(): HttpApi {

private val baseUrl = "http://api.qingyunke.com"

     //最大重连次数
     private var maxRetry: Int = 0

     //存储请求，用于取消
     private val callMap = SimpleArrayMap<Any, Call>()

     //okHttpClient
     private val defaultClient = OkHttpClient.Builder()
         .callTimeout(10, TimeUnit.SECONDS)//完整请求超时时长，从发起到接收返回的数据，默认值为0，不限定
         .connectTimeout(10, TimeUnit.SECONDS)//与服务器建立连接的时长，默认10s
         .readTimeout(10, TimeUnit.SECONDS)//读取服务器返回数据的时长
         .writeTimeout(10, TimeUnit.SECONDS)//向服务器写入数据的时长，默认10s
         .retryOnConnectionFailure(true)//重连
         .followRedirects(false)//重定向
         ///指定缓存数据保存地址
         .cache(Cache(File("sdcard/cache", "okhttp"), 1024))
         .cookieJar(LocalCookieJar())
         .addNetworkInterceptor(HeaderInterceptor())//公共的header 拦截器
         .addNetworkInterceptor(KtHttpLogInterceptor {
             logLevel(KtHttpLogInterceptor.LogLevel.BODY)
         })//添加网络拦截器，可以对okHttp的网络请求做拦截处理，不同于应用拦截器，这里能感知所有网络状态，比如重定向
         .addNetworkInterceptor(RetryInterceptor(maxRetry))
         .build()

     private var mClient = defaultClient

     fun getClient() = mClient

     /**
      * 配置自定义的client
      */
     fun initConfig(client: OkHttpClient) {
         this.mClient = client
     }

     companion object {
         @Volatile
         private var api: EchoHttpApi? = null

         @Synchronized
         fun getInstance(): EchoHttpApi {
             return api ?: EchoHttpApi().also { api = it }
         }
     }

     /**
      * 异步的get请求
      */
     override fun get(params: Map<String, Any>, path: String, callback: EchoHttpCallback) {
//        val url = "$baseUrl$path"
         val urlBuilder = path.toHttpUrl().newBuilder()
         for (param in params) {
             urlBuilder.addQueryParameter(param.key, param.value.toString())
         }
         val request = Request.Builder()
             .get()
             .tag(params)
             .url(urlBuilder.build())
             .cacheControl(CacheControl.FORCE_NETWORK)
             .build()
         val newCall = mClient.newCall(request)
         callMap.put(request.tag(), newCall)
         newCall.enqueue(object : Callback {
             override fun onFailure(call: Call, e: IOException) {
                 e.message?.let { callback.onFailure(it) }
             }

             override fun onResponse(call: Call, response: Response) {
                 callback.onSuccess(response)
             }
         })
     }

     /**
      * 同步的get请求
      */
     override fun getSync(params: Map<String, Any>, path: String): Any? {
         TODO("Not yet implemented")
     }

     /**
      * 异步的post请求
      */
     override fun post(body: Any, path: String, callback: EchoHttpCallback) {
         val url = "$baseUrl$path"
         val request = Request.Builder()
             .post(Gson().toJson(body).toRequestBody())
             .url(path)
             .tag(body)
             .build()
         val newCall = mClient.newCall(request)
         callMap.put(request.tag(), newCall)
         newCall.enqueue(object : Callback {
             override fun onFailure(call: Call, e: IOException) {
                 e.message?.let { callback.onFailure(it) }
             }

             override fun onResponse(call: Call, response: Response) {
                 callback.onSuccess(response)
             }
         })

     }

     /**
      * 同步的post请求
      */
     override fun postSync(body: Any, path: String): Any? {
         TODO("Not yet implemented")
     }

     /**
      * 取消单个请求
      */
     override fun cancelRequest(tag: Any) {
         callMap.get(tag)?.cancel()
     }

     /**
      * 取消所有请求
      */
     override fun cancelAllRequest() {
         for (i in 0 until callMap.size()) {
             callMap.get(callMap.keyAt(i))?.cancel()
         }
     }

     /**
      * 使用协程形式的get请求，使用runblocking，也可以使用suspend修饰
      */
     fun get(params: Map<String, Any>, urlStr: String) = runBlocking {
         val urlBuilder = urlStr.toHttpUrl().newBuilder()
         params.forEach { entry ->
             urlBuilder.addEncodedQueryParameter(entry.key, entry.value.toString())
         }

         val request = Request.Builder()
             .get()
             .tag(params)
             .url(urlBuilder.build())
             .cacheControl(CacheControl.FORCE_NETWORK)
             .build()
         val newCall = mClient.newCall(request)

         //存储请求，用户取消
         callMap.put(request.tag(), newCall)
         newCall.call()
     }

     /**
      * 自定义扩展函数，扩展okhttp的call的异步执行方式，结合协程，返回DataResult的数据响应
      */
     private suspend fun Call.call(async: Boolean = true): Response {
         return suspendCancellableCoroutine { continuation ->
             if (async) {
                 enqueue(object : Callback {
                     override fun onFailure(call: Call, e: IOException) {
                         //避免不必要的冗余调用
                         if (continuation.isCancelled) return
                         continuation.resumeWithException(e)
                     }

                     override fun onResponse(call: Call, response: Response) {
                         continuation.resume(response)
                     }
                 })
             } else {
                 continuation.resume(execute())
             }
             ///协程取消
             continuation.invokeOnCancellation {
                 try {
                     cancel()
                 } catch (ex: Exception) {
                     ex.printStackTrace()
                 }
             }
         }
     }


 }


