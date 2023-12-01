package com.echo.netdemo.support

/**
 * 网络请求接口回调
 */
interface EchoHttpCallback {
    /**
     * 成功回调
     * [data] 回调数据
     */
    fun onSuccess(data: Any)

    /**
     *
     *  失败回调
     *  [error] 失败信息
     */
    fun onFailure(error: Any)

}