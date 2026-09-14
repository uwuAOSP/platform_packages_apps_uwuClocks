package org.uwuaosp.clock

import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.END
import androidx.constraintlayout.widget.ConstraintSet.MATCH_CONSTRAINT
import androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
import androidx.constraintlayout.widget.ConstraintSet.START
import com.android.systemui.customization.clocks.view.DefaultClockFaceLayout
import com.android.systemui.plugins.keyguard.ui.clocks.AodClockBurnInModel
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPreviewConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds

class ClockFaceLayoutImpl(view: View) : DefaultClockFaceLayout(view) {
    override fun applyConstraints(constraints: ConstraintSet): ConstraintSet {
        return super.applyConstraints(constraints).apply { useFullWidthLargeClock() }
    }

    override fun applyPreviewConstraints(
        clockPreviewConfig: ClockPreviewConfig,
        constraints: ConstraintSet,
    ): ConstraintSet {
        return super.applyPreviewConstraints(clockPreviewConfig, constraints).apply {
            useFullWidthLargeClock()
        }
    }

    override fun applyAodBurnIn(aodBurnInModel: AodClockBurnInModel) {
        view.translationX = aodBurnInModel.translationX
        view.translationY = aodBurnInModel.translationY
        view.scaleX = aodBurnInModel.scale
        view.scaleY = aodBurnInModel.scale
    }

    private fun ConstraintSet.useFullWidthLargeClock() {
        if (view.id != ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE) return
        constrainWidth(ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE, MATCH_CONSTRAINT)
        connect(ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE, START, PARENT_ID, START)
        connect(ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE, END, PARENT_ID, END)
    }
}
