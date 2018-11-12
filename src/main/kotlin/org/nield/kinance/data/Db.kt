import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject

private val dbPath = System.getProperty("os.name").toLowerCase().let {
    when {
        System.getProperty("user.name") == "e98594" ->"jdbc:sqlite:F:\\data\\finance.db"
        it.indexOf("win") >= 0 -> "jdbc:sqlite:C:\\Users\\${System.getProperty("user.name")}\\Dropbox\\Data\\finance_rx.db"
        it.matches(Regex("^.*(nix|nux|aux).*$")) -> "jdbc:sqlite:/home/thomas/Dropbox/Data/finance_rx.db"
        else -> throw Exception("OS not identified!")
    }
}
val db = HikariConfig().let {
    it.jdbcUrl = dbPath
    it.maximumPoolSize = 1
    HikariDataSource(it)
}


val refreshRequests = BehaviorSubject.createDefault(Unit)

fun <T> Single<T>.switchReplaySingle() = refreshRequests.map {
    this.toObservable().replay(1).autoConnect().firstOrError()
}.replay(1).autoConnect()
.firstOrError()
.flatMap { it }