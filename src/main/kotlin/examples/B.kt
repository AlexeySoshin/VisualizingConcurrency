package examples

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        repeat(100_000) {
            launch(Dispatchers.Default) {
                println("$it, ${Thread.currentThread().name}")
            }
        }
    }
}
