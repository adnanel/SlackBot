import org.json.JSONObject
import slackapi.SlackContext
import slackapi.types.MessageListener

// config is passed by string and not by JSONObject because it allows the plugin writer
// to use another library (or even version) to use for JSON parsing
// Otherwise we'd all have to use the EXACT same library+version
public abstract class HaberPlugin(configJson : String) : MessageListener {
    abstract val MinTickTimer : Long
    abstract fun Tick()
}

