package render

import com.example.data.CurrencyRepository
import com.example.ui.MainViewModel
import com.example.update.AppUpdater

/**
 * Drives the real MainViewModel (on the stub repository) through the calculator and the history:
 * gradle -q vmCheck. Android's Context comes from android-all, unused beyond being non-null.
 */
fun main() {
    val context = android.content.ContextWrapper(null)
    val vm = MainViewModel(CurrencyRepository(context), AppUpdater(context), com.example.premium.FakeMonetization(com.example.premium.PremiumState(purchased = true)))
    var failures = 0
    fun check(what: String, ok: Boolean) {
        println("${if (ok) "ok  " else "FAIL"} $what")
        if (!ok) failures++
    }
    fun keys(vararg k: String) = k.forEach(vm::onKeypadPress)
    fun s() = vm.state.value

    // The stub starts at amount "1": type 250, then C saves it to the history.
    keys("C", "2", "5", "0")
    check("typed 250", s().sourceAmountRaw == "250")
    keys("C")
    check("C keeps 250 in the history", s().history.map { it.amount } == listOf("250") && s().history[0].expression == null)
    check("zero after C", s().sourceAmountRaw == "0")

    keys("7")

    // Calculator: opens from the amount, a digit starts over, operators continue.
    vm.toggleCalculator()
    check("calculator opens on the amount", s().expression == "7" && !s().calculating)
    keys("+", "1", "5", "%")
    check("7+15% live", s().expression == "7+15%" && s().calculating && s().sourceValue == 8.05 && s().sourceAmountRaw == "8.05")
    keys("=")
    check("= gives the result", s().expression == "8.05" && s().lastExpression == "7+15%" && s().sourceAmountRaw == "8.05" && !s().calculating)
    check("= records the calculation", s().history.first().let { it.amount == "8.05" && it.expression == "7+15%" })
    keys("4")
    check("digit after = starts over", s().expression == "4" && s().lastExpression == null)
    keys("*", "3", "C")
    check("C during a calculation records it", s().history.first().let { it.expression == "4*3" && it.amount == "12" } && s().expression == "0")
    keys("1", "0", "/", "0")
    check("10/0 has no value", s().sourceValue == null && s().calculating)
    keys("⌫", "4")
    check("10/4 = 2.5", s().sourceValue == 2.5)
    keys("+", "=")
    check("= on 10/4+ keeps 10/4", s().lastExpression == "10/4" && s().history.first().expression == "10/4" && s().expression == "2.5")
    keys("+", "=")
    check("= on 2.5+ is just 2.5", s().lastExpression == null && s().expression == "2.5" && s().history.first().expression == "10/4")
    keys("/", "4")
    vm.toggleCalculator()
    check("closing works out what is left", s().expression == null && s().sourceAmountRaw == "0.625" && s().history.first().expression == "2.5/4")

    // History: restore an older entry with its currencies.
    val sizeBefore = s().history.size
    vm.swapCurrencies()
    val entry = s().history.last()
    vm.openHistory()
    vm.restoreHistory(entry)
    check("restore brings back amount and pair", s().sourceAmountRaw == entry.amount && s().sourceCurrency == entry.source && s().targetCurrency == entry.target && !s().showHistory)
    check("restore does not duplicate", s().history.size == sizeBefore)
    vm.onAppHidden()
    check("leaving right after restore adds nothing", s().history.size == sizeBefore)
    keys("5")
    vm.onAppHidden()
    check("leaving after typing records", s().history.first().amount == "5" && s().history.size == sizeBefore + 1)
    vm.clearHistory()
    check("clear", s().history.isEmpty())

    // Premium locked (Play build after the trial): the gates open the sheet, an unlock finishes the action.
    val premium = com.example.premium.FakeMonetization(com.example.premium.PremiumState())
    val locked = MainViewModel(CurrencyRepository(context), AppUpdater(context), premium)
    fun l() = locked.state.value
    if (l().expression != null) locked.toggleCalculator()
    locked.toggleCalculator()
    check("locked calculator opens Premium", l().expression == null && locked.premiumSheet.value == com.example.premium.PremiumReason.CALCULATOR)
    premium.set(com.example.premium.PremiumRules.afterVideo(com.example.premium.PremiumRules.afterVideo(com.example.premium.PremiumState(), System.currentTimeMillis()), System.currentTimeMillis()))
    check("unlocking opens the calculator and closes the sheet", l().expression != null && locked.premiumSheet.value == null)
    premium.set(com.example.premium.PremiumState())
    check("locking again closes the calculator", l().expression == null)
    locked.setColorTheme("ocean")
    check("locked colours open Premium, theme kept", locked.premiumSheet.value == com.example.premium.PremiumReason.COLORS && locked.effectiveColorTheme.value == "sage")
    locked.closePremium()
    val before = premium.conversions
    locked.onKeypadPress("C"); locked.onKeypadPress("7"); locked.onKeypadPress("5")
    check("typing a new amount counts one conversion", premium.conversions == before + 1)

    println(if (failures == 0) "all checks passed" else "$failures checks failed")
    if (failures > 0) kotlin.system.exitProcess(1)
}
