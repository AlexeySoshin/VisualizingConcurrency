
import io.ktor.application.install
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.content.*
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.net.URL
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

fun main() {

    Server.start()

}
const val producers = 8
const val refreshRate = 100L
object Server {
    fun start() {

        val channel = Channel<String>(UNLIMITED)

        val server = embeddedServer(Netty, port = 8080) {
            install(WebSockets)
            routing {
                static("/") {
                    resource("js/main.js")
                    resource("css/main.css")
                    resource("threads.html")
                    resource("multireactor.html")
                    resources("images")
                    defaultResource("index.html")
                }
                webSocket("/ws") {
                    println("Incoming connection")
                    val producer = producer(1, channel)
                    val collector = collector(channel)
                    val processors = List(producers) {
                        consumer(it, producer, channel, collector)
                    }
                    for (m in channel) {
                        send(Frame.Text(m))
                    }
                }
            }
        }
        server.start()
        println("Open: http://localhost:8080")
    }
}


@ExperimentalCoroutinesApi
class Scrapper : CoroutineScope {
    override val coroutineContext = Dispatchers.Default

    fun scrap(url: URL): ReceiveChannel<String> = produce {
        val html = url.fetchAsHtml()
        val links = parseLinks(html)

        for (link in links) {
            send(link)
        }
    }

    private fun download(processors: Int, url: URL) {
        val links = scrap(url)

        List(processors) {
            Downloader(links).start()
        }
    }

    private fun parseLinks(html: String): List<String> {
        saver()
        return listOf()
    }


}

class Downloader(private val links: ReceiveChannel<String>) : CoroutineScope {
    override val coroutineContext = Dispatchers.Default

    fun start() = launch {
        for (link in links) {
            val bytes: ByteArray = link.download()
            //TODO do something with those bytes
        }
    }
}

private fun String.download(): ByteArray {
    return "".toByteArray()
}
data class SaveFileMessage(val name: String, val content: ByteArray)
fun CoroutineScope.saver() = actor<SaveFileMessage> {
    for (msg in channel) {
        saveToDisk(msg.name, msg.content)
    }
}

fun saveToDisk(name: Any, content: Any) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}


private fun URL.fetchAsHtml(): String {
    return this.toString() // does nothing interesting
}


fun CoroutineScope.producer(id: Int,
                            messageBus: Channel<String>): ReceiveChannel<String> = produce(capacity = UNLIMITED) {
    val limit = 1000
    for (i in 1..limit) {
        messageBus.send("producer:$id:${limit - i}")
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
    }
}

data class CollectorMessage(val consumerId: Int, val message: String)

fun CoroutineScope.collector(messageBus: Channel<String>) = actor<CollectorMessage>(capacity = UNLIMITED) {
    for (msg in channel) {
        println("Got $msg")

        messageBus.send("collector:${msg.consumerId}:${msg.message}")
        delay(refreshRate * Random.nextInt(15, 20))
    }
}
