package com.echo.netdemo.support

import com.blankj.utilcode.util.EncryptUtils
import com.echo.netdemo.config.ENT_CONFIG_APPKEY


/**
 * author: huangyaomian
 * created on: 2021/6/12 10:17 上午
 * description:解码和转码工具类
 */
object HymUtils {
    /**
     * 中文转unicode
     */
    fun unicodeEncode(str: String): String {
        var result = ""
        for (i in 0 until str.length) {
            val chr1 = str[i].code
            if (chr1 >= 19968 && chr1 <= 171941) { // 汉字范围 \u4e00-\u9fa5 (中文)
                result += "\\u" + Integer.toHexString(chr1)
            } else {
                result += str[i]
            }
        }
        return result
    }

    /**
     * 判断是否为中文字符
     *
     * @param c 字符
     * @return 是否为中文字符
     */
    fun isChinese(c: Char): Boolean {
        val ub = Character.UnicodeBlock.of(c)
        return ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub === Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub === Character.UnicodeBlock.GENERAL_PUNCTUATION || ub === Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub === Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
    }

    /**
     * unicode 转中文
     */
    fun unicodeDecode(str: String): String {
        val string = StringBuilder()
        val hex = str.split("\\\\u".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        for (s in hex) {
            try {
                // 汉字范围 \u4e00-\u9fa5 (中文)
                if (s.length >= 4) { //取前四个，判断是否是汉字
                    val chinese = s.substring(0, 4)
                    try {
                        val chr = chinese.toInt(16)
                        val isChinese = isChinese(chr.toChar())
                        //转化成功，判断是否在  汉字范围内
                        if (isChinese) { //在汉字范围内
                            // 追加成string
                            string.append(chr.toChar())
                            //并且追加  后面的字符
                            val behindString = s.substring(4)
                            string.append(behindString)
                        } else {
                            string.append(s)
                        }
                    } catch (e1: NumberFormatException) {
                        string.append(s)
                    }
                } else {
                    string.append(s)
                }
            } catch (e: NumberFormatException) {
                string.append(s)
            }
        }
        return string.toString()
    }

    /**
     * 解析返回的data数据
     *
     * @param dataStr 需要解析的数据
     * @return 返回解析完的数据
     */
    fun decodeData(dataStr: String?): String? {
        //java 代码，无自动null判断，需要自行处理
        return if (dataStr != null) {
            kotlin.String(
                EncryptUtils.decryptBase64AES(
                    dataStr.toByteArray(),
                    ENT_CONFIG_APPKEY.getBytes(),
                    "AES/CBC/PKCS7Padding",  //解密的参数
                    "J#y9sJesv*5HmqLq".toByteArray() //解密
                )
            )
        } else {
            null
        }
    }
}