package com.roytuts.httpserver


class ShutDown : Thread() {
    override fun run() {
        MyServer.shutDown()
    }
}