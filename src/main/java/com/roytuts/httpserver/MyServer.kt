package com.roytuts.httpserver

import com.roytuts.httpserver.constant.ServerConstant
import com.roytuts.httpserver.handler.ServerResourceHandler
import com.sun.net.httpserver.HttpServer
import java.lang.Exception
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import kotlin.concurrent.withLock
import kotlin.system.exitProcess

class MyServer : Runnable {
    private var httpServer: HttpServer? = null
    private var executor: ExecutorService? = null


    override fun run() {
        try {
            executor = Executors.newFixedThreadPool(10)
            httpServer = HttpServer.create(InetSocketAddress(ServerConstant.DEFAULT_HOST, port), 0)
            httpServer?.createContext(
                ServerConstant.FORWARD_SINGLE_SLASH, ServerResourceHandler(
                    serverHome + ServerConstant.FORWARD_SINGLE_SLASH + ServerConstant.WEBAPP_DIR,
                    gzippable = true,
                    cacheable = false
                )
            )
            httpServer?.executor = executor
            LOGGER.info("Starting server...")
            httpServer?.start()
            LOGGER.info("Server started => " + ServerConstant.DEFAULT_HOST + ":" + port)

            // Wait here until shutdown is notified
//            synchronized(this) {
//                try {
//                    this.wait()
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
            lock.withLock {           // like synchronized(lock)
                condition.await()     // like wait()
            }

        } catch (e: Exception) {
            LOGGER.severe("Error occurred during server starting...$e")
        }
    }

    companion object {
        private val LOGGER = Logger.getLogger(MyServer::class.java.name)
        private var server: MyServer? = null
        private var serverHome: String? = null
        private var port = 0

        private val lock = ReentrantLock()
        private val condition = lock.newCondition()

        //@JvmStatic
        fun main(args: Array<String>) {
            if (args.isEmpty()) {
                println("Usage: java -jar <jar name> <server home> <port>")
                exitProcess(0)
            }
            serverHome = args[0]
            port = if (args.size == 1) ServerConstant.DEFAULT_PORT else args[1].toInt()

            // port = 8000;
            // serverHome = "C:\\jee_workspace\\httpserver";
            server = MyServer()
            val thread = Thread(server)
            thread.start()
            Runtime.getRuntime().addShutdownHook(ShutDown())
            try {
                thread.join()
            } catch (e: Exception) {
            }
        }

        fun shutDown() {
            try {
                LOGGER.info("Shutting down server...")
                server!!.httpServer!!.stop(0)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            lock.withLock { condition.signalAll() } //synchronized(server!!) { server.notifyAll() }
        }
    }
}