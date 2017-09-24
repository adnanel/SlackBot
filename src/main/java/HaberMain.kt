
import org.json.JSONArray
import org.json.JSONObject
import org.omg.CORBA.Environment
import slackapi.SlackConfig
import slackapi.SlackContext
import utility.log
import java.io.File
import java.io.FileReader
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList


fun loadPlugins(appConfig : String) : List<HaberPlugin> {
    val res = ArrayList<HaberPlugin>()

    val plugins = JSONArray(FileReader(File("plugins.json")).readText())
    for ( it in plugins ) {
        val obj = it as JSONObject

        val path : String = System.getProperty("user.dir") + obj.getString("path")
        val f = File(path)
        if ( !f.exists() ) {
            log("Plugin " + obj.getString("name") + " doesn't exist at path $path")
            continue
        }

        log("Loading plugin " + obj.getString("name"))

        val urlCl = URLClassLoader(arrayOf(f.toURL()), System::class.java.classLoader)
        val pluginClass = urlCl.loadClass(obj.getString("class"))

        res += pluginClass.constructors[0].newInstance(appConfig) as HaberPlugin
    }

    return res
}


fun main(args : Array<String>) {
    val json = JSONObject(FileReader(File("config.json")).readText())

    // Init slack
    val sjson = json.getJSONObject("slack")
    val scfg = SlackConfig(sjson.getString("token"), sjson.getString("username"))
    //val slackContext = SlackContext(scfg)

    // Init zamger
    val zjson = json.getJSONObject("zamger")
    //val zamgerContext = ZamgerContext(zjson.getString("username"), zjson.getString("password"))


    val plugins : List<HaberPlugin> = loadPlugins(json.toString())

    val threads = ArrayList<Thread>()
    val threadsLock = ReentrantLock()

    for ( plugin in plugins ) {
        val thread = object : Thread() {
            override fun run() {
                try {
                    while (true) {
                        Thread.sleep(plugin.MinTickTimer)
                        plugin.Tick()
                    }
                } catch ( ex : Exception ) {
                    log(ex)
                }
                threadsLock.lock()
                threads -= this
                threadsLock.unlock()
            }
        }
        thread.start()
        threads += thread
    }

    log("Entering main loop")
    while ( true ) {
        Thread.sleep(1000)
        if ( threads.size == 0 ) break
    }
    log("Shutting down")
}

