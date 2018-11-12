
import io.reactivex.Flowable
import io.reactivex.Observable
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*


class ClipboardTable {

    private val terminateKeywords = Regex("[0-9]+ of [0-9]+ transactions")

    private val source = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor).toString().split("\n")
            .asSequence().takeWhile { !terminateKeywords.containsMatchIn(it) }.map { it.split("\t") }.toList()

    val headers = source[0]
    val data = source.drop(1)

    private val headerIndices = HashMap<String, Int?>()

    fun indexForHeader(headerRegex: String) =
            headerIndices[headerRegex] ?:
                    headerRegex.let(::Regex)
                            .let { re -> headers.find { re.containsMatchIn(it.toUpperCase()) }?.let { headers.indexOf(it) }.apply { headerIndices.put(re.toString(), this) } }

    fun <T> toSequence(convert: (ConvertWrapper) -> T) = data.asSequence()
            .map { convert(ConvertWrapper(this, it)) }

    override fun toString() = source.toString()
}

class ConvertWrapper(val clipboardTable: ClipboardTable, val data: List<String>) {

    fun getString(header: String) = clipboardTable.indexForHeader(header)?.let { data[it] }

    fun getInt(header: String) = getString(header)?.toInt()

    fun getBigDecimal(header: String) = getString(header)?.toCurrency()

    fun getLocalDate(header: String): LocalDate? = getString(header)?.let {
        if (it.trim() == "")
            ConvertWrapper(clipboardTable, clipboardTable.data[clipboardTable.data.indexOf(data) - 1]).getLocalDate(header)
        else
            it.toLocalDate()
    }
}

val ConvertWrapper.transactionDate: LocalDate get() = getLocalDate("(EFF(ECTIVE)? )?DA?TE?")!!
val ConvertWrapper.description: String? get() = getString("DESC(RIPTION)?")
val ConvertWrapper.amount: BigDecimal get() = getBigDecimal("AM(OUN)?T")!!
val ConvertWrapper.balance: BigDecimal? get() = getBigDecimal("BALANCE")


fun <T,R> Observable<T>.mapNonNulls(mapper: (T) -> R?) = map { Optional.ofNullable(mapper(it)) }.filter { it.isPresent }.map { it.get() }
fun <T,R> Flowable<T>.mapNonNulls(mapper: (T) -> R?) = map { Optional.ofNullable(mapper(it)) }.filter { it.isPresent }.map { it.get() }


fun ResultSet.getLocalDate(colName: String) = LocalDate.parse(getString(colName) ?: "2099-12-31")
fun ResultSet.getLocalDate(colIndex: Int) = LocalDate.parse(getString(colIndex) ?: "2099-12-31")

fun LocalDate.getWeekNum() =  this.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())

fun String.toCurrency(): BigDecimal {
    var newVal = replace(Regex("[$,]"),"")

    val isNegative = newVal.contains(Regex("[-–(]"))

    newVal = newVal.replace(Regex("[-–()]"),"")

    return try { if (isNegative) BigDecimal.ZERO -  BigDecimal(newVal) else BigDecimal(newVal) } catch (e: Exception) { throw Exception("Can't figure out $newVal") }
}

private val format1 = Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}")
private val format2 = Regex("[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}")
private val format3 = Regex("[0-9]{2}-[0-9]{2}-[0-9]{4}")
private val format4 = Regex("[0-9]{1,2}-[0-9]{1,2}-[0-9]{4}")
private val format5 = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
private val format6 = Regex("[A-Za-z]{3} [0-9]{1,2},? [0-9]{4}")

fun String.toLocalDate() = when {
    format1.matches(this) -> LocalDate.parse(this, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
    format2.matches(this) -> LocalDate.parse(this, DateTimeFormatter.ofPattern("M/d/yyyy"))
    format3.matches(this) -> LocalDate.parse(this, DateTimeFormatter.ofPattern("MM-dd-yyyy"))
    format4.matches(this) -> LocalDate.parse(this, DateTimeFormatter.ofPattern("M-d-yyyy"))
    format5.matches(this) -> LocalDate.parse(this, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    format6.matches(this) -> LocalDate.parse(this, DateTimeFormatter.ofPattern("MMM d, yyyy"))
    else -> throw Exception("Unknown LocalDate format $this")
}

fun BorderPane.header(text: String) = label(text) {
    top = this
    textFill = Color.RED
    style {
        fontWeight = FontWeight.BOLD
    }
}

fun <T> Observable<T>.flatCollect() = toList().flatMapObservable { Observable.fromIterable(it) }
fun <T> Flowable<T>.flatCollect() = toList().flatMapPublisher { Flowable.fromIterable(it) }


fun String.discretizeWords() =  split(Regex("\\s")).asSequence()
        .map { it.replace(Regex("[^A-Za-z]"),"").toLowerCase().trim() }
        .filter { it.isNotEmpty() && it !in stopWords }
        .distinct()
        .toSet()


val stopWords = """
a,
about,
above,
after,
again,
against,
all,
am,
an,
and,
any,
are,
aren't,
as,
at,
be,
because,
been,
before,
being,
below,
between,
both,
but,
by,
can't,
cannot,
could,
couldn't,
did,
didn't,
do,
does,
doesn't,
doing,
don't,
down,
during,
each,
few,
for,
from,
further,
had,
hadn't,
has,
hasn't,
have,
haven't,
having,
he,
he'd,
he'll,
he's,
her,
here,
here's,
hers,
herself,
him,
himself,
his,
how,
how's,
i,
i'd,
i'll,
i'm,
i've,
if,
in,
into,
is,
isn't,
it,
it's,
its,
itself,
let's,
me,
more,
most,
mustn't,
my,
myself,
no,
nor,
not,
of,
off,
on,
once,
only,
or,
other,
ought,
our,
ours,
ourselves,
out,
over,
own,
same,
shan't,
she,
she'd,
she'll,
she's,
should,
shouldn't,
so,
some,
such,
than,
that,
that's,
the,
their,
theirs,
them,
themselves,
then,
there,
there's,
these,
they,
they'd,
they'll,
they're,
they've,
this,
those,
through,
to,
too,
under,
until,
up,
very,
was,
wasn't,
we,
we'd,
we'll,
we're,
we've,
were,
weren't,
what,
what's,
when,
when's,
where,
where's,
which,
while,
who,
who's,
whom,
why,
why's,
with,
won't,
would,
wouldn't,
you,
you'd,
you'll,
you're,
you've,
your,
yours,
yourself,
yourselves,
checking,
withdrawal,
tx,
frisco,
dallas,
plano,
allen
""".split(",").asSequence()
        .map { it.replace(Regex("[^A-Za-z]"),"") }
        .toSet()