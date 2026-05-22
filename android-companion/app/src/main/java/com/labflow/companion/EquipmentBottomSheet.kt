package com.labflow.companion

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EquipmentBottomSheet(
    private val colors: CompanionColors,
    private val equipment: EquipmentDto,
    private val currentUserId: Int,
    private val onBorrow: (EquipmentDto) -> Unit,
    private val onReturn: (EquipmentDto) -> Unit,
    private val onReportFault: (EquipmentDto) -> Unit,
    private val onViewFullDetails: (EquipmentDto) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext()).apply {
            setContentView(content())
        }
    }

    private fun content(): View {
        val isBorrowedByCurrentUser = equipment.activeBorrowUserId == currentUserId
        val isUnavailable = equipment.status.equals("BORROWED", true) && !isBorrowedByCurrentUser
        val isDisabledState = equipment.status.equals("DEFECT", true) || equipment.status.equals("MAINTENANCE", true)

        val wrap = ScrollView(requireContext()).apply {
            setBackgroundColor(colors.surface)
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(12), dp(24), dp(26))
        }
        wrap.addView(root)

        root.addView(View(requireContext()).apply {
            background = rounded(colors.border, dp(3), colors.border, 0)
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(6)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(18)
            }
        })

        root.addView(TextView(requireContext()).apply {
            text = equipment.name.orEmpty().ifBlank { "Equipment item" }
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(colors.foreground)
        })
        root.addView(TextView(requireContext()).apply {
            text = listOfNotNull(
                equipment.category?.takeIf { it.isNotBlank() },
                equipment.location?.takeIf { it.isNotBlank() }
            ).joinToString(" | ").ifBlank { "No category or location" }
            textSize = 13f
            setTextColor(colors.muted)
            setPadding(0, dp(6), 0, dp(16))
        })

        root.addView(statusBadge())

        root.addView(infoLine("Serial", equipment.serialNumber.orEmpty().ifBlank { "Not available" }))
        root.addView(infoLine("Last maintenance", equipment.lastMaintenanceDate.orEmpty().ifBlank { "Not recorded" }))
        if (equipment.status.equals("BORROWED", true)) {
            root.addView(infoLine("Borrowed by", equipment.activeBorrowUsername.orEmpty().ifBlank { "Another user" }))
        }
        root.addView(infoLine("QR", equipment.qrCode.orEmpty().ifBlank { "Not generated" }))

        if (isUnavailable || isDisabledState) {
            root.addView(TextView(requireContext()).apply {
                text = when {
                    isDisabledState -> "This item is not available for mobile actions while it is in ${equipment.status.orEmpty().lowercase()} state."
                    else -> "This item is already borrowed by another user."
                }
                textSize = 12.5f
                setTextColor(colors.muted)
                setPadding(0, dp(14), 0, 0)
            })
        }

        val buttonRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(18), 0, 0)
        }
        val borrowTitle = when {
            equipment.status.equals("AVAILABLE", true) -> "Borrow"
            isBorrowedByCurrentUser -> "Return"
            else -> "Unavailable"
        }
        buttonRow.addView(actionButton(borrowTitle, colors.primary, 1f) {
            dismissAllowingStateLoss()
            if (isBorrowedByCurrentUser) onReturn(equipment) else onBorrow(equipment)
        }.apply {
            isEnabled = equipment.status.equals("AVAILABLE", true) || isBorrowedByCurrentUser
            alpha = if (isEnabled) 1f else 0.55f
        })
        buttonRow.addView(space(12, 1))
        buttonRow.addView(actionButton("Report Fault", colors.danger, 1f) {
            dismissAllowingStateLoss()
            onReportFault(equipment)
        }.apply {
            isEnabled = !isDisabledState
            alpha = if (isEnabled) 1f else 0.55f
        })
        root.addView(buttonRow)

        root.addView(TextView(requireContext()).apply {
            text = "View Full Details ->"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(colors.accent)
            setPadding(0, dp(18), 0, 0)
            setOnClickListener {
                dismissAllowingStateLoss()
                onViewFullDetails(equipment)
            }
        })

        return wrap
    }

    private fun statusBadge(): TextView {
        val status = equipment.status.orEmpty().ifBlank { "UNKNOWN" }
        val color = when (status.uppercase()) {
            "AVAILABLE" -> colors.success
            "BORROWED" -> colors.warning
            "DEFECT" -> colors.danger
            "MAINTENANCE" -> colors.primary
            else -> colors.accent
        }
        return TextView(requireContext()).apply {
            text = "● $status"
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color)
            background = rounded(Color.argb(42, Color.red(color), Color.green(color), Color.blue(color)), dp(14), color, 1)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }
    }

    private fun infoLine(label: String, value: String): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(2))
            addView(TextView(requireContext()).apply {
                text = "$label:"
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(colors.muted)
            })
            addView(space(10, 1))
            addView(TextView(requireContext()).apply {
                text = value
                textSize = 13f
                setTextColor(colors.foreground)
            })
        }
    }

    private fun actionButton(textValue: String, color: Int, weight: Float, action: () -> Unit): Button {
        return Button(requireContext()).apply {
            text = textValue
            isAllCaps = false
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(readableText(color))
            background = rounded(color, dp(14), color, 0)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
        }
    }

    private fun rounded(color: Int, radius: Int, strokeColor: Int, strokeWidth: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            if (strokeWidth > 0) {
                setStroke(dp(strokeWidth), strokeColor)
            }
        }
    }

    private fun readableText(background: Int): Int {
        val darkness = 1 - (0.299 * Color.red(background) + 0.587 * Color.green(background) + 0.114 * Color.blue(background)) / 255
        return if (darkness >= 0.42) Color.WHITE else colors.foreground
    }

    private fun space(width: Int, height: Int): View {
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(width), dp(height))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
