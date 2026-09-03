package moe.evil.hyperstatus.xposed.hooks.mobilesignal

import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import moe.evil.hyperstatus.xposed.model.DataActivity
import moe.evil.hyperstatus.xposed.model.MobileSignalOverride
import moe.evil.hyperstatus.xposed.model.Rat

private val RAT_CHOICES = Rat.entries.associateBy { it.cli } + mapOf(
    "gprs" to Rat.G,
    "edge" to Rat.E,
    "umts" to Rat.THREE_G,
    "hspa" to Rat.H,
    "hspap" to Rat.H_PLUS,
    "hplus" to Rat.H_PLUS,
    "3gplus" to Rat.THREE_G_PLUS,
    "4gplus" to Rat.FOUR_G_PLUS,
    "4.5gplus" to Rat.FOUR_DOT_FIVE_G_PLUS,
    "lteplus" to Rat.LTE_PLUS,
    "nr" to Rat.NR,
    "nrsa" to Rat.NR_SA,
    "5gplus" to Rat.FIVE_G_PLUS,
    "5gplusplus" to Rat.FIVE_G_PLUS_PLUS,
)

class SignalCommand(private val state: MobileSignalDebugState) :
    CoreCliktCommand(name = "signal") {
    private val card by option("--card", "--slot").int().required()
    private val reset by option("--reset").flag()
    private val strength by option("--signal-strength", "--level").int()
    private val rat by option("--signal-type", "--type").choice(RAT_CHOICES, ignoreCase = true)
    private val activity by option("--activity")
        .choice(DataActivity.entries.associateBy { it.cli }, ignoreCase = true)
    private val roaming by option().switch("--roaming" to true, "--no-roaming" to false)
    private val inService by option().switch("--service" to true, "--no-service" to false)
    private val softSim by option().switch("--soft-sim" to true, "--no-soft-sim" to false)
    private val ims by option().switch("--ims" to true, "--no-ims" to false)
    private val inet by option().switch("--internet" to 1, "--no-internet" to 0)

    override fun run() {
        if (reset) {
            state.clear(card)
            return
        }
        state.update(card) {
            it.overlay(
                MobileSignalOverride(
                    level = strength,
                    rat = rat,
                    activity = activity,
                    roaming = roaming,
                    inService = inService,
                    softSim = softSim,
                    ims = ims,
                    inetCondition = inet,
                ),
            )
        }
    }
}

class ResetCommand(private val state: MobileSignalDebugState) :
    CoreCliktCommand(name = "reset") {
    override fun run() = state.clearAll()
}
