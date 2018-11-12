import javafx.application.Application
import org.nield.kinance.ux.MainView
import tornadofx.*


fun main(args: Array<String>) = Application.launch(AppLauncher::class.java, *args)
class AppLauncher: App(MainView::class)