package utility

import org.json.JSONObject


fun <T> JSONObject.getSafely(key : String, defVal : T) : T {
    if ( this.has(key) ) {
        return this.get(key) as T
    }
    //log("JSONObject doesn't contain key " + key + ", using defVal. JSON Object : " + this.toString())
    return defVal
}
