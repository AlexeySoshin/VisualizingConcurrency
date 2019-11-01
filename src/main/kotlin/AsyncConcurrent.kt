import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.util.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

fun main() {
    runBlocking {
/*        launch {
            do {
                println("Queue: ${getTaskQueueSize(this)}")
                delay(10)
            } while (getTaskQueueSize(this) > 0)
            println("Queue: ${getTaskQueueSize(this)}")
        }*/

        val chan = Channel<Message>(capacity = UNLIMITED)

        repeat(1_000) {
            launch(Dispatchers.Default) {
                chan.send(Start(it, Thread.currentThread().name))
                sendData()
                chan.send(End(it, Thread.currentThread().name))
            }
        }

        launch {
            for (c in chan) {
                when(c) {
                    is Start -> println(""" "Start ${c.id} on ${c.thread}", """)
                    is End -> println(""" "End ${c.id} on ${c.thread}", """)
                }
            }
        }
    }
}

sealed class Message(open val id: Int, open val thread: String)

data class Start(override val id: Int, override val thread: String) : Message(id, thread)
data class End(override val id: Int, override val thread: String) : Message(id, thread)

fun getTaskQueueSize(coroutineScope: CoroutineScope): Int {

    val contextProperty = coroutineScope::class.memberProperties.single { it.name == "context"}
    val context = contextProperty.call(coroutineScope)
    if (context != null) {
        val elementProperty = context::class.memberProperties.single { it.name == "element"}
        elementProperty.isAccessible = true
        val element = elementProperty.call(context)

        if (element != null) {
            val superclass = element::class.superclasses.first()
            val queueField = superclass.java.declaredFields.single { it.name == "_queue"}

            queueField.isAccessible = true

            val queue = queueField.get(element)

            val sizeProperty = queue::class.memberProperties.single { it.name == "size"}

            val size = sizeProperty.call(queue) as Int
            return size
        }
    }


    return -1
}

suspend fun sendDataAndAwaitAcknowledge() = coroutineScope {
    awaitAll(async {
        awaitAcknowledge()
    }, async {
        sendData()
    })
}

suspend fun sendData(): Boolean {
    repeat(Random().nextInt(1000)) {
        UUID.randomUUID()
        yield()
    }
    return true
}

suspend fun awaitAcknowledge(): Boolean {
    delay(500)
    return false
}