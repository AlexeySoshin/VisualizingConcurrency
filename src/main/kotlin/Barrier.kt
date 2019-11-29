import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

fun main() {
    runBlocking {

        repeat(10) {
            getProfile()
        }
    }
}

suspend fun CoroutineScope.getProfile() {
    val id = ""

    println("getProfile start ${System.nanoTime()}")
    val millis = measureTimeMillis {
        val picture = getPictureAsync(id)
        val orders = getOrdersAsync(id)
        val shifts = getShiftsAsync(id)

        val profile = Profile(
            picture.await(),
            orders.await(),
            shifts.await()
        )
    }
    println("getProfile end ${System.nanoTime()}")

  //  println("Took ${millis}ms")
}

fun CoroutineScope.getOrdersAsync(id: String) = async {
    println("getOrders start ${System.nanoTime()}")
    val orders = getOrders(id) // 200ms

    println("getOrders end ${System.nanoTime()}")

    orders
}

fun CoroutineScope.getShiftsAsync(id: String) = async {
    println("getShift start ${System.nanoTime()}")
    val shifts = getShifts(id)

    println("getShift end ${System.nanoTime()}")
    shifts
}

fun CoroutineScope.getPictureAsync(id: String) = async {
    println("getPicture start ${System.nanoTime()}")
    val picture = getPicture(id) // 100ms

    println("getPictureEnd end ${System.nanoTime()}")
    picture
}

private suspend fun getPicture(id: String) : ByteArray {
    delay(100)
    return byteArrayOf()
}

private suspend fun getOrders(id: String) : List<Order> {
    delay(200)
    return listOf()
}

private suspend fun getShifts(id: String) : List<Shift> {
    delay(300)
    return listOf()
}

data class Order(val id: String)

data class Shift(val id: String)

data class Profile(
    val picture: ByteArray,
    val orders: List<Order>,
    val shifts: List<Shift>
)
