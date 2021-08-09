package com.roytuts.httpserver.utils

import com.roytuts.httpserver.constant.ServerConstant
import java.util.*

object ServerUtil {
    private fun getFileExt(path: String): String {
        val slashIndex = path.lastIndexOf(ServerConstant.FORWARD_SINGLE_SLASH)
        val basename = if (slashIndex < 0) path else path.substring(slashIndex + 1)
        val dotIndex = basename.lastIndexOf('.')
        return if (dotIndex >= 0) {
            basename.substring(dotIndex + 1)
        } else {
            ""
        }
    }

    fun getFileMime(path: String): String? {
        val ext = getFileExt(path).lowercase(Locale.getDefault())
        return ServerConstant.MIME_MAP.getOrDefault(ext, ServerConstant.APPLICATION_OCTET_STREAM)
    }
}