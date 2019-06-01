
import io.ktor.application.install
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

fun main(vararg args: String) {

    Server.start()

}
const val producers = 6
const val refreshRate = 500L
object Server {
    fun start() {

        val channel = Channel<String>(Channel.UNLIMITED)

        val server = embeddedServer(Netty, port = 8080) {
            install(WebSockets)
            routing {
                static("/") {
                    resource("js/main.js")
                    resource("css/main.css")
                    defaultResource("index.html")
                }
                webSocket("/ws") {
                    println("Incoming connection")
                    val producer = producer()
                    val collector = collector(channel)
                    val processors = List(producers) {
                        consumer(it, producer, channel, collector)
                    }
                    for (m in channel) {
                        send(Frame.Text(m))
                        delay(refreshRate)
                    }
                }
            }
        }
        server.start()
        println("Open: http://localhost:8080")
    }
}

fun CoroutineScope.producer(): ReceiveChannel<String> = produce {
    val limit = 1000
    for (i in 1..limit) {
        send((limit - i).toString()) // produce next
        delay(refreshRate)
    }
}


val counter = AtomicLong(0)

fun CoroutineScope.consumer(
    id: Int,
    channel: ReceiveChannel<String>,
    messageBus: Channel<String>,
    collector: SendChannel<CollectorMessage>
) = launch {
    for (msg in channel) {
        messageBus.send("consumer:$id:$msg")
        println("Processor #$id received $msg")
        collector.send(CollectorMessage(id, counter.incrementAndGet().toString()))
        delay(refreshRate * Random.nextInt(2, 5))
    }
}

data class CollectorMessage(val consumerId: Int, val message: String)

fun CoroutineScope.collector(messageBus: Channel<String>) = actor<CollectorMessage>(capacity = UNLIMITED) {
    for (msg in channel) {
        println("Got $msg")

        messageBus.send("collector:${msg.consumerId}:${msg.message}")
    }
}
