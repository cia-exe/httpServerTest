import com.roytuts.httpserver.MyServer

fun main(args: Array<String>) {
    val path = System.getProperty("user.dir")
    println("Hello World! $path : ${args.joinToString()}")
    MyServer.main(arrayOf(path,"9900"))
    //MyServer.main(args)
}