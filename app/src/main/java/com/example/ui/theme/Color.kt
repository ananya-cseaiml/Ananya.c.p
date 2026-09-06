package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Geometric Balance Core Palette
val CanvasBg = Color(0xFFF5F7FA)       // bg-[#F5F7FA] - Clean light slate canvas
val HeaderDark = Color(0xFF0F172A)     // bg-[#0F172A] - Deep midnight slate command header
val WhiteSurface = Color(0xFFFFFFFF)   // Pure white card surfaces
val SurfaceVariant = Color(0xFFF1F5F9) // Slate 100 subtle container/track
val BorderSlate = Color(0xFFE2E8F0)    // border-slate-200
val BorderSlateDark = Color(0xFFCBD5E1)// border-slate-300

// Geometric Balance Navigation & Brand Accents
val BrandBlue = Color(0xFF2563EB)      // Blue 600
val BrandBlueLight = Color(0xFF3B82F6) // Blue 500
val BlueContainer = Color(0xFFDBEAFE)  // Blue 100
val BlueDark = Color(0xFF1D4ED8)       // Blue 700
val BlueAccent = BrandBlue             // Blue accent alias

// Aliases mapped for screens (so all existing screens inherit Geometric Balance)
val NavyDark = CanvasBg                // Background canvas is now Clean Light Slate #F5F7FA
val NavySurface = WhiteSurface         // Card containers are crisp pure white #FFFFFF
val NavyCard = Color(0xFFFFFFFF)       // Cards are pure white
val NavyBorder = BorderSlate           // Card borders are subtle slate-200 #E2E8F0

val CyanAccent = BrandBlue             // Electric primary blue #2563EB
val SkyRadar = BrandBlueLight          // #3B82F6
val BlueDeep = HeaderDark              // #0F172A

// Status & Risk Level Tints (Geometric Balance compliant)
val SafeGreen = Color(0xFF10B981)      // Emerald 500
val SafeGreenDark = Color(0xFF059669)  // Emerald 600
val SafeGreenBg = Color(0xFFD1FAE5)    // Emerald 100 soft pill container
val SafeGreenBorder = Color(0xFFA7F3D0)

val WatchAmber = Color(0xFFEAB308)     // Yellow/Amber 500
val WatchAmberDark = Color(0xFFD97706) // Amber 600
val WatchAmberBg = Color(0xFFFEF3C7)   // Amber 100 soft pill container
val WatchAmberBorder = Color(0xFFFDE68A)

val HighOrange = Color(0xFFEA580C)     // Orange 600
val HighOrangeDark = Color(0xFFC2410C) // Orange 700
val HighOrangeBg = Color(0xFFFFEDD5)   // Orange 100 soft pill container
val HighOrangeBorder = Color(0xFFFED7AA)

val SevereRed = Color(0xFFDC2626)      // Red 600
val SevereRedDark = Color(0xFFB91C1C)  // Red 700
val SevereRedBg = Color(0xFFFEE2E2)    // Red 100 soft pill container
val SevereRedBorder = Color(0xFFFECACA)

// High-contrast slate typography
val TextPrimary = Color(0xFF0F172A)    // Slate 900 primary text
val TextSecondary = Color(0xFF64748B)  // Slate 500 secondary text
val TextMuted = Color(0xFF94A3B8)      // Slate 400 muted text
val TextOnDark = Color(0xFFFFFFFF)     // Pure white for dark header / buttons

val DrainageCyan = Color(0xFF0284C7)   // Sky 600
val WaterBodyBlue = Color(0xFF2563EB)  // Blue 600
