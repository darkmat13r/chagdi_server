////////////////////////// 	class for debug		///////////// 
package engine

import com.smartfoxserver.v2.extensions.SFSExtension
import com.smartfoxserver.v2.extensions.ExtensionLogLevel

class LogOutput : SFSExtension() {
    init {
        inst = this
    }

    fun outputString(output: String?) {
        println(output)
    }

    override fun init() {
        // TODO Auto-generated method stub
    }

    companion object {
        private var inst: LogOutput? = null
        fun instance(): LogOutput? {
            if (inst == null) inst = LogOutput()
            return inst
        }

        fun traceLog(output: String?) {
            //instance().outputString(output);
            instance()?.trace(ExtensionLogLevel.WARN,output ?: "Empty log output")
        }
    }
}