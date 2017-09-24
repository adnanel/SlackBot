package utility

import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import java.io.PrintWriter

fun log(msg : String) {
    System.out.println(msg);
}

fun log(ex : Exception) {
    val s = StringBuilder()
    ex.printStackTrace(PrintStream(object : OutputStream() {
        override fun write( b : Int) {
            s.append(b.toChar())
        }
    }))
    log(s.toString())
}