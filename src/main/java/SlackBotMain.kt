
import org.json.JSONArray
import org.json.JSONObject
import slackapi.SlackConfig
import slackapi.SlackContext
import slackbot.plugins.SlackBotPlugin
import utility.log
import java.io.File
import java.io.FileReader
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.collections.ArrayList

class ProxyClass {}

fun loadPlugins(appConfig : String) : List<SlackBotPlugin> {
    val res = ArrayList<SlackBotPlugin>()

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

        val urlCl = URLClassLoader(arrayOf(f.toURL()), ProxyClass().javaClass.classLoader)
        val pluginClass = urlCl.loadClass(obj.getString("class"))

        val pObj = pluginClass.constructors[0].newInstance() as SlackBotPlugin
        pObj.Initialize(appConfig)
        res += pObj
    }

    return res
}


fun main(args : Array<String>) {
    val json = JSONObject(FileReader(File("config.json")).readText())

    // Init slack
    val sjson = json.getJSONObject("slack")
    val scfg = SlackConfig(sjson.getString("token"), sjson.getString("username"))
    val slackContext = SlackContext(scfg)

    // Init zamger
    val zjson = json.getJSONObject("zamger")
    //val zamgerContext = ZamgerContext(zjson.getString("username"), zjson.getString("password"))

    val mainLogger = object : Consumer<String> {
        val lock = ReentrantLock()
        override fun accept(t: String) {
            lock.lock()
            log(t)
            lock.unlock()
        }
    }

    val plugins : List<SlackBotPlugin> = loadPlugins(json.toString())

    val threads = ArrayList<Thread>()
    val threadsLock = ReentrantLock()

    for ( plugin in plugins ) {
        val thread = object : Thread() {
            override fun run() {
                try {
                    while (true) {
                        Thread.sleep(plugin.MinTickTimer)
                        plugin.Tick(mainLogger)
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


    plugins.forEach {
        slackContext.RTMApi?.AddMessageListener(it)
    }

    log("Entering main loop")
    while ( true ) {
        Thread.sleep(1000)
        if ( threads.size == 0 ) break
    }
    log("Shutting down")
}

