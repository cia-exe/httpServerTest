package com.roytuts.httpserver.handler

import com.roytuts.httpserver.constant.ServerConstant
import com.roytuts.httpserver.utils.ServerUtil
import kotlin.Throws
import com.roytuts.httpserver.enums.HttpMethod
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import java.io.*
import java.lang.IllegalStateException
import java.util.HashMap
import java.util.logging.Logger
import java.util.zip.GZIPOutputStream

class ServerResourceHandler(pathToRoot: String, private val gzippable: Boolean, private val cacheable: Boolean) :
    HttpHandler {
    private val pathToRoot: String =
        if (pathToRoot.endsWith(ServerConstant.FORWARD_SINGLE_SLASH)) pathToRoot else pathToRoot + ServerConstant.FORWARD_SINGLE_SLASH
    private val resources: MutableMap<String, Resource?> = HashMap()

    @Throws(IOException::class)
    override fun handle(httpExchange: HttpExchange) {
        val requestPath = httpExchange.requestURI.path
        LOGGER.info("Requested Path: $requestPath")
        serveResource(httpExchange, requestPath)
    }

    private inner class Resource(val content: ByteArray)

    @Throws(IOException::class)
    private fun processFile(path: String, file: File, gzippable: Boolean) {
        if (!file.isDirectory) {
            resources[path + file.name] = Resource(readResource(FileInputStream(file), gzippable))
        }
        if (file.isDirectory) {
            for (sub in file.listFiles()!!) {
                processFile(path + file.name + ServerConstant.FORWARD_SINGLE_SLASH, sub, gzippable)
            }
        }
    }

    @Throws(IOException::class)
    private fun readResource(`in`: InputStream, gzip: Boolean): ByteArray {
        val bout = ByteArrayOutputStream()
        val gout: OutputStream = if (gzip) GZIPOutputStream(bout) else DataOutputStream(bout)
        val bs = ByteArray(4096)
        var r: Int
        while (`in`.read(bs).also { r = it } >= 0) {
            gout.write(bs, 0, r)
        }
        gout.flush()
        gout.close()
        `in`.close()
        return bout.toByteArray()
    }

    @Throws(IOException::class)
    private fun serveResource(httpExchange: HttpExchange, requestPath: String) {
        val reqPath = requestPath.substring(1).run {

            if (isEmpty()) {
                val t = System.currentTimeMillis() / 1000 % 60
                LOGGER.info("t sec: $t")

                if (t >= 59 || t < 5) Thread.sleep(3000)
                if (t in 0..9) "index.html" else "closed.html"
            } else replace(ServerConstant.FORWARD_DOUBLE_SLASH, ServerConstant.FORWARD_SINGLE_SLASH)
        }

        serveFile(httpExchange, pathToRoot + reqPath)
    }

    @Throws(IOException::class)
    private fun serveFile(httpExchange: HttpExchange, resourcePath: String) {
        val file = File(resourcePath)
        if (file.exists()) {
            val `in`: InputStream = FileInputStream(resourcePath)
            val res: Resource? = if (cacheable) {
                if (resources[resourcePath] == null) {
                    Resource(readResource(`in`, gzippable))
                } else {
                    resources[resourcePath]
                }
            } else {
                Resource(readResource(`in`, gzippable))
            }
            if (gzippable) {
                httpExchange.responseHeaders[ServerConstant.CONTENT_ENCODING] = ServerConstant.ENCODING_GZIP
            }
            val mimeType = ServerUtil.getFileMime(resourcePath)
            writeOutput(httpExchange, res!!.content.size, res.content, mimeType)
        } else {
            showError(httpExchange, 404, "The requested resource was not found on server")
        }
    }

    @Throws(IOException::class)
    private fun writeOutput(httpExchange: HttpExchange, contentLength: Int, content: ByteArray, contentType: String?) {
        if (HttpMethod.HEAD.name == httpExchange.requestMethod) {
            val entries: Set<Map.Entry<String, List<String>>> = httpExchange.requestHeaders.entries
            var response = ""
            for (entry in entries) {
                response += """
                    $entry
                    
                    """.trimIndent()
            }
            httpExchange.responseHeaders[ServerConstant.CONTENT_TYPE] = ServerConstant.TEXT_PLAIN
            httpExchange.sendResponseHeaders(200, response.length.toLong())
            httpExchange.responseBody.write(response.toByteArray())
            httpExchange.responseBody.close()
        } else {
            httpExchange.responseHeaders[ServerConstant.CONTENT_TYPE] = contentType
            httpExchange.sendResponseHeaders(200, contentLength.toLong())
            httpExchange.responseBody.write(content)
            httpExchange.responseBody.close()
        }
    }

    @Suppress("SameParameterValue")
    @Throws(IOException::class)
    private fun showError(httpExchange: HttpExchange, respCode: Int, errDesc: String) {
        val message = "HTTP error $respCode: $errDesc"
        val messageBytes = message.toByteArray(charset(ServerConstant.ENCODING_UTF8))
        httpExchange.responseHeaders[ServerConstant.CONTENT_TYPE] = ServerConstant.TEXT_PLAIN
        httpExchange.sendResponseHeaders(respCode, messageBytes.size.toLong())
        val os = httpExchange.responseBody
        os.write(messageBytes)
        os.close()
    }

    companion object {
        private val LOGGER = Logger.getLogger(ServerResourceHandler::class.java.name)
    }

    init {
        val files = File(pathToRoot).listFiles()
            ?: throw IllegalStateException("Couldn't find webroot: $pathToRoot")
        for (f in files) {
            processFile("", f, gzippable)
        }
    }
}